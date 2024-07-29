package com.iwedia.cltv.platform.`interface`

interface OadUpdateInterface {
    interface OadEventListener {
        fun onFileFound(version : Int)
        fun onFileNotFound()
        fun onScanStart()
        fun onScanProgress(progress : Int)
        fun onDownloadStarted()
        fun onDownloadProgress(progress : Int)
        fun onDownloadFail()
        fun onDownloadSucess()
        fun onUpgradeSuccess(version : Int)
        fun onNewestVersion()
    }

    fun enableOad(enable: Boolean)
    fun startScan()
    fun stopScan()
    fun startDetect()
    fun stopDetect()
    fun startDownload()
    fun stopDownload()
    fun applyOad()
    fun getSoftwareVersion() : Int
    fun registerListener(listener : OadEventListener)
    fun unregisterListener(listener : OadEventListener)
}