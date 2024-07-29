import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.mediatek.twoworlds.tv.MtkTvCI;
import com.mediatek.twoworlds.tv.MtkTvUtil;
import com.mediatek.twoworlds.tv.common.MtkTvCIMsgTypeBase;
import com.mediatek.twoworlds.tv.model.MtkTvCIMMIEnqBase;
import com.mediatek.twoworlds.tv.model.MtkTvCIMMIMenuBase;
import com.mediatek.wwtv.tvcenter.util.Constants;
import com.mediatek.wwtv.tvcenter.util.SaveValue;

public class CIStateChangedCallBack {
    private String TAG = /*"CIStateChangedCallBack"*/"LAZA";

    private static CIStateChangedCallBack mCIState = null;
    private Context mContext;
    boolean regStatus = false;
    // CI Data
    // default slot_id is 0, current support only one slot accroid to ci spec
    private int slot_id = 0;
    private String mCiName = null;
    private MtkTvCI mCi = null;
    private MtkTvCIMMIMenuBase menu = null;
    private MtkTvCIMMIEnqBase enquiry = null;
    // Handler
    private SaveValue mSaveValue;
    public int insertOrRemove = -1;// -1:normal,0:insert,2:remove
    private TvCallbackData mData = null;
    // cam upgrade status,
    // 0: not upgrade
    // 1: receive upgrade message
    // 2: press enter and upgrading
    private int camUpgrade = 0;
    // b_is_list_obj
    private boolean bListObj = false;

//    private CIPinCodeDialog pincodedialog;
    private boolean isMMICloseMsg = false;

    // check after pin code input reply type
    public enum CIPinCodeReplyType {
        CI_PIN_BAD_CODE, CI_PIN_CICAM_BUSY, CI_PIN_CODE_CORRECT, CI_PIN_CODE_UNCONFIRMED, CI_PIN_BLANK_NOT_REQUIRED, CI_PIN_CONTENT_SCRAMBLED
    }

    private CIStateChangedCallBack(Context context) {
        mContext = context;
        mSaveValue = SaveValue.getInstance(mContext);
        sendIRControl(0);
//        mSaveValue.saveValue(CommonIntegration.camUpgrade,0);
    }

//    public void setPinCodeDialog(CIPinCodeDialog dialog) {
//        pincodedialog = dialog;
//    }

//    public CIPinCodeDialog getPinCodeDialog() {
//        return pincodedialog;
//    }

    public void setCIClose() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isMMICloseMsg: " + isMMICloseMsg );
        if (getCIHandle() != null && !isMMICloseMsg) {
            mCi.setMMIClose();
        }
    }

    public synchronized static CIStateChangedCallBack getInstance(Context context) {
        if (null == mCIState) {
            mCIState = new CIStateChangedCallBack(context.getApplicationContext());
        }
        return mCIState;
    }


    public void handleCiCallback(Context megSrc, TvCallbackData data, CIMenuUpdateListener listener) {
        Log.d(Constants.LogTag.CLTV_TAG + "LAZA", "handleCiCallback, " + data.param2);

        try {
            if (data.param1 != slot_id) {
                slot_id = data.param1;
            }
            switch (data.param2) {
                case MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_CARD_INSERT:// 0
                    if (data.param1 != slot_id) {
                        slot_id = data.param1;
                    }
                    insertOrRemove=MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_CARD_INSERT;
                    break;
                case MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_CARD_NAME:// 1
//                    CIMainDialog ciMainDialog = (CIMainDialog) ComponentsManager.getInstance()
//                            .getComponentById(NavBasic.NAV_COMP_ID_CI_DIALOG);
//                    if (ciMainDialog != null) {
//                        ciMainDialog.showChildView(CIViewType.CI_DATA_TYPE_NO_CARD);
//                        ciMainDialog.showNoCardInfo(getCIName());
//                    }
                    break;
                case MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_CARD_REMOVE:// 2
                    slot_id = 0;
                    insertOrRemove=MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_CARD_REMOVE;
                    menu = null;
                    enquiry = null;
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "when card remove set upgrade to 0");
                    camUpgrade = 0;//when remove set to 0
                    sendIRControl(0);
//                    mSaveValue.saveValue(CommonIntegration.camUpgrade,camUpgrade);
                    if (listener != null) {
                        listener.ciRemoved();
                    }
                    break;
                case MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_MMI_ENQUIRY:// 3
                    enquiry = (MtkTvCIMMIEnqBase) (data.paramObj2);
//                    CIMainDialog.resetTryCamScan();
                    if (listener != null) {
                        listener.enqReceived(enquiry);
                    }
                    break;
                case MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_MMI_MENU:// 4
                case MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_MMI_LIST:// 5
        	 /*
	          CIMainDialog mCiMainDialog = (CIMainDialog) ComponentsManager.getInstance()
	          .getComponentById(NavBasic.NAV_COMP_ID_CI_DIALOG);
	          if (mCiMainDialog == null || !mCiMainDialog.isVisible()) {
	            CIMainDialog.setNeedShowInfoDialog(false);
	            break;
	          }
        	  */
                    isMMICloseMsg = false;//reset
                    if (data.param2 == MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_MMI_LIST) {
                        bListObj = true;
                    } else {
                        bListObj = false;
                    }
                    menu = (MtkTvCIMMIMenuBase) (data.paramObj1);
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "scube, menu=" + (MtkTvCIMMIMenuBase) (data.paramObj1));
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "listener:" + listener);
                    if (listener != null) {
                        listener.menuReceived(menu);
                    }
                    if (bListObj) {
                        // here can send upgrade progress
                        if (menu.getTitle() != null && menu.getTitle().contains("Upgrade")
                                && menu.getTitle().contains("Test")) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "CI upgrade begin to send upgrade progress");
                            TvCallbackData tdata = new TvCallbackData();
                            tdata.param2 = MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_FIRMWARE_UPGRADE_PROGRESS;
                            handleCiCallback(megSrc, tdata, listener);
                        } else {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "CI menu title ==" + menu.getTitle());
                        }
                    }
                    break;
                case MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_MMI_CLOSE:// 6
                    if(data.param1 != slot_id){
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "MTKTV_CI_NFY_COND_MMI_CLOSE, " + data.param1 + "," + slot_id);
                        return ;//DTV00612468
                    }
                    if (getCIHandle() != null) {
                        mCi.setMMICloseDone();
                        isMMICloseMsg = true;
                        if (listener != null) {
                            listener.menuEnqClosed();
                        }
                        menu = null;
                        enquiry = null;
                    }
                    break;
                case MtkTvCIMsgTypeBase.MTKTV_CI_CAM_SCAN_ENQ_WARNING:// 11
                case MtkTvCIMsgTypeBase.MTKTV_CI_CAM_SCAN_ENQ_URGENT:// 12
                case MtkTvCIMsgTypeBase.MTKTV_CI_CAM_SCAN_ENQ_NOT_INIT:// 13
                case MtkTvCIMsgTypeBase.MTKTV_CI_CAM_SCAN_ENQ_SCHEDULE:// 14
                    listener.ciCamScan(data.param2);
                    break;
                case MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_FIRMWARE_UPGRADE:
                case MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_FIRMWARE_UPGRADE_PROGRESS:
                    if (MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_FIRMWARE_UPGRADE == data.param2) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "reday to upgrade");
                        camUpgrade = 1;
                        sendIRControl(1);
//                        mSaveValue.saveValue(CommonIntegration.camUpgrade,camUpgrade);
                    }else{
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "upgrade progressing");
                        camUpgrade = 2;
                        sendIRControl(2);
//                        mSaveValue.saveValue(CommonIntegration.camUpgrade,camUpgrade);
                    }
                    break;
                case MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_FIRMWARE_UPGRADE_COMPLETE:
                case MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_FIRMWARE_UPGRADE_ERROR:
                    camUpgrade = 0;
                    sendIRControl(0);
//                    mSaveValue.saveValue(CommonIntegration.camUpgrade,camUpgrade);
                    break;
                case MtkTvCIMsgTypeBase.MTKTV_CI_NFY_COND_PIN_REPLY:// 15
                    checkReplyValue(data.param3);
                    break;
                default:
                    break;
            }
            Constants.slot_id = slot_id;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     *   [MTK Internal] This API is for MTK control IR use only.
     *   IR Remote related features
     *   @value      case 0: Ignore All IR Key except KEY_UP and KEY_DOWN
     *               case 1: Ignore All IR Key
     *               case 2: Ignore All IR except system Key
     *               case 3: Restart IR Key
     *               case 7: Ignore All IR Key except Power IR Key
     *
     **/
    private void sendIRControl (int mCIStatusInt){
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "sendIRControl||mCIStatusInt =: " + mCIStatusInt);
//        mSaveValue.saveValue(CommonIntegration.camUpgrade,mCIStatusInt);
        switch (mCIStatusInt) {
            case 0:
                MtkTvUtil.IRRemoteControl(3);
                break;
            case 1:
                MtkTvUtil.IRRemoteControl(3);
                break;
            case 2:
                MtkTvUtil.IRRemoteControl(7);
                break;
            default:
                MtkTvUtil.IRRemoteControl(3);
                break;
        }
    }
    public boolean camUpgradeStatus() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "camUpgradeStatus, camUpgrade "+camUpgrade);
        return (camUpgrade == 2);
    }

    public void setCamUpgrade(int camUpgradeForPowerOff) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "setCamUpgrade, camUpgradeForPowerOff "+camUpgradeForPowerOff);
        camUpgrade =camUpgradeForPowerOff;
//        mSaveValue.saveValue(CommonIntegration.camUpgrade,camUpgrade);
        sendIRControl(camUpgradeForPowerOff);
    }
    /**
     * this method is used to get CI handle
     *
     * @return instance of MtkTvCI
     */
    public MtkTvCI getCIHandle() {
        if (slot_id == -1) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "getCIHandle, null");
            mCi = null;
        } else {
            mCi = MtkTvCI.getInstance(slot_id);
        }
        return mCi;
    }

    /**
     * this method is used to get cam name
     *
     * @return
     */
    public String getCIName() {
        if (getCIHandle() != null) {
            mCiName = mCi.getCamName();
        } else {
            mCiName = "";
        }

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getCIName, name=" + mCiName);

        return mCiName;
    }

    /**
     * this method is used to get MMIMenu
     *
     */
    public MtkTvCIMMIMenuBase getMtkTvCIMMIMenu() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getMtkTvCIMMIMenuBase, menu=" + menu);

        return this.menu;
    }

    /**
     * this method is used to get MMIEnq
     *
     */
    public MtkTvCIMMIEnqBase getMtkTvCIMMIEnq() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getMtkTvCIMMIEnqBase, enquiry=" + enquiry);

        return this.enquiry;
    }

    /**
     * this method is used to select menu item
     *
     * @param num
     */
    public void selectMenuItem(int num) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "selectMenuItem, num=" + num + "  " + (menu == null));

        if (null != getCIHandle() && menu != null) {
            mCi.setMenuAnswer(menu.getMMIId(), bListObj ? 0 : (num + 1));

/*      if (1 == camUpgrade) {
        camUpgrade = 2;
        mSaveValue.saveValue(CommonIntegration.camUpgrade,camUpgrade);
      }*/
        }
    }

    /**
     * this method is used to answer enquiry
     *
     * @param bAnswer
     * @param data
     */
    public void answerEnquiry(int bAnswer, String data) {
        if (null != getCIHandle()) {
            mCi.setEnqAnswer(getMtkTvCIMMIEnq().getMMIId(), bAnswer, data);
        }
    }

    /**
     * this method is used to get Ans Len
     *
     * @return
     */
    public byte getAnsTextLen() {
        if (enquiry == null) {
            return -1;
        }

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getAnsTextLen, enquiry=" + enquiry);

        return enquiry.getAnsTextLen();
    }

    /**
     * this method is used to check blindans
     *
     * @return
     */
    public boolean isBlindAns() {
        if (enquiry == null) {
            return false;
        }

        Log.d(Constants.LogTag.CLTV_TAG + TAG, "isBlindAns, enquiry=" + enquiry + ",getBlindAns>> "+enquiry.getBlindAns());

        return enquiry.getBlindAns();
    }

    /**
     * this method is used to cancel curr Menu
     */
    public int cancelCurrMenu() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "cancelCurrMenu, menu=" + menu);

        if (null != getCIHandle()) {
            return mCi.setMenuAnswer(menu.getMMIId(), 0);
        }
        return 0;
    }

    /**
     * this method is used to check whether cam is active or not
     *
     * @return
     */
    public boolean isCamActive() {
        boolean active = false;
        if (null != mCi) {
            active = mCi.getSlotActive();
        }
        return active;
    }

    public TvCallbackData getReqShowData() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "getReqShowData");
        return mData;
    }

    public void setReqShowData(TvCallbackData data) {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "setReqShowData");
        if (mData == null) {
            mData = new TvCallbackData();
        }

        mData = data;
    }

    private void checkReplyValue(int ret) {
        CIPinCodeReplyType type = CIPinCodeReplyType.values()[ret];
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "CIPinCodeReplyType is " + type);
        switch (type) {
            case CI_PIN_CODE_CORRECT:
//                if (pincodedialog != null && pincodedialog.isShowing()) {
//                    pincodedialog.dismiss();
//                }
                Toast.makeText(mContext, "Correct!", Toast.LENGTH_LONG).show();
                break;
            case CI_PIN_CODE_UNCONFIRMED:
            case CI_PIN_CONTENT_SCRAMBLED:
            case CI_PIN_CICAM_BUSY:
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "these 3 type do nothing");
                Toast.makeText(mContext, "Some invalid type!", Toast.LENGTH_LONG).show();
                break;
            case CI_PIN_BAD_CODE:
//                if (DestroyApp.isCurTaskTKUI()){
//                    Toast.makeText(mContext, mContext.getString(R.string.menu_setup_ci_pin_code_incorrect_tip),
//                            Toast.LENGTH_LONG).show();
//                }
                break;
            case CI_PIN_BLANK_NOT_REQUIRED:
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "do nothing");
                break;
            default :
                break;
        }
    }

    // Interfaces
    public interface CIMenuUpdateListener {
        void enqReceived(MtkTvCIMMIEnqBase enquiry);

        void menuReceived(MtkTvCIMMIMenuBase menu);

        void menuEnqClosed();

        void ciRemoved();

        void ciCamScan(int message);

    }
}
