import api.HandlerAPI

class ReferenceCiPlusHandler : HandlerAPI {
    override fun dispose() {
    }

    override fun setup() {
    }

    var listener: RefCiHandlerListener? = null

    fun getCiName(): String {
        return ""
    }

    fun selectMenuItem(position: Int) {

    }

    fun isCamActive(): Boolean {
        return false
    }

    fun getMenuListID(): Int {
        return -1
    }

    fun getEnqId(): Int {
        return -1
    }

    fun setMMICloseDone() {

    }

    fun enterMMI() {

    }

    fun cancelCurrMenu() {

    }

    fun isChannelScrambled(): Boolean? {
        return false
    }

    interface RefCiHandlerListener {
        fun onMenuReceived(menuItems: MutableList<String>)
    }
}