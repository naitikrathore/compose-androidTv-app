package com.iwedia.cltv.platform.model.foryou

/**
 * class used to create one Rail. Rail can be for Channels, Events, Watchlist, Recordings or Scheduled recordings.
 *
 * @author Boris Tirkajla
 */
data class RailItem(
    var id: Int,
    var railName: String,
    var rail: MutableList<Any>?,
    var type: RailItemType
) {

    /**
     * used to distinguish Rail types in RailAdapter.
     */
    enum class RailItemType {
        EVENT,
        CHANNEL,
        RECORDING,
        VOD,
        SCHEDULED_RECORDING,
        BROADCAST_CHANNEL;
    }
    fun areRailItemsIdSame(railItem: RailItem) = this.id == railItem.id
}


