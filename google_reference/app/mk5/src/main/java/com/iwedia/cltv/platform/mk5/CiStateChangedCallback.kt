package com.iwedia.cltv.platform.mk5

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.iwedia.cltv.platform.model.Constants
import com.mediatek.twoworlds.tv.MtkTvCI
import com.mediatek.twoworlds.tv.MtkTvUtil
import com.mediatek.twoworlds.tv.common.MtkTvCIMsgTypeBase
import com.mediatek.twoworlds.tv.model.MtkTvCIMMIEnqBase
import com.mediatek.twoworlds.tv.model.MtkTvCIMMIMenuBase
import com.mediatek.wwtv.tvcenter.util.SaveValue

class CiStateChangedCallback(context: Context) {
    private val TAG =  /*"CiStateChangedCallback"*/"LAZA"

    private var mCIState: CiStateChangedCallback? = null
    private var mContext: Context? = null
    var regStatus = false

    // CI Data
    // default slot_id is 0, current support only one slot accroid to ci spec
    private var slot_id = 0
    private var mCiName: String? = null
    private var mCi: MtkTvCI? = null
    private var menu: MtkTvCIMMIMenuBase? = null
    private var enquiry: MtkTvCIMMIEnqBase? = null

    // Handler
    private var mSaveValue: SaveValue? = null
    var insertOrRemove = -1 // -1:normal,0:insert,2:remove

    private var mData: TvCallbackData? = null

    // cam upgrade status,
    // 0: not upgrade
    // 1: receive upgrade message
    // 2: press enter and upgrading
    private var camUpgrade = 0

    // b_is_list_obj
    private var bListObj = false

    //    private CIPinCodeDialog pincodedialog;
    private var isMMICloseMsg = false

    // check after pin code input reply type
    enum class CIPinCodeReplyType {
        CI_PIN_BAD_CODE, CI_PIN_CICAM_BUSY, CI_PIN_CODE_CORRECT, CI_PIN_CODE_UNCONFIRMED, CI_PIN_BLANK_NOT_REQUIRED, CI_PIN_CONTENT_SCRAMBLED
    }

    init {
        mContext = context
        mSaveValue = SaveValue.getInstance(mContext)
        sendIRControl(0)
//        mSaveValue.saveValue(CommonIntegration.camUpgrade,0)
    }

//    public void setPinCodeDialog(CIPinCodeDialog dialog) {
//        pincodedialog = dialog;
//    }

//    public CIPinCodeDialog getPinCodeDialog() {
//        return pincodedialog;
//    }

    //    public void setPinCodeDialog(CIPinCodeDialog dialog) {
    //        pincodedialog = dialog;
    //    }
    //    public CIPinCodeDialog getPinCodeDialog() {
    //        return pincodedialog;
    //    }
    fun setCIClose() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isMMICloseMsg: $isMMICloseMsg")
        if (getCIHandle() != null && !isMMICloseMsg) {
            mCi!!.setMMIClose()
        }
    }

    @Synchronized
    fun getInstance(context: Context): CiStateChangedCallback? {
        if (null == mCIState) {
            mCIState = CiStateChangedCallback(context)
        }
        return mCIState
    }


    fun handleCiCallback(megSrc: Context?, data: TvCallbackData, listener: CIMenuUpdateListener?) {
        Log.d(Constants.LogTag.CLTV_TAG + "LAZA", "handleCiCallback, " + data.param2)
        try {
            if (data.param1 != slot_id) {
                slot_id = data.param1
            }
            when (data.param2) {
                MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_CARD_INSERT -> {
                    if (data.param1 != slot_id) {
                        slot_id = data.param1
                    }
                    insertOrRemove = MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_CARD_INSERT
                }

                MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_CARD_NAME -> {}
                MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_CARD_REMOVE -> {
                    slot_id = 0
                    insertOrRemove = MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_CARD_REMOVE
                    menu = null
                    enquiry = null
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "when card remove set upgrade to 0")
                    camUpgrade = 0 //when remove set to 0
                    sendIRControl(0)
                    //                    mSaveValue.saveValue(CommonIntegration.camUpgrade,camUpgrade);
                    listener?.ciRemoved()
                }

                MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_MMI_ENQUIRY -> {
                    enquiry = data.paramObj2 as MtkTvCIMMIEnqBase
                    //                    CIMainDialog.resetTryCamScan();
                    listener?.enqReceived(enquiry)
                }

                MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_MMI_MENU, MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_MMI_LIST -> {
                    /*
	          CIMainDialog mCiMainDialog = (CIMainDialog) ComponentsManager.getInstance()
	          .getComponentById(NavBasic.NAV_COMP_ID_CI_DIALOG);
	          if (mCiMainDialog == null || !mCiMainDialog.isVisible()) {
	            CIMainDialog.setNeedShowInfoDialog(false);
	            break;
	          }
        	  */isMMICloseMsg = false //reset
                    bListObj = if (data.param2 == MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_MMI_LIST) {
                        true
                    } else {
                        false
                    }
                    menu = data.paramObj1 as MtkTvCIMMIMenuBase
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "scube, menu=" + data.paramObj1 as MtkTvCIMMIMenuBase)
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "listener:$listener")
                    listener?.menuReceived(menu)
                    if (bListObj) {
                        // here can send upgrade progress
                        if (menu!!.title != null && menu!!.title.contains("Upgrade")
                            && menu!!.title.contains("Test")
                        ) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "CI upgrade begin to send upgrade progress")
                            val tdata = TvCallbackData()
                            tdata.param2 =
                                MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_FIRMWARE_UPGRADE_PROGRESS
                            handleCiCallback(megSrc, tdata, listener)
                        } else {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "CI menu title ==" + menu!!.title)
                        }
                    }
                }

                MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_MMI_CLOSE -> {
                    if (data.param1 != slot_id) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "MTKTV_CI_NFY_COND_MMI_CLOSE, " + data.param1 + "," + slot_id)
                        return  //DTV00612468
                    }
                    if (getCIHandle() != null) {
                        mCi!!.setMMICloseDone()
                        isMMICloseMsg = true
                        listener?.menuEnqClosed()
                        menu = null
                        enquiry = null
                    }
                }

                MtkTvCIMsgTypeBase.MTKTV_CI_CAM_SCAN_ENQ_WARNING, MtkTvCIMsgTypeBase.MTKTV_CI_CAM_SCAN_ENQ_URGENT, MtkTvCIMsgTypeBase.MTKTV_CI_CAM_SCAN_ENQ_NOT_INIT, MtkTvCIMsgTypeBase.MTKTV_CI_CAM_SCAN_ENQ_SCHEDULE -> listener!!.ciCamScan(
                    data.param2
                )

                MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_FIRMWARE_UPGRADE, MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_FIRMWARE_UPGRADE_PROGRESS -> if (MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_FIRMWARE_UPGRADE == data.param2) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "reday to upgrade")
                    camUpgrade = 1
                    sendIRControl(1)
                    //                        mSaveValue.saveValue(CommonIntegration.camUpgrade,camUpgrade);
                } else {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "upgrade progressing")
                    camUpgrade = 2
                    sendIRControl(2)
                    //                        mSaveValue.saveValue(CommonIntegration.camUpgrade,camUpgrade);
                }

                MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_FIRMWARE_UPGRADE_COMPLETE, MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_FIRMWARE_UPGRADE_ERROR -> {
                    camUpgrade = 0
                    sendIRControl(0)
                }

                MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_PIN_REPLY -> checkReplyValue(data.param3)
                else -> {}
            }
            com.mediatek.wwtv.tvcenter.util.Constants.slot_id = slot_id
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    /**
     * [MTK Internal] This API is for MTK control IR use only.
     * IR Remote related features
     * @value      case 0: Ignore All IR Key except KEY_UP and KEY_DOWN
     * case 1: Ignore All IR Key
     * case 2: Ignore All IR except system Key
     * case 3: Restart IR Key
     * case 7: Ignore All IR Key except Power IR Key
     */
    private fun sendIRControl(mCIStatusInt: Int) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "sendIRControl||mCIStatusInt =: $mCIStatusInt")
        when (mCIStatusInt) {
            0 -> MtkTvUtil.IRRemoteControl(3)
            1 -> MtkTvUtil.IRRemoteControl(3)
            2 -> MtkTvUtil.IRRemoteControl(7)
            else -> MtkTvUtil.IRRemoteControl(3)
        }
    }

    fun camUpgradeStatus(): Boolean {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "camUpgradeStatus, camUpgrade $camUpgrade")
        return camUpgrade == 2
    }

    fun setCamUpgrade(camUpgradeForPowerOff: Int) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "setCamUpgrade, camUpgradeForPowerOff $camUpgradeForPowerOff")
        camUpgrade = camUpgradeForPowerOff
        //        mSaveValue.saveValue(CommonIntegration.camUpgrade,camUpgrade);
        sendIRControl(camUpgradeForPowerOff)
    }

    /**
     * this method is used to get CI handle
     *
     * @return instance of MtkTvCI
     */
    fun getCIHandle(): MtkTvCI? {
        mCi = if (slot_id == -1) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getCIHandle, null")
            null
        } else {
            MtkTvCI.getInstance(slot_id)
        }
        return mCi
    }

    /**
     * this method is used to get cam name
     *
     * @return
     */
    fun getCIName(): String? {
        mCiName = if (getCIHandle() != null) {
            mCi!!.camName
        } else {
            ""
        }
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getCIName, name=$mCiName")
        return mCiName
    }

    /**
     * this method is used to get MMIMenu
     *
     */
    fun getMtkTvCIMMIMenu(): MtkTvCIMMIMenuBase? {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getMtkTvCIMMIMenuBase, menu=$menu")
        return menu
    }

    /**
     * this method is used to get MMIEnq
     *
     */
    fun getMtkTvCIMMIEnq(): MtkTvCIMMIEnqBase? {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getMtkTvCIMMIEnqBase, enquiry=$enquiry")
        return enquiry
    }

    /**
     * this method is used to select menu item
     *
     * @param num
     */
    fun selectMenuItem(num: Int) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "selectMenuItem, num=" + num + "  " + (menu == null))
        if (null != getCIHandle() && menu != null) {
            mCi!!.setMenuAnswer(menu!!.mmiId, if (bListObj) 0 else num + 1)

            /*      if (1 == camUpgrade) {
        camUpgrade = 2;
        mSaveValue.saveValue(CommonIntegration.camUpgrade,camUpgrade);
      }*/
        }
    }

    fun getMenu(): MtkTvCIMMIMenuBase? {
        if (menu == null) {
            Log.d(Constants.LogTag.CLTV_TAG + "CI+", "menu == null")
        }
        return menu
    }

    /**
     * this method is used to answer enquiry
     *
     * @param bAnswer
     * @param data
     */
    fun answerEnquiry(bAnswer: Int, data: String?) {
        if (null != getCIHandle()) {
            mCi!!.setEnqAnswer(getMtkTvCIMMIEnq()!!.mmiId, bAnswer, data)
        }
    }

    /**
     * this method is used to get Ans Len
     *
     * @return
     */
    fun getAnsTextLen(): Byte {
        if (enquiry == null) {
            return -1
        }
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAnsTextLen, enquiry=$enquiry")
        return enquiry!!.ansTextLen
    }

    /**
     * this method is used to check blindans
     *
     * @return
     */
    fun isBlindAns(): Boolean {
        if (enquiry == null) {
            return false
        }
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isBlindAns, enquiry=" + enquiry + ",getBlindAns>> " + enquiry!!.blindAns)
        return enquiry!!.blindAns
    }

    /**
     * this method is used to cancel curr Menu
     */
    fun cancelCurrMenu(): Int {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "cancelCurrMenu, menu=$menu")
        return if (null != getCIHandle() && menu != null) {
            mCi!!.setMenuAnswer(menu!!.mmiId, 0)
        } else 0
    }

    /**
     * this method is used to check whether cam is active or not
     *
     * @return
     */
    fun isCamActive(): Boolean {
        var active = false
        if (null != mCi) {
            active = mCi!!.slotActive
        }
        return active
    }

    fun getReqShowData(): TvCallbackData? {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getReqShowData")
        return mData
    }

    fun setReqShowData(data: TvCallbackData) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "setReqShowData")
        if (mData == null) {
            mData = TvCallbackData()
        }
        mData = data
    }

    private fun checkReplyValue(ret: Int) {
        val type: CIPinCodeReplyType = CiStateChangedCallback.CIPinCodeReplyType.values().get(ret)
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "CIPinCodeReplyType is $type")
        when (type) {
            CIPinCodeReplyType.CI_PIN_CODE_CORRECT -> //                if (pincodedialog != null && pincodedialog.isShowing()) {
//                    pincodedialog.dismiss();
//                }
                Toast.makeText(mContext, "Correct!", Toast.LENGTH_LONG).show()

            CIPinCodeReplyType.CI_PIN_CODE_UNCONFIRMED, CIPinCodeReplyType.CI_PIN_CONTENT_SCRAMBLED, CIPinCodeReplyType.CI_PIN_CICAM_BUSY -> {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "these 3 type do nothing")
                Toast.makeText(mContext, "Some invalid type!", Toast.LENGTH_LONG).show()
            }

            CIPinCodeReplyType.CI_PIN_BAD_CODE -> {}
            CIPinCodeReplyType.CI_PIN_BLANK_NOT_REQUIRED -> Log.d(Constants.LogTag.CLTV_TAG + TAG, "do nothing")
            else -> {}
        }
    }

    // Interfaces
    interface CIMenuUpdateListener {
        fun showCiPopup()
        fun closeCiPopup()
        fun enqReceived(enquiry: MtkTvCIMMIEnqBase?)
        fun menuReceived(menu: MtkTvCIMMIMenuBase?)
        fun menuEnqClosed()
        fun ciRemoved()
        fun ciCamScan(message: Int)
    }
}