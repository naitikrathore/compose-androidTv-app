package com.iwedia.cltv.platform.mk5;

public class TvCallbackConst {
    public String toString() {
        return "com.iwedia.cltv.platform.mk5.TvCallbackConst";
    }

    //static variables
    public static final int MSG_CB_BASE_MASK            = 0x0000FFFF;
    public static final int MSG_CB_BASE_FLAG            = 0x70000000;

    //notifyConfigMessage
    public static final int MSG_CB_CONFIG               = MSG_CB_BASE_FLAG + 1;

    //notifyChannelListUpdateMsg
    public static final int MSG_CB_CHANNELIST           = MSG_CB_BASE_FLAG + 2;

    //notifyShowOSDMessage
    public static final int MSG_CB_SHOW_OSD             = MSG_CB_BASE_FLAG + 3;

    //notifyHideOSDMessage
    public static final int MSG_CB_HIDE_OSD             = MSG_CB_BASE_FLAG + 4;

    //notifyScanNotification
    public static final int MSG_CB_SCAN_NOTIFY          = MSG_CB_BASE_FLAG + 5;

    //notifySvctxNotificationCode
    public static final int MSG_CB_SVCTX_NOTIFY         = MSG_CB_BASE_FLAG + 6;

    //notifyGingaMessage
    public final static int MSG_CB_GINGA_MSG            = MSG_CB_BASE_FLAG + 7;

    //notifyAmpVolCtrlMessage
    public final static int MSG_CB_AMP_VOL_CTRL         = MSG_CB_BASE_FLAG + 8;

    //notifyCecNotificationCode
    public static final int MSG_CB_CEC_NFY              = MSG_CB_BASE_FLAG + 9;

    //notifyCecFrameInfo
    public static final int MSG_CB_CEC_FRAME_INFO       = MSG_CB_BASE_FLAG + 10;

    //notifySysAudMod
    public static final int MSG_CB_SYS_AUD_MOD          = MSG_CB_BASE_FLAG + 10 + 1;

    //notifyCecActiveSource
    public static final int MSG_CB_CEC_ACTIVE_SRC       = MSG_CB_BASE_FLAG + 10 + 2;

    //notifyMhlScratchpadData
    public static final int MSG_CB_MHL_SCRATCHPAD       = MSG_CB_BASE_FLAG + 10 + 3;

    //notifySpdInfoFrame
    public static final int MSG_CB_SPD_INFO_FRAME       = MSG_CB_BASE_FLAG + 10 + 4;

    //notifyEventNotification
    public static final int MSG_CB_EVENT_NFY            = MSG_CB_BASE_FLAG + 10 + 5;

    //notifyPVRPlayNotification
    public static final int MSG_CB_PVR_PLAY             = MSG_CB_BASE_FLAG + 10 + 6;

    //notifyChannelListUpdateMsgByType
    public static final int MSG_CB_CHANNEL_LIST_UPDATE  = MSG_CB_BASE_FLAG + 10 + 7;

    //notifyTimeshiftNotification
    public static final int MSG_CB_TIME_SHIFT_NFY       = MSG_CB_BASE_FLAG + 10 + 8;

    //notifyPipPopMessage
    public static final int MSG_CB_PIP_POP_MSG          = MSG_CB_BASE_FLAG + 10 + 9;

    //notifyAVModeMessage
    public static final int MSG_CB_AV_MODE_MSG          = MSG_CB_BASE_FLAG + 2 * 10;

    //notifyMHEG5Message
    public static final int MSG_CB_MHEG5_MSG            = MSG_CB_BASE_FLAG + 2 * 10 + 1;

    //notifyMHPMessage
    public static final int MSG_CB_MHP_MSG              = MSG_CB_BASE_FLAG + 2 * 10 + 2;

    //notifyCIMessage
    public static final int MSG_CB_CI_MSG               = MSG_CB_BASE_FLAG + 2 * 10 + 3;

    //notifyOADMessage
    public static final int MSG_CB_OAD_MSG              = MSG_CB_BASE_FLAG + 2 * 10 + 4;

    //notifyHBBTVMessage
    public static final int MSG_CB_HBBTV_MSG            = MSG_CB_BASE_FLAG + 2 * 10 + 5;

    //notifyNoUsedkeyMessage
    public static final int MSG_CB_NO_USED_KEY_MSG      = MSG_CB_BASE_FLAG + 2 * 10 + 6;

    //notifySubtitleMessage
    public static final int MSG_CB_SUBTITLE_MSG         = MSG_CB_BASE_FLAG + 2 * 10 + 7;

    //notifyWarningMessage
    public static final int MSG_CB_WARNING_MSG          = MSG_CB_BASE_FLAG + 2 * 10 + 8;

    //notifyEASMessage
    public static final int MSG_CB_EAS_MSG              = MSG_CB_BASE_FLAG + 2 * 10 + 9;

    //notifyInputSourceMessage
    public static final int MSG_CB_INPUT_SOURCE_MSG     = MSG_CB_BASE_FLAG + 3 * 10;

    //notifyTeletextMessage
    public static final int MSG_CB_TTX_MSG              = MSG_CB_BASE_FLAG + 3 * 10 + 1;

    //notifyFeatureMessage
    public static final int MSG_CB_FEATURE_MSG          = MSG_CB_BASE_FLAG + 3 * 10 + 2;

    //notifyUpgradeMessage
    public static final int MSG_CB_UPGRADE_MSG          = MSG_CB_BASE_FLAG + 3 * 10 + 3;

    //notifyBannerMessage
    public static final int MSG_CB_BANNER_MSG           = MSG_CB_BASE_FLAG + 3 * 10 + 4;

    //notifyCCMessage
    public static final int MSG_CB_CC_MSG               = MSG_CB_BASE_FLAG + 3 * 10 + 5;

    //notifyPWDDialogMessage
    public static final int MSG_CB_PWD_DLG_MSG          = MSG_CB_BASE_FLAG + 3 * 10 + 6;

    //notifyScreenSaverMessage
    public static final int MSG_CB_SCREEN_SAVER_MSG     = MSG_CB_BASE_FLAG + 3 * 10 + 7;

    //notifyATSCEventMessage
    public static final int MSG_CB_ATSC_EVENT_MSG       = MSG_CB_BASE_FLAG + 3 * 10 + 8;

    //notifyOpenVCHIPMessage
    public static final int MSG_CB_OPEN_VCHIP_MSG       = MSG_CB_BASE_FLAG + 3 * 10 + 9;

    //notifyDeviceDiscovery
    public static final int MSG_CB_DEVICE_DISCOVERY     = MSG_CB_BASE_FLAG + 4 * 10;

    //notifyRecordNotification
    public static final int MSG_CB_RECORD_NFY           = MSG_CB_BASE_FLAG + 4 * 10 + 1;

    //notifyBANNER MSG_CB_BANNER_CHANNEL_LOGO
    public static final int MSG_CB_BANNER_CHANNEL_LOGO  = MSG_CB_BASE_FLAG + 4 * 10 + 2;

    //notifyUiMsDisplay
    public static final int MSG_CB_NFY_CEC_UI_DISP      = MSG_CB_BASE_FLAG + 4 * 10 + 3;

    //notifyNativeAppStatus
    public static final int MSG_CB_NFY_NATIVE_APP_STATUS= MSG_CB_BASE_FLAG + 4 * 10 + 4;

    //notifySatlListUpdateMsg
    public static final int MSG_CB_NFY_UPDATE_SATELLITE_LIST= MSG_CB_BASE_FLAG + 4 * 10 + 5;

    //notifyTvproviderUpdateMsg
    public static final int MSG_CB_NFY_UPDATE_TV_PROVIDER_LIST= MSG_CB_BASE_FLAG + 4 * 10 + 6;

    //notifyTslIdUpdateUpdateMsg
    public static final int MSG_CB_NFY_TSL_ID_UPDATE_MSG= MSG_CB_BASE_FLAG + 4 * 10 + 7;

    //notifyTslIdUpdateUpdateMsg
    public static final int MSG_CB_NFY_TUNE_CHANNEL_BROADCAST_MSG= MSG_CB_BASE_FLAG + 4 * 10 + 8;

    //notifyEWSPAMessage
    public static final int MSG_CB_EWS_MSG              = MSG_CB_BASE_FLAG + 4 * 10 + 9;

    //notifyFVPMessage
    public static final int MSG_CB_FVP_MSG              = MSG_CB_BASE_FLAG + 4 * 10 + 10;
    //notifyGingaMessage
    public final static int MSG_CB_GINGA_VOLUME_MSG     = MSG_CB_BASE_FLAG + + 5 * 10 + 1;

    //notifyBMLMessage
    public static final int MSG_CB_BML_MSG              = MSG_CB_BASE_FLAG + 5 * 10 + 2;

    //notifyGingaUpdateMessage
    public static final int MSG_CB_GINGA_UPDATE_MSG     = MSG_CB_BASE_FLAG + 5 * 10 + 3;
    
    //notifyEWSMessage
    public static final int MSG_CB_EWS_SA_MSG              = MSG_CB_BASE_FLAG + 5 * 10 + 4;


    //notifyOnScreenMessageNotification
    public static final int MSG_CB_ON_SCREEN_A3WAM_MSG     = MSG_CB_BASE_FLAG + 5 * 10 + 5;

    //notifyVideoInfoMessage
    public static final int MSG_CB_VIDEO_INFO_MSG     = MSG_CB_BASE_FLAG + 5 * 10 + 6;
}
