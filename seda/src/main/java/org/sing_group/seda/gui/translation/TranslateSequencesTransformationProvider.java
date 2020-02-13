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
package org.sing_group.seda.gui.translation;

import static org.sing_group.seda.gui.translation.TranslateSequencesTransformationChangeType.TRANSLATION_CONFIGURATION;

import java.beans.PropertyChangeEvent;

import org.sing_group.seda.datatype.DatatypeFactory;
import org.sing_group.seda.datatype.configuration.SequenceTranslationConfiguration;
import org.sing_group.seda.plugin.spi.AbstractTransformationProvider;
import org.sing_group.seda.transformation.dataset.SequencesGroupDatasetTransformation;
import org.sing_group.seda.transformation.dataset.TranslateSequencesGroupDatasetTransformation;

public class TranslateSequencesTransformationProvider extends AbstractTransformationProvider {

  private SequenceTranslationConfigurationPanel sequenceTranslationPanel;

  public TranslateSequencesTransformationProvider(SequenceTranslationConfigurationPanel sequenceTranslationPanel) {
    this.sequenceTranslationPanel = sequenceTranslationPanel;
    this.sequenceTranslationPanel.addPropertyChangeListener(this::sequenceTranslationPropertyChanged);
  }

  public void sequenceTranslationPropertyChanged(PropertyChangeEvent evt) {
    switch (evt.getPropertyName()) {
      case SequenceTranslationConfigurationPanel.PROPERTY_CODON_TABLE:
      case SequenceTranslationConfigurationPanel.PROPERTY_FRAMES:
      case SequenceTranslationConfigurationPanel.PROPERTY_JOIN_FRAMES:
      case SequenceTranslationConfigurationPanel.PROPERTY_REVERSE_SEQUENCES:
        fireTransformationsConfigurationModelEvent(
          TRANSLATION_CONFIGURATION, this.sequenceTranslationPanel.getSequenceTranslationConfiguration()
        );
        break;
    }
  }

  @Override
  public boolean isValidTransformation() {
    return this.sequenceTranslationPanel.isValidUserSelection();
  }

  @Override
  public SequencesGroupDatasetTransformation getTransformation(DatatypeFactory factory) {
    return new TranslateSequencesGroupDatasetTransformation(getTranslationConfiguration(), factory);
  }

  private SequenceTranslationConfiguration getTranslationConfiguration() {
    return this.sequenceTranslationPanel.getSequenceTranslationConfiguration();
  }
}
