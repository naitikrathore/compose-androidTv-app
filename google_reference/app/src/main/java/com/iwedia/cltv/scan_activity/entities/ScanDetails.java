package com.iwedia.cltv.scan_activity.entities;

/**
 * Scan details entity
 *
 * @author Dragan Krnjaic
 */
public class ScanDetails {

    /**
     * Scan type
     */
    private String scanType;

    /**
     * Tv programmes number
     */
    private int tvProgrammesNumber;

    /**
     * Radio programmes number
     */
    private int radioProgrammesNumber;

    /**
     * Constructor
     *
     * @param scanType scan type
     * @param tvProgrammesNumber tv programmes number
     * @param radioProgrammesNumber radio programmes number
     */
    public ScanDetails(String scanType, int tvProgrammesNumber, int radioProgrammesNumber) {
        this.scanType = scanType;
        this.tvProgrammesNumber = tvProgrammesNumber;
        this.radioProgrammesNumber = radioProgrammesNumber;
    }

    public String getScanType() {
        return scanType;
    }

    public int getTvProgrammesNumber() {
        return tvProgrammesNumber;
    }

    public int getRadioProgrammesNumber() {
        return radioProgrammesNumber;
    }
}
