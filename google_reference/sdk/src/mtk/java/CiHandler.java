import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import com.mediatek.twoworlds.tv.common.MtkTvCIMsgTypeBase;

public class CiHandler {
    private Context mContext;
    private CIStateChangedCallBack.CIMenuUpdateListener ciMenuUpdateListener;
    private CIStateChangedCallBack mCIState;
    private final static int SELECTED_CHANNEL_CAM_SCAN = 20;


    public CiHandler(Context context) {
        this.mContext = context;
        TvCallbackHandler.getInstance()
                .addCallBackListener(TvCallbackConst.MSG_CB_CI_MSG, mHandler);
        mCIState = CIStateChangedCallBack.getInstance(context);
    }

    public void setListener(CIStateChangedCallBack.CIMenuUpdateListener listener) {
        this.ciMenuUpdateListener = listener;
    }

    public final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            TvCallbackData data = (TvCallbackData) msg.obj;
            if (data == null) {
                Log.d(Constants.LogTag.CLTV_TAG + "LAZA", "msg data null");
                return;
            }
            handleCIMessage(data);
        }
    };


    public void handleCIMessage(TvCallbackData data) {

        Log.d(Constants.LogTag.CLTV_TAG + "LAZA", "HANDLE CI MESSAGE");


        if (data != null && mContext != null) {
            // every message cancel 0xF5 handle
            Log.d(Constants.LogTag.CLTV_TAG + "LAZA", "handleCiMessage, data is not null");
            CIStateChangedCallBack.getInstance(mContext).handleCiCallback(mContext, data, ciMenuUpdateListener);
        } else {
            Log.d(Constants.LogTag.CLTV_TAG + "LAZA", "handleCIMessage, data is null!!!");
            return;
        }

        int curDensity = (int) mContext.getResources().getDisplayMetrics().density;
        if (data.param2 == 1) {
            Log.d(Constants.LogTag.CLTV_TAG + "LAZA", "data.param2 == 1");
            Toast toast1 = Toast.makeText(mContext, "INSERTED CARD", Toast.LENGTH_SHORT);
            toast1.setGravity(Gravity.BOTTOM | Gravity.END, 20 * curDensity, 20 * curDensity);
            toast1.show();
        } else if (data.param2 == 2) {
            Log.d(Constants.LogTag.CLTV_TAG + "LAZA", "data.param2 == 2");
            Toast toast2 = Toast.makeText(mContext, "REMOVED CARD", Toast.LENGTH_SHORT);
            toast2.setGravity(Gravity.BOTTOM | Gravity.END, 20 * curDensity, 20 * curDensity);
            toast2.show();
        }


        boolean needShowInfoDialog = false;
        if (data.param2 == MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_MMI_ENQUIRY ||
                data.param2 == MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_MMI_MENU ||
                data.param2 == MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_MMI_LIST) {
            Log.d(Constants.LogTag.CLTV_TAG + "LAZA", "handleCIMessage MTKTV_CI_NFY_COND_MMI_ENQUIRY/MTKTV_CI_NFY_COND_MMI_MENU/MTKTV_CI_NFY_COND_MMI_LIST");
            needShowInfoDialog = true;
        }
        Log.d(Constants.LogTag.CLTV_TAG + "LAZA", "handleCIMessage needShowInfoDialog: " + needShowInfoDialog);

        if (data.param2 == MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_PROFILE_SEARCH_CANCELED
                || data.param2 == MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_PROFILE_SEARCH_ENDED) {
            Log.d(Constants.LogTag.CLTV_TAG + "LAZA", "handleCIMessage:select channel start:");
            mHandler.removeMessages(SELECTED_CHANNEL_CAM_SCAN);
            mHandler.sendEmptyMessageDelayed(SELECTED_CHANNEL_CAM_SCAN, 1000);
        }
    }

    public CIStateChangedCallBack getmCIState() {
        return mCIState;
    }
}


