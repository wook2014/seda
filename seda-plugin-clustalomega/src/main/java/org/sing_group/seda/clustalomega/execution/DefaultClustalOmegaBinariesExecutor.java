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
package org.sing_group.seda.clustalomega.execution;

import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.sing_group.seda.util.OsUtils;

@XmlRootElement
public class DefaultClustalOmegaBinariesExecutor extends AbstractClustalOmegaBinariesExecutor {
  @XmlElement
  private final File clustalOmegaExecutable;

  public DefaultClustalOmegaBinariesExecutor() {
    this(new File(getClustalOmegaBinaryFileName()));
  }
  public DefaultClustalOmegaBinariesExecutor(File clustalOmegaExecutable) {
    this.clustalOmegaExecutable = clustalOmegaExecutable;
  }

  public void executeAlignment(
    File inputFile, File outputFile, int numThreads, String additionalParameters
  ) throws IOException, InterruptedException {
    executeAlignment(
      asList(this.clustalOmegaExecutable.getPath()), inputFile, outputFile, numThreads, additionalParameters
    );
  }

  @Override
  protected String getClustalOmegaCommand() {
    return this.clustalOmegaExecutable.getPath();
  }

  @Override
  protected String toFilePath(File file) {
    return file.getAbsolutePath();
  }

  public static String getClustalOmegaBinaryFileName() {
    if (OsUtils.isWindows()) {
      return "clustalo.exe";
    } else {
      return "clustalo";
    }
  }

  public File getClustalOmegaExecutable() {
    return clustalOmegaExecutable;
  }
}
