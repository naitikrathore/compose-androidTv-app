package com.example.visaapp

import android.widget.CheckBox
import java.io.Serializable

data class MyData(
    val id:Long,
    var FirstName: String,
    var LastName: String,
    var Country: String
) : Serializable
