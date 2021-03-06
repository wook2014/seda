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
package org.sing_group.seda.gui.merge;

import static java.awt.BorderLayout.CENTER;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.BoxLayout.Y_AXIS;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;

import org.jdesktop.swingx.JXTextField;
import org.sing_group.gc4s.event.DocumentAdapter;
import org.sing_group.gc4s.input.InputParameter;
import org.sing_group.gc4s.input.InputParametersPanel;
import org.sing_group.gc4s.ui.CenteredJPanel;
import org.sing_group.seda.gui.reformat.ReformatFastaConfigurationPanel;

public class MergeConfigurationPanel extends JPanel {
  private static final long serialVersionUID = 1L;

  private JXTextField nameTextField;
  private MergeTransformationProvider transformationProvider;
  private ReformatFastaConfigurationPanel reformatPanel;

  public MergeConfigurationPanel() {
    this.init();
    this.initTransformationProvider();
  }

  private void init() {
    this.setLayout(new BorderLayout());
    this.add(getMainPanel(), CENTER);
  }

  private JPanel getMainPanel() {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, Y_AXIS));
    mainPanel.add(getParametersPanel());
    mainPanel.add(getReformatFastaConfigurationPanel());

    return new CenteredJPanel(mainPanel);
  }

  private JPanel getParametersPanel() {
    return new InputParametersPanel(getInputParameters());
  }

  private InputParameter getInputParameters() {
    this.nameTextField = new JXTextField("Name");
    this.nameTextField.setColumns(20);
    this.nameTextField.getDocument().addDocumentListener(
      new DocumentAdapter() {

        @Override
        public void removeUpdate(DocumentEvent e) {
          nameChanged();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
          nameChanged();
        }
      }
    );

    return new InputParameter("Name:", this.nameTextField, "The name of the merged file.");
  }

  private Component getReformatFastaConfigurationPanel() {
    this.reformatPanel = new ReformatFastaConfigurationPanel();
    this.reformatPanel.setBorder(createTitledBorder("Reformat output file"));

    return this.reformatPanel;
  }

  private void nameChanged() {
    this.transformationProvider.setName(this.nameTextField.getText());
  }

  private void initTransformationProvider() {
    this.transformationProvider = new MergeTransformationProvider();
    this.transformationProvider.setReformatFastaTransformationProvider(this.reformatPanel.getTransformationProvider());
  }

  public MergeTransformationProvider getTransformationProvider() {
    return this.transformationProvider;
  }

  public void setTransformationProvider(MergeTransformationProvider transformationProvider) {
    this.transformationProvider = transformationProvider;

    this.nameTextField.setText(this.transformationProvider.getName());
    this.reformatPanel.setTransformationProvider(this.transformationProvider.getReformatFastaTransformationProvider());
  }
}
