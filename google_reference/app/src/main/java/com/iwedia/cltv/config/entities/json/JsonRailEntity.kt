package com.iwedia.cltv.config.entities.json

import com.google.gson.annotations.SerializedName

class JsonRailEntity {
    @SerializedName("index")
    var index: String? = null

    @SerializedName("name")
    var name: String? = null

    @SerializedName("id")
    var id: String? = null
}