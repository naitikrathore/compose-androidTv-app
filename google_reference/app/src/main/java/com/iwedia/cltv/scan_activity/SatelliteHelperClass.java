package com.iwedia.cltv.scan_activity;

import com.iwedia.cltv.scan_activity.entities.SatelliteTransponder;

/**
 * Satellite manual tuning helper class
 *
 * @author Dejan Nadj
 */
public class SatelliteHelperClass {

    /**
     * Satellite position in list
     */
    private static int satellitePosition;

    /**
     * Satellite name
     */
    private static String satelliteName;

    /**
     * Satellite transponder
     */
    private static SatelliteTransponder satelliteTransponder;

    /**
     * Selected transponder position
     */
    private static int selectedTransponderPos;

    /**
     * Set satellite position
     * @param position
     */
    public static void setSatellitePosition(int position) {
        satellitePosition = position;
    }

    /**
     * Set satellite name
     * @param name
     */
    public static void setSatelliteName(String name) {
        satelliteName = name;
    }

    /**
     * Get satellite position
     * @return
     */
    public static int getSatellitePosition() {
        return satellitePosition;
    }

    /**
     * Get satellite name
     * @return
     */
    public static String getSatelliteName(){
        return satelliteName;
    }

    /**
     * Get satellite transponder
     * @return
     */
    public static SatelliteTransponder getSatelliteTransponder() {
        return satelliteTransponder;
    }

    /**
     * Set satellite transponder
     * @param satelliteTransponder
     */
    public static void setSatelliteTransponder(SatelliteTransponder satelliteTransponder) {
        SatelliteHelperClass.satelliteTransponder = satelliteTransponder;
    }

    /**
     * Get selected transponder position
     * @return
     */
    public static int getSelectedTransponderPos() {
        return selectedTransponderPos;
    }

    /**
     * Set selected transponder position
     * @param selectedTransponderPos
     */
    public static void setSelectedTransponderPos(int selectedTransponderPos) {
        SatelliteHelperClass.selectedTransponderPos = selectedTransponderPos;
    }
}
