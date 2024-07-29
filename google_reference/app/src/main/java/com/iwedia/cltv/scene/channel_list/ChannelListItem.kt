package com.iwedia.cltv.scene.channel_list

import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.TvEvent

/**
 * ChannelListItem
 *
 * @author Aleksandar Milojevic
 */
open class ChannelListItem(
    var channel: TvChannel,
    var event: TvEvent?
) {
    var isCurrentChannel: Boolean = false
}