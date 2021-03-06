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
package org.sing_group.seda.core.rename;

import static java.util.Arrays.asList;
import static java.util.regex.Pattern.quote;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class IntervalReplaceRenamer extends WordReplaceRenamer {
  @XmlElement
  private String from;
  @XmlElement
  private String to;

  public IntervalReplaceRenamer() {}

  public IntervalReplaceRenamer(HeaderTarget target, String from, String to, String replacement) {
    super(target, replacement, true, asList(new String(quote(from) + ".*" + quote(to))));
    this.from = from;
    this.to = to;
  }

  public String getFrom() {
    return from;
  }

  public String getTo() {
    return to;
  }
}
