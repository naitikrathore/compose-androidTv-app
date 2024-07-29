package com.cltv.mal.model.entities

/**
 * Channel filter item type
 *
 * @author Dejan Nadj
 */
enum class FilterItemType {
    ALL_ID(0),
    GENRE_ID(100),
    TIF_INPUT_CATEGORY(500),
    FAVORITE_ID(600),
    RECENTLY_WATCHED_ID(800),
    RADIO_CHANNELS_ID(900),
    TERRESTRIAL_TUNER_TYPE_ID(1000),
    CABLE_TUNER_TYPE_ID(1100),
    SATELLITE_TUNER_TYPE_ID(1200),
    ANALOG_ANTENNA_TUNER_TYPE_ID(1300),
    ANALOG_CABLE_TUNER_TYPE_ID(1400),

    //Below value used as error code to show Toast
    ONLY_ONE_CHANNEL_IN_LIST(2002);

    private var id: Int = -1

    constructor(id: Int) {
        this.id = id
    }

    fun getFilterId(): Int = id

    companion object {
        fun getFilterTypeById(id: Int): FilterItemType {
            return when (id) {
                0 -> ALL_ID
                100 -> GENRE_ID
                600 -> FAVORITE_ID
                800 -> RECENTLY_WATCHED_ID
                900 -> RADIO_CHANNELS_ID
                1000 -> TERRESTRIAL_TUNER_TYPE_ID
                1100 -> CABLE_TUNER_TYPE_ID
                1200 -> SATELLITE_TUNER_TYPE_ID
                1300 -> ANALOG_ANTENNA_TUNER_TYPE_ID
                1400 -> ANALOG_CABLE_TUNER_TYPE_ID
                2002 -> ONLY_ONE_CHANNEL_IN_LIST
                else ->
                    if (id >= TIF_INPUT_CATEGORY.id && id < FAVORITE_ID.id) {
                        TIF_INPUT_CATEGORY
                    } else {
                        ALL_ID
                    }
            }
        }
    }

}