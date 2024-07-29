package com.iwedia.cltv.platform.mal_service

import android.os.Bundle
import com.cltv.mal.IServiceAPI
import com.iwedia.cltv.platform.`interface`.PlatformOsInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.PlatformType

class PlatformOsInterfaceImpl(private val serviceImpl: IServiceAPI) : PlatformOsInterface {
    override fun dispose() {}

    override fun setup() {}

    override fun clearMemory() {
        serviceImpl.clearMemory()
    }

    override fun getData(dataType: Int, callback: IAsyncDataCallback<Any>) {}

    override fun getDataByPage(dataType: Int, pageIndex: Int, callback: IAsyncDataCallback<Any>) {}

    override fun getDataPageCount(dataType: Int, callback: IAsyncDataCallback<Int>) {}

    override fun getPlatformDetails(): List<Any> = listOf()

    override fun getPlatformName(): String {
        return serviceImpl.platformName
    }

    override fun getPlatformOsVersion(): String {
        return serviceImpl.platformOsVersion
    }

    override fun loadImage(view: Any, imageUrl: Any) {}

    override fun refreshCurrentTime() {}

    override fun getCurrentTime(): Long {
        return 0
    }

    override fun runOnNewThread(function: () -> Unit) {}

    override fun runOnUiThread(function: () -> Unit) {}

    override fun showNotification(type: Int, message: String) {}

    override fun refreshCurrentNotification() {}

    override fun writeFile(content: String, path: String) {
        serviceImpl.writeFile(content, path)
    }

    override fun readFileContent(): Any {
        return ""
    }

    override fun doesFileExist(filePath: String): Boolean {
        return serviceImpl.doesFileExist(filePath)
    }

    override fun getPlatformType(): PlatformType {
        return PlatformType.values()[serviceImpl.platformType]
    }

    override fun sendPlatformPrivateCommand(action: String, data: Bundle) {
        serviceImpl.sendPlatformPrivateCommand(action, data)
    }
}