package com.iwedia.cltv.platform.t56;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.mediatek.twoworlds.tv.MtkTvTVCallbackHandler;

public class ClosedCaptionCallback extends MtkTvTVCallbackHandler {

    private ClosedCaptionInterfaceImpl closedCaptionInterfaceImpl;
    public ClosedCaptionCallback(ClosedCaptionInterfaceImpl closedCaptionInterfaceImpl) {
        this.closedCaptionInterfaceImpl = closedCaptionInterfaceImpl;
        TvCallbackHandler.getInstance()
                .addCallBackListener(TvCallbackConst.MSG_CB_BANNER_MSG, baseHandler);
    }

    public final Handler baseHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == TvCallbackConst.MSG_CB_BANNER_MSG) {
                TvCallbackData specialMsgData = (TvCallbackData) msg.obj;
                if (specialMsgData.param1 == 1 &&
                        (specialMsgData.param2 == 16 || specialMsgData.param2 == 27 ||
                                specialMsgData.param2 == 33 || specialMsgData.param2 == 34)) {
                    if (closedCaptionInterfaceImpl != null) {
                        closedCaptionInterfaceImpl.isCCTrackAvailable();
                    }
                }
            }
        }
    };

    public void removeCallback() {
        TvCallbackHandler.getInstance()
                .removeCallBackListener(TvCallbackConst.MSG_CB_BANNER_MSG, baseHandler);
    }

}
