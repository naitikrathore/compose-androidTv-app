package com.iwedia.cltv.platform.`interface`

import android.os.Bundle
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PlatformType

interface PlatformOsInterface {

    fun dispose()

    fun setup()

    fun clearMemory()

    fun getData(dataType: Int, callback: IAsyncDataCallback<Any>)
    fun getDataByPage(dataType: Int, pageIndex: Int, callback: IAsyncDataCallback<Any>)
    fun getDataPageCount(dataType: Int, callback: IAsyncDataCallback<Int>)

    fun getPlatformDetails(): List<Any>
    fun getPlatformName(): String
    fun getPlatformOsVersion(): String

    fun loadImage(view: Any, imageUrl: Any)

    fun refreshCurrentTime()
    fun getCurrentTime(): Long

    fun runOnNewThread(function: () -> Unit)
    fun runOnUiThread(function: () -> Unit)
    
    fun showNotification(type: Int, message: String)
    fun refreshCurrentNotification()

    fun writeFile(content: String, path: String)
    fun readFileContent(): Any
    fun doesFileExist(filePath: String): Boolean
    fun getPlatformType(): PlatformType
    fun sendPlatformPrivateCommand(action: String, data: Bundle)
}