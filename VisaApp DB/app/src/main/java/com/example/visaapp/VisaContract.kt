package com.example.visaapp

import android.net.Uri
import android.provider.BaseColumns

object VisaContract {
    const val CONTENT_AUTHORITY = "com.example.visaapp.provider"
    val BASE_CONTENT_URI: Uri = Uri.parse("content://$CONTENT_AUTHORITY")

    object VisaEntry : BaseColumns {
        const val TABLE_NAME = "visadata"
        const val COLUMN_FIRST_NAME = "first_name"
        const val COLUMN_LAST_NAME = "last_name"
        const val COLUMN_COUNTRY = "country"

        val CONTENT_URI: Uri = BASE_CONTENT_URI.buildUpon().appendPath(TABLE_NAME).build()
    }
}
