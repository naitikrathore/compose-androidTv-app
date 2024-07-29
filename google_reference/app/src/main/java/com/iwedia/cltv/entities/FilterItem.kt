package com.iwedia.cltv.entities

import core_entities.EntityCategory

class FilterItem {

    companion object {
        /**
         * Undefined
         */
        val UNDEFINED = -1

        /**
         * All id
         */
        val ALL_ID = 0

        /**
         * Recent id
         */
        val RECENT_ID = 2

        /**
         * Movie id
         */
        val MOVIE_ID = 3

        /**
         * Kids id
         */
        val KIDS_ID = 4

        /**
         * Documentary id
         */
        val DOCUMENTARY_ID = 5

        /**
         * Sport id
         */
        val SPORT_ID = 6

        /**
         * TV shows id
         */
        val TV_SHOWS_ID = 7

        /**
         * News id
         */
        val NEWS_ID = 8

        /**
         * Music id
         */
        val MUSIC_ID = 9

        /**
         * Educational id
         */
        val EDUCATIONAL_ID = 10

        /**
         * Genre category
         */
        var GENRE_CATEGORY = 100

        /**
         * Tif input category
         */
        val TIF_INPUT_CATEGORY = 500

        /**
         * Favourite id
         */
        val FAVORITE_ID = 600

        /**
         * Favourite id
         */
        val RECENTLY_WATCHED_ID = 800

        /**
         * Radio channels id
         */
        var RADIO_CHANNELS_ID = 900

        /**
         * Terrestrial tuner type id
         */
        var TERRESTRIAL_TUNER_TYPE_ID = 1000

        /**
         * Cable tuner type id
         */
        var CABLE_TUNER_TYPE_ID = 1100

        /**
         * SATELLITE tuner type id
         */
        var SATELLITE_TUNER_TYPE_ID = 1200

        /**
         * ANALOG Antenna tuner type id
         */
        var ANALOG_ANTENNA_TUNER_TYPE_ID = 1300

        /**
         * ANALOG Cable tuner type id
         */
        var ANALOG_CABLE_TUNER_TYPE_ID = 1400
    }

    /**
     * Info item id
     */
    var id = 0

    /**
     * Info list name
     */
    var name: String? = null

    /**
     * Is info item selected
     */
    var isSelected = false

    /**
     * Neon Entity category
     */
    var entityCategory: EntityCategory? = null

    /**
     * Filter item priority
     */
    var priority: Int = 0

    /**
     * Constructor
     *
     * @param id   info id
     * @param name info name
     */
    constructor (id: Int, name: String?) {
        this.id = id
        this.name = name
    }
}