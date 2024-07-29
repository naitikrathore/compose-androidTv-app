package com.example.anotherapp

import java.io.Serializable

data class Movie(
    var id: Long = 0,
    var title: String? = null,
    var studio: String? = null
) : Serializable {

}