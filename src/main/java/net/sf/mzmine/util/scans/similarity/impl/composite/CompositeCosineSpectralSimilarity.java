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

package net.sf.mzmine.util.scans.similarity.impl.composite;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.sf.mzmine.datamodel.DataPoint;
import net.sf.mzmine.parameters.ParameterSet;
import net.sf.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import net.sf.mzmine.util.maths.similarity.Similarity;
import net.sf.mzmine.util.scans.ScanAlignment;
import net.sf.mzmine.util.scans.similarity.SpectralSimilarity;
import net.sf.mzmine.util.scans.similarity.SpectralSimilarityFunction;
import net.sf.mzmine.util.scans.similarity.Weights;

/**
 * Similar to NIST search algorithm for GC-MS data with lots of signals (more an identity check than
 * similarity).<br>
 * Uses the relative intensity ratios of adjacent signals.
 * 
 * @author
 *
 */
public class CompositeCosineSpectralSimilarity extends SpectralSimilarityFunction {

  /**
   * Returns mass and intensity values detected in given scan
   */
  @Override
  public SpectralSimilarity getSimilarity(ParameterSet parameters, MZTolerance mzTol, int minMatch,
      DataPoint[] library, DataPoint[] query) {
    Weights weights =
        parameters.getParameter(CompositeCosineSpectralSimilarityParameters.weight).getValue();
    double minCos =
        parameters.getParameter(CompositeCosineSpectralSimilarityParameters.minCosine).getValue();

    // align
    List<DataPoint[]> aligned = alignDataPoints(mzTol, library, query);
    int queryN = query.length;
    int overlap = calcOverlap(aligned);

    if (overlap >= minMatch) {
      // relative factor ranges from 0-1
      double relativeFactor = calcRelativeNeighbourFactor(aligned);

      // weighted cosine
      double[][] diffArray =
          ScanAlignment.toIntensityMatrixWeighted(aligned, weights.getIntensity(), weights.getMz());
      double diffCosine = Similarity.COSINE.calc(diffArray);

      // composite dot product identity score
      // NIST search similar
      double composite = (queryN * diffCosine + overlap * relativeFactor) / (queryN + overlap);


      if (composite >= minCos)
        return new SpectralSimilarity(getName(), composite, overlap, library, query, aligned);
      else
        return null;
    }
    return null;
  }

  /**
   * sum of relative ratios of neighbours in both mass lists
   * 
   * @param aligned
   * @return
   */
  private double calcRelativeNeighbourFactor(List<DataPoint[]> aligned) {
    // remove all unaligned signals
    List<DataPoint[]> filtered = removeUnaligned(aligned);
    // sort by mz
    sortByMZ(filtered);

    // overlapping within mass tolerance
    int overlap = calcOverlap(aligned);

    // sum of relative ratios of neighbours in both mass lists
    double factor = 0;
    for (int i = 1; i < filtered.size(); i++) {
      DataPoint[] match1 = filtered.get(i - 1);
      DataPoint[] match2 = filtered.get(i);

      // 0 is library
      // 1 is query
      double ratioLibrary = match2[0].getIntensity() / match1[0].getIntensity();
      double ratioQuery = match2[1].getIntensity() / match1[1].getIntensity();
      factor += Math.min(ratioLibrary, ratioQuery) / Math.max(ratioLibrary, ratioQuery);
    }
    // factor ranges from 0-1 * overlap
    return factor / (overlap);
  }

  /**
   * Sort aligned datapoints by their minimum mz values (ascending)
   * 
   * @param filtered
   * @return
   */
  private void sortByMZ(List<DataPoint[]> filtered) {
    filtered.sort((a, b) -> {
      return Double.compare(getMinMZ(a), getMinMZ(b));
    });
  }

  /**
   * Minimum mz of all aligned datapoints
   * 
   * @param dp
   * @return
   */
  private double getMinMZ(DataPoint[] dp) {
    return Arrays.stream(dp).filter(Objects::nonNull).mapToDouble(DataPoint::getMZ).min().orElse(0);
  }

  @Override
  @Nonnull
  public String getName() {
    return "Composite dot -product identity (similar to NIST search)";
  }

  @Override
  public @Nullable Class<? extends ParameterSet> getParameterSetClass() {
    return CompositeCosineSpectralSimilarityParameters.class;
  }
}
