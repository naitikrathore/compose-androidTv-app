package com.iwedia.cltv.platform.refplus5

import android.os.Bundle
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.`interface`.PlatformOsInterface
import com.iwedia.cltv.platform.model.CoroutineHelper
import com.iwedia.cltv.platform.model.PlatformType
import kotlinx.coroutines.Dispatchers

internal class PlatformOsInterfaceImpl: PlatformOsInterface {
    override fun dispose() {
        TODO("Not yet implemented")
    }

    override fun setup() {
        TODO("Not yet implemented")
    }

    override fun clearMemory() {
        TODO("Not yet implemented")
    }

    override fun getData(dataType: Int, callback: IAsyncDataCallback<Any>) {
        TODO("Not yet implemented")
    }

    override fun getDataByPage(dataType: Int, pageIndex: Int, callback: IAsyncDataCallback<Any>) {
        TODO("Not yet implemented")
    }

    override fun getDataPageCount(dataType: Int, callback: IAsyncDataCallback<Int>) {
        TODO("Not yet implemented")
    }

    override fun getPlatformDetails(): List<Any> {
        TODO("Not yet implemented")
    }

    override fun getPlatformName(): String {
        return "RefPlus_5.0"
    }

    override fun getPlatformOsVersion(): String {
        TODO("Not yet implemented")
    }

    override fun loadImage(view: Any, imageUrl: Any) {
        TODO("Not yet implemented")
    }

    override fun refreshCurrentTime() {
        TODO("Not yet implemented")
    }

    override fun getCurrentTime(): Long {
        TODO("Not yet implemented")
    }

    override fun runOnNewThread(function: () -> Unit) {
        CoroutineHelper.runCoroutine(function, Dispatchers.Main)
    }

    override fun runOnUiThread(function: () -> Unit) {
        CoroutineHelper.runCoroutine(function, Dispatchers.Main)
    }

    override fun showNotification(type: Int, message: String) {
        TODO("Not yet implemented")
    }

    override fun refreshCurrentNotification() {
        TODO("Not yet implemented")
    }

    override fun writeFile(content: String, path: String) {
        TODO("Not yet implemented")
    }

    override fun readFileContent(): Any {
        TODO("Not yet implemented")
    }

    override fun doesFileExist(filePath: String): Boolean {
        TODO("Not yet implemented")
    }

    override fun getPlatformType(): PlatformType {
        TODO("Not yet implemented")
    }

    override fun sendPlatformPrivateCommand(action: String, data: Bundle) {
        TODO("Not yet implemented")
    }
}