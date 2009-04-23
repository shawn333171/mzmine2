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
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin
 * St, Fifth Floor, Boston, MA 02110-1301 USA
 */
package net.sf.mzmine.project.io;

import com.Ostermiller.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import net.sf.mzmine.data.DataPoint;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleChromatographicPeak;
import net.sf.mzmine.data.impl.SimpleDataPoint;
import net.sf.mzmine.data.impl.SimplePeakIdentity;
import net.sf.mzmine.data.impl.SimplePeakList;
import net.sf.mzmine.data.impl.SimplePeakListAppliedMethod;
import net.sf.mzmine.data.impl.SimplePeakListRow;
import net.sf.mzmine.data.impl.SimpleScan;
import net.sf.mzmine.main.MZmineCore;
import net.sf.mzmine.project.MZmineProject;
import net.sf.mzmine.project.impl.RawDataFileImpl;
import net.sf.mzmine.util.Range;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PeakListOpen extends DefaultHandler {
	
	private RawDataFileImpl buildingRawDataFile;
	private SimplePeakListRow buildingRow;
	private int peakColumnID,  rawDataFileID,  quantity;
	private double mass,  rt,  height,  area;
	private int[] scanNumbers;
	private double[] retentionTimes,  masses,  intensities;
	private String peakStatus,  peakListName,  name,  formula,  identificationMethod,  identityID,  rawDataName;
	private boolean preferred;
	private String dateCreated;
	private Range rtRange,  mzRange;
	private boolean peakListFlag = false;
	private boolean scanFlag = false;
	private boolean mzPeakFlag = false;
	private StringBuffer charBuffer;
	private PeakList buildingPeakList;
	private TreeMap<Integer, RawDataFile> buildingArrayRawDataFiles;
	private Vector<String> appliedProcess;
	private int parsedRows;
	private int totalRows;
	private double progress;
	private ZipInputStream zipInputStream;
	private ZipFile zipFile;

	public PeakListOpen(ZipInputStream zipInputStream, ZipFile zipFile) {
		buildingArrayRawDataFiles = new TreeMap<Integer, RawDataFile>();
		charBuffer = new StringBuffer();
		appliedProcess = new Vector<String>();
		this.zipInputStream = zipInputStream;
		this.zipFile = zipFile;
	}

	/**
	 * Loads the peak list from the zip file reading the XML peak list file
	 * @throws java.lang.Exception
	 */
	public void readPeakList() throws Exception {

		// Parses the XML file
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser saxParser = factory.newSAXParser();
		saxParser.parse(zipFile.getInputStream(zipInputStream.getNextEntry()), this);

		if (buildingPeakList == null || buildingPeakList.getNumberOfRows() == 0) {
			return;
		}
		// Add new peaklist to the project or MZviewer.desktop
		MZmineProject currentProject = MZmineCore.getCurrentProject();
		currentProject.addPeakList(buildingPeakList);
	}

	/**
	 * @return the progress of these functions loading the peak list from the zip file.
	 */
	public double getProgress() {
		return progress;
	}

	/**
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
	 *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String namespaceURI, String lName, // local name
			String qName, // qualified name
			Attributes attrs) {

		// <PEAKLIST>
		if (qName.equals(PeakListElementName.PEAKLIST.getElementName())) {

			peakListFlag = true;
		}

		// <RAWFILE>
		if (qName.equals(PeakListElementName.RAWFILE.getElementName())) {

			rawDataFileID = Integer.parseInt(attrs.getValue(PeakListElementName.ID.getElementName()));
			peakListFlag = false;
		}

		// <SCAN>
		if (qName.equals(PeakListElementName.SCAN.getElementName())) {

			quantity = Integer.parseInt(attrs.getValue(PeakListElementName.QUANTITY.getElementName()));
			scanFlag = true;
		}

		// <ROW>
		if (qName.equals(PeakListElementName.ROW.getElementName())) {

			if (buildingPeakList == null) {
				initializePeakList();
			}
			int rowID = Integer.parseInt(attrs.getValue(PeakListElementName.ID.getElementName()));
			buildingRow = new SimplePeakListRow(rowID);
		}

		// <PEAK_IDENTITY>
		if (qName.equals(PeakListElementName.PEAK_IDENTITY.getElementName())) {

			identityID = attrs.getValue(PeakListElementName.ID.getElementName());
			preferred = Boolean.parseBoolean(attrs.getValue(PeakListElementName.PREFERRED.getElementName()));
		}

		// <PEAK>
		if (qName.equals(PeakListElementName.PEAK.getElementName())) {

			peakColumnID = Integer.parseInt(attrs.getValue(PeakListElementName.COLUMN.getElementName()));
			mass = Double.parseDouble(attrs.getValue(PeakListElementName.MASS.getElementName()));
			rt = Double.parseDouble(attrs.getValue(PeakListElementName.RT.getElementName()));
			height = Double.parseDouble(attrs.getValue(PeakListElementName.HEIGHT.getElementName()));
			area = Double.parseDouble(attrs.getValue(PeakListElementName.AREA.getElementName()));
			peakStatus = attrs.getValue(PeakListElementName.STATUS.getElementName());
		}

		// <MZPEAK>
		if (qName.equals(PeakListElementName.MZPEAK.getElementName())) {

			quantity = Integer.parseInt(attrs.getValue(PeakListElementName.QUANTITY.getElementName()));
			mzPeakFlag = true;
		}

	}

	/**
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
	 *      java.lang.String, java.lang.String)
	 */
	public void endElement(String namespaceURI, String sName, // simple name
			String qName // qualified name
			) {

		// <NAME>
		if (qName.equals(PeakListElementName.NAME.getElementName())) {

			name = getTextOfElement();
			if (peakListFlag) {
				peakListName = name;
			}
		}

		// <PEAKLIST_DATE>
		if (qName.equals(PeakListElementName.PEAKLIST_DATE.getElementName())) {

			dateCreated = getTextOfElement();
		}

		// <QUANTITY>
		if (qName.equals(PeakListElementName.QUANTITY.getElementName())) {

			String text = getTextOfElement();
			text = text.trim();
			totalRows = Integer.parseInt(text);
		}

		// <PROCESS>
		if (qName.equals(PeakListElementName.PROCESS.getElementName())) {

			String text = getTextOfElement();
			if (text.length() != 0) {
				appliedProcess.add(text);
			}
		}

		// <SCAN_ID>
		if (qName.equals(PeakListElementName.SCAN_ID.getElementName())) {

			if (scanFlag) {
				String valueText = getTextOfElement();
				String values[] = valueText.split(PeakListElementName.SEPARATOR.getElementName());
				scanNumbers = new int[quantity];
				for (int i = 0; i < quantity; i++) {
					scanNumbers[i] = Integer.parseInt(values[i]);
				}
			} else if (mzPeakFlag) {
				byte[] bytes = Base64.decodeToBytes(getTextOfElement());
				// make a data input stream
				DataInputStream dataInputStream = new DataInputStream(
						new ByteArrayInputStream(bytes));
				scanNumbers = new int[quantity];
				for (int i = 0; i < quantity; i++) {
					try {
						scanNumbers[i] = dataInputStream.readInt();
					} catch (IOException ex) {
						Logger.getLogger(PeakListOpen.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
			}
		}

		// <RT>
		if (qName.equals(PeakListElementName.RT.getElementName())) {

			String valueText = getTextOfElement();
			String values[] = valueText.split(PeakListElementName.SEPARATOR.getElementName());
			retentionTimes = new double[quantity];
			for (int i = 0; i < quantity; i++) {
				retentionTimes[i] = Double.parseDouble(values[i]);
			}
		}

		// <MASS>
		if (qName.equals(PeakListElementName.MASS.getElementName())) {

			byte[] bytes = Base64.decodeToBytes(getTextOfElement());
			// make a data input stream
			DataInputStream dataInputStream = new DataInputStream(
					new ByteArrayInputStream(bytes));
			masses = new double[quantity];
			for (int i = 0; i < quantity; i++) {
				try {
					masses[i] = (double) dataInputStream.readFloat();
				} catch (IOException ex) {
					Logger.getLogger(PeakListOpen.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}

		// <HEIGHT>
		if (qName.equals(PeakListElementName.HEIGHT.getElementName())) {

			byte[] bytes = Base64.decodeToBytes(getTextOfElement());
			// make a data input stream
			DataInputStream dataInputStream = new DataInputStream(
					new ByteArrayInputStream(bytes));
			intensities = new double[quantity];
			for (int i = 0; i < quantity; i++) {
				try {
					intensities[i] = (double) dataInputStream.readFloat();
				} catch (IOException ex) {
					Logger.getLogger(PeakListOpen.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}

		// <FORMULA>
		if (qName.equals(PeakListElementName.FORMULA.getElementName())) {

			formula = getTextOfElement();
		}

		// <IDENTIFICATION>
		if (qName.equals(PeakListElementName.IDENTIFICATION.getElementName())) {

			identificationMethod = getTextOfElement();
		}

		// <RTRANGE>
		if (qName.equals(PeakListElementName.RTRANGE.getElementName())) {

			String valueText = getTextOfElement();
			String values[] = valueText.split("-");
			double min = Double.parseDouble(values[0]);
			double max = Double.parseDouble(values[1]);
			rtRange = new Range(min, max);
		}

		// <MZRANGE>
		if (qName.equals(PeakListElementName.MZRANGE.getElementName())) {

			String valueText = getTextOfElement();
			String values[] = valueText.split("-");
			double min = Double.parseDouble(values[0]);
			double max = Double.parseDouble(values[1]);
			mzRange = new Range(min, max);
		}

		// <MZPEAK>
		if (qName.equals(PeakListElementName.MZPEAK.getElementName())) {

			mzPeakFlag = false;
		}

		// <PEAK>
		if (qName.equals(PeakListElementName.PEAK.getElementName())) {

			DataPoint[] mzPeaks = new DataPoint[quantity];
			Range peakRTRange = null, peakMZRange = null, peakIntensityRange = null;
			for (int i = 0; i < quantity; i++) {
				double rt = buildingArrayRawDataFiles.get(peakColumnID).getScan(scanNumbers[i]).getRetentionTime();
				double mz = masses[i];
				double intensity = intensities[i];
				if (i == 0) {
					peakRTRange = new Range(rt);
					peakMZRange = new Range(mz);
					peakIntensityRange = new Range(intensity);
				} else {
					peakRTRange.extendRange(rt);
					peakMZRange.extendRange(mz);
					peakIntensityRange.extendRange(intensity);
				}
				if(intensity > 0.0){					
					mzPeaks[i] = new SimpleDataPoint(mz, intensity);
				}
			}

			SimpleChromatographicPeak peak = new SimpleChromatographicPeak(
					buildingArrayRawDataFiles.get(peakColumnID), mass, rt,
					height, area, scanNumbers, mzPeaks, PeakStatus.valueOf(
					PeakStatus.class, peakStatus), -1, -1, peakRTRange,
					peakMZRange, peakIntensityRange);

			buildingRow.addPeak(buildingArrayRawDataFiles.get(peakColumnID),
					peak);
		}

		// <PEAK_IDENTITY>
		if (qName.equals(PeakListElementName.PEAK_IDENTITY.getElementName())) {

			SimplePeakIdentity identity = new SimplePeakIdentity(identityID,
					name, new String[0], formula, null, identificationMethod);
			buildingRow.addPeakIdentity(identity, preferred);
		}

		// <ROW>
		if (qName.equals(PeakListElementName.ROW.getElementName())) {

			buildingPeakList.addRow(buildingRow);
			buildingRow = null;
			progress = (double) parsedRows / totalRows;
			parsedRows++;
		}

		// <SCAN>
		if (qName.equals(PeakListElementName.SCAN.getElementName())) {

			if (buildingRawDataFile == null) {
				try {
					buildingRawDataFile = new RawDataFileImpl(name);
				} catch (IOException ex) {
					Logger.getLogger(PeakListOpen.class.getName()).log(Level.SEVERE, null, ex);
				}
			}

			for (int i = 0; i < quantity; i++) {
				SimpleScan newScan = new SimpleScan(buildingRawDataFile,
						scanNumbers[i], 1, retentionTimes[i], -1, 0f, 0, null,
						new DataPoint[0], false);
				buildingRawDataFile.addScan(newScan);
			}
			scanFlag = false;
		}

		// <RAWFILE>
		if (qName.equals("rawdata_name")) {
			rawDataName = getTextOfElement();
		}

		if (qName.equals(PeakListElementName.RAWFILE.getElementName())) {
			for (RawDataFile rawDataFile : MZmineCore.getCurrentProject().getDataFiles()) {
				if (rawDataFile.getName().matches(rawDataName)) {
					buildingRawDataFile = (RawDataFileImpl) rawDataFile;
					buildingRawDataFile.setRTRange(1, rtRange);
					buildingRawDataFile.setMZRange(1, mzRange);
					buildingArrayRawDataFiles.put(rawDataFileID, buildingRawDataFile);
					break;
				}
			}
			buildingArrayRawDataFiles.put(rawDataFileID, buildingRawDataFile);
			buildingRawDataFile = null;
		}

	}

	/**
	 * Return a string without tab an EOF characters
	 *
	 * @return String element text
	 */
	private String getTextOfElement() {

		String text = charBuffer.toString();
		text = text.replaceAll("[\n\r\t]+", "");
		text = text.replaceAll("^\\s+", "");
		charBuffer.delete(0, charBuffer.length());
		return text;
	}

	/**
	 * characters()
	 *
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	public void characters(char buf[], int offset, int len) throws SAXException {
		charBuffer = charBuffer.append(buf, offset, len);
	}

	/**
	 * Initializes the peak list
	 */
	private void initializePeakList() {

		RawDataFile[] dataFiles = buildingArrayRawDataFiles.values().toArray(
				new RawDataFile[0]);
		buildingPeakList = new SimplePeakList(peakListName, dataFiles);
		String[] process = appliedProcess.toArray(new String[0]);
		for (String description : process) {
			((SimplePeakList) buildingPeakList).addDescriptionOfAppliedTask(new SimplePeakListAppliedMethod(
					description));
		}
		((SimplePeakList) buildingPeakList).setDateCreated(dateCreated);
	}
}