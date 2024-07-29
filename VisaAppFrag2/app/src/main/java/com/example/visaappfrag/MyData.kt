
package com.example.visaapp

import android.widget.CheckBox
import java.io.Serializable

data class MyData (
    var FirstName: String,
    var LastName: String,
    var Country: String
) : Serializable
