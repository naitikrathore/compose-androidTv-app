package com.iwedia.cltv.scene.parental_control.locked_channel

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.*
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.scene.parental_control.EnterPinAdapter
import com.iwedia.cltv.scene.parental_control.PinItem
import com.iwedia.cltv.utils.Utils
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import com.iwedia.guide.android.widgets.helpers.BaseLinearLayoutManager
import core_entities.Error
import listeners.AsyncReceiver

class ParentalChannelLockScene(context: Context, sceneListener: ParentalChannelLockSceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.INPUT_OR_CHANNEL_LOCKED_SCENE,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.INPUT_OR_CHANNEL_LOCKED_SCENE),
    sceneListener
) {

    var titleTv: TextView? = null
    var messageTv: TextView? = null
    var pressOk: TextView? = null
    var lockedIv : ImageView? = null
    var unlockBtn : ReferenceDrawableButton? = null
    var channelLogo :ImageView?=null
    var channelName:TextView?=null
    var channelIndex:TextView?=null
    var activeChannel: TvChannel?=null
    lateinit var pinAdapter : EnterPinAdapter
    lateinit var pinRecycler : RecyclerView
    private val NUMBER_OF_PIN_ITEMS = 4
    var currentPinPosition = -1
    var isShowPin: Boolean? = false

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.layout_scene_parental_channel_lock, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {
                findRefs()
                sceneListener.onSceneInitialized()
                (sceneListener as ParentalChannelLockSceneListener).requestActiveChannel()

            }
        })



    }

    override fun refresh(data: Any?) {
        if (data is Boolean) {

            if (data == true) {
                showPin(false)
            } else {
                hidePin(false)
            }
        }
        //Handling locked input scene
        if (data is Int) {

            when (data) {
                1 -> {
                    hidePin(true)
                }
                2 -> {
                    showPin(true)
                }
                -1 -> {
                    pinAdapter!!.reset()
                }
            }

        }
        if(data is TvChannel)
        {
            activeChannel=data
            Utils.loadImage(
                activeChannel!!.logoImagePath!!,
                channelLogo!!,
                object : AsyncReceiver {
                    override fun onFailed(error: Error?) {
                        channelName!!.visibility=View.VISIBLE
                        channelLogo!!.visibility=View.GONE
                        channelName!!.text = activeChannel!!.name
                    }

                    override fun onSuccess() {
                    }
                })

            channelIndex!!.text = activeChannel!!.getDisplayNumberText()


        }
        super.refresh(data)
    }

    private fun hidePin(isInputBlock: Boolean) {
        lockedIv!!.setImageDrawable(
            ReferenceApplication.applicationContext().getDrawable(R.drawable.ic_locked_ic)
        )
        lockedIv!!.imageTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        if(isInputBlock){
            titleTv!!.text = ConfigStringsManager.getStringById("input_locked")
            messageTv!!.text = ConfigStringsManager.getStringById("enter_pin_to_unlock_input")
        }else{
            titleTv!!.text = ConfigStringsManager.getStringById("channel_locked")
            messageTv!!.text = ConfigStringsManager.getStringById("enter_pin_to_unlock")
        }
        pressOk!!.visibility = View.INVISIBLE
        unlockBtn!!.visibility = View.VISIBLE
        pinRecycler.visibility = View.GONE
        unlockBtn!!.requestFocus()
        isShowPin = false
    }

    private fun showPin(isInputBlock: Boolean) {
        lockedIv!!.setImageDrawable(
            ReferenceApplication.applicationContext().getDrawable(R.drawable.ic_unlock_ic)
        )
        lockedIv!!.imageTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        if(isInputBlock){
            titleTv!!.text = ConfigStringsManager.getStringById("unlock_input")
        }else{
            titleTv!!.text = ConfigStringsManager.getStringById("unlock_channel")
        }
        messageTv!!.text = ConfigStringsManager.getStringById("enter_pin_using_number")
        unlockBtn!!.visibility = View.GONE

        //setup recycler and adapter
        pinRecycler!!.visibility = View.VISIBLE
        var layoutManager = LinearLayoutManager(context)
        if(!(sceneListener as ParentalChannelLockSceneListener).isAccessibilityEnabled()) {
            layoutManager = BaseLinearLayoutManager(context)
        }

        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        pinRecycler!!.layoutManager = layoutManager
        pinRecycler!!.adapter = pinAdapter

        if((sceneListener as ParentalChannelLockSceneListener).isAccessibilityEnabled()) {
            pinRecycler!!.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
            pinRecycler!!.isFocusable = false
        }

        pinAdapter!!.registerListener(object : EnterPinAdapter.EnterPinListener {

            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                (sceneListener as ParentalChannelLockSceneListener).setSpeechText(text = text, importance = importance)
            }

            override fun onPinConfirmed(pinCode: String?) {
                (sceneListener as ParentalChannelLockSceneListener).checkPin(pinCode!!)
            }

            override fun getAdapterPosition(position: Int) {
                currentPinPosition = position
                if (position == 3) {
                    pressOk!!.visibility = View.VISIBLE
                } else {
                    pressOk!!.visibility = View.INVISIBLE
                }
            }

            override fun previous() {
            }

            override fun next() {
                currentPinPosition+=1
                if (currentPinPosition > NUMBER_OF_PIN_ITEMS - 1) {
                    currentPinPosition = NUMBER_OF_PIN_ITEMS - 1
                }
                if(!(sceneListener as ParentalChannelLockSceneListener).isAccessibilityEnabled()) {
                    pinRecycler!!.getChildAt(currentPinPosition).requestFocus()
                }
            }

            override fun validationEnabled() {
            }

            override fun isAccessibilityEnabled(): Boolean {
                return (sceneListener as ParentalChannelLockSceneListener).isAccessibilityEnabled()
            }

        })


        pinAdapter!!.refresh(getPinItems(NUMBER_OF_PIN_ITEMS)!!)
        if(!(sceneListener as ParentalChannelLockSceneListener).isAccessibilityEnabled()) {
            pinRecycler!!.requestFocus()
        }
        isShowPin = true
    }

    override fun dispatchKeyEvent(keyCode: Int, keyEvent: Any?): Boolean {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && (keyEvent as KeyEvent).action == KeyEvent.ACTION_UP) {
            if (isShowPin == true) {
                hidePin(true)
            }
        }
        return super.dispatchKeyEvent(keyCode, keyEvent)
    }

    fun findRefs() {
        val relativeLayout: ConstraintLayout = view!!.findViewById(R.id.relativeLayout)
        relativeLayout.setBackgroundColor(Color.parseColor(ConfigColorManager.getColor("color_background")))

        //setup title
        titleTv = view!!.findViewById(R.id.title)
        titleTv!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        titleTv!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )
        titleTv!!.text = ConfigStringsManager.getStringById("channel_locked")

        //setup message
        messageTv = view!!.findViewById(R.id.message)
        messageTv!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        messageTv!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )
        messageTv!!.text = ConfigStringsManager.getStringById("enter_pin_to_unlock")

        //setup second title
        pressOk = view!!.findViewById(R.id.pressOkTv)
        pressOk!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        pressOk!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )
        pressOk!!.text = ConfigStringsManager.getStringById("press_ok_to_confirm")
        pressOk!!.visibility = View.INVISIBLE

        //setup lock icon
        lockedIv = view!!.findViewById(R.id.lockIv)
        lockedIv!!.setImageDrawable(
            ReferenceApplication.applicationContext().getDrawable(R.drawable.ic_locked_ic)
        )
        lockedIv!!.imageTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        //setup unlock button
        unlockBtn = view!!.findViewById(R.id.unlock)
        unlockBtn!!.textToSpeechHandler.setupTextToSpeechTextSetterInterface((sceneListener as ParentalChannelLockSceneListener))
        unlockBtn!!.setText(ConfigStringsManager.getStringById("unlock"))
        unlockBtn!!.getTextView().typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )
        unlockBtn!!.setOnClickListener {
            (sceneListener as ParentalChannelLockSceneListener).onUnlockPressed()
        }
        pinRecycler = view!!.findViewById(R.id.new_pin_items)

        channelIndex=view!!.findViewById(R.id.lock_scene_index)
        channelIndex!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        channelLogo=view!!.findViewById(R.id.lock_scene_logo)
        channelName=view!!.findViewById(R.id.lock_scene_name)

        unlockBtn!!.requestFocus()

        pinAdapter = EnterPinAdapter(getPinItems(NUMBER_OF_PIN_ITEMS))

        unlockBtn!!.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(view: View?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT || keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    return true
                }
                return false
            }
        })
    }

    /**
     * Get pin items
     *
     * @return Pin items
     */
    private fun getPinItems(numberOfItems: Int): MutableList<PinItem>{
        val pinItems = mutableListOf<PinItem>()
        for (i in 0 until numberOfItems) {
            pinItems.add(PinItem(i, PinItem.TYPE_PASSWORD))
        }
        return pinItems
    }


    override fun parseConfig(sceneConfig: SceneConfig?) {

    }
}