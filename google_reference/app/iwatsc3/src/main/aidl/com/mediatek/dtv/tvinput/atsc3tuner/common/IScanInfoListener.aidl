
package com.mediatek.dtv.tvinput.atsc3tuner.common;
import android.os.Bundle;
interface IScanInfoListener {
    /**
     * Notify the scan progress.
     *
     * <p><em><b>Detail</b></em>
     * The timing of notification is defined for each broadcasting standard.
     *
     * <p>Pre-condition
     * <p>- Scan is running.
     *
     * <p>Post-condition
     * <p>- Must be in the same state as Pre-Condition.
     *
     * @param progressParams Scan progress status defined for each broadcast standard
     *-------------------------*
     * KEY_SCAN_PROGRESS (int) 
     * KEY_EVENT_TYPE_TV_SERVICE_NUMBER (int) ==> Number of channels scanned so far
     *-------------------------*
     */
    void onScanProgress(in Bundle progressParams);

    /**
     * Notify the scan result.
     *
     * <p><em><b>Detail</b></em>
     * <p>Pre-condition
     * <p>One of the following conditions is met.
     * <p>- Scan has completed.
     * <p>- Scan cancellation processing by client request has completed.
     * <p>- Scan processing has failed at a trigger other than the client request.
     *
     * <p>Post-condition
     * <p>- Must be in the same state as Pre-Condition.
     *
     * @param resultParams Scan result status. (success, failure, etc.)<br>
     *                     {@link Constants#KEY_SCAN_RESULT} is mandatory key.
     *----------------------------------------------------*
     * KEY_SCAN_RESULT (int) -> 0 : failure 1 : success   *
     * if failure : KEY_ROUTE (int), KEY_BROADCAST (int)  *
     *----------------------------------------------------*
     */
    void onScanCompleted(in Bundle resultParams);

    /**
     * General purpose callback.
     *
     * <p><em><b>Detail</b></em>
     * <p>(T.B.D)
     * <p>TODO determine the spec.
     *
     * @param params (T.B.D)
     *---------------------------------------------------------------------------------------*
     * eventType :                                                                           *
     *           KEY_EVENT_TYPE_QUALITY = 0 : signalQualityChange,                           *
     *           KEY_EVENT_TYPE_STRENGTH = 1 : signalStrengthChange,                         *
     *           KEY_EVENT_TYPE_FREQ = 2 : scanTuneFrequencyChanged,                         *
     *           KEY_EVENT_TYPE_TV = 3 : scanTvServiceNumber,                                *
     *           KEY_EVENT_TYPE_RADIO = 4 : scanRadioServiceNumber                           *
     * params :                                                                              *
     *          KEY_SIGNAL_QUALITY (int), KEY_SIGNAL_STRENGTH (int), KEY_FREQ_CHANGED (int), *
     *          KEY_TV_SERVICE (int), KEY_RADIO_SERVICE (int)                                *
     *---------------------------------------------------------------------------------------*
     */
    void onEvent(int eventType, in Bundle params);
}
