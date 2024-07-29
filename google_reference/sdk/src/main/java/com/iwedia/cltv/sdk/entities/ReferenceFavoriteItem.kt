package com.iwedia.cltv.sdk.entities

import default_sdk.entities.DefaultFavoriteItem

/**
 * Reference favorite item
 *
 * @author Dejan Nadj
 */
class ReferenceFavoriteItem constructor(
    var tvChannel: ReferenceTvChannel,
    var favListIds: ArrayList<String> = arrayListOf()
): DefaultFavoriteItem(tvChannel.id, FavoriteItemType.TV_CHANNEL, favListIds) {
}