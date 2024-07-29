package com.iwedia.cltv.platform.refplus5

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.iwedia.cltv.platform.ApplicationMode
import com.iwedia.cltv.platform.base.CiPlusInterfaceBaseImpl
import com.iwedia.cltv.platform.`interface`.CiPlusInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface
import com.iwedia.cltv.platform.`interface`.TvInterface
import com.iwedia.cltv.platform.`interface`.UtilsInterface
import com.iwedia.cltv.platform.model.IAsyncCallback
import com.iwedia.cltv.platform.model.channel.TunerType
import com.iwedia.cltv.platform.refplus5.provider.ChannelDataProvider
import com.iwedia.cltv.platform.refplus5.screenMode.Constants
import com.mediatek.dtv.tvinput.client.cam.appinfo.CamAppInfoListener
import com.mediatek.dtv.tvinput.client.cam.appinfo.CamAppInfoService
import com.mediatek.dtv.tvinput.client.cam.caminfo.CamInfoListener
import com.mediatek.dtv.tvinput.client.cam.caminfo.CamMonitoringService
import com.mediatek.dtv.tvinput.client.cam.camprofile.CamProfileInterface
import com.mediatek.dtv.tvinput.client.cam.contentcontrol.campin.CamPinCapabilityListener
import com.mediatek.dtv.tvinput.client.cam.contentcontrol.campin.CamPinService
import com.mediatek.dtv.tvinput.client.cam.contentcontrol.campin.CamPinStatusListener
import com.mediatek.dtv.tvinput.client.cam.hostcontrol.CamHostControlInfoListener
import com.mediatek.dtv.tvinput.client.cam.hostcontrol.CamHostControlService
import com.mediatek.dtv.tvinput.client.cam.hostcontrol.CamHostControlTuneQuietlyFlag
import com.mediatek.dtv.tvinput.client.cam.hostcontrol.CamHostControlTuneQuietlyFlagListener
import com.mediatek.dtv.tvinput.client.cam.mmi.EnterMenuErrorCallback
import com.mediatek.dtv.tvinput.client.cam.mmi.MmiInterface
import com.mediatek.dtv.tvinput.client.cam.mmi.MmiSession
import com.mediatek.dtv.tvinput.client.cam.mmi.MmiStatusCallback
import com.mediatek.dtv.tvinput.client.scan.TvScan
import com.mediatek.dtv.tvinput.dvbtuner.scan.ScanConstants
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.cam.Constants.KEY_MMI_ENQ_INPUTTEXT
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.cam.Constants.KEY_MMI_MENULIST_FOOTERNOTE
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.cam.Constants.KEY_MMI_MENULIST_ITEMLIST
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.cam.Constants.KEY_MMI_MENULIST_SUBTITLE
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.cam.Constants.KEY_MMI_MENULIST_TITLE
import com.mediatek.dtv.tvinput.framework.tifextapi.dvb.cam.Constants.KEY_MMI_ENQ_ISBLIND
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.concurrent.thread


open class CiPlusInterfaceImpl(var context : Context, val utilsInterfaceImpl: UtilsInterface, val playerInterface: PlayerInterface, val tvInterface: TvInterface): CiPlusInterfaceBaseImpl() {

    val TAG = "CiPlusInterfaceImpl"
    var listeners = mutableListOf<CiPlusInterface.RefCiHandlerListener>()
    companion object {
        val inputid = "com.mediatek.dtv.tvinput.dvbtuner/.DvbTvInputService/HW0"
        private var staticInstance : CiPlusInterfaceImpl? = null
        public fun getInstance() : CiPlusInterfaceImpl? {
            return staticInstance
        }
    }
    var mMmiInterface: MmiInterface = MmiInterface(context, inputid)
    var mCamMonitoring: CamMonitoringService = CamMonitoringService(context, inputid)
    var mCamInfoMonitoring: CamAppInfoService = CamAppInfoService(context, inputid)
    var mCamHostControl = CamHostControlService(context, inputid)
    var mCamProfileName = CamProfileInterface(context, inputid)
    var mCamQuietTune = CamHostControlTuneQuietlyFlag(context, inputid)
    var mCamPinService = CamPinService(context, inputid)
    val MTK_EVENT_EXTRA_TV_SCAN_FOR_COMMON_ACTION = "com.mediatek.tv.oneworld.wizardkt.tvscan.TVCommonActivity"
    val MTK_EVENT_EXTRA_ACTION_ID = "ACTION_ID"
    val EXTRA_SATELLITE_CAM_SCAN = 0x929
    val WIZARD_PACKAGE = "com.mediatek.tv.oneworld.wizardkt"

    var KEY_MMI_MENU_STRING = "MENU_STR"
    val KEY_IS_CAM_INSERTED = "IS_CAM_INSERTED"
    val KEY_CAM_SLOT_TYPE = "KEY_CAM_SLOT_TYPE"

    var mmiSession : MmiSession? = null
    var mmiSessionInit = false
    var mmiSessionFirstInitDone = false
    var mmiMenuId = -1
    var mmiEnqId = -1
    var mmiLevel = 0

    val AUTHORITY = "com.mediatek.tv.internal.data"
    val GLOBAL_PROVIDER_ID = "global_value"
    val GLOBAL_PROVIDER_URI_URI = Uri.parse("content://$AUTHORITY/$GLOBAL_PROVIDER_ID")
    val CFG_PWD_PASSWORD = "CFG_PWD_PASSWORD"

    val PROFILE_SUPPORT_DVBC = 1
    val PROFILE_SUPPORT_DVBS = 2
    val PROFILE_SUPPORT_DVBT = 4

    val BS_SRC_AIR = 0
    val BS_SRC_CABLE = 1
    val BS_SRC_SAT = 2

    val TYPE_DVB_T = 0
    val TYPE_DVB_C = 1
    val TYPE_DVB_S = 2

    val CABLE_BRDCSTER = "CABLE_BRDCSTER"
    val PROFILE_NAME = "profile_name"

    val COUNTRY = "M_CURRENT_COUNTRY_REGION"
    val KEY_SLOT_NUMBER = "SLOT_NUMBER"

    val KEY_CAM_PROFILE_NAME = "CAM_PROFILE_NAME"
    val KEY_CAM_PROFILE_TYPE = "CAM_PROFILE_TYPE"
    val KEY_NEED_SERVICE_UPDATE = "NEED_SERVICE_UPDATE"
    val KEY_CAM_DELIVERY_SYSTEM_HINT = "CAM_DELIVERY_SYSTEM_HINT"

    val CAM_OP_INITIAL_AUTO_TUNE = 0 //  (Display user confirmation and start cam scan)
    val CAM_OP_UPDATE_CAM_NIT = 1 //  (No Action required from Application)
    val CAM_OP_UPDATE_AUTO_TUNE = 2 // (Display user confirmation and start cam scan)
    val CAM_OP_UPDATE_ADVANCE_WARNING =  3 //  (Display the CAM service update required in Google Notification)
    val CAM_OP_UPDATE_SCHEDULED = 4 // (No Action required from Application)

    var mCurrentProfileType : Int = 0
    var mCurrentNeedServiceUpdate : Int = 0
    var mCurrentDeliverySystemHint : Int = 0
    var mCurrentProfileName = ""
    var instance : CiPlusInterfaceImpl = this

    val TUNER_MODE_PREFER_SAT = "TUNER_MODE_PREFER_SAT"
    val OPERATOR_GENERAL_SATELLITE = 902
    val BS_SAT_GENERAL = 0
    val BS_SAT_PREFER = 1

    val CFG_GRP_BS_PREFIX = "g_bs__"
    val CFG_BS_BS_SAT_BRDCSTER = CFG_GRP_BS_PREFIX + "bs_sat_brdcster"

    private val mCamInfoListener =
        object : CamInfoListener() {
            override fun onCamInfoChanged(slotId: Int, updatedCamInfo: Bundle) {
            }

            override fun onSlotInfoChanged(slotId: Int, updatedSlotInfo: Bundle) {
                val isCamInserted: Int = updatedSlotInfo.getInt(KEY_IS_CAM_INSERTED, 0)
                if (isCamInserted == 1) {
                    for(listener in listeners) {
                        listener.onCICardEventInserted()
                    }
                } else {
                    mmiSession?.closeMmi()
                    for(listener in listeners) {
                        listener.onCICardEventRemoved()
                    }
                }
            }
        }

    private val mCamAppInfoListener =
        object : CamAppInfoListener() {
            override fun onCamAppInfoChanged(slotId: Int, appInfo: Bundle?) {
            }
        }

    private val mCamHostControlInfoListener =
        object : CamHostControlInfoListener() {
            override fun onCamHostControlInfoChanged(clientToken: String, sessionStatus: Int) {
            }
        }

    private val mCamHostControlTuneQuietlyFlagListener =
        object : CamHostControlTuneQuietlyFlagListener() {
            override fun onHcTuneQuietlyFlagChanged(
                clientToken: String,
                appPresentationStatus: Int
            ) {
                var quietTuneStatus = false
                if(appPresentationStatus == 1) {
                    quietTuneStatus = true
                }
                playerInterface.setQuietTuneEnabled(quietTuneStatus)
            }
        }

    private val mCamPinCapabilityListener =
        object : CamPinCapabilityListener() {
            override fun onCamPinCapabilityChanged(slotId: Int, bundle: Bundle) {
                var capability = bundle.getInt("PIN_CAPABILITY", -1)
              }
        }

    init {
        mCamMonitoring.addCamInfoListener(mCamInfoListener)
        mCamInfoMonitoring.addCamAppInfoListener(mCamAppInfoListener)
        mCamHostControl.addCamHostcontrolInfoListener(mCamHostControlInfoListener)
        mCamQuietTune.addHcTuneQuietlyFlagListener(mCamHostControlTuneQuietlyFlagListener)
        mCamPinService.addCamPinCapabilityListener(mCamPinCapabilityListener)
        staticInstance = this

        val mSlotIds: IntArray = mCamMonitoring.getSlotIds()
        if (mSlotIds.isNotEmpty()) {
            var slot = mCamMonitoring.getSlotIds()[0]
            mmiSession = mMmiInterface.openSession(slot, mmiStatusCallback(slot))
            mmiSessionInit = true
            mmiSessionFirstInitDone = true
        }

        mCamProfileName.requestResendProfileInfoBroadcastACON()

        CiPlusOPBroadcastReceiver.registerScanStartListener( object :
            CiPlusOPBroadcastReceiver.CiPlusScanStartListener {
            override fun onStartScanReceived(
                profileType: Int,
                profileName: String,
                needServiceUpdate: Int,
                deliverySystemHint: Int
            ) {
                if(getProfileSupport(context, deliverySystemHint)) {
                    (utilsInterfaceImpl as UtilsInterfaceImpl).saveMtkInternalGlobalValue(context!!, PROFILE_NAME,profileName,true)
                    utilsInterfaceImpl.savePreference(KEY_CAM_PROFILE_TYPE , profileType)
                    utilsInterfaceImpl.savePreference(KEY_NEED_SERVICE_UPDATE, needServiceUpdate)
                    utilsInterfaceImpl.savePreference(KEY_CAM_DELIVERY_SYSTEM_HINT, deliverySystemHint)
                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "onStartScanReceived written type: $profileType profile name: $profileName need update: $needServiceUpdate hint: $deliverySystemHint ")
                } else {
                    Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "Delivery system not support!")
                }

                if(needServiceUpdate == CAM_OP_UPDATE_SCHEDULED) {
                    Handler(Looper.getMainLooper()).post {
                        playerInterface.stop()
                        instance.startCAMScan(profileType, profileName, needServiceUpdate, deliverySystemHint,false)
                    }
                }

                if((needServiceUpdate == CAM_OP_UPDATE_CAM_NIT) ||
                    (needServiceUpdate == CAM_OP_UPDATE_SCHEDULED)) {
                    return
                }

                if((needServiceUpdate != CAM_OP_INITIAL_AUTO_TUNE) &&
                    (needServiceUpdate != CAM_OP_UPDATE_AUTO_TUNE) &&
                    (needServiceUpdate != CAM_OP_UPDATE_ADVANCE_WARNING) ) {
                    return
                }

                mCurrentProfileType = profileType
                mCurrentNeedServiceUpdate = needServiceUpdate
                mCurrentDeliverySystemHint = deliverySystemHint
                mCurrentProfileName = profileName

                for (listener in listeners) {
                    try {
                        listener.onStartScanReceived(profileName)
                    }catch (E: Exception){
                        E.printStackTrace()
                    }
                }
            }
        })

       (utilsInterfaceImpl as UtilsInterfaceImpl).readMtkInternalGlobalValue(context, PROFILE_NAME)?.let {
           mCurrentProfileName = it
       }
    }

    private fun isServiceListEmpty(systemHint : Int) : Boolean {
        for(service in tvInterface.getChannelList()) {
            if(((systemHint and PROFILE_SUPPORT_DVBT) == PROFILE_SUPPORT_DVBT) &&
                (service.tunerType == TunerType.TERRESTRIAL_TUNER_TYPE))
            {
                return false
            }
            if(((systemHint and PROFILE_SUPPORT_DVBC) == PROFILE_SUPPORT_DVBC) &&
                (service.tunerType == TunerType.CABLE_TUNER_TYPE))
            {
                return false
            }

            if(((systemHint and PROFILE_SUPPORT_DVBS) == PROFILE_SUPPORT_DVBS) &&
                (service.tunerType == TunerType.SATELLITE_TUNER_TYPE))
            {
                return false
            }
        }
        return true
    }

    //Due to our application unified channel list this will only return false
    //if we have scanned channels with requested system(tuner) but are on some other
    //channel with different system(tuner)
    open fun getProfileSupport(context: Context?, camDeliverySystemHint: Int): Boolean {
        val tunerMode = readMtkInternalGlobalIntValue(context!!,Constants.CFG_BS_BS_SRC,0)

        Log.d(TAG, "getProfileSupport for tunerMode: $tunerMode camDeliverySystemHint: $camDeliverySystemHint")

        if(isServiceListEmpty(camDeliverySystemHint)) {
            Log.d(TAG, "Empty service list for system hint $camDeliverySystemHint, allowing")
            return true
        }

        return when (tunerMode) {
            0 -> return (camDeliverySystemHint and PROFILE_SUPPORT_DVBT) == PROFILE_SUPPORT_DVBT
            1 -> return (camDeliverySystemHint and PROFILE_SUPPORT_DVBC) == PROFILE_SUPPORT_DVBC
            2, 3 -> return (camDeliverySystemHint and PROFILE_SUPPORT_DVBS) == PROFILE_SUPPORT_DVBS
            else -> {
                Log.e(TAG, "Wrong system hint, expected $tunerMode got $camDeliverySystemHint")
                false
            }
        }
    }

    private fun getBroadcastType(tunerType: Int) =
        when (tunerType) {
            BS_SRC_AIR -> ScanConstants.BROADCAST_TYPE_DVB_T
            BS_SRC_CABLE -> ScanConstants.BROADCAST_TYPE_DVB_C
            else -> ScanConstants.BROADCAST_TYPE_DVB_S
        }

    private fun readMtkInternalGlobalIntValue(context: Context, name : String, default : Int) : Int {
        try {
            var value = (utilsInterfaceImpl as UtilsInterfaceImpl).readMtkInternalGlobalValue(context, name)
            return Integer.parseInt(value)
        } catch(e : Exception) {
            Log.e(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG +TAG,"readMtkInternalGlobalIntValue: Int conversion failed ${e.message}")
        }
        return default
    }

    private fun getSatelliteOperator() : Int {
        if(readMtkInternalGlobalIntValue(context!!, TUNER_MODE_PREFER_SAT,BS_SAT_GENERAL) == BS_SAT_PREFER) {
            return readMtkInternalGlobalIntValue(context!!, CFG_BS_BS_SAT_BRDCSTER,OPERATOR_GENERAL_SATELLITE)
        } else {
            return OPERATOR_GENERAL_SATELLITE
        }
    }

    private fun getOperator(context: Context, tunerType: Int) =
        when (tunerType) {
            BS_SRC_AIR -> ScanConstants.OPERATOR_DVBT_OTHERS
            BS_SRC_CABLE -> readMtkInternalGlobalIntValue(context, CABLE_BRDCSTER, ScanConstants.OPERATOR_DVBC_OTHERS)
            else -> getSatelliteOperator()
        }

    class CamScanInfoListener(val tvInterface: TvInterface, val utilsInterface: UtilsInterface, val context : Context, val camTvScan: TvScan, val profileType : Int) : TvScan.ScanInfoListener {
        override fun onProgress(progress: Bundle) {
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + "CamScanInfoListener", "onProgress")
        }

        private fun switchToCamList() {
            var oneValue = 1
            (utilsInterface as UtilsInterfaceImpl).saveMtkInternalGlobalValue(context, ChannelDataProvider.CFG_MISC_CH_LST_TYPE, oneValue.toString(),true)
            tvInterface.forceChannelsRefresh()
        }

        override fun onScanCompleted(result: Bundle) {
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + "CamScanInfoListener", "onScanCompleted")

            var scanResult = result.getInt(ScanConstants.KEY_SCAN_RESULT, 0)
            thread {
                when (scanResult) {
                    0 -> {
                        println("scanResult $scanResult profile Type : $profileType")
                        when (profileType) {
                            0 -> {
                                camTvScan.releaseScanSession()
                                switchToCamList()
                                tvInterface.startInitialPlayback(object : IAsyncCallback {
                                    override fun onSuccess() {}

                                    override fun onFailed(error: Error) {}
                                }, ApplicationMode.DEFAULT)
                            }

                            else -> {
                                camTvScan.storeChannelList()
                            }
                        }
                    }

                    1 -> {
                        camTvScan.resetScan()
                        when (profileType) {
                            0 -> {
                                camTvScan.releaseScanSession()
                                switchToCamList()
                                tvInterface.startInitialPlayback(object : IAsyncCallback {
                                    override fun onSuccess() {}

                                    override fun onFailed(error: Error) {}
                                }, ApplicationMode.DEFAULT)
                            }

                            else -> {
                                camTvScan.storeChannelList()
                            }
                        }
                    }

                    2 -> {
                        camTvScan.releaseScanSession()
                        switchToCamList()
                        tvInterface.startInitialPlayback(object : IAsyncCallback {
                            override fun onSuccess() {}

                            override fun onFailed(error: Error) {}
                        }, ApplicationMode.DEFAULT)
                    }

                    else -> {
                        tvInterface.startInitialPlayback(object : IAsyncCallback {
                            override fun onSuccess() {}

                            override fun onFailed(error: Error) {}
                        }, ApplicationMode.DEFAULT)
                    }
                }
            }
        }

        override fun onStoreCompleted(result: Bundle) {
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + "CamScanInfoListener", "onStoreCompleted")
            thread {
                camTvScan.releaseScanSession()
                switchToCamList()
                tvInterface.startInitialPlayback(object : IAsyncCallback {
                    override fun onSuccess() {}

                    override fun onFailed(error: Error) {}
                }, ApplicationMode.DEFAULT)
            }
        }

        override fun onClearCompleted(result: Bundle) {
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + "CamScanInfoListener", "onClearCompleted")
        }

        override fun onEvent(eventType: Int, params: Bundle) {
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + "CamScanInfoListener", "onEvent start, eventType=$eventType, result=$params")
        }
    }

    override fun startCAMScan(isTriggeredByUser : Boolean, isCanceled: Boolean) {
        var needServiceUpdate = mCurrentNeedServiceUpdate
        var profileName = mCurrentProfileName
        var profileType = mCurrentProfileType
        var deliverySystemHint = mCurrentDeliverySystemHint

        if(isTriggeredByUser && mCamMonitoring.getSlotIds().isNotEmpty()) {
            var bundle = mCamProfileName?.getCamServiceUpdateInfo(mCamMonitoring!!.getSlotIds()[0])
            needServiceUpdate = bundle!!.getInt(KEY_NEED_SERVICE_UPDATE)
            profileName = bundle.getString(KEY_CAM_PROFILE_NAME, "")
            profileType = bundle.getInt(KEY_CAM_PROFILE_TYPE)
            deliverySystemHint = bundle.getInt(KEY_CAM_DELIVERY_SYSTEM_HINT)
        }
        startCAMScan(profileType, profileName, needServiceUpdate, deliverySystemHint, isCanceled)
    }

    private fun startCAMScan(profileType : Int , profileName: String, needServiceUpdate : Int, deliverySystemHint: Int, isCanceled: Boolean) {
        if(isCanceled){
            return
        }
        if (getProfileSupport(context, deliverySystemHint)) {
            Handler(Looper.getMainLooper()).post {
                playerInterface.stop()
            }

            //Due to unified channel list if CAM wishes to scan on empty service list
            //we need only to switch to different tuner, no limitations
            if(isServiceListEmpty(deliverySystemHint)) {
                var tunerMode = PlayerInterfaceImpl.BS_SRC_AIR
                if((deliverySystemHint and PROFILE_SUPPORT_DVBT) == PROFILE_SUPPORT_DVBT)
                {
                    tunerMode = PlayerInterfaceImpl.BS_SRC_AIR
                }
                if((deliverySystemHint and PROFILE_SUPPORT_DVBC) == PROFILE_SUPPORT_DVBC)
                {
                    tunerMode = PlayerInterfaceImpl.BS_SRC_CABLE
                }

                if((deliverySystemHint and PROFILE_SUPPORT_DVBS) == PROFILE_SUPPORT_DVBS)
                {
                    tunerMode = PlayerInterfaceImpl.BS_SRC_SAT
                }

                val currentTunerMode =  (utilsInterfaceImpl as UtilsInterfaceImpl).readMtkInternalGlobalIntValue(context, Constants.CFG_BS_BS_SRC,-1)
                if(currentTunerMode != tunerMode) {
                    (utilsInterfaceImpl as UtilsInterfaceImpl).saveMtkInternalGlobalValue(
                        context,
                        Constants.CFG_BS_BS_SRC,
                        tunerMode.toString(),
                        true
                    )
                    (utilsInterfaceImpl as UtilsInterfaceImpl).saveMtkInternalGlobalValue(
                        context,
                        Constants.CFG_BS_BS_USER_SRC,
                        tunerMode.toString(),
                        true
                    )
                }
            }

            (utilsInterfaceImpl as UtilsInterfaceImpl).saveMtkInternalGlobalValue(context!!, PROFILE_NAME,profileName,true)
            utilsInterfaceImpl.savePreference(KEY_CAM_PROFILE_TYPE , profileType)
            utilsInterfaceImpl.savePreference(KEY_NEED_SERVICE_UPDATE, needServiceUpdate)
            utilsInterfaceImpl.savePreference(KEY_CAM_DELIVERY_SYSTEM_HINT, deliverySystemHint)
            val tunerMode: Int = readMtkInternalGlobalIntValue(context!!, Constants.CFG_BS_BS_SRC,0)
            if(tunerMode >= BS_SRC_SAT) {
                mmiSession?.closeMmi()
                mmiSession?.close()
                mmiSession = null
                mmiSessionInit = false
                mmiMenuId = -1
                mmiEnqId = -1
                mmiLevel = 0
                val intent = Intent(MTK_EVENT_EXTRA_TV_SCAN_FOR_COMMON_ACTION)
                intent.putExtra(MTK_EVENT_EXTRA_ACTION_ID, EXTRA_SATELLITE_CAM_SCAN)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.setComponent(ComponentName(WIZARD_PACKAGE, MTK_EVENT_EXTRA_TV_SCAN_FOR_COMMON_ACTION))
                context.startActivity(intent)
            } else {
                var tvScan = when (tunerMode) {
                    BS_SRC_AIR -> TvScan(context, TYPE_DVB_T)
                    BS_SRC_CABLE -> TvScan(context, TYPE_DVB_C)
                    else -> TvScan(context, TYPE_DVB_S)
                }

                tvScan.setScanListener(
                    CamScanInfoListener(
                        tvInterface,
                        utilsInterfaceImpl,
                        context,
                        tvScan,
                        profileType
                    )
                )

                if ((profileType != 0) && (profileType != 2)) {
                    try {
                        val clearBundle = Bundle()
                        clearBundle.putInt(
                            ScanConstants.KEY_BROADCAST_TYPE,
                            getBroadcastType(tunerMode)
                        )
                        clearBundle.putInt(
                            ScanConstants.KEY_OPERATOR_ID,
                            getOperator(context, tunerMode)
                        )
                        clearBundle.putString(
                            ScanConstants.KEY_COUNTRY_CODE,
                            utilsInterfaceImpl.readMtkInternalGlobalValue(context, COUNTRY)
                        )
                        clearBundle.putInt(
                            ScanConstants.KEY_SCAN_TYPE,
                            ScanConstants.SCAN_TYPE_FULL
                        )
                        if (mCamMonitoring.getSlotIds().isNotEmpty())
                            clearBundle.putInt(KEY_SLOT_NUMBER, mCamMonitoring!!.getSlotIds()[0])

                        tvScan.clearChannelList(clearBundle)
                    } catch (E: Exception) {
                        E.printStackTrace()
                    }
                }

                var scanBundle = Bundle()
                scanBundle.putInt(ScanConstants.KEY_BROADCAST_TYPE, getBroadcastType(tunerMode))
                scanBundle.putInt(ScanConstants.KEY_OPERATOR_ID, getOperator(context, tunerMode))
                scanBundle.putString(
                    ScanConstants.KEY_COUNTRY_CODE,
                    utilsInterfaceImpl.readMtkInternalGlobalValue(context, COUNTRY)
                )
                if (mCamMonitoring.getSlotIds().isNotEmpty())
                    scanBundle.putInt(KEY_SLOT_NUMBER, mCamMonitoring!!.getSlotIds()[0])
                scanBundle.putInt(ScanConstants.KEY_SCAN_TYPE, ScanConstants.SCAN_TYPE_FULL)

                tvScan.startScan(scanBundle)
            }
        }
    }

    override fun registerListener(listener: CiPlusInterface.RefCiHandlerListener) {
        listeners.add(listener)
    }

    override fun getCiName(): String {
        var bundle = Bundle()

        if(mCamMonitoring.getSlotIds().isNotEmpty()) {
            mCamInfoMonitoring.getCamAppInfo(mCamMonitoring.getSlotIds()[0], bundle)
        }
        return bundle.getString(KEY_MMI_MENU_STRING) ?: ""
    }

    override fun selectMenuItem(position: Int) {
        mmiLevel++
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"selectMenuItem mmiLevel = $mmiLevel")
        mmiSession?.setMenuListAnswer(position + 1)
    }

    override fun enquiryAnswer(abort: Boolean, answer: String) {
        if(abort) {
            thread {
                for (listener in listeners) {
                    listener.closePopup()
                }
            }
            mmiSession?.closeMmi()
            mmiMenuId = -1
            mmiEnqId = -1
            mmiLevel = 0
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"enquiryAnswer close mmiLevel = $mmiLevel")
            return
        }
        mmiSession?.setEnquiryAnswer(1, answer )
        mmiLevel--
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"enquiryAnswer mmiLevel = $mmiLevel")
    }

    override fun isCamPinEnabled(): Boolean {
        var bundle = Bundle()
        if(mCamMonitoring.getSlotIds().isNotEmpty()) {
            var retCap = mCamPinService.getCamPinCapability(mCamMonitoring.getSlotIds()[0], bundle)
        }
        var cap = bundle.getInt("PIN_CAPABILITY",-1)

        return cap == 2
    }

    override fun setCamPin(pin: String) {
        var pinCode = IntArray(4) { 0 }
        var index = 0
        for(number in pin) {
            pinCode[index] = number.toString().toInt()
            index++
        }

        thread {
            var bundle = Bundle()

            val epochNanoseconds = ChronoUnit.NANOS.between(Instant.EPOCH, Instant.now())
            bundle.putInt("KEY_PIN_CAP_CAPABILITY",0)
            bundle.putLong("KEY_PIN_CAP_DATE_TIME",epochNanoseconds)

            if(mCamMonitoring.getSlotIds().isNotEmpty()) {
                var resultCap =
                    mCamPinService.getCamPinCapability(mCamMonitoring.getSlotIds()[0], bundle)

                var cap = bundle.getInt("KEY_PIN_CAP_CAPABILITY")

                mCamPinService.requestCamPinValidation(
                    mCamMonitoring.getSlotIds()[0],
                    pinCode,
                    object : CamPinStatusListener() {
                        override fun onCamPinValidationReply(slotId: Int, bundle: Bundle) {
                            val pinCodeStatus = bundle.getInt("PINCODE_STATUS")

                            var result = CiPlusInterface.CachedPinResult.CACHED_PIN_FAIL

                            if (pinCodeStatus == 0) { // 0 : Incorrect PIN passed for CICAM PIN verification
                                result = CiPlusInterface.CachedPinResult.CACHED_PIN_FAIL
                            } else if (pinCodeStatus == 1) { // 1 : Host may retry the CAM PIN entry again.
                                result = CiPlusInterface.CachedPinResult.CACHED_PIN_RETRY
                            } else if (pinCodeStatus == 2) { // 2 : Correct PIN passed for CICAM PIN verification
                                (utilsInterfaceImpl as UtilsInterfaceImpl).savePreference(
                                    CASHED_CAM_PIN,
                                    pin
                                )
                                result = CiPlusInterface.CachedPinResult.CACHED_PIN_OK
                            } else if (pinCodeStatus == 3) { // 3 : Host may not be required for retry the CAM PIN entry again.

                            }
                            if (pinCodeStatus != 3) {
                                for (listener in listeners) {
                                    listener.onPinResult(result)
                                }
                            }
                        }
                    })
            }
        }
    }

    override fun getMenu(): MutableList<String> {
        return mutableListOf()
    }

    override fun isCamActive(): Boolean {
        var camSlots = mCamMonitoring.getSlotIds()
        if(camSlots.isNotEmpty()) {
            return true
        }
        return false
    }

    override fun getMenuListID(): Int {
        return mmiMenuId
    }

    override fun getEnqId(): Int {
        return mmiEnqId
    }

    override fun setMMICloseDone() {
        mmiSession?.closeMmi()
        mmiMenuId = -1
        mmiEnqId = -1
    }

    private class EnterMenuErrorCb(val slotId: Int) : EnterMenuErrorCallback() {
        override fun onAppInfoEnterMenuError() {
        }
    }

    inner class mmiStatusCallback(val slotId: Int) : MmiStatusCallback() {

        override fun onMmiListMenu(bundle: Bundle?) {
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"onMmiListMenu received")
            if(bundle != null) {
                var camMenu = CiPlusInterface.CamMenu()
                if(bundle.getStringArray(KEY_MMI_MENULIST_ITEMLIST) != null) {
                    camMenu.menuItems =
                        bundle.getStringArray(KEY_MMI_MENULIST_ITEMLIST)!!.toMutableList()
                }
                camMenu.title = bundle.getString(KEY_MMI_MENULIST_TITLE, "")
                camMenu.subTitle = bundle.getString(KEY_MMI_MENULIST_SUBTITLE, "")
                camMenu.bottom = bundle.getString(KEY_MMI_MENULIST_FOOTERNOTE, "")
                camMenu.id = mmiMenuId

                for(listener in listeners) {
                    listener.onMenuReceived(camMenu)
                }
                mmiMenuId++
            }
        }

        override fun onMmiEnq(bundle: Bundle?) {

            if(bundle != null) {
                Log.d(TAG,"onMmiEnq received")

                var title = bundle.getString(KEY_MMI_MENULIST_TITLE, "")
                var inputText = bundle.getString(KEY_MMI_ENQ_INPUTTEXT, "")
                var blind = bundle.getBoolean(KEY_MMI_ENQ_ISBLIND,false)
                var enquiry = CiPlusInterface.Enquiry()
                enquiry.id = mmiEnqId
                enquiry.title = title
                enquiry.inputText = inputText
                enquiry.blind = blind

                for (listener in listeners) {
                    try {
                        listener.onEnquiryReceived(enquiry)
                    }catch (E: Exception){
                        E.printStackTrace()
                    }
                }
                mmiEnqId++
            }
        }

        override fun onMmiClose() {
            for(listener in listeners) {
                listener.closePopup()
            }
            mmiMenuId = -1
            mmiEnqId = -1
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"onMmiClose mmiLevel = $mmiLevel")
        }
    }

    override fun enterMMI() {
        mmiLevel = 0
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"enterMMI mmiLevel = $mmiLevel")
        if(mCamMonitoring.getSlotIds().isNotEmpty()) {
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"enterMMI appInfoEnterMenu call")
            mMmiInterface.appInfoEnterMenu(
                mCamMonitoring.getSlotIds()[0],
                EnterMenuErrorCb(mCamMonitoring.getSlotIds()[0])
            )
        }
    }

    override fun cancelCurrMenu() {
        if(mmiLevel > 0) {
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG,"cancelCurrMenu setMenuListAnswer mmiLevel = $mmiLevel")
            mmiSession?.setMenuListAnswer(0)
            mmiLevel--
        } else {
            mmiSession?.closeMmi()
            mmiLevel = 0
            Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG," cancelCurrMenu closeMmi mmiLevel = $mmiLevel")
            thread {
                for (listener in listeners) {
                    listener.closePopup()
                }
            }
        }
    }

    override fun isChannelScrambled(): Boolean? {
        return false
    }

    override fun isContentClear(): Boolean {
        return false
    }

    override fun dispose() {

    }

    override fun getProfileName(): String {
        var profileName = (utilsInterfaceImpl as UtilsInterfaceImpl).readMtkInternalGlobalValue(context, PROFILE_NAME)
        if((!ChannelDataProvider.camServicesAvailable) || (profileName == null)) {
            return ""
        }

        return profileName
    }

    override fun deleteProfile(profileName: String) {
        mCurrentProfileName = ""
        (utilsInterfaceImpl as UtilsInterfaceImpl).saveMtkInternalGlobalValue(context!!, PROFILE_NAME,"",true)

        val tunerMode: Int = readMtkInternalGlobalIntValue(context!!, Constants.CFG_BS_BS_SRC,0)
        var tvScan = when (tunerMode) {
            BS_SRC_AIR -> TvScan(context, TYPE_DVB_T)
            BS_SRC_CABLE -> TvScan(context, TYPE_DVB_C)
            else -> TvScan(context, TYPE_DVB_S)
        }

        val bundle = Bundle()
        bundle.putInt(ScanConstants.KEY_BROADCAST_TYPE, getBroadcastType(tunerMode))
        bundle.putInt(ScanConstants.KEY_OPERATOR_ID, getOperator(context, tunerMode))
        if(tunerMode != BS_SRC_SAT) {
            if(mCamMonitoring.getSlotIds().isNotEmpty())
            bundle.putInt(KEY_SLOT_NUMBER, mCamMonitoring!!.getSlotIds()[0])
        }
        bundle.putString(ScanConstants.KEY_COUNTRY_CODE, (utilsInterfaceImpl as UtilsInterfaceImpl).readMtkInternalGlobalValue(context, COUNTRY))
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "clearChannelList setScanListener.")
        tvScan.setScanListener(object : TvScan.ScanInfoListener {
            override fun onClearCompleted(result: Bundle) {
            }

            override fun onEvent(eventType: Int, params: Bundle) {
            }

            override fun onProgress(progress: Bundle) {
            }

            override fun onScanCompleted(result: Bundle) {
            }

            override fun onStoreCompleted(result: Bundle) {
                Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "onStoreCompleted")
                CoroutineScope(Dispatchers.IO).launch {
                    tvScan.releaseScanSession()
                }
            }

        })
        tvScan.clearChannelList(bundle)
        tvScan.storeChannelList()
    }

    override fun enableProfileInstallation() {
        Log.d(com.iwedia.cltv.platform.model.Constants.LogTag.CLTV_TAG + TAG, "enableProfileInstallation")
        CiPlusOPBroadcastReceiver.enableProfileInstallation()
    }

    override fun platformSpecificOperations(operation : CiPlusInterface.PlatformSpecificOperation, parameter : Boolean) {
        when(operation) {
            CiPlusInterface.PlatformSpecificOperation.PSO_RESET_MMI_INTERFACE -> {
                Log.d(TAG, "platformSpecificOperations PSO_RESET_MMI_INTERFACE mmiSessionFirstInitDone $mmiSessionFirstInitDone mmiSessionInit $mmiSessionInit parameter $parameter")
                if (mmiSessionFirstInitDone && !mmiSessionInit && parameter) {
                    Handler(Looper.getMainLooper()).post {
                        //Because under some scenarios MTK DVBS operator CAM scan locks MMI and can only be unlocked by calling for playback stop
                        //should not create glitches because it is called in MainActivity onStart before start of playback.
                        //Figured out trough blood, sweat and tears, so don't remove it
                        playerInterface.stop()
                    }
                    val mSlotIds: IntArray = mCamMonitoring.getSlotIds()
                    if (mSlotIds.isNotEmpty()) {
                        var slot = mCamMonitoring.getSlotIds()[0]
                        mmiSession = mMmiInterface.openSession(slot, mmiStatusCallback(slot))
                        mCamProfileName.requestResendProfileInfoBroadcastACON()
                        mmiSessionInit = true
                    }
                }
            }
            else -> {}
        }
    }

    override fun isCamScanEnabled(): Boolean {
        var bundle = mCamProfileName.getCamServiceUpdateInfo(mCamMonitoring!!.getSlotIds()[0])
        var deliverySystemHint = bundle?.getInt(KEY_CAM_DELIVERY_SYSTEM_HINT, -1)
        Log.d(TAG, "isCamScanEnabled check for $deliverySystemHint")

        if((deliverySystemHint != null) && (deliverySystemHint != -1)) {
            return getProfileSupport(context, deliverySystemHint)
        }
        return false
    }
}