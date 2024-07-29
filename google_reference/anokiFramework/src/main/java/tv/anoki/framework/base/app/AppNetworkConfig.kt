package tv.anoki.framework.base.app

import tv.anoki.framework.BuildConfig

class AppNetworkConfig : NetworkConfig() {
    override fun baseUrl(): String {
        return BuildConfig.BASE_URL
    }

    override fun timeOut(): Long {
        return 30L
    }

    override fun isDev(): Boolean {
        return true
    }
}