package com.iwedia.cltv.scan_activity.entities;

/**
 * Satellite transponder entity
 *
 * @author Dejan Nadj
 */
public class SatelliteTransponder {

    /**
     * Polarization enumeration
     */
    public enum Polarization {
        VERTICAL("Vertical"),
        HORIZONTAL("Horizontal");

        private String text;
        Polarization(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    /**
     * Tyning type enumeration
     */
    public enum TyningType {
        DVB_S("DVB-S"),
        DVB_S2("DVB-S2");

        private String text;
        TyningType(String text) {
            this.text = text;
        }

        public String getText() {
            return text;
        }
    }

    /**
     * Frequency
     */
    private int frequency;

    /**
     * Symbol rate
     */
    private int symbolRate;

    /**
     * Polarization
     */
    private Polarization polarization;

    /**
     * Tyning type
     */
    private TyningType tyningType;

    /**
     * Constructor
     *
     * @param frequency
     * @param symbolRate
     * @param polarization
     * @param tyningType
     */
    public SatelliteTransponder(int frequency, int symbolRate, Polarization polarization, TyningType tyningType) {
        this.frequency = frequency;
        this.symbolRate = symbolRate;
        this.polarization = polarization;
        this.tyningType = tyningType;
    }

    /**
     * Get frequency
     * @return  frequency
     */
    public int getFrequency() {
        return frequency;
    }

    /**
     * Get symbol rate
     * @return  symbol rate
     */
    public int getSymbolRate() {
        return symbolRate;
    }

    /**
     * Get polarization
     * @return  polarization
     */
    public Polarization getPolarization() {
        return polarization;
    }

    /**
     * Get tyning type
     * @return  tyning type
     */
    public TyningType getTyningType() {
        return tyningType;
    }

    @Override
    public String toString() {
        String p = polarization == Polarization.VERTICAL ? "V" : "H";
        return frequency + "," + p + "," + symbolRate;
    }
}
