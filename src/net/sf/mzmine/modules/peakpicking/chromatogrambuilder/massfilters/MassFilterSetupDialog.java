/*
 * Copyright 2006-2009 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massfilters;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.Parameter;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.Scan;
import net.sf.mzmine.desktop.Desktop;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.MzPeak;
import net.sf.mzmine.modules.peakpicking.chromatogrambuilder.massdetection.MassDetector;
import net.sf.mzmine.modules.visualization.spectra.PlotMode;
import net.sf.mzmine.modules.visualization.spectra.SpectraPlot;
import net.sf.mzmine.modules.visualization.spectra.SpectraVisualizerWindow;
import net.sf.mzmine.modules.visualization.spectra.datasets.MzPeaksDataSet;
import net.sf.mzmine.modules.visualization.spectra.datasets.ScanDataSet;
import net.sf.mzmine.util.GUIUtils;
import net.sf.mzmine.util.dialogs.ParameterSetupDialog;

/**
 * This class extends ParameterSetupDialog class, including a spectraPlot. This
 * is used to preview how the selected mass detector and his parameters works
 * over the raw data file.
 */
public class MassFilterSetupDialog extends ParameterSetupDialog implements
		ActionListener, PropertyChangeListener {

	public static final Color removedPeaksColor = Color.orange;

	private RawDataFile previewDataFile;
	private RawDataFile[] dataFiles;
	private String[] fileNames;

	// Dialog components
	private JPanel pnlPlotXY, pnlFileNameScanNumber;
	private JComboBox comboDataFileName, comboScanNumber;
	private JCheckBox preview;
	private JButton nextScanBtn;
	private JButton prevScanBtn;
	private int indexComboFileName;

	// Currently loaded scan
	private Scan currentScan;
	private String[] currentScanNumberlist;
	private int[] listScans;

	// XYPlot
	private SpectraPlot spectrumPlot;
	private MzPeaksDataSet removedPeaksDataSet, remainingPeaksDataSet;

	// Mass detector and filter;
	private MassDetector massDetector;
	private MassFilter massFilter;

	// Desktop
	private Desktop desktop = MZmineCore.getDesktop();

	/**
	 * @param parameters
	 * @param massFilterTypeNumber
	 */
	public MassFilterSetupDialog(MassDetector massDetector,
			MassFilter massFilter) {

		super(massFilter.getName() + "'s parameter setup dialog ", massFilter
				.getParameters(), massFilter.getHelpFileLocation());

		dataFiles = MZmineCore.getCurrentProject().getDataFiles();
		this.massFilter = massFilter;
		this.massDetector = massDetector;

		if (dataFiles.length != 0) {

			if (desktop.getSelectedDataFiles().length != 0)
				previewDataFile = desktop.getSelectedDataFiles()[0];
			else
				previewDataFile = dataFiles[0];

			// List of scan to apply mass detector
			listScans = previewDataFile.getScanNumbers(1);
			currentScanNumberlist = new String[listScans.length];
			for (int i = 0; i < listScans.length; i++)
				currentScanNumberlist[i] = String.valueOf(listScans[i]);

			fileNames = new String[dataFiles.length];

			for (int i = 0; i < dataFiles.length; i++) {
				fileNames[i] = dataFiles[i].getName();
				if (fileNames[i].equals(previewDataFile.getName()))
					indexComboFileName = i;
			}

			// Set a listener in all parameters's fields to add functionality to
			// this dialog
			for (Parameter p : massFilter.getParameters().getParameters()) {

				JComponent field = getComponentForParameter(p);
				field.addPropertyChangeListener("value", this);
				if (field instanceof JCheckBox)
					((JCheckBox) field).addActionListener(this);
				if (field instanceof JComboBox)
					((JComboBox) field).addActionListener(this);
			}

		}

		addComponents();

	}

	/**
	 * This function set all the information into the plot chart
	 * 
	 * @param scanNumber
	 */
	private void loadScan(final int scanNumber) {
		// Formats
		NumberFormat rtFormat = MZmineCore.getRTFormat();
		NumberFormat mzFormat = MZmineCore.getMZFormat();
		NumberFormat intensityFormat = MZmineCore.getIntensityFormat();

		currentScan = previewDataFile.getScan(scanNumber);
		ScanDataSet scanDataSet = new ScanDataSet(currentScan);

		spectrumPlot.removeAllDataSets();
		spectrumPlot.addDataSet(scanDataSet, SpectraVisualizerWindow.scanColor,
				false);
		spectrumPlot.addDataSet(removedPeaksDataSet, removedPeaksColor, false);
		spectrumPlot.addDataSet(remainingPeaksDataSet,
				SpectraVisualizerWindow.peaksColor, false);

		// if the scan is centroided, switch to centroid mode
		if (currentScan.isCentroided()) {
			spectrumPlot.setPlotMode(PlotMode.CENTROID);
		} else {
			spectrumPlot.setPlotMode(PlotMode.CONTINUOUS);
		}

		// Set window and plot titles

		String title = "[" + previewDataFile.toString() + "] scan #"
				+ currentScan.getScanNumber();

		String subTitle = "MS" + currentScan.getMSLevel() + ", RT "
				+ rtFormat.format(currentScan.getRetentionTime());

		DataPoint basePeak = currentScan.getBasePeak();
		if (basePeak != null) {
			subTitle += ", base peak: " + mzFormat.format(basePeak.getMZ())
					+ " m/z ("
					+ intensityFormat.format(basePeak.getIntensity()) + ")";
		}
		spectrumPlot.setTitle(title, subTitle);

	}

	/**
	 * @see net.sf.mzmine.util.dialogs.ParameterSetupDialog#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent event) {

		super.actionPerformed(event);

		Object src = event.getSource();
		String command = event.getActionCommand();

		if ((src == comboScanNumber)
				|| ((src instanceof JCheckBox) && (src != preview))
				|| ((src instanceof JComboBox) && (src != comboDataFileName))) {
			if (preview.isSelected()) {
				int ind = comboScanNumber.getSelectedIndex();
				setPeakListDataSet(ind);
				loadScan(listScans[ind]);
			}
		}

		if (src == comboDataFileName) {
			int ind = comboDataFileName.getSelectedIndex();
			if (ind >= 0) {
				previewDataFile = dataFiles[ind];
				listScans = previewDataFile.getScanNumbers(1);
				currentScanNumberlist = new String[listScans.length];
				for (int i = 0; i < listScans.length; i++)
					currentScanNumberlist[i] = String.valueOf(listScans[i]);
				ComboBoxModel model = new DefaultComboBoxModel(
						currentScanNumberlist);
				comboScanNumber.setModel(model);
				comboScanNumber.setSelectedIndex(0);

				setPeakListDataSet(0);
				loadScan(listScans[0]);
			}
		}

		if (src == preview) {
			if (preview.isSelected()) {
				int ind = comboScanNumber.getSelectedIndex();
				mainPanel.add(pnlPlotXY, BorderLayout.EAST);
				pnlFileNameScanNumber.setVisible(true);
				pack();
				setPeakListDataSet(ind);
				loadScan(listScans[ind]);
				this.setResizable(true);
				setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
			} else {
				mainPanel.remove(pnlPlotXY);
				pnlFileNameScanNumber.setVisible(false);
				this.setResizable(false);
				pack();
				setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
			}
		}

		if (command.equals("PREVIOUS_SCAN")) {
			int ind = comboScanNumber.getSelectedIndex() - 1;
			if (ind >= 0)
				comboScanNumber.setSelectedIndex(ind);
		}

		if (command.equals("NEXT_SCAN")) {
			int ind = comboScanNumber.getSelectedIndex() + 1;
			if (ind < (listScans.length - 1))
				comboScanNumber.setSelectedIndex(ind);
		}

	}

	/**
	 * 
	 */
	private void setPeakListDataSet(int ind) {

		updateParameterSetFromComponents();

		Scan scan = previewDataFile.getScan(listScans[ind]);
		MzPeak mzValues[] = massDetector.getMassValues(scan);
		MzPeak remainingMzValues[] = massFilter.filterMassValues(mzValues);

		Vector<MzPeak> removedPeaks = new Vector<MzPeak>();
		removedPeaks.addAll(Arrays.asList(mzValues));
		removedPeaks.removeAll(Arrays.asList(remainingMzValues));
		MzPeak removedMzValues[] = removedPeaks.toArray(new MzPeak[0]);

		removedPeaksDataSet = new MzPeaksDataSet("Removed peaks",
				removedMzValues);
		remainingPeaksDataSet = new MzPeaksDataSet("Remaining peaks",
				remainingMzValues);

	}

	/**
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent e) {
		if (preview.isSelected()) {
			int ind = comboScanNumber.getSelectedIndex();
			setPeakListDataSet(ind);
			loadScan(listScans[ind]);
		}
	}

	/**
	 * This function add all the additional components for this dialog over the
	 * original ParameterSetupDialog.
	 * 
	 */
	private void addComponents() {

		// Button's parameters
		String leftArrow = new String(new char[] { '\u2190' });
		String rightArrow = new String(new char[] { '\u2192' });

		// Elements of pnlpreview
		JPanel pnlpreview = new JPanel(new BorderLayout());

		preview = new JCheckBox(" Show preview of mass peak detection ");
		preview.addActionListener(this);
		preview.setHorizontalAlignment(SwingConstants.CENTER);
		pnlpreview.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);

		pnlpreview.add(new JSeparator(), BorderLayout.NORTH);
		pnlpreview.add(preview, BorderLayout.CENTER);

		// Elements of pnlLab
		JPanel pnlLab = new JPanel();
		pnlLab.setLayout(new BoxLayout(pnlLab, BoxLayout.Y_AXIS));
		pnlLab.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		pnlLab.add(new JLabel("Data file "));
		pnlLab.add(Box.createVerticalStrut(25));
		pnlLab.add(new JLabel("Scan number "));

		// Elements of pnlFlds
		JPanel pnlFlds = new JPanel();
		pnlFlds.setLayout(new BoxLayout(pnlFlds, BoxLayout.Y_AXIS));
		pnlFlds.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		comboDataFileName = new JComboBox(fileNames);
		comboDataFileName.setSelectedIndex(indexComboFileName);
		comboDataFileName.addActionListener(this);

		comboScanNumber = new JComboBox(currentScanNumberlist);
		comboScanNumber.setSelectedIndex(0);
		comboScanNumber.addActionListener(this);

		pnlFlds.add(comboDataFileName);
		pnlFlds.add(Box.createVerticalStrut(10));

		// --> Elements of pnlScanArrows

		JPanel pnlScanArrows = new JPanel();
		pnlScanArrows.setLayout(new BoxLayout(pnlScanArrows, BoxLayout.X_AXIS));

		prevScanBtn = GUIUtils.addButton(pnlScanArrows, leftArrow, null,
				(ActionListener) this, "PREVIOUS_SCAN");
		prevScanBtn.setFont(new Font("SansSerif", Font.BOLD, 14));

		pnlScanArrows.add(Box.createHorizontalStrut(5));
		pnlScanArrows.add(comboScanNumber);
		pnlScanArrows.add(Box.createHorizontalStrut(5));

		nextScanBtn = GUIUtils.addButton(pnlScanArrows, rightArrow, null,
				(ActionListener) this, "NEXT_SCAN");
		nextScanBtn.setFont(new Font("SansSerif", Font.BOLD, 14));

		// <--

		pnlFlds.add(pnlScanArrows);

		// Elements of pnlSpace
		JPanel pnlSpace = new JPanel();
		pnlSpace.setLayout(new BoxLayout(pnlSpace, BoxLayout.Y_AXIS));
		pnlSpace.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

		pnlSpace.add(Box.createHorizontalStrut(50));

		// Put all together
		pnlFileNameScanNumber = new JPanel(new BorderLayout());

		pnlFileNameScanNumber.add(pnlpreview, BorderLayout.NORTH);
		pnlFileNameScanNumber.add(pnlLab, BorderLayout.WEST);
		pnlFileNameScanNumber.add(pnlFlds, BorderLayout.CENTER);
		pnlFileNameScanNumber.add(pnlSpace, BorderLayout.EAST);
		pnlFileNameScanNumber.setVisible(false);

		JPanel pnlVisible = new JPanel(new BorderLayout());

		pnlVisible.add(pnlpreview, BorderLayout.NORTH);

		JPanel tmp = new JPanel();
		tmp.add(pnlFileNameScanNumber);
		pnlVisible.add(tmp, BorderLayout.CENTER);

		// Panel for XYPlot
		pnlPlotXY = new JPanel(new BorderLayout());
		Border one = BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
		Border two = BorderFactory.createEmptyBorder(10, 10, 10, 10);
		pnlPlotXY.setBorder(BorderFactory.createCompoundBorder(one, two));
		pnlPlotXY.setBackground(Color.white);

		spectrumPlot = new SpectraPlot(this);

		pnlPlotXY.add(spectrumPlot, BorderLayout.CENTER);

		componentsPanel.add(pnlVisible, BorderLayout.CENTER);

		pack();
		setLocationRelativeTo(MZmineCore.getDesktop().getMainFrame());
	}

}