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
package org.sing_group.seda.bio.consensus;

import org.sing_group.seda.bio.SequenceType;
import org.sing_group.seda.datatype.SequenceBuilder;

public class SequencesGroupConsensusFactory {

  public static SequencesGroupConsensus getConsensusCreator(
    SequenceType sequenceType, ConsensusBaseStrategy consensusBaseStrategy, SequenceBuilder sequenceBuilder,
    double minimumPresence, boolean verbose
  ) {
    if (consensusBaseStrategy.equals(ConsensusBaseStrategy.MOST_FREQUENT)) {
      if (sequenceType.equals(SequenceType.NUCLEOTIDE)) {
        return new NucleotideSequencesGroupMostFrequentConsensus(sequenceBuilder, minimumPresence, verbose);
      } else {
        return new ProteinSequencesGroupMostFrequentConsensus(sequenceBuilder, minimumPresence, verbose);
      }
    } else {
      if (sequenceType.equals(SequenceType.NUCLEOTIDE)) {
        return new NucleotideSequencesGroupAboveThresholdConsensus(sequenceBuilder, minimumPresence, verbose);
      } else {
        return new ProteinSequencesGroupAboveThresholdConsensus(sequenceBuilder, minimumPresence, verbose);
      }
    }
  }
}
