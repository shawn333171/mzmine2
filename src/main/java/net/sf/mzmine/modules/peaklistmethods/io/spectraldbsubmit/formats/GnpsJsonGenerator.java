/*
 * Copyright 2006-2018 The MZmine 2 Development Team
 * 
 * This file is part of MZmine 2.
 * 
 * MZmine 2 is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine 2; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */
/*
 * This module was prepared by Abi Sarvepalli, Christopher Jensen, and Zheng Zhang at the Dorrestein
 * Lab (University of California, San Diego).
 * 
 * It is freely available under the GNU GPL licence of MZmine2.
 * 
 * For any questions or concerns, please refer to:
 * https://groups.google.com/forum/#!forum/molecular_networking_bug_reports
 * 
 * Credit to the Du-Lab development team for the initial commitment to the MGF export module.
 */

package net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.formats;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.param.LibraryMetaDataParameters;
import net.sf.mzmine.modules.peaklistmethods.io.spectraldbsubmit.param.LibrarySubmitIonParameters;
import net.sf.mzmine.parameters.Parameter;
import net.sf.mzmine.util.spectraldb.entry.DBEntryField;

/**
 * Json for GNPS library entry submission
 * 
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 *
 */
public class GnpsJsonGenerator {
  /**
   * Whole JSON entry
   * 
   * @param param
   * @param dps
   * @return
   */
  public static String generateJSON(LibrarySubmitIonParameters param, DataPoint[] dps) {
    LibraryMetaDataParameters meta = (LibraryMetaDataParameters) param
        .getParameter(LibrarySubmitIonParameters.META_PARAM).getValue();

    boolean exportRT = meta.getParameter(LibraryMetaDataParameters.EXPORT_RT).getValue();

    JsonObjectBuilder json = Json.createObjectBuilder();
    // tag spectrum from mzmine2
    json.add(DBEntryField.SOFTWARE.getGnpsJsonID(), "mzmine2");
    // ion specific
    Double precursorMZ = param.getParameter(LibrarySubmitIonParameters.MZ).getValue();
    if (precursorMZ != null)
      json.add(DBEntryField.MZ.getGnpsJsonID(), precursorMZ);

    Integer charge = param.getParameter(LibrarySubmitIonParameters.CHARGE).getValue();
    if (charge != null)
      json.add(DBEntryField.CHARGE.getGnpsJsonID(), charge);

    String adduct = param.getParameter(LibrarySubmitIonParameters.ADDUCT).getValue();
    if (adduct != null && !adduct.trim().isEmpty())
      json.add(DBEntryField.IONTYPE.getGnpsJsonID(), adduct);

    if (exportRT) {
      Double rt =
          meta.getParameter(LibraryMetaDataParameters.EXPORT_RT).getEmbeddedParameter().getValue();
      if (rt != null)
        json.add(DBEntryField.RT.getGnpsJsonID(), rt);
    }

    // add data points array
    json.add("peaks", genJSONData(dps));

    // add meta data
    for (Parameter<?> p : meta.getParameters()) {
      if (!p.getName().equals(LibraryMetaDataParameters.EXPORT_RT.getName())) {
        String key = p.getName();
        Object value = p.getValue();
        if (value instanceof Double) {
          if (Double.compare(0d, (Double) value) == 0)
            json.add(key, 0);
          else
            json.add(key, (Double) value);
        } else if (value instanceof Float) {
          if (Float.compare(0f, (Float) value) == 0)
            json.add(key, 0);
          else
            json.add(key, (Float) value);
        } else if (value instanceof Integer)
          json.add(key, (Integer) value);
        else {
          if (value == null || (value instanceof String && ((String) value).isEmpty()))
            value = "N/A";
          json.add(key, value.toString());
        }
      }
    }

    // return Json.createObjectBuilder().add("spectrum", json.build()).build().toString();
    return json.build().toString();
  }

  /**
   * JSON of data points array
   * 
   * @param dps
   * @return
   */
  private static JsonArray genJSONData(DataPoint[] dps) {
    NumberFormat mzForm = new DecimalFormat("0.######");
    JsonArrayBuilder data = Json.createArrayBuilder();
    JsonArrayBuilder signal = Json.createArrayBuilder();
    for (DataPoint dp : dps) {
      // round to five digits. thats more than enough
      signal.add(mzForm.format(dp.getMZ()));
      signal.add(dp.getIntensity());
      data.add(signal.build());
    }
    return data.build();
  }
}
