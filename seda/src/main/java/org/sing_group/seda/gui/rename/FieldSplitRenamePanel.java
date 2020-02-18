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
package org.sing_group.seda.gui.rename;

import static java.lang.Integer.parseInt;
import static java.util.Collections.emptyList;

import java.awt.event.ItemEvent;
import java.util.LinkedList;
import java.util.List;

import org.jdesktop.swingx.JXTextField;
import org.sing_group.gc4s.input.InputParameter;
import org.sing_group.gc4s.input.InputParametersPanel;
import org.sing_group.gc4s.input.RadioButtonsPanel;
import org.sing_group.seda.core.rename.FieldSplitRenamer;
import org.sing_group.seda.core.rename.FieldSplitRenamer.Mode;
import org.sing_group.seda.core.rename.HeaderRenamer;
import org.sing_group.seda.core.rename.HeaderTarget;
import org.sing_group.seda.datatype.DatatypeFactory;

public class FieldSplitRenamePanel extends AbstractRenamePanel {
  private static final long serialVersionUID = 1L;
  
  private static final String HELP_FIELDS =
    "<html>The comma-separated list of fields starting at 1.<br>Note that when the <i>Keep</i> mode is used, then "
      + "the order of the fields is preserved in the output, meaning that it is possible to swap fields.";

  private JXTextField fieldDelimiterTextField;
  private JXTextField joinDelimiterTextField;
  private RadioButtonsPanel<Mode> modeRbtnPanel;
  private JXTextField fieldsTextField;

  public FieldSplitRenamePanel() {
    this.init();
  }

  private void init() {
    this.add(new InputParametersPanel(getParameters()));
  }

  private InputParameter[] getParameters() {
    InputParameter[] toret = new  InputParameter[4];
    toret[0] = getFieldDelimiterParameter();
    toret[1] = getJoinDelimiterParameter();
    toret[2] = getModeParameter();
    toret[3] = getFieldsParameter();

    return toret;
  }

  private InputParameter getFieldDelimiterParameter() {
    this.fieldDelimiterTextField = new JXTextField("Field delimiter");
    this.fieldDelimiterTextField.getDocument().addDocumentListener(documentListener);

    return new InputParameter("Field delimiter", this.fieldDelimiterTextField, "The field delimiter.");
  }

  private InputParameter getJoinDelimiterParameter() {
    this.joinDelimiterTextField = new JXTextField("Join delimiter");
    this.joinDelimiterTextField.getDocument().addDocumentListener(documentListener);

    return new InputParameter("Join delimiter", this.joinDelimiterTextField, "The join delimiter.");
  }

  private InputParameter getModeParameter() {
    this.modeRbtnPanel = new RadioButtonsPanel<>(Mode.values(), 1, 2);
    this.modeRbtnPanel.addItemListener(this::modeValueChanged);

    return new InputParameter("Mode", this.modeRbtnPanel, "Wether to keep or remove fields.");
  }

  private void modeValueChanged(ItemEvent event) {
    if (event.getStateChange() == ItemEvent.SELECTED) {
      this.renameConfigurationChanged();
    }
  }

  private InputParameter getFieldsParameter() {
    this.fieldsTextField = new JXTextField("1, 2, 3");
    this.fieldsTextField.getDocument().addDocumentListener(documentListener);

    return new InputParameter("Fields", this.fieldsTextField, HELP_FIELDS);
  }

  private String getFieldDelimiter() {
    return this.fieldDelimiterTextField.getText();
  }

  private String getJoinDelimiter() {
    return this.joinDelimiterTextField.getText();
  }

  private List<Integer> getFields() {
    String fields = this.fieldsTextField.getText();
    if (fields.isEmpty()) {
      return emptyList();
    }
    List<Integer> toret = new LinkedList<>();
    String[] split = fields.split(",");
    for (String field : split) {
      try {
        Integer fieldInt = parseInt(field) - 1;
        if (!toret.contains(fieldInt)) {
          toret.add(fieldInt);
        }
      } catch (Exception ex) {
        return emptyList();
      }
    }
    return toret;
  }

  private Mode getMode() {
    return this.modeRbtnPanel.getSelectedItem().get();
  }

  @Override
  public boolean isValidConfiguration() {
    return !getFieldDelimiter().isEmpty() && !getFields().isEmpty();
  }

  @Override
  public HeaderRenamer getHeaderRenamer(DatatypeFactory factory, HeaderTarget target) {
    return new FieldSplitRenamer(factory, target, getFieldDelimiter(), getJoinDelimiter(), getMode(), getFields());
  }
}
