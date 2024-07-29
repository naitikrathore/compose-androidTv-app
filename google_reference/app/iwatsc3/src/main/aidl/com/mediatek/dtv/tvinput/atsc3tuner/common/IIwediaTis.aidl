package com.mediatek.dtv.tvinput.atsc3tuner.common;

import android.os.Bundle;


interface IIwediaTis {

    /**
     * Gets binder object  for  calling APIs for  SCAN/CC/PrefLang/Clock.
     *
     @param
            type : int , type of binder , Refer IDefines.aidl
     */

    IBinder queryBinder(int type);

    /**
     * Fetches if Atsc 3.0 is supported
     * return : true => Supported, false => not supported
     */

    boolean isAtsc3Supported();

     /**
     * For fetching Signal Information
     * returns Bundle :-
     * IDefines.INFO_SIGNAL_QUALITY => type : long
     * IDefines.INFO_SIGNAL_STRENGTH => type : long
     * IDefines.INFO_BER => type : long
     * IDefines.INFO_SYMBOL_RATE => type : long
     * IDefines.INFO_UEC => type : long
     * IDefines.INFO_AGC => type : long
     */

    Bundle getSignalParameters();

    /**
     * Toggle Audio/Video Description service status.
     * @param
     * int type => IDefines.TYPE_AUDIO_DESC , IDefines.TYPE_VIDEO_DESC
     *  int status => IDefines.DESC_SRVC_ENABLE (1) , IDefines.DESC_SRVC_DISABLE (0)
     */
    void setAudioProfileStatus(int type, int status);

    int getAudioProfileStatus(int type);


}
