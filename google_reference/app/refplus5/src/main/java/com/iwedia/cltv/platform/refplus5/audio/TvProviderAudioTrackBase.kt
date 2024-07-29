package com.iwedia.cltv.platform.refplus5.audio

import com.mediatek.dtv.tvinput.framework.tifextapi.common.audio.Constants

class TvProviderAudioTrackBase {
    companion object {
        /*audio type*/
        const val AUD_TYPE_UNKNOWN = 0
        const val AUD_TYPE_CLEAN = 1
        const val AUD_TYPE_HEARING_IMPAIRED = 2
        const val AUD_TYPE_VISUAL_IMPAIRED = 3
        const val AUD_TYPE_RESERVED = 4
        const val AUD_TYPE_COMPLETE_MAIN = 5
        const val AUD_TYPE_MUSIC_AND_EFFECT = 6
        const val AUD_TYPE_DIALOGUE = 7
        const val AUD_TYPE_COMMENTARY = 8
        const val AUD_TYPE_EMERGENCY = 9
        const val AUD_TYPE_VOICE_OVER = 8
        const val AUD_TYPE_KARAOKE = 9


        /*audio mix type*/
        const val AUD_MIX_TYPE_UNKNOWN = Constants.AudioMixType.AUDIO_MIX_TYPE_UNKNOWN
        const val AUD_MIX_TYPE_SUPPLEMENTARY = Constants.AudioMixType.AUDIO_MIX_TYPE_DEPENDENT
        const val AUD_MIX_TYPE_INDEPENDENT = Constants.AudioMixType.AUDIO_MIX_TYPE_INDEPENDENT

        /* audio editorial classification coding */
        const val AUD_EDITORIAL_CLASS_RESERVED = Constants.AudioEClass.AUDIO_E_CLASS_RESERVED
        const val AUD_EDITORIAL_CLASS_MAIN = Constants.AudioEClass.AUDIO_E_CLASS_MAIN
        const val AUD_EDITORIAL_CLASS_VISUAL_IMPAIRED_AD =
            Constants.AudioEClass.AUDIO_E_CLASS_VISUAL_IMPAIRED
        const val AUD_EDITORIAL_CLASS_HEARING_IMPAIRED_CLEAN =
            Constants.AudioEClass.AUDIO_E_CLASS_HEARING_IMPAIRED
        const val AUD_EDITORIAL_CLASS_VISUAL_IMPAIRED_SPOKEN_SUBTITLE =
            Constants.AudioEClass.AUDIO_E_CLASS_SPOKEN_SUBTITLES


        /** < Audio Encoding type. */
        const val AUD_ENC_UNKNOWN = 0
        const val AUD_ENC_AC3 = 1
        const val AUD_ENC_MPEG_1 = 2
        const val AUD_ENC_MPEG_2 = 3
        const val AUD_ENC_PCM = 4
        const val AUD_ENC_TV_SYS = 5
        const val AUD_ENC_DTS = 6
        const val AUD_ENC_AAC = 7
        const val AUD_ENC_EU_CANAL_PLUS = 8
        const val AUD_ENC_WMA_V1 = 9
        const val AUD_ENC_WMA_V2 = 10
        const val AUD_ENC_WMA_V3 = 11
        const val AUD_ENC_E_AC3 = 12
        const val AUD_ENC_LPCM = 13
        const val AUD_ENC_FM_RADIO = 14
        const val AUD_ENC_COOK = 15
        const val AUD_ENC_DRA = 16
        const val AUD_ENC_VORBIS = 17
        const val AUD_ENC_WMA_PRO = 18
        const val AUD_ENC_WMA_LOSSLESS = 19
        const val AUD_ENC_AWB = 20
        const val AUD_ENC_AMR = 21
        const val AUD_ENC_FLAC = 22
        const val AUD_ENC_APE = 23
        const val AUD_ENC_ALAC = 24
        const val AUD_ENC_OPUS = 25
        const val AUD_ENC_AC4 = 26
        const val AUD_DECODE_TYPE_UNKNOWN = 0
        const val AUD_DECODE_TYPE_AC3 = 1
        const val AUD_DECODE_TYPE_EAC3 = 2
        const val AUD_DECODE_TYPE_FLAC = 3
        const val AUD_DECODE_TYPE_AAC = 4
        const val AUD_DECODE_TYPE_HEAAC = 5
        const val AUD_DECODE_TYPE_HEAAC_V2 = 6
        const val AUD_DECODE_TYPE_LPCM_ALAW = 7
        const val AUD_DECODE_TYPE_LPCM_ULAW = 8
        const val AUD_DECODE_TYPE_MPEG1_LAYER3 = 9
        const val AUD_DECODE_TYPE_MPEG1_LAYR2 = 10
        const val AUD_DECODE_TYPE_MPEG1_LAYER1 = 11
        const val AUD_DECODE_TYPE_MPEG2_LAYER3 = 12
        const val AUD_DECODE_TYPE_MPEG2_LAYER2 = 13
        const val AUD_DECODE_TYPE_MPEG2_LAYER1 = 14
        const val AUD_DECODE_TYPE_DTS_NORMAL = 15
        const val AUD_DECODE_TYPE_DTS_EXPRESS = 16
        const val AUD_DECODE_TYPE_DTS_HS = 17
        const val AUD_DECODE_TYPE_AC4 = 18
        const val AUD_DECODE_TYPE_PCM = 19
        const val AUD_FMT_UNKNOWN = 0

        /** < Unknown audio format */
        const val AUD_FMT_MONO = 1

        /** < Monophonic audio */
        const val AUD_FMT_DUAL_MONO = 2

        /** < Dual mono audio */
        const val AUD_FMT_STEREO = 3

        /** < Stereo sound */
        const val AUD_FMT_TYPE_5_1 = 4

        /** < 5.1 channel audio */
        const val AUD_FMT_SUBSTREAM = 5

        /* TODO */
        const val A_MAIN_AUDIO = "a_main_audio"
        const val A_MAIN_AUDIO_HAS_AD = "a_main_audio_has_ad"
        const val A_MAIN_AUDIO_HAS_SPS = "a_main_audio_has_sps"
        const val A_MAIN_AUDIO_HAS_AD_SPS = "a_main_audio_has_ad_sps"
        const val A_HI_AUDIO = "a_hi_audio"
        const val A_BROADCAST_MIXED_VI_AUDIO = "a_broadcast_mixed_vi_audio"
        const val A_RECEIVE_MIX_VI = "a_receive_mix_vi"
        const val A_RECEIVE_MIXED_VI_MAIN = "a_receive_mixed_vi_main"
        const val A_RECEIVE_MIXED_VI_MAIN_ID = "a_receive_mixed_vi_main_id"
        const val A_RECEIVE_MIXED_VI_MAIN_MESSAGE_ID = "a_receive_mixed_vi_main_msg_id"
        const val A_DISPLAY_AUDIO_LANGUAGE = "a_display_audio_language"
        const val A_SPS_MIX_AUDIO = "a_sps_mix_audio"
        const val A_SPS_MIXED_MAIN = "a_sps_mixed_main"
        const val A_SPS_MIXED_MAIN_ID = "a_sps_mixed_main_id"
        const val A_AD_SPS_MIX_AUDIO = "a_ad_sps_mix_audio"
        const val A_AD_SPS_MIXED_MAIN_AUDIO = "a_ad_sps_mixed_main_audio"
        const val A_AD_SPS_MIXED_MAIN_ID_AUDIO = "a_ad_sps_mixed_main_id_audio"

        const val SCDB_OPTION_MASK_AC4_PRE_STRM_COMP = 8
        const val SCDB_OPTION_MASK_AC4_AUX_STREAM_COMP = 4

        const val DOBLY_TYPE_NONE = 0
        const val DOBLY_TYPE_ATOMS = 1
        const val DOBLY_TYPE_AUDIO = 2

        val VALUE_TIS_AUDIO_CODEC_DOLBY_PULSE: Int =
            Constants.AudioCodec.AUDIO_CODEC_DOLBY_PULSE // TODO: not found
        val VALUE_TIS_AUDIO_CODEC_AAC: Int =
            Constants.AudioCodec.AUDIO_CODEC_AAC // MIMETYPE_AUDIO_AAC = "audio/mp4a-latm"
        val VALUE_TIS_AUDIO_CODEC_HE_AAC: Int =
            Constants.AudioCodec.AUDIO_CODEC_HE_AAC // TODO: not found
        val VALUE_TIS_AUDIO_CODEC_DTS_MONO: Int =
            Constants.AudioCodec.AUDIO_CODEC_DTS_MONO // TODO: not found
        val VALUE_TIS_AUDIO_CODEC_DTS_STEREO: Int =
            Constants.AudioCodec.AUDIO_CODEC_DTS_STEREO // TODO: not found
        val VALUE_TIS_AUDIO_CODEC_DTS_SURROUND: Int =
            Constants.AudioCodec.AUDIO_CODEC_DTS_SURROUND // TODO: not found
        val VALUE_TIS_AUDIO_CODEC_DRA: Int = Constants.AudioCodec.AUDIO_CODEC_DRA // TODO: not found
        val VALUE_TIS_AUDIO_CODEC_AC3: Int =
            Constants.AudioCodec.AUDIO_CODEC_AC3 // MIMETYPE_AUDIO_AC3 = "audio/ac3";
        val VALUE_TIS_AUDIO_CODEC_E_AC3: Int =
            Constants.AudioCodec.AUDIO_CODEC_E_AC3 // MIMETYPE_AUDIO_EAC3 = "audio/eac3";
        val VALUE_TIS_AUDIO_CODEC_HE_AAC_V2: Int =
            Constants.AudioCodec.AUDIO_CODEC_HE_AAC_V2 // TODO: not found
        val VALUE_TIS_AUDIO_CODEC_DTS: Int = Constants.AudioCodec.AUDIO_CODEC_DTS // TODO: not found
        val VALUE_TIS_AUDIO_CODEC_AC4: Int =
            Constants.AudioCodec.AUDIO_CODEC_AC4 // MIMETYPE_AUDIO_AC4 = "audio/ac4";
        val VALUE_TIS_AUDIO_CODEC_DOLBY_TRUE_HD: Int =
            Constants.AudioCodec.AUDIO_CODEC_DOLBY_TRUE_HD // TODO: not found
        // val VALUE_TIS_AUDIO_CODEC_DOLBY_MAT: Int =
        //     Constants.AudioCodec.AUDIO_CODEC_DOLBY_MAT // TODO: not found
        val VALUE_TIS_AUDIO_CODEC_DOLBY_DD_PLUS: Int =
            Constants.AudioCodec.AUDIO_CODEC_DOLBY_DD_PLUS // Dolby DD+.
        val VALUE_TIS_AUDIO_CODEC_DOLBY_AC4: Int =
            Constants.AudioCodec.AUDIO_CODEC_DOLBY_AC4 // Dolby AC4.
        val VALUE_TIS_AUDIO_CODEC_DOLBY_ATMOS_TRUE_HD: Int =
            Constants.AudioCodec.AUDIO_CODEC_DOLBY_ATMOS_TRUE_HD // Dolby Atmos TrueHD.
        val VALUE_TIS_AUDIO_CODEC_DOLBY_ATMOS_DD_PLUS: Int =
            Constants.AudioCodec.AUDIO_CODEC_DOLBY_ATMOS_DD_PLUS // Dolby Atmos DD+.
        val VALUE_TIS_AUDIO_CODEC_DOLBY_ATMOS_AC4: Int =
            Constants.AudioCodec.AUDIO_CODEC_DOLBY_ATMOS_AC4 // Dolby Atmos AC4.
        val VALUE_TIS_AUDIO_CODEC_MPEG_L2: Int = Constants.AudioCodec.AUDIO_CODEC_MPEG_L2
        val VALUE_TIS_AUDIO_CODEC_DTS_HD: Int =
            Constants.AudioCodec.AUDIO_CODEC_DTS_HD //  MIMETYPE_DTS_HD ="audio/vnd.dts.hd"
        val VALUE_TIS_AUDIO_CODEC_DTS_HD_LBR: Int =
            Constants.AudioCodec
                .AUDIO_CODEC_DTS_HD_LBR //  AUDIO_DTS_HD_LBR ="audio/vnd.dts.hd; profile=lbr"
        val VALUE_TIS_AUDIO_CODEC_DTS_HD_HR: Int =
            Constants.AudioCodec
                .AUDIO_CODEC_DTS_HD_HR //  AUDIO_DTS_HD_HR ="audio/vnd.dts.hd; profile=DTSHR"
        val VALUE_TIS_AUDIO_CODEC_DTS_HD_MA: Int =
            Constants.AudioCodec
                .AUDIO_CODEC_DTS_HD_MA //  AUDIO_DTS_HD_MA ="audio/vnd.dts.hd; profile=DTSMA"
        val VALUE_TIS_AUDIO_CODEC_DTS_X_P1: Int =
            Constants.AudioCodec
                .AUDIO_CODEC_DTS_X_P1 //  AUDIO_DTS_X_P1 = "audio/vnd.dts.hd; profile=MULTI1"
        val VALUE_TIS_AUDIO_CODEC_DTS_X_P2: Int =
            Constants.AudioCodec
                .AUDIO_CODEC_DTS_X_P2 //  AUDIO_DTS_X_P2 ="audio/vnd.dts.hd; profile=p2"
        val VALUE_TIS_AUDIO_CODEC_DTS_UHD_P2: Int =
            Constants.AudioCodec
                .AUDIO_CODEC_DTS_UHD_P2 //  AUDIO_CODEC_DTS_UHD_P2 ="audio/vnd.dts.uhd; profile=p2
        val VALUE_TIS_AUDIO_CODEC_MPEG4_AAC_LC: Int =
            Constants.AudioCodec.AUDIO_CODEC_MPEG4_AAC_LC //  MPEG4 AAC_LC.
        val VALUE_TIS_AUDIO_CODEC_MPEG2_AAC_LC: Int =
            Constants.AudioCodec.AUDIO_CODEC_MPEG2_AAC_LC //  MPEG2 AAC_LC.
        val VALUE_TIS_AUDIO_CODEC_RAW: Int =
            Constants.AudioCodec.AUDIO_CODEC_RAW //  AUDIO_RAW = "audio/raw"
        val VALUE_TIS_AUDIO_CODEC_MPEGH_MHA1: Int = Constants.AudioCodec.AUDIO_CODEC_MPEGH_MHA1
        val VALUE_TIS_AUDIO_CODEC_MPEGH_MHM1: Int = Constants.AudioCodec.AUDIO_CODEC_MPEGH_MHM1
        val VALUE_TIS_AUDIO_CODEC_MPEGH_BL_L3: Int = Constants.AudioCodec.AUDIO_CODEC_MPEGH_BL_L3
        val VALUE_TIS_AUDIO_CODEC_MPEGH_BL_L4: Int = Constants.AudioCodec.AUDIO_CODEC_MPEGH_BL_L4
        val VALUE_TIS_AUDIO_CODEC_MPEGH_LC_L3: Int = Constants.AudioCodec.AUDIO_CODEC_MPEGH_LC_L3
        val VALUE_TIS_AUDIO_CODEC_MPEGH_LC_L4: Int = Constants.AudioCodec.AUDIO_CODEC_MPEGH_LC_L4

        const val MPEG1_L2 = "MPEG1-L2"
        const val PCM = "PCM"
        const val AAC = "AAC"
        const val HE_AAC = "HE_AAC"
        const val HE_AACv2 = "HE_AACv2"
        const val DTS = "DTS"
        const val AAC_LC = "AAC_LC"
        const val DTS_HD = "DTS-HD"
        const val DTSX = "DTS:X"
        const val MPEG_H = "MPEG-H"
        const val DRA = "DRA"
        const val DTS_EXPRESS = "DTS_EXPRESS"
        const val DTS_HD_MASTER_AUDIO = "DTS_HD Master Audio"



    }
}