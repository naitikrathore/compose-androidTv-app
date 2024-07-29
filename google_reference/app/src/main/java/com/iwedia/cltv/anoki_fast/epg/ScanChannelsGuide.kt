package com.iwedia.cltv.anoki_fast.epg

import android.content.Context
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.components.ButtonType
import com.iwedia.cltv.components.CustomButton
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.utils.Utils

/**
 * @author Gaurav Jain
 * this class is used to show scan button while there are no broadcast channels in broadcast tab
 */
class ScanChannelsGuide(context: Context,listener:GuideScanListener) : ConstraintLayout(context) {
     private var scanButton : CustomButton? =null
    private var isScanButtonClicked = false

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_widget_guide_scan_widget, this, true)
        scanButton = findViewById(R.id.guide_scan_btn)
        scanButton?.update(ButtonType.CUSTOM_TEXT, ConfigStringsManager.getStringById("scan_channels_text"))
        scanButton?.setOnClickListener {
            Utils.viewClickAnimation(it, object :
                com.iwedia.cltv.utils.AnimationListener {
                override fun onAnimationEnd() {
                    isScanButtonClicked = true
                    listener.startScan()
                    it.clearAnimation()
                    ReferenceApplication.worldHandler!!.destroyOtherExisting(ReferenceWorldHandler.SceneId.LIVE)
                }
            })
        }


        scanButton?.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (event!!.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        return true
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        listener.requestFocusOnTopMenu()
                        return true
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        return true
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        return true
                    }
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        isScanButtonClicked = false
                    }
                } else if (event!!.action == KeyEvent.ACTION_UP) {
                    //To block the focus going to broadcast tab when returned from channel scan scene
                    if (keyCode == KeyEvent.KEYCODE_BACK && isScanButtonClicked) {
                        return true
                    }
                }
                return false
            }
        })
    }

    fun setFocusToScanButton(){
        scanButton?.requestFocus()
    }

}
interface GuideScanListener {
    fun startScan()
    fun requestFocusOnTopMenu()
}
