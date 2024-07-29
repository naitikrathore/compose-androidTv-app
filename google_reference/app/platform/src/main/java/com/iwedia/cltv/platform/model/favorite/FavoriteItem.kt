package com.iwedia.cltv.platform.model.favorite

import com.iwedia.cltv.platform.model.TvChannel

data class FavoriteItem(
    var id : Int,
    val type: FavoriteItemType,
    var data: Any?,
    var tvChannel: TvChannel,
    var favListIds: ArrayList<String>

)
