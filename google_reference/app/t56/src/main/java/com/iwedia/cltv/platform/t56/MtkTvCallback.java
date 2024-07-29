package com.iwedia.cltv.platform.t56;

import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.iwedia.cltv.platform.model.Constants;
import com.mediatek.twoworlds.tv.MtkTvTVCallbackHandler;

//Because this is MTK you can't create callback in Kotlin and send it to SDK
public class MtkTvCallback extends MtkTvTVCallbackHandler {
    private static final String TAG = "MtkTvCallback";



    interface HbbTvHandleInterface {
        void handleHbbTvMessage(Integer type, Integer message);
        void notifyNoUsedKeyMessage(int updateType, int argv1, int argv2, long argv3);
    }

    interface TtxTvHandleInterface {
        void ttxStateChanged(TTXInterfaceImpl.TTXState newState);
    }

    private static MtkTvCallback instance = null;

    public static MtkTvCallback getInstance() {
        if(instance == null) {
            instance = new MtkTvCallback();
        }
        return instance;
    }

    private MtkTvCallback() {

    }

    HbbTvHandleInterface mHbbTvHandleInterface = null;
    TtxTvHandleInterface mTtxTvHandleInterface = null;

    public int notifyHBBTVMessage(int callbackType, int[] callbackData, int callbackDataLen){
        Log.d(Constants.LogTag.CLTV_TAG + TAG,"notifyHBBTVMessage callbackType->"+callbackType);
        Message msg = Message.obtain();
        //msg
        int message = 0;
        try{
            message = callbackData[0];
        }
        catch(Exception ex){
            ex.printStackTrace();
        }

        if(mHbbTvHandleInterface != null) {
            mHbbTvHandleInterface.handleHbbTvMessage(callbackType, message);
        }

        return 0;
    }

    public int notifyNoUsedkeyMessage(int updateType, int argv1, int argv2, long argv3){
        if(mHbbTvHandleInterface != null) {
            mHbbTvHandleInterface.notifyNoUsedKeyMessage(updateType, argv1, argv2, argv3);
        }
        return 0;
    }

    void registerHbbTvHandleInterface(HbbTvHandleInterface hbbTvInterface) {
        mHbbTvHandleInterface = hbbTvInterface;
    }

    void registerTTXHandleInterface(TtxTvHandleInterface ttxTvHandleInterface) {
        mTtxTvHandleInterface = ttxTvHandleInterface;
    }

    public int notifyTeletextMessage(int msg_id, int argv1, int argv2, int argv3) {
        int sRv = 0;
        Log.i(TAG, "notifyTeletextMessage: msg_id ="+msg_id);
        switch (msg_id) {
            case 2:
                mTtxTvHandleInterface.ttxStateChanged(TTXInterfaceImpl.TTXState.TTX_ACTIVE);
                break;
            case 3:
                mTtxTvHandleInterface.ttxStateChanged(TTXInterfaceImpl.TTXState.TTX_INACTIVE);
                break;
            case 1:
            case 4:
                mTtxTvHandleInterface.ttxStateChanged(TTXInterfaceImpl.TTXState.NO_TTX);
                break;
        }
        try {
            sRv = super.notifyTeletextMessage(msg_id, argv1, argv2, argv3);
        } catch (Exception ex) {

        }

        return sRv;
    }

}
