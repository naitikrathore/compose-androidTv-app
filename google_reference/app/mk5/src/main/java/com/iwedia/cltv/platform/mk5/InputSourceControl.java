package com.iwedia.cltv.platform.mk5;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.iwedia.cltv.platform.model.Constants;
import com.iwedia.cltv.platform.model.information_bus.events.Events;
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus;
import com.iwedia.cltv.platform.model.information_bus.events.Events;
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus;
import com.mediatek.twoworlds.tv.model.MtkTvVideoInfoBase;

import java.util.ArrayList;

public class InputSourceControl {

    public InputSourceControl() {
        TvCallbackHandler.getInstance()
                .addCallBackListener(TvCallbackConst.MSG_CB_VIDEO_INFO_MSG, baseHandler);
    }


    public final Handler baseHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            TvCallbackData data = (TvCallbackData) msg.obj;
            if (data == null) {
                return;
            }
            handleCallbackMsg(msg);
        }
    };

    private void handleCallbackMsg(Message msg) {
        TvCallbackData data = (TvCallbackData) msg.obj;
        if (msg.what == TvCallbackConst.MSG_CB_VIDEO_INFO_MSG) {
            if (data.param1 == MtkTvVideoInfoBase.VIDEOINFO_NFY_TYPE_HDR && data.param2 == MtkTvVideoInfoBase.VIDEOINFO_HDR_COND_CHG) {
                ArrayList<Object> list = new ArrayList<>();
                Log.d(Constants.LogTag.CLTV_TAG + "InputSourceControl", "InputSourceControl handle message ");

                list.add(data.param3);
                list.add(data.param4);
                try {
                    InformationBus.informationBusEventListener.submitEvent(
                            Events.GET_INPUT_RESOLUTION_DATA, list);
                } catch (Exception ex) {
                    Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +"InputSourceControl","Failed to send intent:"+ex.getMessage());
                }
            }
        }
    }

    public void removeCallback() {
        TvCallbackHandler.getInstance()
                .removeCallBackListener(TvCallbackConst.MSG_CB_VIDEO_INFO_MSG, baseHandler);
    }
}

