package com.mediatek.dtv.tvinput.atsc3tuner.common;

import com.mediatek.dtv.tvinput.atsc3tuner.common.IIntrAppStatusCallback;


/*
    Support AIDL Interface for iWedia TV Input Service.
*/

interface IIntrAppControl{

    /**
    *    Registers the callback listener to be invoked by TIS to send Teatro Status to App.
    *  @return
    *    id (int) - which is used to unregister the callback
    */
    int registerIntrAppStatusCallback(IIntrAppStatusCallback callback);
    /**
    * Unregisters the TeatroStatusCallback.
    * @param
    * id(int) : id of registered callback .
    */
    IIntrAppStatusCallback unregisterIntrAppStatusCallback(int id);
    /**
     * Sets true if MENU UI has focus
    * @param
    * isUiMenuFocused(boolean) : MENU UI Focus value
    */
    void setUiMenuFocused(boolean isUiMenuFocused);
}
