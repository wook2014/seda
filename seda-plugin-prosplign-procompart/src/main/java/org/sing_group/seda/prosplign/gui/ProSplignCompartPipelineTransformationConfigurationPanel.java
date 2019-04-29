/*
 * #%L
 * SEquence DAtaset builder ProSplign/ProCompart plugin
 * %%
 * Copyright (C) 2017 - 2019 Jorge Vieira, Cristina Vieira, Noé Vázquez, Miguel Reboiro-Jato and Hugo López-Fernández
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
package org.sing_group.seda.prosplign.gui;

import static java.awt.BorderLayout.CENTER;
import static java.lang.System.getProperty;
import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.BoxLayout.Y_AXIS;
import static javax.swing.SwingUtilities.invokeLater;
import static org.sing_group.gc4s.ui.CardsPanel.PROPERTY_VISIBLE_CARD;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;

import org.sing_group.gc4s.event.DocumentAdapter;
import org.sing_group.gc4s.input.InputParameter;
import org.sing_group.gc4s.input.InputParametersPanel;
import org.sing_group.gc4s.input.filechooser.JFileChooserPanel;
import org.sing_group.gc4s.input.filechooser.JFileChooserPanelBuilder;
import org.sing_group.gc4s.input.filechooser.SelectionMode;
import org.sing_group.gc4s.input.text.JIntegerTextField;
import org.sing_group.gc4s.ui.CardsPanel;
import org.sing_group.gc4s.ui.CardsPanelBuilder;
import org.sing_group.gc4s.ui.CenteredJPanel;
import org.sing_group.seda.blast.execution.BlastBinariesExecutor;
import org.sing_group.seda.blast.gui.BlastExecutionConfigurationPanel;
import org.sing_group.seda.gui.CommonFileChooser;
import org.sing_group.seda.gui.GuiUtils;
import org.sing_group.seda.gui.execution.BinaryExecutionConfigurationPanel;
import org.sing_group.seda.plugin.spi.TransformationProvider;
import org.sing_group.seda.prosplign.execution.ProSplignCompartBinariesExecutor;

public class ProSplignCompartPipelineTransformationConfigurationPanel extends JPanel {
  private static final long serialVersionUID = 1L;

  private static final String HELP_QUERY_FILE = "The genome query file (nucleotides).";
  private static final String HELP_MAX_TARGET_SEQS = "Value of the max_target_seqs BLAST parameter.";

  private JFileChooserPanel fileQuery;
  private JIntegerTextField maxTargetSeqs;
  private CardsPanel proSplignCompartExecutableCardsPanel;
  private BlastExecutionConfigurationPanel blastExecutionConfigurationPanel;
  private ProSplignCompartPipelineTransformationProvider transformationProvider;

  public ProSplignCompartPipelineTransformationConfigurationPanel() {
    this.init();
    this.transformationProvider = new ProSplignCompartPipelineTransformationProvider(this);
  }

  private void init() {
    this.setLayout(new BorderLayout());
    this.add(getMainPanel(), CENTER);
  }

  private JPanel getMainPanel() {
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, Y_AXIS));
    mainPanel.add(getQueryConfigurationPanel());

    return new CenteredJPanel(mainPanel);
  }

  private InputParametersPanel getQueryConfigurationPanel() {
    InputParametersPanel queryConfigurationPanel = new InputParametersPanel(getParameters());

    return queryConfigurationPanel;
  }

  private InputParameter[] getParameters() {
    List<InputParameter> parameters = new LinkedList<InputParameter>();
    parameters.add(getProSplignCompartExecutableParameter());
    parameters.add(getBlastExecutableParameter());
    parameters.add(getGenomeQueryFileParameter());
    parameters.add(getMaxTargetSeqsParameter());

    return parameters.toArray(new InputParameter[parameters.size()]);
  }

  private InputParameter getProSplignCompartExecutableParameter() {
    SystemBinaryExecutionConfigurationPanel systemBinaryExecutionConfigurationPanel =
      new SystemBinaryExecutionConfigurationPanel();
    systemBinaryExecutionConfigurationPanel.addBinaryConfigurationPanelListener(this::proSplignCompartExecutorChanged);

    DockerExecutionConfigurationPanel dockerExecutionConfigurationPanel = new DockerExecutionConfigurationPanel();
    dockerExecutionConfigurationPanel.addBinaryConfigurationPanelListener(this::proSplignCompartExecutorChanged);

    CardsPanelBuilder builder =
      CardsPanelBuilder.newBuilder()
        .withCard("Docker image", dockerExecutionConfigurationPanel);

    if (!getProperty(GuiUtils.PROPERTY_ENABLE_LOCAL_EXECUTION, "true").equals("false")) {
      builder = builder.withCard("System binary", systemBinaryExecutionConfigurationPanel);
    }

    this.proSplignCompartExecutableCardsPanel =
      builder
        .withSelectionLabel("Execution mode")
        .build();

    this.proSplignCompartExecutableCardsPanel.setBorder(createTitledBorder("ProSplign/ProCompart configuration"));

    this.proSplignCompartExecutableCardsPanel
      .addPropertyChangeListener(PROPERTY_VISIBLE_CARD, this::proSplignCompartBinaryExecutorCardChanged);

    return new InputParameter("", proSplignCompartExecutableCardsPanel, "The mode to execute ProSplign/ProCompart.");
  }

  private void proSplignCompartExecutorChanged(BinaryExecutionConfigurationPanel<ProSplignCompartBinariesExecutor> source) {
    this.proSplignCompartExecutorChanged();
  }

  private void proSplignCompartBinaryExecutorCardChanged(PropertyChangeEvent event) {
    this.proSplignCompartExecutorChanged();
  }

  private void proSplignCompartExecutorChanged() {
    notifyTransformationProvider(this.transformationProvider::proSplignCompartExecutorChanged);
  }
  
  private void notifyTransformationProvider(Runnable r) {
    invokeLater(() -> {
      this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      r.run();
      this.setCursor(Cursor.getDefaultCursor());
    });
  }

  public Optional<ProSplignCompartBinariesExecutor> getProSplignCompartBinariesExecutor() {
    @SuppressWarnings("unchecked")
    BinaryExecutionConfigurationPanel<ProSplignCompartBinariesExecutor> selectedCard =
      ((BinaryExecutionConfigurationPanel<ProSplignCompartBinariesExecutor>) this.proSplignCompartExecutableCardsPanel
        .getSelectedCard());

    return selectedCard.getBinariesExecutor();
  }

  private InputParameter getBlastExecutableParameter() {
    this.blastExecutionConfigurationPanel = new BlastExecutionConfigurationPanel(this::blastExecutorChanged);
    this.blastExecutionConfigurationPanel.setBorder(createTitledBorder("BLAST configuration"));

    return new InputParameter("", blastExecutionConfigurationPanel, "The mode to execute BLAST.");
  }
  
  private void blastExecutorChanged(BinaryExecutionConfigurationPanel<BlastBinariesExecutor> source) {
    this.blastExecutorChanged();
  }

  private void blastExecutorChanged() {
    notifyTransformationProvider(this.transformationProvider::blastExecutorChanged);
  }

  public Optional<BlastBinariesExecutor> getBlastBinariesExecutor() {
   return this.blastExecutionConfigurationPanel.getBlastBinariesExecutor();
  }
  
  private InputParameter getGenomeQueryFileParameter() {
    this.fileQuery =
      JFileChooserPanelBuilder
        .createOpenJFileChooserPanel()
        .withFileChooser(CommonFileChooser.getInstance().getFilechooser())
        .withFileChooserSelectionMode(SelectionMode.FILES)
        .withLabel("")
        .build();
    this.fileQuery.addFileChooserListener(this::fileQueryChanged);

    return new InputParameter("External file query:", this.fileQuery, HELP_QUERY_FILE);
  }

  private void fileQueryChanged(ChangeEvent event) {
    notifyTransformationProvider(this.transformationProvider::queryFileChanged);
  }

  private InputParameter getMaxTargetSeqsParameter() {
    this.maxTargetSeqs = new JIntegerTextField(1);
    this.maxTargetSeqs.setColumns(4);
    this.maxTargetSeqs.getDocument()
      .addDocumentListener(new MyDocumentAdater(() -> notifyTransformationProvider(this.transformationProvider::maxTargetSeqsChanged)));

    return new InputParameter("Max. target seqs.:", this.maxTargetSeqs, HELP_MAX_TARGET_SEQS);
  }

  public TransformationProvider getModel() {
    return this.transformationProvider;
  }

  public int getMaxTargetSeqs() {
    return this.maxTargetSeqs.getValue();
  }

  private class MyDocumentAdater extends DocumentAdapter {

    private Runnable runnable;

    public MyDocumentAdater(Runnable runnable) {
      this.runnable = runnable;
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      valueChanged();
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
      valueChanged();
    }
    
    private void valueChanged() {
      runnable.run();
    }
  }

  public File getQueryFile() {
    return this.fileQuery.getSelectedFile();
  }
}