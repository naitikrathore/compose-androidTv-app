package com.mediatek.dtv.tvinput.atsc3tuner.common;

import com.mediatek.dtv.tvinput.atsc3tuner.common.IScanInfoListener;
import android.os.Bundle;

interface IScanSession {
 
    /**
     * Implement to set scan listener to TIS.
     *
     */
    void setScanListener(in IScanInfoListener listener);

    /**
     * Start scan with specified parameters.
     *
     * <p><em><b>Detail</b></em>
     * <p>Notify the progress and result of scan with IScanListener.
     *
     * <p>Pre-condition
     * <p>- The scan session has been generated.
     * <p>- Scan is not running.
     * <p>- Temporary held channel list is not being stored to tv.db.
     *
     * <p>Post-condition
     * <p>- Accepts execution request for starting scan.<br>
     * <br>
     * @param scanParams Parameters defined for each broadcast standards.
     */
    void startScan(in Bundle param);

    /**
     * Cancel the running scan.
     *
     * <p><em><b>Detail</b></em>
     * <p>Notify the scan result (canceled) with IScanListener.
     *
     * <p>Pre-condition
     * <p>- The scan session has been generated.
     *
     * <p>Post-condition
     * <p>- Accepts execution request for canceling scan.
     *
     * <p><em><b>Note</b></em>
     * <p>Don't delete the channel list that is already stored.
     *
     */
    void cancelScan();  
}
