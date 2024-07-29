import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.util.SparseArray;

import com.mediatek.twoworlds.tv.MtkTvTVCallbackHandler;
import com.mediatek.twoworlds.tv.model.MtkTvCIMMIEnqBase;
import com.mediatek.twoworlds.tv.model.MtkTvCIMMIMenuBase;
import com.mediatek.wwtv.tvcenter.util.DataSeparaterUtil;
import com.mediatek.wwtv.tvcenter.util.MtkLog;
import com.mediatek.wwtv.tvcenter.util.RxBus;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class TvCallbackHandler{
    private static final String TAG = "TvCallbackHandler";

    //private variables
    private static TvCallbackHandler mCallback = null;
    private final Disposable subscribe;
    private IntenalCallbackHandler mHandler = null;
    private DataSeparaterUtil dataSeparaterUtil;

    private TvCallbackHandler(){
        try{
            mHandler = new IntenalCallbackHandler();
            dataSeparaterUtil=DataSeparaterUtil.getInstance();
        }
        catch(Exception ex){
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "TvCallbackHandler, " + ex);
        }
        subscribe = RxBus.instance.onEvent(Agent.class)
                .observeOn(Schedulers.single())
                .subscribe(it -> mHandler.dispatchMessage(it.getMessage()), Throwable::printStackTrace);
    }

    private static class Agent {
        private final Message msg;

        public Agent(Message msg) {
            this.msg = msg;
        }

        public Message getMessage() {
            return msg;
        }
    }


    public static synchronized TvCallbackHandler getInstance() {
        if(mCallback == null){
            mCallback = new TvCallbackHandler();
        }
        return mCallback;
    }

    /**
     * this method is used to add listener for recieving all native message
     *
     * @param listener Handler
     * @return
     */
    public boolean addCallBackListener(Handler listener){
        return addCallBackListener(0, listener);
    }

    /**
     * this method is used to add listener for recieving one native message
     *
     * @param callbackId, refer class TvCallbackConst
     * @param listener, Handler
     * @return
     */
    public boolean addCallBackListener(int callbackId, Handler listener){
        int key = callbackId & TvCallbackConst.MSG_CB_BASE_MASK;
        try{
            if(mHandler == null){
                mHandler = new IntenalCallbackHandler();
            }

            List<Handler> handlers = mHandler.mHandlers.get(key);

            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "AddCallBackListener, key=" + key);
            if(handlers != null){
                for(Handler handler : handlers){
                    if(listener.equals(handler)){
                        MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "AddCallBackListener, already existed");
                        return false;//already existed
                    }
                }
            }
            else{
                MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "AddCallBackListener, new ArrayList");
                handlers = new ArrayList<Handler>();
                mHandler.mHandlers.append(key, handlers);
            }

            return handlers.add(listener);
        }
        catch(Exception ex){
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "addCallBackListener, " + ex);
        }

        return false;
    }

    /**
     * this method is used to remove listener
     * @param listener Handler
     * @return
     */
    public boolean removeCallBackListener(Handler listener){
        if(listener==null){
            return true;
        }
        return removeCallBackListener(0, listener);
    }

    /**
     * this method is used to remove all
     * @return
     */
    public boolean removeAll() {
        synchronized(TvCallbackHandler.class) {
            mCallback = null;
        }

        if (subscribe != null) {
            subscribe.dispose();
        }

        try{
            mHandler.mHandlers.clear();
            mHandler  = null;
        }catch(Exception ex){
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "removeAll, " + ex);
        }

        return true;
    }

    /**
     * this method is used to remove listener
     *
     * @param callbackId, refer class TvCallbackConst
     * @param listener Handler
     * @return
     */
    public boolean removeCallBackListener(int callbackId, Handler listener){
        int key = callbackId & TvCallbackConst.MSG_CB_BASE_MASK;

        try{
            List<Handler> handlers = mHandler.mHandlers.get(key);

            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "removeCallBackListener, key=" + key);

            synchronized(IntenalCallbackHandler.class){
                if (handlers != null) {
                    handlers.remove(listener);
                }
            }
        }catch(Exception ex){
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "removeCallBackListener, " + ex);
        }

        return true;
    }

    private class IntenalCallbackHandler extends MtkTvTVCallbackHandler {
        public SparseArray<List<Handler>> mHandlers;//local variables
        private boolean isClear = false;

        public IntenalCallbackHandler(){
            super();

            mHandlers = new SparseArray<List<Handler>> ();
        }

        public void dispatchMessage(Message msg) {
            Message temp;

            try{
                synchronized(IntenalCallbackHandler.class){
                    List<Handler> generalHandlers = mHandlers.get(0);
                    if(generalHandlers != null){
                        for(Handler handler : generalHandlers){

                            temp = Message.obtain();
                            temp.copyFrom(msg);
                            if(handler != null){

                                handler.sendMessage(temp);
                            }else{
                                isClear = true;
                            }
                        }
                    }

                    List<Handler> handlers = mHandlers.get(
                            msg.what & TvCallbackConst.MSG_CB_BASE_MASK);

                    if(handlers != null){
                        for(Handler handler : handlers){
                            if((generalHandlers == null) || !generalHandlers.contains(handler)){
                                temp = Message.obtain();
                                temp.copyFrom(msg);
                                if(handler != null){
                                    handler.sendMessage(temp);
                                }else{
                                    isClear = true;
                                }
                            }
                        }
                    }
                    if(isClear){
                        isClear = false;

                        //clean handler
                        for(int i = 0; i < mHandlers.size(); i++){
                            List<Handler> tmps = mHandlers.get(mHandlers.keyAt(i));

                            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "keyAt:" + i+ ", key = " + mHandlers.keyAt(i));
                            for(int j = 0; j < tmps.size(); j++){
                                Handler handler = tmps.get(j);

                                if(handler == null){
                                    MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "handler : (null)");
                                    tmps.remove(j);
                                }
                                else{
                                    MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "handler :" + handler);
                                }
                            }
                        }
                    }
                }
            }catch(Exception ex){
                ex.printStackTrace();
            }
        }

        /**
         * this method is used to send message to handlers
         * @param msg
         * @return
         */
        private int sendMessage(Message msg){
            RxBus.instance.send(new Agent(msg));
            return 0;
        }

        public String toString(){
            String str = "";
            try{
                if(mHandlers == null) {
                    str += "empty";
                }
                else {
                    for(int i = 0; i < mHandlers.size(); i++){
                        str += "* key:" + mHandlers.keyAt(i) + "\n";

                        List<Handler> temp = mHandlers.get(mHandlers.keyAt(i));
                        if(temp != null){
                            for(Handler hander : temp) {
                                if(hander != null){
                                    str += "handler: " + hander.toString() + "\n";
                                }
                                else{
                                    str += "handler: null\n";
                                }
                            }
                        }
                    }
                }
            }
            catch(Exception ex){
                ex.printStackTrace();
            }

            return str;
        }
        /////////////////////////////////
        //all format methods
        /////////////////////////////////

        /**
         * this method is used to format callback data
         *
         * @return the object of TvCallbackData
         */
        private TvCallbackData formatData(int param1){
            TvCallbackData backData = new TvCallbackData();
            backData.param1 = param1;

            return backData;
        }

        /**
         * this method is used to format callback data
         *
         * @param param1
         * @param param2
         * @return
         */
        private TvCallbackData formatData(int param1, int param2){
            TvCallbackData backData = new TvCallbackData();
            backData.param1 = param1; backData.param2 = param2;

            return backData;
        }

        /**
         * this method is used to format callback data
         *
         * @param param1
         * @param param2
         * @param param3
         * @return
         */
        private TvCallbackData formatData(int param1, int param2, int param3){
            TvCallbackData backData = new TvCallbackData();
            backData.param1 = param1; backData.param2 = param2; backData.param3 = param3;

            return backData;
        }

        /**
         * this method is used to format callback data
         *
         * @param param1
         * @param param2
         * @param param3
         * @param param4
         * @return
         */
        private TvCallbackData formatData(int param1, int param2, int param3, int param4){
            TvCallbackData backData = new TvCallbackData();
            backData.param1 = param1; backData.param2 = param2; backData.param3 = param3; backData.param4 = param4;

            return backData;
        }

        private TvCallbackData formatData(int param1, int param2, int param3, long param4){
            TvCallbackData backData = new TvCallbackData();
            backData.param1 = param1; backData.param2 = param2; backData.param3 = param3; backData.paramLong1 = param4;

            return backData;
        }
        /**
         * this method is used to format callback data
         *
         * @param param1
         * @param param2
         * @param paramStr1
         * @param param3
         * @return
         */
        private TvCallbackData formatData(int param1, int param2, String paramStr1, int param3){
            TvCallbackData backData = new TvCallbackData();
            backData.param1 = param1; backData.param2 = param2; backData.paramStr1 = paramStr1; backData.param3 = param3;

            return backData;
        }

        /////////////////////////////////
        //Below is all callback APIs
        /////////////////////////////////

        public int notifySvctxNotificationCode(int code){
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "notifySvctxNotificationCode>>>" + code);
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_SVCTX_NOTIFY;
            msg.obj = formatData(code);

            return sendMessage(msg);
        }

        public int notifyConfigMessage(int notifyId, int data){
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_CONFIG;
            msg.obj = formatData(notifyId, data);

            return sendMessage(msg);
        }

        public int notifyChannelListUpdateMsg(int condition, int reason, int data){
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "notifyChannelListUpdateMsg>>" + condition + "  " + reason + "  " + data);
            if (reason == 0 || condition == 1) {//reason 0:unknown, 1<<1:add, 1<<2:delete 1<<3:update  condition 1:updating 2:updated
                return -1;
            }
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_CHANNELIST;
            msg.obj = formatData(condition, reason, data);

            return sendMessage(msg);
        }

        public int notifySatlListUpdateMsg(int condition, int reason, int data) {
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_NFY_UPDATE_SATELLITE_LIST;
            msg.obj = formatData(condition, reason, data);
            return sendMessage(msg);
        }

        public int notifyShowOSDMessage(int stringId, int msgType){
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_SHOW_OSD;
            msg.obj = formatData(stringId, msgType);

            return sendMessage(msg);
        }

        public int notifyHideOSDMessage(){
            Message msg = Message.obtain();
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_HIDE_OSD;

            return sendMessage(msg);
        }

        public int notifyScanNotification(int msgid, int scanProgress, int channelNum,int argv4){
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_SCAN_NOTIFY;
            msg.obj = formatData(msgid, scanProgress, channelNum, argv4);

            return sendMessage(msg);
        }

        public int notifyGingaMessage(int updateType, String sAppID, String sAppName){
            Message msg = Message.obtain();
            TvCallbackData backData = new TvCallbackData();
            //backData
            backData.param1 = updateType;
            backData.paramStr1 = sAppID;
            backData.paramObj1 = sAppName;
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_GINGA_MSG;
            msg.obj = backData;

            return sendMessage(msg);
        }

        public int notifyGingaVolumeChanged(int updateType, int level)
                throws RemoteException {
            // TODO Auto-generated method stub
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "(Default Handler) notifyGingaVolumeChanged updateType=" + updateType + " level="
                    + level);
            Message msg = Message.obtain();
            TvCallbackData backData = new TvCallbackData();
            //backData
            backData.param1 = updateType;
            backData.param2 =level;
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_GINGA_VOLUME_MSG;
            msg.obj = backData;

            return sendMessage(msg);
        }
        public int notifyEASMessage(int updateType, int argv1, int argv2, int argv3){
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_EAS_MSG;
            msg.obj = formatData(updateType, argv1, argv2, argv3);

            return sendMessage(msg);
        }

        public int notifyPipPopMessage(int updateType, int argv1, int argv2, int argv3){//donot care it anymore
            return 0;
        }

        public int notifyTeletextMessage(int updateType, int argv1, int argv2, int argv3){
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_TTX_MSG;
            msg.obj = formatData(updateType, argv1, argv2, argv3);

            return sendMessage(msg);
        }

        public int notifyUpgradeMessage(int updateType, int argv1, int argv2, int argv3){
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_UPGRADE_MSG;
            msg.obj = formatData(updateType, argv1, argv2, argv3);

            return sendMessage(msg);
        }

        public int notifyChannelListUpdateMsgByType(int svlid, int condition, int reason, int data){
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "notifyChannelListUpdateMsgByType>>" + svlid + "  " + condition + "  " + reason + "   " + data);
            if (reason == 0 || condition == 1) {//reason 0:unknown, 1:add, 2:delete 3:update  condition 1:updating 2:updated
                return -1;
            }
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_CHANNEL_LIST_UPDATE;
            msg.obj = formatData(svlid, reason, condition, data);

            return sendMessage(msg);
        }

        //match with TV-API, CL: 1626538
        public int notifyRecordNotification(int updateType, int argv1, int argv2){
            Message msg = Message.obtain();

            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_RECORD_NFY;
            msg.obj = formatData(updateType, argv1, argv2);
            return sendMessage(msg);
        }

        public int notifyAVModeMessage(int updateType, int argv1, int argv2, int argv3){
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_AV_MODE_MSG;
            msg.obj = formatData(updateType, argv1, argv2, argv3);

            return sendMessage(msg);
        }

        public int notifyMHEG5Message(int updateType, int argv1, int argv2, int argv3){
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_MHEG5_MSG;
            msg.obj = formatData(updateType, argv1, argv2, argv3);

            return sendMessage(msg);
        }

        public int notifyEWSPAMessage(int updateType){
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "notifyEWSPAMessage,updateType:"+updateType);
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_EWS_MSG;
            msg.obj = formatData(updateType);

            return sendMessage(msg);
        }

        /*
         * this notify is for SA Philippines country EWS
         */
        public int notifyEWSMessage(int updateType){
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "notifyEWSPAMessage,updateType:"+updateType);
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_EWS_SA_MSG;
            msg.obj = formatData(updateType);

            return sendMessage(msg);
        }

        public int notifyMHPMessage(int updateType, int argv1, int argv2, int argv3){
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_MHP_MSG;
            msg.obj = formatData(updateType, argv1, argv2, argv3);

            return sendMessage(msg);
        }

        public int notifyCIMessage(int slotid, int messageType, int arg3, int arg4, MtkTvCIMMIMenuBase cMMIMenu, MtkTvCIMMIEnqBase cMMIEnq){
            if(!dataSeparaterUtil.isSupportCI()){
                MtkLog.d(Constants.LogTag.CLTV_TAG + TAG,"notifyCIMessage !dataSeparaterUtil.isSupportCI()");
                return -1;
            }

            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_CI_MSG;
            msg.obj = formatData(slotid, messageType, arg3, arg4);
            ((TvCallbackData)msg.obj).paramObj1 = cMMIMenu;
            ((TvCallbackData)msg.obj).paramObj2 = cMMIEnq;

            return sendMessage(msg);
        }
        public int notifyFeatureMessage(int updateType, int argv1, int argv2, int argv3){
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_FEATURE_MSG;
            msg.obj = formatData(updateType, argv1, argv2, argv3);

            return sendMessage(msg);
        }

        public int notifyScreenSaverMessage(int updateType, int argv1, int argv2, int argv3){
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG,"notifyScreenSaverMessage updateType ="+updateType+"argv1 ="+argv1+"argv2 ="+argv2+"argv3 ="+argv3);
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_SCREEN_SAVER_MSG;
            msg.obj = formatData(updateType, argv1, argv2, argv3);

            return sendMessage(msg);
        }

        @Override
        public int notifyPWDDialogMessage(int updateType, int argv1, int argv2,
                                          int argv3) throws RemoteException {
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG,"notifyPWDDialogMessage updateType ="+updateType+"argv1 ="+argv1+"argv2 ="+argv2+"argv3 ="+argv3);

            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_PWD_DLG_MSG;
            msg.obj = formatData(updateType, argv1, argv2, argv3);

            return sendMessage(msg);
        }

        public int notifyOADMessage(int updateType, String oadSchedule, int progress, boolean autoDld, int argv5){
            Message msg = Message.obtain();
            TvCallbackData backData = new TvCallbackData();
            backData.param1 = updateType;
            backData.param2 = progress;
            backData.param3 = argv5;
            backData.paramStr1 = oadSchedule;
            backData.paramBool2 = autoDld;

            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_OAD_MSG;
            msg.obj = backData;

            return sendMessage(msg);
        }

        public int notifyDeviceDiscovery() {
            Message msg = Message.obtain();

            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_DEVICE_DISCOVERY;
            return sendMessage(msg);
        }

        public int notifyBannerMessage(int msgType, int msgName, int argv2, int argv3){
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "notifyBannerMessage BANNER_MSG_NAV msgType= "+msgType+", msgName="+msgName);
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_BANNER_MSG;
            msg.obj = formatData(msgType, msgName, argv2, argv3);

            return sendMessage(msg);
        }

        public int notifyTimeshiftNotification(int updateType, long argv1){
            Message msg = Message.obtain();
            TvCallbackData backData = new TvCallbackData();
            backData.param1 = updateType; backData.paramLong1 = argv1;
            //msg
            msg.obj = backData;
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_TIME_SHIFT_NFY;
            return sendMessage(msg);
        }

        public int notifyHBBTVMessage(int callbackType, int[] callbackData, int callbackDataLen){
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG,"callbackType->"+callbackType);
//            if(!DestroyApp.isCurTaskTKUI() &&
//                    callbackType != 257 &&
//                    callbackType != 258 &&
//                    callbackType != 262 &&
//                    callbackType != 263 &&
//                    MtkTvConfig.getInstance().getConfigValue(MtkTvConfigType.CFG_MISC_AV_COND_MMP_MODE)!=0) {
//                if(callbackType==4) {
//                    callbackType=10000;
//                } else {
//                    return 0;
//                }
//            }
            Message msg = Message.obtain();
            //msg
            int message = 0;
            try{
                message = callbackData[0];
            }
            catch(Exception ex){
                ex.printStackTrace();
            }

            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_HBBTV_MSG;
            msg.obj = formatData(callbackType, message);

            return sendMessage(msg);
        }


        public int notifyWarningMessage(int updateType, int channelID,
                                        String eventInfo, int duration, int args5) {
            // TODO Auto-generated method stub
            Message msg = Message.obtain();
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_WARNING_MSG;
            msg.obj = formatData(updateType, channelID, eventInfo, duration);
            return sendMessage(msg);
        }

        /*****
         * CEC Function
         */
        public int notifyCecNotificationCode(int code) {
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_CEC_NFY;
            msg.obj = formatData(code);
            return sendMessage(msg);
        }

        public int notifyAmpVolCtrlMessage(int volume, boolean isMute) {
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_AMP_VOL_CTRL;
            return sendMessage(msg);
        }

        public int notifyCecFrameInfo(int initLA, int destLA, int opcode, int[] operand, int operandSize) {
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_CEC_FRAME_INFO;
            return sendMessage(msg);
        }

        public int notifyCecActiveSource(int destLA, int destPA, boolean activeRoutingPath) {
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_CEC_ACTIVE_SRC;
            return sendMessage(msg);
        }

        public int notifyEventNotification(int updateType, int argv1, int argv2, long argv3){
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_EVENT_NFY;
            msg.obj = formatData(updateType, argv1, argv2);
            ((TvCallbackData)msg.obj).paramLong1 = argv3;

            return sendMessage(msg);
        }

        public int notifyNoUsedkeyMessage(int updateType, int argv1, int argv2, long argv3){
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_NO_USED_KEY_MSG;
            msg.obj = formatData(updateType, argv1);
            return sendMessage(msg);
        }

        /***
         * PVR
         */
        public int notifyRecordPBNotification(int updateType, int argv1, int argv2, long argv3) {
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_RECORD_NFY;
            msg.obj = formatData(updateType,argv1,argv2,argv3);
            return sendMessage(msg);
        }
        /***
         * EPG For US
         */
        @Override
        public int notifyATSCEventMessage(int updateType, int argv1, int argv2, long argv3)
                throws RemoteException {
            // TODO Auto-generated method stub
            MtkLog.d(Constants.LogTag.CLTV_TAG + "TVCallBackHandler", "notifyATSCEventMessage");
            Message msg = Message.obtain();
            //msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_ATSC_EVENT_MSG;
            msg.obj = formatData(updateType,argv1,argv2,argv3);
            return sendMessage(msg);
        }

        /***
         * channel logo for SA TV
         */
        @Override
        public int notifyCDTLogoMessage(int updateType, int argv1, int argv2,
                                        int argv3) {
            // TODO Auto-generated method stub
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "come in notifyCDTLogoMessage,updateType = "
                    + updateType + ",argv1 = " + argv1 + ", argv2 = " + argv2
                    + ", argv3 = " + argv3);
            Message msg = Message.obtain();
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_BANNER_CHANNEL_LOGO;
            msg.obj = formatData(updateType, argv1, argv2, argv3);
            return sendMessage(msg);
        }

        /**
         * this method is used to notify UI Display.
         *
         * @param uiType
         *            ui display type.
         * @param show
         *            true :display ui,false:hide ui
         * @return 0 callback success. others callback fail.
         */
        public int notifyUiMsDisplay(int uiType, boolean show){
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "come in notifyUiMsDisplay,uiType=" + uiType + ",show=" + show);

            TvCallbackData backData = new TvCallbackData();
            Message msg = Message.obtain();

            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_NFY_CEC_UI_DISP;
            backData.param1 = uiType;
            backData.paramBool1 = show;

            msg.obj = backData;
            return sendMessage(msg);
        }

        public int notifyNativeAppStatus(int nativeAppId, boolean show) {
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "come in notifyNativeAppStatus,nativeAppId=" + nativeAppId + ",show=" + show);

            TvCallbackData backData = new TvCallbackData();
            Message msg = Message.obtain();

            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_NFY_NATIVE_APP_STATUS;
            backData.param1 = nativeAppId;
            backData.paramBool1 = show;

            msg.obj = backData;
            return sendMessage(msg);
        }

        /**
         * [MTK Internal] This method is used to notify linux world's message to android app when the channel list is
         * changing/changed for some reason.
         *
         * @param svlid
         *            the svl id.
         *
         * @param count
         *            count the count of event.
         *
         * @param eventType
         *            eventType the event type array.
         *
         * @param svlRecId
         *            svlRecId the record id  array
         * @return 0, the callback method successful others, method failed
         */

        public int notifyTvproviderUpdateMsg(int svlid, int count, int[] eventType, int[] svlRecId) {
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "(Default Handler) notifyTvproviderUpdateMsg: svlid = " + svlid + ", count = " + count+ "\n"
                    + "  eventType>>" + eventType.length + "   svlRecId>>>" + svlRecId.length);
            Message msg = Message.obtain();
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_NFY_UPDATE_TV_PROVIDER_LIST;
            msg.obj = formatData(svlid, count);
            return sendMessage(msg);
        }

        /**
         * [MTK Internal] This method is used to notify linux world's message to android app when the tsl id is
         * changing/changed for some reason.
         *
         * @param condition
         *            TSL_UPDATING:1,  TSL_UPDATED:2.
         *
         * @param reason
         *            TSL_REASON_UNKNOWN:0, TSL_RECORD_ADD:1, TSL_RECORD_DEL:4, TSL_RECORD_MOD:8.
         *
         * @param data
         *             reserver.
         * @return 0, the callback method successful others, method failed
         */
        public int notifyTslIdUpdateMsg(int condition, int reason, int data) throws RemoteException {
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG,
                    "(Default Handler) notifyTslIdUpdateMsg: condition = "
                            + condition + ",reason = " + reason + ",data = "
                            + data + "\n");
            Message msg = Message.obtain();
            // msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_NFY_TSL_ID_UPDATE_MSG;
            msg.obj = formatData(condition, reason, data);

            return sendMessage(msg);
        }

        /**
         * This method is used to notify Broadcast message information. Please override this function and perform your behavior.
         * <p>
         *
         *            , ch_select_result type: 4 : other: not used.
         * @param argv1
         *            , msgvalue. 0:success other:failed
         * @param argv2
         *            , reserved, not used.
         * @param argv3
         *            , reserved, not used.
         * @return 0 callback success. others callback fail.
         *         </p>
         */
        public int notifyBroadcastMessage(int msgType, int argv1, int argv2, int argv3) throws RemoteException {
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "1111(Default Handler) notifyBroadcastMessage type=" + msgType + "argv1=" + argv1);
            Message msg = Message.obtain();
            // msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_NFY_TUNE_CHANNEL_BROADCAST_MSG;
            msg.obj = formatData(msgType, argv1);

            return sendMessage(msg);
        }
        /**
         * This method is used to notify html agent warning message, such as active, background.
         *
         * @param callbackType 0: HTML_AGENT_STATUS_UNKNOWN,
        HTML_AGENT_STATUS_BACKGROUND,
        HTML_AGENT_STATUS_ACTIVE,
        HTML_AGENT_STATUS_MAX,
         * @param callbackData ; reserved
         * @param callbackDataLen ; reserved
         * @return 0: callback success. <BR>
         *         others: callback fail.
         */
        public int notifyHtmlAgentMessage(int callbackType, int[] callbackData, int callbackDataLen)
                throws RemoteException {
            // TODO Auto-generated method stub
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "(Default Handler) notifyHtmlAgentMessage=" + callbackType);
            Message msg = Message.obtain();
            //msg
            int message = 0;
            try{
                message = callbackData[0];
            }
            catch(Exception ex){
                ex.printStackTrace();
            }

            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_FVP_MSG;
            msg.obj = formatData(callbackType, message);

            return sendMessage(msg);

        }

        /**
         * This method is used to notify some  message, such as BML native app status.
         *
         * @param callbackType 0: BML_COMP_UNKNOWN <BR>
         *            1: BML_COMP_ACTIVE  <BR>
         *            2: BML_COMP_INACTIVE <BR>
         * @param callbackData ; reserved
         * @param callbackDataLen ; reserved
         * @return 0: callback success. <BR>
         *         others: callback fail.
         */
        public int notifyBMLMessage(int callbackType, int[] callbackData, int callbackDataLen){
            Message msg = Message.obtain();
            //msg
            int message = 0;
            try{
                message = callbackData[0];
            }
            catch(Exception ex){
                ex.printStackTrace();
            }

            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_BML_MSG;
            msg.obj = formatData(callbackType, message);

            return sendMessage(msg);
        }
        /**
         * This method is called to notify gingaTvDialog about loading status:
         * updateType = 2 means ginga is loading and should show loading ui
         * updateType = 3 means ginga has been loaded completely
         * and should dismiss loading ui
         * */
        public int notifyGingaUpdateMessage(int updateType, String json, int len)
                throws RemoteException {
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "notifyGingaUpdateMessage||updateType=" + updateType);
            Message msg = Message.obtain();
            TvCallbackData backData = new TvCallbackData();
            // backData
            backData.param1 = updateType;
            backData.paramStr1 = json;
            // msg
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_GINGA_UPDATE_MSG;
            msg.obj = backData;

            return sendMessage(msg);
        }

        /**
         * This method is used to notify on screen message notification for atsc 3.0. Please override this function and perform your behavior.
         *
         * @param updateType  0:Keep Screen Clean
         * @param enableClean : true:enter "clean screen" status;
         *                      false:end "clean screen" status;
         * @param duration: In this duration ,need keep screen clean status,only valid when  "enableClean" is true , and the "duration" value will be in second.And the value will be 1*60~60*60 (seconds).
         *                  if enableClean is false, can omit the "duration".
         * @param argv1:reserved.
         *
         *
         * @return 0: callback success. others: callback fail.
         */
        public int notifyOnScreenMessageNotification(int updateType, boolean enableClean, int duration, int argv1) throws RemoteException {
            // TODO Auto-generated method stub
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "(Default Handler) DO_notifyOnScreenMessageNotification. updateType = " + updateType + " ,enableClean:"+enableClean+",duration"+duration);
            Message msg = Message.obtain();
            TvCallbackData backData = new TvCallbackData();
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_ON_SCREEN_A3WAM_MSG;
            backData.param1 = updateType;
            backData.param2 = duration;
            backData.param3 = argv1;
            backData.paramBool1 = enableClean;
            backData.paramBool2 = true;//onscreen
            msg.obj = backData;
            return sendMessage(msg);
        }

        /**
         * @nfyType 0:start,1:stop
         */
        public int notifyA3WamMessage(int nfyType,String activeAppUrl, int UrlLen) throws RemoteException {
            // TODO Auto-generated method stub
            MtkLog.d(Constants.LogTag.CLTV_TAG + TAG, "(Default Handler) notifyA3WamMessage. activeAppUrl = " + activeAppUrl +
                    "nfyType" + nfyType + "UrlLen " + UrlLen);
            Message msg = Message.obtain();
            TvCallbackData backData = new TvCallbackData();
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_ON_SCREEN_A3WAM_MSG;
            backData.param4 = nfyType;//
            backData.param2 = UrlLen;
            backData.paramStr1 = activeAppUrl;
            backData.paramBool2 = false;//A3Wam
            msg.obj = backData;
            return sendMessage(msg);
        }

        @Override
        public int notifyVideoInfoMessage(int updateType, int argv1, int argv2, int argv3,
                                          int argv4, int argv5, int argv6) throws RemoteException {
            Message msg = Message.obtain();
            msg.what = msg.arg1 = msg.arg2 = TvCallbackConst.MSG_CB_VIDEO_INFO_MSG;
            msg.obj = formatData(updateType,argv1,argv2);
            return sendMessage(msg);
        }
    }
}

