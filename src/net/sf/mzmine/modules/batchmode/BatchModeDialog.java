/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.batchmode;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import net.sf.mzmine.data.ParameterSet;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.main.MZmineModule;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.components.HelpButton;
import net.sf.mzmine.util.dialogs.ExitCode;

class BatchModeDialog extends JDialog implements ActionListener {

    static final int PADDING_SIZE = 5;

    private Logger logger = Logger.getLogger(this.getClass().getName());

    private ExitCode exitCode = ExitCode.CANCEL;

    private Vector<BatchStepWrapper> batchSteps;

    // dialog components
    private JComboBox methodsCombo;
    private JList currentStepsList;
    private JButton btnAdd, btnConfig, btnRemove, btnOK, btnCancel, btnHelp;

    public BatchModeDialog(Vector<BatchStepWrapper> batchSteps) {

        // make dialog modal
        super(MZmineCore.getDesktop().getMainFrame(), "Batch mode setup", true);

        this.batchSteps = batchSteps;

        currentStepsList = new JList(batchSteps);
        currentStepsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        methodsCombo = new JComboBox();

        MZmineModule allModules[] = MZmineCore.getAllModules();

        for (BatchStepCategory category : BatchStepCategory.values()) {
            methodsCombo.addItem("--- " + category + " ---");
            for (MZmineModule module : allModules) {
                if (module instanceof BatchStep) {
                    BatchStep step = (BatchStep) module;
                    if (step.getBatchStepCategory() == category)
                        methodsCombo.addItem(step);
                }
            }
        }

        JPanel pnlRight = new JPanel();
        pnlRight.setLayout(new BoxLayout(pnlRight, BoxLayout.Y_AXIS));
        btnConfig = GUIUtils.addButton(pnlRight, "Configure", null, this);
        btnRemove = GUIUtils.addButton(pnlRight, "Remove", null, this);

        JPanel pnlCenter = new JPanel(new BorderLayout());
        pnlCenter.add(new JLabel("Current batch:"), BorderLayout.NORTH);
        pnlCenter.add(new JScrollPane(currentStepsList), BorderLayout.CENTER);

        JPanel pnlBottom = new JPanel(new BorderLayout());
        btnAdd = GUIUtils.addButton(pnlBottom, "Add", null, this);
        pnlBottom.add(btnAdd, BorderLayout.EAST);
        pnlBottom.add(methodsCombo, BorderLayout.CENTER);

        JPanel pnlMain = new JPanel(new BorderLayout());
        pnlMain.add(pnlCenter, BorderLayout.CENTER);
        pnlMain.add(pnlBottom, BorderLayout.SOUTH);
        pnlMain.add(pnlRight, BorderLayout.EAST);

        // Setup buttons
        JPanel pnlButtons = new JPanel();
        btnOK = GUIUtils.addButton(pnlButtons, "Run batch", null, this);
        btnCancel = GUIUtils.addButton(pnlButtons, "Cancel", null, this);
        btnHelp = new HelpButton("net/sf/mzmine/modules/batchmode/help/BatchMode.html");
        pnlButtons.add(btnHelp);

        JPanel pnlAll = new JPanel(new BorderLayout());
        pnlAll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        pnlAll.add(pnlMain, BorderLayout.CENTER);
        pnlAll.add(pnlButtons, BorderLayout.SOUTH);

        add(pnlAll);

        // finalize the dialog
        pack();
        setResizable(false);
        setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());

    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    public void actionPerformed(ActionEvent event) {

        Object src = event.getSource();

        if (src == btnOK) {

            if (batchSteps.size() == 0) {
                MZmineCore.getDesktop().displayErrorMessage(
                        "Please select at least one method");
                return;
            }

            exitCode = ExitCode.OK;
            dispose();
            return;
        }

        if (src == btnCancel) {
            exitCode = ExitCode.CANCEL;
            dispose();
            return;
        }

        if (src == btnAdd) {

            if (!(methodsCombo.getSelectedItem() instanceof BatchStep))
                return;
            BatchStep selectedMethod = (BatchStep) methodsCombo.getSelectedItem();
            logger.finest("Adding " + selectedMethod);

            // clone the parameters to
            ParameterSet paramsCopy = selectedMethod.getParameterSet().clone();
            ExitCode exitCode = selectedMethod.setupParameters(paramsCopy);
            if (exitCode != ExitCode.OK)
                return;
            selectedMethod.setParameters(paramsCopy);

            BatchStepWrapper newStep = new BatchStepWrapper(selectedMethod,
                    paramsCopy);
            batchSteps.add(newStep);
            currentStepsList.setListData(batchSteps);
            return;
        }

        if (src == btnRemove) {

            BatchStepWrapper selected = (BatchStepWrapper) currentStepsList.getSelectedValue();
            logger.finest("Removing " + selected);
            batchSteps.remove(selected);
            currentStepsList.setListData(batchSteps);
            return;
        }

        if (src == btnConfig) {

            BatchStepWrapper selected = (BatchStepWrapper) currentStepsList.getSelectedValue();
            if (selected == null)
                return;
            logger.finest("Configuring " + selected);
            selected.getMethod().setupParameters(selected.getParameters());
            return;
        }

    }

    public ExitCode getExitCode() {
        return exitCode;
    }

}