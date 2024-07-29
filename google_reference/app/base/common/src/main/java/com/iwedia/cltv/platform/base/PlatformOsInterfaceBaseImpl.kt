package com.iwedia.cltv.platform.base

import android.os.Bundle
import com.iwedia.cltv.platform.`interface`.PlatformOsInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PlatformType

class PlatformOsInterfaceBaseImpl : PlatformOsInterface {
    override fun dispose() {
    }

    override fun setup() {
    }

    override fun clearMemory() {
    }

    override fun getData(dataType: Int, callback: IAsyncDataCallback<Any>) {
    }

    override fun getDataByPage(dataType: Int, pageIndex: Int, callback: IAsyncDataCallback<Any>) {
    }

    override fun getDataPageCount(dataType: Int, callback: IAsyncDataCallback<Int>) {
    }

    override fun getPlatformDetails(): List<Any> {
        TODO("Not yet implemented")
    }

    override fun getPlatformName(): String {
        return ""
    }

    override fun getPlatformOsVersion(): String {
        return ""
    }

    override fun loadImage(view: Any, imageUrl: Any) {
    }

    override fun refreshCurrentTime() {
    }

    override fun getCurrentTime(): Long {
        return 0
    }

    override fun runOnNewThread(function: () -> Unit) {
    }

    override fun runOnUiThread(function: () -> Unit) {
    }

    override fun showNotification(type: Int, message: String) {
    }

    override fun refreshCurrentNotification() {
    }

    override fun writeFile(content: String, path: String) {
    }

    override fun readFileContent(): Any {
        return ""
    }

    override fun doesFileExist(filePath: String): Boolean {
        return false
    }

    override fun getPlatformType(): PlatformType {
        return PlatformType.BASE
    }

    override fun sendPlatformPrivateCommand(action: String, data: Bundle) {
    }
}