package com.iwedia.cltv.sdk.handlers

import com.iwedia.cltv.sdk.entities.ReferenceTvChannel
import data_type.GList
import handlers.CategoryHandler
import handlers.DataProvider
import listeners.AsyncDataReceiver
import listeners.AsyncReceiver
import core_entities.Error

class ReferenceCategoryHandler : CategoryHandler {


    constructor(dataProvider: DataProvider<*>) : super(dataProvider)

    override fun init(callback: AsyncReceiver) {

        dataProvider!!.getDataAsync<ReferenceTvChannel>(
            DataProvider.DataType.TV_CHANNEL,
            object : AsyncDataReceiver<GList<ReferenceTvChannel>> {
                override fun onReceive(data: GList<ReferenceTvChannel>) {
                    scanCategories(mapCategoryType("TvChannel"), data, object : AsyncReceiver {
                        override fun onSuccess() {
                            callback.onSuccess()
                        }

                        override fun onFailed(error: Error?) {
                            callback.onSuccess()
                        }
                    })
                }

                override fun onFailed(error: Error?) {
                    callback.onSuccess()
                }
            })
    }

    override fun mapCategoryType(item: Any): Int {
        if (item is String) {
            if (item == "TvChannel") {
                return 0
            } else if (item == "TvEvent") {
                return 1
            } else if (item == "Vod") {
                return 2
            }
        }

        return -1
    }
}