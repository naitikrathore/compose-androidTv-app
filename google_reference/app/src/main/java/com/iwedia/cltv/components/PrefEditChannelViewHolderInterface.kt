package com.iwedia.cltv.components

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnKeyListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.downActionBackKeyDone
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.platform.`interface`.TTSSpeakTextForFocusedViewInterface
import com.iwedia.cltv.platform.`interface`.TTSInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.IAsyncDataCallback
import com.iwedia.cltv.platform.model.text_to_speech.Type
import com.iwedia.cltv.utils.Utils
import listeners.AsyncReceiver

private const val TAG = "PrefEditChannelViewHolder"
/**
 * @author Gaurav Jain
 * this class used as a view for skip, delete, move, swap items in channel edit option in set up menu
 */
class PrefEditChannelViewHolderInterface(
    val parent: ViewGroup,
    private val ttsSetterInterface: TTSSetterInterface,
    private val ttsSetterForSelectableViewInterface: TTSSetterForSelectableViewInterface
) : RecyclerView.ViewHolder(
    LayoutInflater.from(parent.context).inflate(R.layout.pref_edit_channel, parent, false)
), PrefComponentInterface<ChannelItem>, TTSSpeakTextForFocusedViewInterface {

    //List item
    var listItem: LinearLayout = itemView.findViewById(R.id.channel_category_item_root_view)!!
    var channelName: TextView = itemView.findViewById(R.id.channel_name)!!
    var channelNumber: TextView = itemView.findViewById(R.id.channel_number)!!
    var icon: ImageView = itemView.findViewById(R.id.icon)!!
    var channelLogo:ImageView = itemView.findViewById(R.id.channel_logo)!!

    var isChecked = false
    var isFocused = false
    var isLongPressed = false

    init {
        //Set references

        //background
        listItem.background =
            ContextCompat.getDrawable(
                ReferenceApplication.applicationContext(),
                R.drawable.transparent_shape
            )


        channelName.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )
        channelNumber.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )


        val textSize =
            ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_10)
        channelName.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            textSize
        )
        channelNumber.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            textSize
        )
    }

    private fun setFocus(isFocused : Boolean){
        this.isFocused = isFocused
        if (isFocused) {
            listItem.animate().scaleY(1.06f).scaleX(1.06f).duration =
                0
            listItem.background = ConfigColorManager.generateButtonBackground()

            speakTextForFocusedView()

            if (isChecked) {

                var draw = ContextCompat.getDrawable(
                    ReferenceApplication.applicationContext(),
                    R.drawable.ic_small_radio_filled
                )

                when((data as ChannelItem).editChannel){
                    EditChannel.CHANNEL_MOVE->{
                        draw = ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.checkbox_checked
                        )

                    }
                    EditChannel.CHANNEL_SKIP->{
                        draw = ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.checkbox_checked
                        )
                    }
                    EditChannel.CHANNEL_SWAP->{
                        ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.ic_small_radio_filled
                        )

                    }
                    EditChannel.CHANNEL_DELETE->{

                    }
                }


                draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_preference_checked")))

                icon.setImageDrawable(
                    draw
                )

            } else {
                var draw = ContextCompat.getDrawable(
                    ReferenceApplication.applicationContext(),
                    R.drawable.ic_small_radio_empty
                )


                when((data as ChannelItem).editChannel){
                    EditChannel.CHANNEL_MOVE->{
                        draw = ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.ic_field
                        )

                    }
                    EditChannel.CHANNEL_SKIP->{
                        draw = ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.ic_field
                        )
                    }
                    EditChannel.CHANNEL_SWAP->{
                        ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.ic_small_radio_empty
                        )

                    }
                    EditChannel.CHANNEL_DELETE->{

                    }
                }
                draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_background")))
                icon.setImageDrawable(
                    draw
                )
            }

            channelName.setTextColor(
                Color.parseColor(ConfigColorManager.Companion.getColor("color_background"))
            )
            channelNumber.setTextColor(
                Color.parseColor(ConfigColorManager.Companion.getColor("color_background"))
            )
        } else {
            //should animate to its original size otherwise even item will appear.
            listItem.animate().scaleY(1f).scaleX(1f).duration = 0
            if (isChecked) {
                var draw = ContextCompat.getDrawable(
                    ReferenceApplication.applicationContext(),
                    R.drawable.ic_small_radio_filled
                )
                when((data as ChannelItem).editChannel){
                    EditChannel.CHANNEL_MOVE->{
                        draw = ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.checkbox_checked
                        )

                    }
                    EditChannel.CHANNEL_SKIP->{
                        draw = ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.checkbox_checked
                        )
                    }
                    EditChannel.CHANNEL_SWAP->{
                        ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.ic_small_radio_filled
                        )

                    }
                    EditChannel.CHANNEL_DELETE->{

                    }
                }
                draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_preference_checked")))
                icon.setImageDrawable(
                    draw
                )
            } else {
                var draw = ContextCompat.getDrawable(
                    ReferenceApplication.applicationContext(),
                    R.drawable.ic_small_radio_empty
                )

                when((data as ChannelItem).editChannel){
                    EditChannel.CHANNEL_MOVE->{
                        draw = ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.ic_field
                        )

                    }
                    EditChannel.CHANNEL_SKIP->{
                        draw = ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.ic_field
                        )
                    }
                    EditChannel.CHANNEL_SWAP->{
                        ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.ic_small_radio_empty
                        )

                    }
                    EditChannel.CHANNEL_DELETE->{

                    }
                }
                draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                icon.setImageDrawable(
                    draw
                )
            }
            channelName.setTextColor(
                Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"))
            )
            channelNumber.setTextColor(
                Color.parseColor(ConfigColorManager.Companion.getColor("color_main_text"))
            )

            channelName.animate().scaleY(1f).scaleX(1f).duration = 0
            channelNumber.animate().scaleY(1f).scaleX(1f).duration = 0
            listItem.background =
                ContextCompat.getDrawable(
                    ReferenceApplication.applicationContext(),
                    R.drawable.transparent_shape
                )

        }
    }

    override fun requestFocus() {
        itemView.requestFocus()
    }

    override fun clearFocus() {
        itemView.clearFocus()
    }



    fun changeSwitchStatus(isChecked :Boolean){
        this.isChecked = isChecked
        if (isChecked) {
            var draw :Drawable? =   ContextCompat.getDrawable(
                ReferenceApplication.applicationContext(),
                R.drawable.ic_small_radio_filled
            )

            when((data as ChannelItem).editChannel){
                EditChannel.CHANNEL_MOVE->{
                    draw = ContextCompat.getDrawable(
                        ReferenceApplication.applicationContext(),
                        R.drawable.checkbox_checked
                    )

                }
                EditChannel.CHANNEL_SKIP->{
                     draw = ContextCompat.getDrawable(
                        ReferenceApplication.applicationContext(),
                        R.drawable.checkbox_checked
                    )
                }
                EditChannel.CHANNEL_SWAP->{
                    ContextCompat.getDrawable(
                        ReferenceApplication.applicationContext(),
                        R.drawable.ic_small_radio_filled
                    )

                }
                EditChannel.CHANNEL_DELETE->{

                }
            }

            draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_preference_checked")))

            icon.setImageDrawable(
                draw
            )
        } else {
            var draw = ContextCompat.getDrawable(
                ReferenceApplication.applicationContext(),
                R.drawable.ic_small_radio_empty
            )

            when((data as ChannelItem).editChannel){
                EditChannel.CHANNEL_MOVE->{
                    draw = ContextCompat.getDrawable(
                        ReferenceApplication.applicationContext(),
                        R.drawable.ic_field
                    )

                }
                EditChannel.CHANNEL_SKIP->{
                    draw = ContextCompat.getDrawable(
                        ReferenceApplication.applicationContext(),
                        R.drawable.ic_field
                    )
                }
                EditChannel.CHANNEL_SWAP->{
                    ContextCompat.getDrawable(
                        ReferenceApplication.applicationContext(),
                        R.drawable.ic_small_radio_empty
                    )

                }
                EditChannel.CHANNEL_DELETE->{

                }
            }

            if (isFocused) {
                draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_background")))
            } else {
                draw!!.setTint(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
            }
            icon.setImageDrawable(
                draw
            )
        }

    }

    var data: ChannelItem? = null
    var listener: PrefItemListener? = null

    override fun refresh(data: ChannelItem, id:Pref, listener: PrefItemListener) {
        this.data = data
        this.listener = listener
        itemView.setOnFocusChangeListener { _, hasFocus ->
            if(hasFocus) listener.onAction(Action.FOCUSED,id)
            setFocus(hasFocus)
        }

        when(data.editChannel){
            EditChannel.CHANNEL_DELETE->{
                icon.visibility = View.GONE
            }
            EditChannel.CHANNEL_MOVE -> {
                isLongPressed = data.isLongPressed
                updateItemStatus(data)
            }
            else ->{
                icon.visibility = View.VISIBLE
            }
        }

        setFocus(isFocused)
        changeSwitchStatus(data.isChecked)
        channelName.text = data.title
        channelNumber.text = data.tvChannel.getDisplayNumberText()
        Utils.loadImage(data.tvChannel.logoImagePath, channelLogo, object : AsyncReceiver {
            override fun onFailed(error: core_entities.Error?) {
            }

            override fun onSuccess() {
            }
        })
        val keyListener: OnKeyListener = object : OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (!ReferenceApplication.worldHandler?.isEnableUserInteraction!!) {
                    return true
                }
                if (event!!.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        return if (ViewCompat.getLayoutDirection(parent) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                            listener.onAction(Action.LEFT,id)
                        } else {
                            listener.onAction(Action.RIGHT,id)
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        return if (ViewCompat.getLayoutDirection(parent) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                            listener.onAction(Action.RIGHT,id)
                        } else {
                            listener.onAction(Action.LEFT,id)
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        return listener.onAction(Action.DOWN,id)
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        return listener.onAction(Action.UP,id)
                    }
                    if (keyCode == KeyEvent.KEYCODE_BACK){
                        downActionBackKeyDone = true
                    }
                }

                if (event.action == KeyEvent.ACTION_UP) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        if (!downActionBackKeyDone)return true
                        downActionBackKeyDone = false
                        return listener.onAction(Action.BACK,id)
                    }
                }
                return false
            }
        }

        itemView.setOnKeyListener(keyListener)

        itemView.setOnClickListener {

            data.compoundListener.onChange(!isChecked,data.id,id,object :IAsyncDataCallback<Boolean>{
                override fun onFailed(error: Error) {}

                override fun onReceive(data: Boolean) {
                    if (!isLongPressed) {
                        this@PrefEditChannelViewHolderInterface.data!!.isChecked = data
                        listener.onAction(Action.UPDATE, id)
                        changeSwitchStatus(this@PrefEditChannelViewHolderInterface.data!!.isChecked)
                        speakTextForFocusedView()
                    } else {
                        listener.onAction(Action.UPDATE, id)
                    }
                    isLongPressed = false
                }
            })
        }

        itemView.setOnLongClickListener {
                isLongPressed = true
                listener.onAction(Action.LONG_CLICK,id)
        }
    }

    var infoListener: PreferenceSubMenuAdapter.Listener? = null

    fun setListener(listener: PreferenceSubMenuAdapter.Listener) {
        infoListener = listener
    }

    override fun notifyPreferenceUpdated() {
    }

    override fun speakTextForFocusedView() {
        if ((data as ChannelItem).editChannel != EditChannel.CHANNEL_DELETE) {
            ttsSetterForSelectableViewInterface.setSpeechTextForSelectableView(
                channelNumber.text.toString(),
                channelName.text.toString(),
                type = Type.CHECK,
                isChecked = isChecked
            )
        } else {
            ttsSetterInterface.setSpeechText(
                channelNumber.text.toString(),
                channelName.text.toString()
            )
        }
    }

    private fun updateItemStatus(data: ChannelItem) {
        itemView.alpha = if(data.isEnabled )1.0f else 0.2f
    }

}