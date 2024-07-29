package com.iwedia.cltv.platform.t56;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.iwedia.cltv.platform.model.Constants;
import com.iwedia.cltv.platform.model.eas.EasEventInfo;
import com.iwedia.cltv.platform.model.information_bus.events.Events;
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus;
import com.iwedia.cltv.platform.model.eas.EasEventInfo;
import com.iwedia.cltv.platform.model.information_bus.events.Events;
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus;
import com.mediatek.twoworlds.tv.MtkTvEASBase;
import com.mediatek.twoworlds.tv.model.MtkTvEasEventInfoBase;

import java.util.ArrayList;

public class EasControl {
    private final MtkTvEasEventInfoBase mEventInfo;

    public EasControl(MtkTvEasEventInfoBase eventInfo) {
        this.mEventInfo = eventInfo;
        TvCallbackHandler.getInstance()
                .addCallBackListener(TvCallbackConst.MSG_CB_EAS_MSG, baseHandler);
    }


    public final Handler baseHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            TvCallbackData data = (TvCallbackData) msg.obj;
            if (data == null) {
                return;
            }
            if ((msg.what == msg.arg1)
                    && (msg.what == msg.arg2)
                    && ((msg.what & TvCallbackConst.MSG_CB_BASE_FLAG) != 0)) {
                handleCallbackMsg(msg);
            }
        }
    };

    private void handleCallbackMsg(Message msg) {
        TvCallbackData data = (TvCallbackData) msg.obj;
        if (msg.what == TvCallbackConst.MSG_CB_EAS_MSG) {
            ArrayList<Object> list = new ArrayList<>();
            Log.d(Constants.LogTag.CLTV_TAG + "EasControl", "EAS object created.... ");
            MtkTvEASBase eas = new MtkTvEASBase();
            if (eas != null) {
                list.add(data.param1);
                MtkTvEasEventInfoBase eventInfo = eas.getEasEventDataInfo();
                if (eventInfo != null) {
                    list.add(new EasEventInfo(eventInfo.getAlertText(),
                            eventInfo.getActivationText(),
                            eventInfo.getIsAtsc3(),
                            "",false
                            ));
                }
            }
            InformationBus.informationBusEventListener.submitEvent(
                    Events.IS_EAS_PLAYING, list);
        }
    }

    public void removeCallback() {
        TvCallbackHandler.getInstance()
                .removeCallBackListener(TvCallbackConst.MSG_CB_EAS_MSG, baseHandler);
    }
}

