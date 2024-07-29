package com.iwedia.cltv.components

import android.graphics.Color
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnKeyListener
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.IAsyncDataCallback

/**
 * @author Gaurav Jain
 * used to create seekbar for preference sub-menu adapter
 */
class PrefSeekBar(val parent: ViewGroup) : RecyclerView.ViewHolder (
    LayoutInflater.from(parent.context).inflate(R.layout.pref_seekbar, parent, false)
), PrefComponentInterface<SeekBarItem>{

    var volumeProgressBar: ProgressBar? = null
    var volumePercentText: TextView? = null
    val TAG = javaClass.simpleName
    var title: TextView? = null

    init {
        //Set references
        title = itemView.findViewById(R.id.volume_text)
        volumePercentText = itemView.findViewById(R.id.volume_percent_text)!!
        volumeProgressBar = itemView.findViewById(R.id.progress_bar)!!

        title!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        volumePercentText!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        volumePercentText!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")

        )
        volumePercentText!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )

        itemView.setOnFocusChangeListener { view, hasFocus ->
            setFocus(hasFocus)
        }

        setFocus(false)
    }

    private fun setFocus(isFocused : Boolean){
        if (isFocused) {
            try {
                val color_context =
                    Color.parseColor(ConfigColorManager.getColor("color_background"))
                itemView!!.background = ConfigColorManager.generateButtonBackground()

                Log.d(Constants.LogTag.CLTV_TAG + TAG, "setFocus: Exception color_context $color_context")
                title!!.setTextColor(
                    color_context
                )
                volumePercentText!!.setTextColor(
                    color_context
                )
            } catch (ex: Exception) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "setFocus: Exception color rdb $ex")
            }

        } else {
            itemView.background =
                ContextCompat.getDrawable(
                    ReferenceApplication.applicationContext(),
                    R.drawable.transparent_shape
                )
            title!!.setTextColor(
                Color.parseColor(ConfigColorManager.getColor("color_main_text"))
            )
            volumePercentText!!.setTextColor(
                Color.parseColor(ConfigColorManager.getColor("color_main_text"))
            )
        }
    }

    override fun requestFocus() {
        itemView.requestFocus()
    }

    override fun clearFocus() {
        itemView.clearFocus()
    }

    fun setProgress(progress :Int){
        volumePercentText?.text = "$progress %"
        volumeProgressBar?.progress = progress
    }

    override fun refresh(data: SeekBarItem,id:Pref, listener: PrefItemListener) {
        title?.text = data.title
        setProgress(data.progress)
        val keyListener: OnKeyListener = object : OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (!ReferenceApplication.worldHandler?.isEnableUserInteraction!!) {
                    return true
                }
                if (event!!.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if (ViewCompat.getLayoutDirection(parent) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                            change(data,-1,id)
                            return true
                        } else {
                            change(data,+1,id)
                            return true
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        return if (ViewCompat.getLayoutDirection(parent) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                            change(data,+1,id)
                            true
                        } else {
                            change(data,-1,id)
                            true
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        return listener.onAction(Action.DOWN,id)
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        return listener.onAction(Action.UP,id)
                    }
                }

                if (event.action == KeyEvent.ACTION_UP) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        return listener.onAction(Action.BACK,id)
                    }
                }
                return false
            }
        }

        itemView.setOnKeyListener(keyListener)
    }

    private fun change(data: SeekBarItem, increment : Int, id : Pref){

        var progress = volumeProgressBar!!.progress + increment

        if(progress<0) progress = 0
        if(progress>100) progress = 100

        data.seekBarListener.onChange(progress,id,object :IAsyncDataCallback<Int>{
            override fun onFailed(error: Error) {
            }

            override fun onReceive(progress: Int) {
                setProgress(progress)
                data.progress = progress
            }
        })

    }

    override fun notifyPreferenceUpdated() {
    }
}