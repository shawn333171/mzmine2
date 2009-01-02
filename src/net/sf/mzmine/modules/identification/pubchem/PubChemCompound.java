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

package net.sf.mzmine.modules.identification.pubchem;

import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.IsotopePattern;

public class PubChemCompound implements PeakIdentity, Comparable {

    private String compoundID, compoundName, alternateNames[], compoundFormula;
    private String databaseEntryURL, identificationMethod, scopeNote;
    private String exactMass = "", isotopePatternScore = "";
    private IsotopePattern isotopePattern;
    private String structure;
    private static final String pubchemAddress = "http://pubchem.ncbi.nlm.nih.gov/summary/summary.cgi?cid=";

    /**
     * @param compoundID
     * @param compoundName
     * @param alternateNames
     * @param compoundFormula
     * @param databaseEntryURL
     * @param identificationMethod
     * @param scopeNote
     */
    public PubChemCompound(String compoundID, String compoundName,
            String[] alternateNames, String compoundFormula,
            String databaseEntryURL, String identificationMethod, String scopeNote) {
        this.compoundName = compoundName;
        this.compoundFormula = compoundFormula;
        
        if (compoundName == null){
            this.compoundName = compoundFormula;
        }
        else if (compoundName.equals("")){
            this.compoundName = compoundFormula;
        }
        
        this.compoundID = compoundID;
        this.alternateNames = alternateNames;
        if (databaseEntryURL == null){
        	this.databaseEntryURL =  pubchemAddress + compoundID;
        }
        else{
            this.databaseEntryURL = databaseEntryURL;
        }
        this.identificationMethod = identificationMethod;
        this.scopeNote = scopeNote;
    }
	
    /**
     * @return Returns the alternateNames.
     */
    public String[] getAlternateNames() {
        return alternateNames;
    }

    /**
     * @param alternateNames The alternateNames to set.
     */
    public void setAlternateNames(String[] alternateNames) {
        this.alternateNames = alternateNames;
    }

    /**
     * @return Returns the compoundFormula.
     */
    public String getCompoundFormula() {
        return compoundFormula;
    }

    /**
     * @param compoundFormula The compoundFormula to set.
     */
    public void setCompoundFormula(String compoundFormula) {
        this.compoundFormula = compoundFormula;
    }

    /**
     * @return Returns the compoundID.
     */
    public String getID() {
        return compoundID;
    }

    /**
     * @param compoundID The compoundID to set.
     */
    public void setCompoundID(String compoundID) {
        this.compoundID = compoundID;
    }

    /**
     * @return Returns the compoundName.
     */
    public String getName() {
        return compoundName;
    }

    /**
     * @param compoundName The compoundName to set.
     */
    public void setCompoundName(String compoundName) {
        this.compoundName = compoundName;
    }

    /**
     * @return Returns the databaseEntryURL.
     */
    public String getDatabaseEntryURL() {
        return databaseEntryURL;
    }

    /**
     * @param databaseEntryURL The databaseEntryURL to set.
     */
    public void setDatabaseEntryURL(String databaseEntryURL) {
        this.databaseEntryURL = databaseEntryURL;
    }

    /**
     * @return Returns the identificationMethod.
     */
    public String getIdentificationMethod() {
        return identificationMethod;
    }

    /**
     * @param identificationMethod The identificationMethod to set.
     */
    public void setIdentificationMethod(String identificationMethod) {
        this.identificationMethod = identificationMethod;
    }
    
    /**
     * @param scopeNote The scope note to set
     */
    public void setScopeNote(String scopeNote){
    	this.scopeNote = scopeNote;
    }
    
    /**
     * @return Returns scopeNote The scope note
     */
    public String getScopeNote(){
    	return scopeNote;
    }
    
	/**
	 * Set the difference between this compound and the detected peak
	 * 
	 * @return String exact mass
	 */
    public void setExactMassDifference (String exactMass){
    	this.exactMass = exactMass;
    }
    
	/**
	 * Returns the difference between this compound and the detected peak
	 * 
	 * @return String exact mass
	 */
    public String getExactMassDifference(){
    	return exactMass;
    }
    
	/**
	 * Set the isotope pattern (predicted) of this compound
	 * 
	 * @return String exact mass
	 */
    public void setIsotopePatterScore(String score){
    	this.isotopePatternScore = score;
    }
    
	/**
	 * Returns the isotope pattern (predicted) of this compound
	 * 
	 * @return IsotopePattern
	 */
    public String getIsotopePatternScore(){
    	return isotopePatternScore;
    }

	/**
	 * Returns the isotope pattern of this compound identity.
	 * 
	 * @return isotopePattern
	 */
	public IsotopePattern getIsotopePattern() {
		return isotopePattern;
	}

	
	/**
	 * Assign an isotope pattern to this compound identity.
	 * 
	 * @param isotopePattern
	 */
	public void setIsotopePattern(IsotopePattern isotopePattern) {
		this.isotopePattern = isotopePattern;
	}
	
	/**
	 * Assign the structure (SDF format) of this compound.
	 * 
	 * @param structure
	 */
	public void  setStructure(String structure){
		this.structure =  structure;
	}

	/**
	 * Returns the structure (SDF format) of this compound.
	 * 
	 * @return structure
	 */
	public String getStructure(){
		return structure;
	}

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
    	String ret;
		ret = compoundName + " (" + compoundFormula + 
			") CID" + compoundID + " identification method: " + identificationMethod;
        return ret;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object value) {
        
        if (value == UNKNOWN_IDENTITY) return 1;
        
        PeakIdentity identityValue = (PeakIdentity) value;
        String valueName = identityValue.getName();
        if (valueName == null) return 1;
        return valueName.compareTo(compoundName);
    }
}
