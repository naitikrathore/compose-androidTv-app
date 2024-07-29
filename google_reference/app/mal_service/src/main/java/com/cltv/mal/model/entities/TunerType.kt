package com.cltv.mal.model.entities

import android.media.tv.TvContract

enum class TunerType {
    TERRESTRIAL_TUNER_TYPE,
    CABLE_TUNER_TYPE,
    SATELLITE_TUNER_TYPE,
    ANALOG_TUNER_TYPE,
    DEFAULT;

    companion object {
        const val TYPE_ANALOG_ANTENNA = 1
        const val TYPE_ANALOG_CABLE = 2

        fun getTunerType(type: String): TunerType {
            return when (type) {
                TvContract.Channels.TYPE_DVB_T,
                TvContract.Channels.TYPE_DVB_T2,
                TvContract.Channels.TYPE_ATSC_T,
                TvContract.Channels.TYPE_ATSC3_T,
                TvContract.Channels.TYPE_DTMB,
                TvContract.Channels.TYPE_ISDB_T,
                TvContract.Channels.TYPE_T_DMB -> TERRESTRIAL_TUNER_TYPE

                TvContract.Channels.TYPE_DVB_C,
                TvContract.Channels.TYPE_ATSC_C,
                TvContract.Channels.TYPE_DVB_C2 -> CABLE_TUNER_TYPE

                TvContract.Channels.TYPE_NTSC -> ANALOG_TUNER_TYPE

                TvContract.Channels.TYPE_DVB_S,
                TvContract.Channels.TYPE_DVB_S2 -> SATELLITE_TUNER_TYPE

                else -> DEFAULT
            }

        }
    }
}