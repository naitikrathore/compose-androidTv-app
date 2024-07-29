package com.iwedia.cltv.platform.mk5

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.Gravity
import android.widget.Toast
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.information_bus.events.InformationBus
import com.iwedia.cltv.platform.model.information_bus.events.Events
import com.mediatek.twoworlds.tv.common.MtkTvCIMsgTypeBase

class CiHandler(context: Context) {
    private var mContext: Context? = null
    private var ciMenuUpdateListener: CiStateChangedCallback.CIMenuUpdateListener? = null
    private var mCIState: CiStateChangedCallback? = null
    private val SELECTED_CHANNEL_CAM_SCAN = 20

    private val CODE_AUDIO_ONLY_SVC = 20
    private val CODE_VIDEO_ONLY_SVC = 21
    private val CODE_AUDIO_VIDEO_SVC = 22
    private val CODE_SCRAMBLED_AUDIO_VIDEO_SVC = 23
    private val CODE_SCRAMBLED_AUDIO_CLEAR_VIDEO_SVC = 24
    private val CODE_SCRAMBLED_AUDIO_NO_VIDEO_SVC = 25
    private val CODE_SCRAMBLED_VIDEO_CLEAR_AUDIO_SVC = 26
    private val CODE_SCRAMBLED_VIDEO_NO_AUDIO_SVC = 27

    var isClear = true

    val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val data = msg.obj as TvCallbackData
            if (data == null) {
                Log.d(Constants.LogTag.CLTV_TAG + "LAZA", "msg data null")
                return
            }
            handleCIMessage(data)
        }
    }

    val serviceStatusMsgHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            val data = msg.obj as TvCallbackData
            Log.d(Constants.LogTag.CLTV_TAG + "CiHandler", "scrambleStatusMsgHandler param=" + data.param1)
            when (data.param1) {
                CODE_AUDIO_ONLY_SVC, CODE_VIDEO_ONLY_SVC, CODE_AUDIO_VIDEO_SVC -> {
                    isClear = true

                    InformationBus.informationBusEventListener?.submitEvent(Events.SCRAMBLED_STATUS)
                }

                CODE_SCRAMBLED_AUDIO_VIDEO_SVC, CODE_SCRAMBLED_AUDIO_NO_VIDEO_SVC, CODE_SCRAMBLED_VIDEO_NO_AUDIO_SVC -> {
                    isClear = false
                    InformationBus.informationBusEventListener?.submitEvent(Events.SCRAMBLED_STATUS)
                }
            }
            return
        }
    }

    init {
        mContext = context
        TvCallbackHandler.getInstance()
            .addCallBackListener(TvCallbackConst.MSG_CB_CI_MSG, mHandler)
        TvCallbackHandler.getInstance()
            .addCallBackListener(TvCallbackConst.MSG_CB_SVCTX_NOTIFY, serviceStatusMsgHandler)
        mCIState = CiStateChangedCallback(context)
    }

    fun setListener(listener: CiStateChangedCallback.CIMenuUpdateListener?) {
        ciMenuUpdateListener = listener
    }


    fun handleCIMessage(data: TvCallbackData?) {
        Log.d(Constants.LogTag.CLTV_TAG + "LAZA", "HANDLE CI MESSAGE")
        if (data != null && mContext != null) {
            // every message cancel 0xF5 handle
            Log.d(Constants.LogTag.CLTV_TAG + "LAZA", "handleCiMessage, data is not null")
            CiStateChangedCallback(mContext!!)
                .handleCiCallback(mContext, data, ciMenuUpdateListener)
        } else {
            Log.d(Constants.LogTag.CLTV_TAG + "LAZA", "handleCIMessage, data is null!!!")
            return
        }
        val curDensity = mContext!!.resources.displayMetrics.density.toInt()
        if (data.param2 == 1) {
            Log.d(Constants.LogTag.CLTV_TAG + "LAZA", "data.param2 == 1")
            val toast1 = Toast.makeText(mContext, "INSERTED CARD", Toast.LENGTH_SHORT)
            toast1.setGravity(Gravity.BOTTOM or Gravity.END, 20 * curDensity, 20 * curDensity)
            toast1.show()
        } else if (data.param2 == 2) {
            Log.d(Constants.LogTag.CLTV_TAG + "LAZA", "data.param2 == 2")
            val toast2 = Toast.makeText(mContext, "REMOVED CARD", Toast.LENGTH_SHORT)
            toast2.setGravity(Gravity.BOTTOM or Gravity.END, 20 * curDensity, 20 * curDensity)
            toast2.show()

            //TODO LARA CHECK
            ciMenuUpdateListener!!.closeCiPopup()
        }
        var needShowInfoDialog = false
        println("%%%%%%% handleCIMessage " + data.param2)
        if (data.param2 == MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_MMI_ENQUIRY || data.param2 == MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_MMI_MENU || data.param2 == MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_MMI_LIST) {
            Log.d(Constants.LogTag.CLTV_TAG + 
                "LAZA",
                "handleCIMessage MTKTV_CI_NFY_COND_MMI_ENQUIRY/MTKTV_CI_NFY_COND_MMI_MENU/MTKTV_CI_NFY_COND_MMI_LIST"
            )
            needShowInfoDialog = true
        }
        if (needShowInfoDialog) {
            ciMenuUpdateListener!!.showCiPopup()
        }
        if (data.param2 == MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_MMI_CLOSE) {
            //TODO LARA CHECK
            ciMenuUpdateListener!!.closeCiPopup()
        }
        Log.d(Constants.LogTag.CLTV_TAG + "LAZA", "handleCIMessage needShowInfoDialog: $needShowInfoDialog")
        if (data.param2 == MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_PROFILE_SEARCH_CANCELED
            || data.param2 == MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_PROFILE_SEARCH_ENDED
        ) {
            Log.d(Constants.LogTag.CLTV_TAG + "LAZA", "handleCIMessage:select channel start:")
            mHandler.removeMessages(SELECTED_CHANNEL_CAM_SCAN)
            mHandler.sendEmptyMessageDelayed(SELECTED_CHANNEL_CAM_SCAN, 1000)
        }
    }

    fun getmCIState(): CiStateChangedCallback? {
        return mCIState
    }
}