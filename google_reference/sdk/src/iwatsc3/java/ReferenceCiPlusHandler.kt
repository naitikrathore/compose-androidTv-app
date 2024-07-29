import api.HandlerAPI

class ReferenceCiPlusHandler : HandlerAPI {

    override fun dispose() {
    }

    override fun setup() {
    }

    fun isChannelScrambled(): Boolean? {
        return false
    }

    fun isCamActive(): Boolean {
        return false
    }
}