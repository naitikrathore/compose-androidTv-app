import android.text.TextUtils
import android.util.Log
import api.HandlerAPI
import com.iwedia.cltv.sdk.ReferenceSdk
import com.mediatek.twoworlds.tv.MtkTvCIBase
import com.mediatek.twoworlds.tv.MtkTvChannelListBase
import com.mediatek.twoworlds.tv.model.MtkTvCIMMIEnqBase
import com.mediatek.twoworlds.tv.model.MtkTvCIMMIMenuBase

class ReferenceCiPlusHandler : HandlerAPI {

    var ciHandler: CiHandler? = null
    var listener: RefCiHandlerListener? = null

    fun selectMenuItem(position: Int) {
        ciHandler!!.getmCIState()
            .selectMenuItem(position)
    }


    override fun setup() {
        //Setup CI handler
        val ciHandler = CiHandler(ReferenceSdk.context)
        this.ciHandler = ciHandler

        ciHandler.setListener(object : CIStateChangedCallBack.CIMenuUpdateListener {
            override fun enqReceived(enquiry: MtkTvCIMMIEnqBase?) {

                // send msg to escape may dismiss the ci info dialog
                ciHandler.mHandler.sendEmptyMessage(0XF2)
                ciHandler.mHandler.removeMessages(0xF4)
            }

            override fun menuReceived(menu: MtkTvCIMMIMenuBase?) {
                Log.d(Constants.LogTag.CLTV_TAG + "ZOLA", "ON MENU RECEIVED")
                var list = mutableListOf<String>()

                if (menu != null) {
                    if (menu.itemList == null) {
                        list.add("back")
                    }

                    for ((_, s) in menu.itemList.withIndex()) {
                        if (!TextUtils.isEmpty(s)) {
                            list.add(s)
                        }
                    }

                    listener?.let {
                        it.onMenuReceived(list)
                    }
                }
            }

            override fun menuEnqClosed() {
                Log.d(Constants.LogTag.CLTV_TAG + "ZOLA", "MENU ENQ CLOSED")
            }

            override fun ciRemoved() {

            }

            override fun ciCamScan(message: Int) {
                Log.d(Constants.LogTag.CLTV_TAG + "ZOLA", "CI CAM SCAN")
            }

        })
    }

    fun isCamActive(): Boolean {


        //TODO CHECK THIS IF THIS IS OK?

        return ciHandler!!.getmCIState().getCIName() != null && ciHandler!!.getmCIState()
            .getCIName() != ""

//        return ciHandler!!.getmCIState().isCamActive
    }

    fun getCiName(): String {
        return CIStateChangedCallBack.getInstance(ReferenceSdk.context).ciName
    }

    fun getMenuListID(): Int {
        return MtkTvCIBase.getMenuListID()
    }

    fun getEnqId(): Int {
        return MtkTvCIBase.getEnqID()
    }

    fun setMMICloseDone() {
        ciHandler!!.getmCIState().ciHandle.setMMICloseDone()
    }

    fun enterMMI() {
        ciHandler!!.getmCIState().ciHandle.enterMMI()
    }

    fun cancelCurrMenu() {
        ciHandler!!.getmCIState().cancelCurrMenu()
    }

    fun isChannelScrambled(): Boolean {

        if (MtkTvChannelListBase.getCurrentChannel() == null) {
            return false
        }
        return MtkTvChannelListBase.getCurrentChannel().isScrambled
    }

    override fun dispose() {
        ciHandler = null
        listener = null
    }

    interface RefCiHandlerListener {
        fun onMenuReceived(menuItems: MutableList<String>)
    }
}