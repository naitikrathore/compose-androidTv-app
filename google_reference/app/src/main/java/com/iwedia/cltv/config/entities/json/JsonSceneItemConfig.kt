package com.iwedia.cltv.config.entities.json

import com.google.gson.annotations.SerializedName

/**
 * Json config for single widget in scene
 */
class JsonSceneItemConfig {

    @SerializedName("id")
    var id: String? = null

    @SerializedName("name")
    var name: String? = null

    @SerializedName("visibility")
    var visibility: String? = null

    @SerializedName("source")
    var source: String? = null

    @SerializedName("height")
    var height: String? = null

    @SerializedName("width")
    var width: String? = null

    @SerializedName("background")
    var background: String? = null

    @SerializedName("alignmentX")
    var alignmentX: String? = null

    @SerializedName("alignmentY")
    var alignmentY: String? = null

    @SerializedName("leftMargin")
    var leftMargin: String? = null

    @SerializedName("rightMargin")
    var rightMargin: String? = null

    @SerializedName("topMargin")
    var topMargin: String? = null

    @SerializedName("bottomMargin")
    var bottomMargin: String? = null

    @SerializedName("data")
    var data: HashMap<String, String>? = null

    @SerializedName("rail_data")
    var railData: MutableList<JsonRailEntity>? = null
}
