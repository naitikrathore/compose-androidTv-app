package com.iwedia.cltv.config.entities.json

import com.google.gson.annotations.SerializedName

/**
 * List of json configs for all widgets
 * @author Veljko Ilkic
 */
class JsonSceneConfigParams {

    @SerializedName("title")
    var title: String? = null

    @SerializedName("items")
    var items: MutableList<JsonSceneItemConfig>? = null
}