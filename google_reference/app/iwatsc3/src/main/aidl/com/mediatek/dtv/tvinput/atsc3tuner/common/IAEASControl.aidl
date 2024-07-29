package com.mediatek.dtv.tvinput.atsc3tuner.common;

import android.os.Bundle;
import com.mediatek.dtv.tvinput.atsc3tuner.common.IAEASAlertCallback;

/*
	Defines the interface to register callbacks for A-EAS alert notification.
*/

interface IAEASControl{

    /**
    * Registers the callback listener to be invoked by TIS to send A-EAS alert to client.
    *  @return
    *	id (int) - which is used to unregister the callback
    */
    int registerAEASAlert(IAEASAlertCallback callback);

    /**
    * Unregisters the callback A-EASAlertCallback
    * @param
    * id(int) : id of registered callback .
    */
    void unregisterAEASAlert(int id);

    /**
    * Sets the AEAT filter for audience and Priority
    * @param
    * iaudience(int) : Audience value of AEAT for controllled access.
    * priority(int):  Priority level of AEAT
    */
    void setAEASFilter(int audience, int priority);

    /**
    * Sets the AEAT filter for audience and Priority
    * @param none
    * @retun Bundle
    * audience(int) : Audience value of AEAT for controllled access.
    * priority(int):  Priority level of AEAT
    */
    Bundle getAEASFilter();
}
