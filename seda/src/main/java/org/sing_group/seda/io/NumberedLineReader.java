/*
 * #%L
 * SEquence DAtaset builder
 * %%
 * Copyright (C) 2017 - 2020 Jorge Vieira, Cristina Vieira, Noé Vázquez, Miguel Reboiro-Jato and Hugo López-Fernández
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package org.sing_group.seda.io;

import static java.nio.file.Files.newInputStream;
import static java.nio.file.StandardOpenOption.READ;
import static java.util.Arrays.asList;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class NumberedLineReader implements AutoCloseable {
  private final static Set<String> SUPPORTED_CHARSETS = new HashSet<>(asList(
    "ISO-8859-1", "ISO-8859-13", "ISO-8859-15", "ISO-8859-2", "ISO-8859-3",
    "ISO-8859-4", "ISO-8859-5", "ISO-8859-6", "ISO-8859-7", "ISO-8859-8",
    "ISO-8859-9",
    "US-ASCII",
    "UTF-16", "UTF-16BE", "UTF-16LE", "UTF-32", "UTF-32BE", "UTF-32LE",
    "UTF-8",
    "windows-1250", "windows-1251", "windows-1252", "windows-1253",
    "windows-1254", "windows-1255", "windows-1256", "windows-1257",
    "windows-1258"
  ));
  
  private final Charset charset;
  private final Reader input;
  private final LineReader reader;

  private static interface LineReader {
    public Line readLine() throws IOException;
  }

  public NumberedLineReader(Path file, Charset charset) throws IOException {
    this(newInputStream(file, READ), charset);
  }
  
  public static void main(String[] args) {
    Charset.availableCharsets().entrySet().stream()
      .forEach(entry -> System.out.println(entry.getKey()));
  }
  
  public NumberedLineReader(InputStream input, Charset charset) throws IOException {
    if (charset == null) {
      this.charset = null;
      this.input = new InputStreamReader(input);
      this.reader = new SingleByteByCharLineReader(this.input);
    } else if (isSupported(charset)) {
      if (charset.newEncoder().maxBytesPerChar() == 1f) {
        this.charset = charset;
        this.input = new InputStreamReader(input, charset);
        this.reader = new SingleByteByCharLineReader(this.input);
      } else if (isUtf(charset)) {
        final InputStream bufferedInput = new BufferedInputStream(input);
        final BomInformation bomInformation = getBomLength(charset, bufferedInput);
        this.charset = bomInformation.getCharset();
        
        // bomInformation's charset must not be used for the InputStreamReader
        this.input = new InputStreamReader(bufferedInput, charset);
        this.reader = new MultipleByteByCharLineReader(
          this.input, bomInformation.getCharset(), bomInformation.getBomLength()
        );
      } else {
        this.charset = charset;
        this.input = new InputStreamReader(input, charset);
        this.reader = new MultipleByteByCharLineReader(this.input, charset);
      }
    } else {
      throw new IllegalArgumentException("Unsupported charset: " + charset);
    }
  }
  
  public Charset getCharset() {
    return this.charset;
  }
  
  public Line readLine() throws IOException {
    return this.reader.readLine();
  }

  private static boolean isSupported(Charset charset) {
    return SUPPORTED_CHARSETS.contains(charset.displayName());
  }
  
  private static boolean isUtf(Charset charset) {
    return charset.displayName().startsWith("UTF-");
  }

  private static class BomInformation {
    private final Charset charset;
    private final int bomLength;

    public BomInformation(Charset charset, int bomLength) {
      this.charset = charset;
      this.bomLength = bomLength;
    }
    
    public Charset getCharset() {
      return charset;
    }

    public int getBomLength() {
      return bomLength;
    }
  }
  
  private static BomInformation getBomLength(Charset charset, InputStream input) throws IOException {
    input.mark(4);
    
    int bomLength = 0;
    switch(charset.displayName()) {
      case "UTF-8":
        if (input.read() == 0xEF && input.read() == 0xBB && input.read() == 0xBF) {
          bomLength = 3;
        }
        break;
      case "UTF-16BE":
      case "UTF-16LE":
        bomLength = 2;
        break;
      case "UTF-16": {
        int first = input.read();
        int second = input.read();
        
        if (first == 0xFF && second == 0xFE) {
          charset = Charset.forName("UTF-16LE");
          bomLength = 2;
        } else if (first == 0xFE && second == 0xFF) {
          charset = Charset.forName("UTF-16BE");
          bomLength = 2;
        }
        
        break;
      }
      case "UTF-32BE":
      case "UTF-32LE":
        bomLength = 4;
        break;
      case "UTF-32": {
        int first = input.read();
        int second = input.read();
        int third = input.read();
        int fourth = input.read();
        if (first == 0xFF && second == 0xFE && third == 0x00 && fourth == 0x00) {
          charset = Charset.forName("UTF-32LE");
          bomLength = 4;
        } else if (first == 0x00 && second == 0x00 && third == 0xFE && fourth == 0xFF) {
          charset = Charset.forName("UTF-32BE");
          bomLength = 4;
        }
        break;
      }
      default:
        throw new IllegalArgumentException("Invalid charset");
    }
    
    input.reset();
    
    return new BomInformation(charset, bomLength);
  }
  
  @Override
  public void close() {
    try {
      this.input.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  private static class SingleByteByCharLineReader implements LineReader {
    private final Reader input;
    
    private long location;
    
    
    public SingleByteByCharLineReader(Reader input) throws IOException {
      this.input = input;
      
      this.location = 0;
    }
    
    private int readChar() throws IOException {
      int read = this.input.read();
      
      if (read != -1) {
        this.location++;
      }
      
      return read;
    }
    
    @Override
    public Line readLine() throws IOException {
      final StringBuilder sb = new StringBuilder();
      long start = -1;
      long endTextPosition = start;
      
      String endLine = null;
      
      boolean lfRead = false;
      boolean completed = false;
      
      int read;
      do {
        read = this.readChar();
        
        if (read == -1) {
          completed = true;
        } else {
          if (start == -1) {
            start = this.location;
            endTextPosition = this.location;
          }
          
          final char cread = (char) read;
          switch (cread) {
            case '\r':
              if (lfRead) {
                sb.append('\r');
              }
              lfRead = true;
              break;
            case '\n':
              endLine = lfRead ? "\r\n" : "\n";
              lfRead = false;
              completed = true;
              break;
            default:
              if (lfRead) {
                sb.append('\r');
                lfRead = false;
              }
              sb.append(cread);
              endTextPosition = this.location;
          }
        }
      } while (!completed);

      if (read == -1 && sb.length() == 0) {
        return null;
      } else {
        return new SingleByteByCharLine(start, endTextPosition, this.location, sb.toString(), endLine);
      }
    }
  }
  
  private class MultipleByteByCharLineReader implements LineReader {
    private final Reader input;
    private final CharsetEncoder encoder;
    
    private long charStart;
    private long charEnd;
    private long nextChar;
    private int charLength;
    
    public MultipleByteByCharLineReader(Reader input, Charset charset) throws IOException {
      this(input, charset, 0);
    }
    
    public MultipleByteByCharLineReader(Reader input, Charset charset, int initialOffset) throws IOException {
      if (!charset.canEncode())
        throw new IllegalArgumentException("Encode not supported for charset: " + charset.displayName());
      
      this.input = input;
      this.encoder = charset.newEncoder();
      
      this.charStart = initialOffset;
      this.charEnd = -1;
      this.nextChar = initialOffset;
      this.charLength = 0;
    }
    
    private int readChar() throws IOException {
      int read = this.input.read();
      
      if (read != -1) {
        final CharBuffer charBuffer = CharBuffer.wrap(new char[] { (char) read });
        final ByteBuffer byteBuffer = encoder.encode(charBuffer);
        
        this.charLength = byteBuffer.rewind().remaining();
        this.charStart = this.nextChar;
        this.charEnd = this.charStart + this.charLength - 1;
        this.nextChar += this.charLength;
      }
      
      return read;
    }
    
    @Override
    public Line readLine() throws IOException {
      final StringBuilder sb = new StringBuilder();
      final List<Integer> lengths = new ArrayList<>();
      long start = -1;
      long endTextPosition = start;
      
      String endLine = null;
      
      boolean lfRead = false;
      boolean completed = false;
      
      int read;
      do {
        read = this.readChar();
        
        if (read == -1) {
          completed = true;
        } else {
          if (start == -1) {
            start = this.charStart;
            endTextPosition = this.charEnd;
          }
          lengths.add(this.charLength);
          
          final char cread = (char) read;
          switch (cread) {
            case '\r':
              if (lfRead) {
                sb.append('\r');
              }
              lfRead = true;
              break;
            case '\n':
              endLine = lfRead ? "\r\n" : "\n";
              lfRead = false;
              completed = true;
              break;
            default:
              if (lfRead) {
                sb.append('\r');
                lfRead = false;
              }
              sb.append(cread);
              endTextPosition = this.charEnd;
          }
        }
      } while (!completed);

      final long endLinePosition = this.charEnd;
      if (read == -1 && sb.length() == 0) {
        return null;
      } else {
        final byte[] charLengths = new byte[lengths.size()];
        for (int i = 0; i < lengths.size(); i++) {
          charLengths[i] = lengths.get(i).byteValue();
        }
        
        return new MultiByteByCharLine(start, endTextPosition, endLinePosition, charLengths, sb.toString(), endLine);
      }
    }
  }
  
  public static interface Line {
    public long getStart();
    public long getTextEnd();
    public long getLineEnd();
    public int getTextLength();
    public int getLineLength();
    public int getCharLength(int index);
    public long getCharPosition(int index);
    public int getTextLengthTo(int index);
    public int getTextLengthFrom(int index);
    public String getLine();
    public String getLineEnding();
    public int getLineEndingLength();
  }
  
  private final static class SingleByteByCharLine implements Line {
    private final long start;
    private final long textEnd;
    private final long lineEnd;
    private final String line;
    private final String lineEnding;

    public SingleByteByCharLine(long start, long textEnd, long lineEnd, String line, String lineEnding) {
      this.start = start;
      this.textEnd = textEnd;
      this.lineEnd = lineEnd;
      this.line = line;
      this.lineEnding = lineEnding;
    }

    @Override
    public long getStart() {
      return start;
    }
    
    @Override
    public long getTextEnd() {
      return textEnd;
    }

    @Override
    public long getLineEnd() {
      return lineEnd;
    }

    @Override
    public int getTextLength() {
      return this.line.length() - this.getLineEndingLength();
    }
    
    @Override
    public int getLineLength() {
      return this.line.length();
    }
    
    @Override
    public int getCharLength(int index) {
      if (index < 0 || index >= this.getTextLength()) {
        throw new IndexOutOfBoundsException("Invalid index: " + index);
      }
      return 1;
    }
    
    @Override
    public long getCharPosition(int index) {
      if (index < 0 || index >= this.getTextLength()) {
        throw new IndexOutOfBoundsException("Invalid index: " + index);
      }
      
      return this.start + index;
    }
    
    @Override
    public int getTextLengthTo(int index) {
      if (index < 0 || index >= this.getTextLength()) {
        throw new IndexOutOfBoundsException("Invalid index: " + index);
      }
      return index + 1;
    }
    
    @Override
    public int getTextLengthFrom(int index) {
      if (index < 0 || index >= this.textEnd) {
        throw new IndexOutOfBoundsException("Invalid index: " + index);
      }
      return this.getTextLength() - index;
    }

    @Override
    public String getLine() {
      return line;
    }

    @Override
    public String getLineEnding() {
      return lineEnding;
    }
    
    @Override
    public int getLineEndingLength() {
      return this.lineEnding == null ? 0 : (int) (this.lineEnd - this.textEnd);
    }
  }
  
  private final static class MultiByteByCharLine implements Line {
    private final long start;
    private final long textEnd;
    private final long lineEnd;
    private final int textLength;
    private final int lineLength;
    private final byte[] charLengths;
    private final String line;
    private final String lineEnding;

    public MultiByteByCharLine(long start, long textEnd, long lineEnd, byte[] charLengths, String line, String lineEnding) {
      this.start = start;
      this.textEnd = textEnd;
      this.lineEnd = lineEnd;
      this.charLengths = charLengths;
      this.line = line;
      this.lineEnding = lineEnding;
      
      int textLength = 0;
      int lineLength = 0;
      
      final int lineEndingLength = lineEnding == null ? 0 : lineEnding.length();
      final int lineEndingStart = this.charLengths.length - lineEndingLength;
      for (int i = 0; i < this.charLengths.length; i++) {
        if (i < lineEndingStart)
          textLength += this.charLengths[i];
        lineLength += this.charLengths[i];
      }
      
      this.textLength = textLength;
      this.lineLength = lineLength;
    }

    @Override
    public long getStart() {
      return start;
    }
    
    @Override
    public long getTextEnd() {
      return textEnd;
    }

    @Override
    public long getLineEnd() {
      return lineEnd;
    }

    @Override
    public int getTextLength() {
      return this.textLength;
    }
    
    @Override
    public int getLineLength() {
      return this.lineLength;
    }
    
    @Override
    public int getCharLength(int index) {
      if (index < 0 || index >= this.getTextLength()) {
        throw new IndexOutOfBoundsException("Invalid index: " + index);
      }
      return this.charLengths[index];
    }
    
    @Override
    public long getCharPosition(int index) {
      if (index < 0 || index >= this.getTextLength()) {
        throw new IndexOutOfBoundsException("Invalid index: " + index);
      }
      
      return this.start + this.getTextLengthTo(index);
    }
    
    @Override
    public int getTextLengthTo(int index) {
      if (index < 0 || index >= this.getTextLength()) {
        throw new IndexOutOfBoundsException("Invalid index: " + index);
      }
      int length = 0;
      
      for (int i = 0; i < index; i++) {
        length += this.charLengths[i];
      }
      
      return length;
    }
    
    @Override
    public int getTextLengthFrom(int index) {
      if (index < 0 || index >= this.getTextLength()) {
        throw new IndexOutOfBoundsException("Invalid index: " + index);
      }
      int length = 0;
      
      for (int i = index; i < this.charLengths.length - this.lineEnding.length(); i++) {
        length += this.charLengths[i];
      }
      
      return length;
    }

    @Override
    public String getLine() {
      return line;
    }

    @Override
    public String getLineEnding() {
      return lineEnding;
    }
    
    @Override
    public int getLineEndingLength() {
      return this.lineEnding == null ? 0 : (int) (this.lineEnd - this.textEnd);
    }
  }
}
