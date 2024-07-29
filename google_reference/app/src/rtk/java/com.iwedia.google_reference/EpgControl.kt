package com.iwedia.cltv

import android.content.Context
import com.iwedia.cltv.platform.model.Constants

class EpgControl private constructor() {

    fun registerReceiver(context: Context) {
    }

    companion object {
        private const val TAG = "EpgControl"

        @get:Synchronized
        var instance: EpgControl? = null
            get() {
                android.util.Log.d(Constants.LogTag.CLTV_TAG + TAG, "Getting EpgControl instance ")
                if (field == null) {
                    field = EpgControl()
                }
                return field
            }
            private set
    }
}