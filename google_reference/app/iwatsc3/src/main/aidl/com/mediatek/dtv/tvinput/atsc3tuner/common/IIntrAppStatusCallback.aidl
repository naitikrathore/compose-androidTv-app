package com.mediatek.dtv.tvinput.atsc3tuner.common;
import android.os.Bundle;

/**
    Defines the callback invoked by TIS to inform browser status to App.
*/


interface IIntrAppStatusCallback{

    /*
        Using this callback , TIS sends the teatro browser status to App.
        @param teatro_status - true means browser window is up, false means browser window is down
    */
    void onReceiveIntrAppStatus(in boolean status);

    /*
        Using this callback , TIS sends the teatro browser KEYLIST to App.
        @param bundleAcquiredKeys - bundle containing ArrayList<Integer> KEYLIST
    */
    void onReceiveIntrAppKeyList(in Bundle bundleAcquiredKeys);
}
