package com.iwedia.cltv.platform.model.fast_backend_utils

import java.util.*

object LocaleHelper {
    fun getCurrentLocale(): String {
        return Locale.getDefault().toString()
    }
}