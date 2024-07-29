package com.iwedia.cltv.platform.rtk.util

class MtsUtil {

    companion object {
        // AUDIO_MTS_TYPE
        private val AUDIO_MTS_TYPE_MONO = 1
        private val AUDIO_MTS_TYPE_STEREO = 2
        private val AUDIO_MTS_TYPE_SAP_MONO = 4
        private val AUDIO_MTS_TYPE_SAP_STEREO = 5
        private val AUDIO_MTS_TYPE_DAUL_A = 6
        private val AUDIO_MTS_TYPE_DUAL_B = 7
        private val AUDIO_MTS_TYPE_DUAL_AB = 8
        private val AUDIO_MTS_TYPE_AUTO = 9
        private val AUDIO_MTS_TYPE_NICAM_MONO = 10


        private val LISTTYPE_MONO = 0
        private val LISTTYPE_STEREO = 1
        private val LISTTYPE_DAUL = 2
        private val LISTTYPE_NICAM_DAUL = 3
        private val LISTTYPE_SAP_MONO = 4
        private val LISTTYPE_SAP_STEREO = 5
        private val LISTTYPE_NICAM_MONO = 6

        fun findMtsTypeIndex(listType: Int, listIndex: Int): Int {
            return when (listType) {
                LISTTYPE_MONO -> AUDIO_MTS_TYPE_MONO
                LISTTYPE_STEREO -> when (listIndex) {
                    0 -> AUDIO_MTS_TYPE_MONO
                    1 -> AUDIO_MTS_TYPE_STEREO
                    else -> AUDIO_MTS_TYPE_AUTO
                }

                LISTTYPE_DAUL -> when (listIndex) {
                    0 -> AUDIO_MTS_TYPE_DAUL_A
                    1 -> AUDIO_MTS_TYPE_DUAL_B
                    2 -> AUDIO_MTS_TYPE_DUAL_AB
                    else -> AUDIO_MTS_TYPE_AUTO
                }

                LISTTYPE_NICAM_DAUL -> when (listIndex) {
                    0 -> AUDIO_MTS_TYPE_MONO
                    1 -> AUDIO_MTS_TYPE_DAUL_A
                    2 -> AUDIO_MTS_TYPE_DUAL_B
                    3 -> AUDIO_MTS_TYPE_DUAL_AB
                    else -> AUDIO_MTS_TYPE_AUTO
                }

                LISTTYPE_SAP_MONO -> when (listIndex) {
                    0 -> AUDIO_MTS_TYPE_MONO
                    1 -> AUDIO_MTS_TYPE_SAP_MONO
                    else -> AUDIO_MTS_TYPE_AUTO
                }

                LISTTYPE_SAP_STEREO -> when (listIndex) {
                    0 -> AUDIO_MTS_TYPE_MONO
                    1 -> AUDIO_MTS_TYPE_STEREO
                    2 -> AUDIO_MTS_TYPE_SAP_STEREO
                    else -> AUDIO_MTS_TYPE_AUTO
                }

                LISTTYPE_NICAM_MONO -> when (listIndex) {
                    0 -> AUDIO_MTS_TYPE_MONO
                    1 -> AUDIO_MTS_TYPE_NICAM_MONO
                    else -> AUDIO_MTS_TYPE_AUTO
                }

                else -> AUDIO_MTS_TYPE_AUTO
            }
        }
    }
}