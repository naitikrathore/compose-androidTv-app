package com.iwedia.cltv.platform.base

import com.iwedia.cltv.platform.`interface`.OadUpdateInterface
import com.iwedia.cltv.platform.`interface`.PlayerInterface

open class OadUpdateInterfaceBaseImpl(playerInterface: PlayerInterface) : OadUpdateInterface {
    override fun enableOad(enable: Boolean) {
    }

    override fun startScan() {
    }

    override fun stopScan() {
    }

    override fun startDetect() {
    }

    override fun stopDetect() {
    }

    override fun startDownload() {
    }

    override fun stopDownload() {
    }

    override fun applyOad() {
    }

    override fun getSoftwareVersion(): Int {
        return 0
    }

    override fun registerListener(listener: OadUpdateInterface.OadEventListener) {
    }

    override fun unregisterListener(listener: OadUpdateInterface.OadEventListener) {
    }
}