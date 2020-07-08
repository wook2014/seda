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
package org.sing_group.seda.pfam.gui;

import static java.awt.BorderLayout.CENTER;
import static javax.swing.SwingUtilities.invokeLater;
import static javax.swing.UIManager.getColor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;

import org.sing_group.gc4s.event.DocumentAdapter;
import org.sing_group.gc4s.event.RunnableDocumentAdapter;
import org.sing_group.gc4s.input.InputParameter;
import org.sing_group.gc4s.input.InputParametersPanel;
import org.sing_group.gc4s.input.text.DoubleTextField;
import org.sing_group.gc4s.input.text.EmailJXTextField;
import org.sing_group.gc4s.input.text.JIntegerTextField;
import org.sing_group.gc4s.ui.CenteredJPanel;
import org.sing_group.gc4s.utilities.ColorUtils;
import org.sing_group.seda.pfam.PfamScanSequenceErrorPolicy;

public class PfamScanTransformationConfigurationPanel extends JPanel {
  private static final long serialVersionUID = 1L;

  private static final Color COLOR_VALID_INPUT = getColor("TextField.background");

  private static final String HELP_EMAIL =
    "<html>A valid e-mail address. This is required by EMBL-EBI so they can contact you in the event of:<ul>" +
      "<li>Problems with the service which affect your jobs.</li>" +
      "<li>Scheduled maintenance which affects services you are using.</li>" +
      "<li>Deprecation and retirement of a service you are using.</li></ul>";

  private static final String HELP_ASP = "Whether to predict active site residues for Pfam-A matches or not.";
  private static final String HELP_EVALUE = "Optionally, the expectation value cut-off.";

  private static final String HELP_ERROR_POLICY =
    "<html>The policy to apply with sequences that fail when analyzed with PfamScan:<ul>" +
      "<li><i>Annotate sequence as error</i>: if a sequence analysis fails, this is annotated as an error "
      + "in the output FASTA.</li>" +
      "<li><i>Ignore sequences</i>: if a sequence analysis fails, it is ignored and not included in the output FASTA.</li>"
      + "<li><i>Produce an error (stop operation)</i>: if a sequence analysis fails an error is produced and the "
      + "whole operation is stopped.</li></ul>";

  private static final String HELP_BATCH_DELAY =
    "<html>The delay factor between batches. SEDA runs PfamScan queries in batches of 30 sequences to meet the EMBL-EBI "
      + "guidelines regarding the usage of resources. <br/>A delay factor of 1 means that SEDA waits a time between "
      + "batches equal to the time required to analyze the first batch.</html>";

  private PfamScanTransformationProvider transformationProvider;

  private EmailJXTextField eMailTf;
  private JCheckBox activeSitePredictionCb;
  private DoubleTextField eValue;
  private JComboBox<PfamScanSequenceErrorPolicy> errorPolicy;
  private JIntegerTextField batchDelayFactor;

  public PfamScanTransformationConfigurationPanel() {
    this.init();
    this.initTransformationProvider();
  }

  private void initTransformationProvider() {
    this.transformationProvider =
      new PfamScanTransformationProvider(
        this.activeSitePredictionCb.isSelected(),
        this.getErrorPolicy(),
        this.batchDelayFactor.getValue()
      );
  }

  private void init() {
    this.setLayout(new BorderLayout());
    this.add(getMainPanel(), CENTER);
  }

  private JPanel getMainPanel() {
    return new CenteredJPanel(new InputParametersPanel(getParameters()));
  }

  private InputParameter[] getParameters() {
    List<InputParameter> parameters = new LinkedList<InputParameter>();
    parameters.add(getEmailParameter());
    parameters.add(getActiveSitePredictionParameter());
    parameters.add(getEvalueParameter());
    parameters.add(getErrorPolicyParameter());
    parameters.add(getBatchDelayFactorParameter());

    return parameters.toArray(new InputParameter[parameters.size()]);
  }

  private InputParameter getEmailParameter() {
    this.eMailTf = new EmailJXTextField();
    this.eMailTf.setMinimumSize(new Dimension(250, 20));
    this.eMailTf.getDocument().addDocumentListener(new DocumentAdapter() {

      @Override
      public void removeUpdate(DocumentEvent e) {
        eMailChanged();
      }

      @Override
      public void insertUpdate(DocumentEvent e) {
        eMailChanged();
      }
    });

    return new InputParameter("E-mail:", this.eMailTf, HELP_EMAIL);
  }

  private void eMailChanged() {
    invokeLater(() -> {
      if (this.eMailTf.isValidInput()) {
        this.transformationProvider.setEmail(this.eMailTf.getText());
      } else {
        this.transformationProvider.clearEmail();
      }
    });
  }

  private InputParameter getActiveSitePredictionParameter() {
    this.activeSitePredictionCb = new JCheckBox();
    this.activeSitePredictionCb.addItemListener(this::activeSitePredictionChanged);

    return new InputParameter("Active site prediction:", this.activeSitePredictionCb, HELP_ASP);
  }

  private void activeSitePredictionChanged(ItemEvent event) {
    invokeLater(() -> {
      this.transformationProvider.setActiveSitePrediction(this.activeSitePredictionCb.isSelected());
    });
  }

  private InputParameter getEvalueParameter() {
    this.eValue = new DoubleTextField();
    this.eValue.getDocument().addDocumentListener(new RunnableDocumentAdapter(this::expectValueChanged));

    return new InputParameter("Expectation value:", this.eValue, HELP_EVALUE);
  }

  private void expectValueChanged() {
    invokeLater(() -> {
      if (this.eValue.getText().isEmpty()) {
        this.transformationProvider.clearEvalue();
        this.eValue.setBackground(COLOR_VALID_INPUT);
      } else {
        try {
          Double eValue = getExpectValue();
          this.transformationProvider.setEvalue(eValue);
          this.eValue.setBackground(eValue > 0 ? COLOR_VALID_INPUT : ColorUtils.COLOR_INVALID_INPUT);
        } catch (NumberFormatException e) {
          this.transformationProvider.setEvalue(-1d);
          this.eValue.setBackground(ColorUtils.COLOR_INVALID_INPUT);
        }
      }
    });
  }

  private Double getExpectValue() {
    return Double.parseDouble(this.eValue.getText().replace(",", ""));
  }

  private InputParameter getErrorPolicyParameter() {
    this.errorPolicy = new JComboBox<>(PfamScanSequenceErrorPolicy.values());
    this.errorPolicy.addItemListener(this::errorPolicyChanged);

    return new InputParameter("Sequence error policy:", this.errorPolicy, HELP_ERROR_POLICY);
  }

  private void errorPolicyChanged(ItemEvent event) {
    if (event.getStateChange() == ItemEvent.SELECTED) {
      invokeLater(() -> {
        this.transformationProvider.setErrorPolicy(getErrorPolicy());
      });
    }
  }

  private PfamScanSequenceErrorPolicy getErrorPolicy() {
    return (PfamScanSequenceErrorPolicy) this.errorPolicy.getSelectedItem();
  }

  private InputParameter getBatchDelayFactorParameter() {
    this.batchDelayFactor = new JIntegerTextField(1);
    this.batchDelayFactor.getDocument().addDocumentListener(new RunnableDocumentAdapter(this::batchDelayFactorChanged));

    return new InputParameter("Batch delay factor:", this.batchDelayFactor, HELP_BATCH_DELAY);
  }

  private void batchDelayFactorChanged() {
    invokeLater(() -> {
      if (this.batchDelayFactor.getText().isEmpty()) {
        this.batchDelayFactor.setBackground(ColorUtils.COLOR_INVALID_INPUT);
        this.transformationProvider.setBatchDelayFactor(-1);
      } else {
        try {
          int batchDelayFactoryValue = getBatchDelayFactor();
          this.transformationProvider.setBatchDelayFactor(batchDelayFactoryValue);
          this.batchDelayFactor
            .setBackground(batchDelayFactoryValue >= 0 ? COLOR_VALID_INPUT : ColorUtils.COLOR_INVALID_INPUT);
        } catch (NumberFormatException e) {
          this.transformationProvider.setBatchDelayFactor(-1);
          this.batchDelayFactor.setBackground(ColorUtils.COLOR_INVALID_INPUT);
        }
      }
    });
  }

  private Integer getBatchDelayFactor() {
    return Integer.parseInt(this.batchDelayFactor.getText());
  }

  public PfamScanTransformationProvider getTransformationProvider() {
    return this.transformationProvider;
  }

  public void setTransformationProvider(PfamScanTransformationProvider transformationProvider) {
    this.transformationProvider = transformationProvider;

    String eMailValue = this.transformationProvider.geteMail();
    if (eMailValue != null) {
      this.eMailTf.setText(eMailValue);
    }

    this.activeSitePredictionCb.setSelected(this.transformationProvider.isActiveSitePrediction());

    Double newEvalue = this.transformationProvider.getEvalue();
    if (newEvalue != null) {
      this.eValue.setValue(newEvalue);
    }

    PfamScanSequenceErrorPolicy newErrorPolicy = this.transformationProvider.getErrorPolicy();
    if (newErrorPolicy != null) {
      this.errorPolicy.setSelectedItem(newErrorPolicy);
    }

    this.batchDelayFactor.setValue(this.transformationProvider.getBatchDelayFactor());
  }
}