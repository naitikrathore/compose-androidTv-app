package com.iwedia.cltv.config

import android.annotation.SuppressLint
import android.content.ContentResolver
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.content_provider.ContentProvider
import com.iwedia.cltv.platform.model.content_provider.Contract
import kotlinx.coroutines.Dispatchers

class ConfigurableKeysManager {
    data class ConfigurableKey(var keyActionType: Int, var description: String)

    companion object {
        var configurableKeys: HashMap<String, ConfigurableKey> = HashMap()

        fun setup() {
            CoroutineHelper.runCoroutine({
                parseGeneralInfo()
            }, context = Dispatchers.IO)
        }

        @SuppressLint("Range")
        private fun parseGeneralInfo() {
            val contentResolver: ContentResolver = ReferenceApplication.applicationContext().contentResolver
            var cursor = contentResolver.query(
                ContentProvider.CONFIGURABLE_KEYS_URI,
                null,
                null,
                null,
                null
            )
            var keyName: String = ""
            var keyActionType = 0
            var keyDescription = "0"

            if (cursor!!.count > 0) {
                cursor.moveToFirst()
                do {
                    if (cursor.getString(cursor.getColumnIndex(Contract.ConfigurableKeys.KEY_NAME_COLUMN)) != null) {
                        keyName = cursor.getString(
                            cursor.getColumnIndex(
                                Contract.ConfigurableKeys.KEY_NAME_COLUMN
                            )
                        )
                    }
                    if (cursor.getInt(cursor.getColumnIndex(Contract.ConfigurableKeys.KEY_ACTION_TYPE_COLUMN)) != null) {
                        keyActionType = cursor.getInt(
                            cursor.getColumnIndex(
                                Contract.ConfigurableKeys.KEY_ACTION_TYPE_COLUMN
                            )
                        )
                    }
                    if (cursor.getString(cursor.getColumnIndex(Contract.ConfigurableKeys.KEY_DESCRIPTION_COLUMN)) != null) {
                        keyDescription = cursor.getString(
                            cursor.getColumnIndex(
                                Contract.ConfigurableKeys.KEY_DESCRIPTION_COLUMN
                            )
                        )
                    }

                    configurableKeys.put(keyName, ConfigurableKey(keyActionType, keyDescription))
                } while (cursor.moveToNext())
                cursor.close()
            } else {
                configurableKeys.put("liveDPadUp", ConfigurableKey(0, "0"))
                configurableKeys.put("liveDPadDown", ConfigurableKey(0, "0"))
                configurableKeys.put("liveDPadRight", ConfigurableKey(0, "0"))
                configurableKeys.put("liveDPadLeft", ConfigurableKey(0, "0"))
                configurableKeys.put("liveOk", ConfigurableKey(1, "0"))
                configurableKeys.put("liveRed", ConfigurableKey(1, "0"))
                configurableKeys.put("liveGreen", ConfigurableKey(1, "0"))
                configurableKeys.put("liveYellow", ConfigurableKey(1, "0"))
                configurableKeys.put("liveBlue", ConfigurableKey(1, "0"))
            }
        }

        fun getConfigurableKey(keyName: String): ConfigurableKey? {
            return configurableKeys.get(keyName)
        }
    }
}