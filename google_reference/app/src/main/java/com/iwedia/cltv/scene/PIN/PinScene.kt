package com.iwedia.cltv.scene.PIN

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import android.os.Build
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.`interface`.TTSInstructionsInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.model.recording.Recording
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.scene.parental_control.EnterPinAdapter
import com.iwedia.cltv.scene.parental_control.PinItem
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import com.iwedia.guide.android.widgets.helpers.BaseLinearLayoutManager

private const val TAG = "PinScene"

/**
 * PIN scene
 *
 * @author Nishant Bansal
 */
class PinScene(context: Context, sceneListener: PinSceneListener) : ReferenceScene(
    context,
    ReferenceWorldHandler.SceneId.PIN_SCENE,
    ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.PIN_SCENE),
    sceneListener
), TTSInstructionsInterface{
    var titleTv: TextView? = null
    var messageTv: TextView? = null
    var pressOk: TextView? = null
    var lockedIv : ImageView? = null
    private lateinit var defaultPinTextView: TextView
    lateinit var pinAdapter : EnterPinAdapter
    lateinit var pinRecycler : RecyclerView
    private val NUMBER_OF_PIN_ITEMS = 4
    var currentPinPosition = -1
    var colorMainText = Color.parseColor(ConfigColorManager.getColor("color_main_text"))
    var colorBackground = Color.parseColor(ConfigColorManager.getColor("color_background"))
    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.layout_pin_scene, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {
                view?.let {
                    findRefs()
                    sceneListener.onSceneInitialized()
                }

            }
        })

    }

    override fun refresh(data: Any?) {
        if (data is String) {
            if (data == ConfigStringsManager.getStringById("lock")) {
                lockChannel()
            }
            if (data == ConfigStringsManager.getStringById("unlock")) {
                unlockChannel()
            }
            if (data==ConfigStringsManager.getStringById("enter_parental_settings")){
                enterParentalCheck()
            }
        }
        speakInstructions()
        if (data is Recording) {
            unlockRecordedEvent(data.name)
        }
        if (data is Int) {
            pinAdapter.reset()
        }

        super.refresh(data)
    }

    private fun lockChannel(){
        lockedIv!!.setImageDrawable(
            ReferenceApplication.applicationContext().getDrawable(R.drawable.ic_locked_ic)
        )
        lockedIv!!.imageTintList = ColorStateList.valueOf(colorMainText)
        titleTv!!.text = ConfigStringsManager.getStringById("lock_channel")
        messageTv!!.text = ConfigStringsManager.getStringById("enter_pin_using_number")
    }

    private fun enterParentalCheck(){
        lockedIv!!.setImageDrawable(
            ReferenceApplication.applicationContext().getDrawable(R.drawable.ic_locked_ic)
        )
        lockedIv!!.imageTintList = ColorStateList.valueOf(colorMainText)
        titleTv!!.text = ConfigStringsManager.getStringById("enter_pin_to_see_settings")
        messageTv!!.text = ConfigStringsManager.getStringById("enter_pin_using_number")
    }
    private fun unlockChannel(){
        lockedIv!!.setImageDrawable(
            ReferenceApplication.applicationContext().getDrawable(R.drawable.ic_unlock_ic)
        )
        lockedIv!!.imageTintList = ColorStateList.valueOf(colorMainText)
        titleTv!!.text = ConfigStringsManager.getStringById("unlock_channel")
        messageTv!!.text = ConfigStringsManager.getStringById("enter_pin_using_number")
    }

    private fun unlockRecordedEvent(eventName: String) {
        lockedIv!!.setImageDrawable(
            ReferenceApplication.applicationContext().getDrawable(R.drawable.ic_unlock_ic)
        )
        lockedIv!!.imageTintList =
            ColorStateList.valueOf(colorMainText)
        titleTv!!.text = ConfigStringsManager.getStringById("unlock").plus(" ").plus(eventName)
        messageTv!!.text = ConfigStringsManager.getStringById("enter_pin_using_number")
    }

    override fun speakInstructions() {
        Log.d(Constants.LogTag.CLTV_TAG + TAG, "speakInstructions: BIIII")
        (sceneListener as PinSceneListener).setSpeechText(
            titleTv!!.text.toString(),
            messageTv!!.text.toString(),
            importance = SpeechText.Importance.HIGH
        )
    }

    fun findRefs() {
        val relativeLayout: ConstraintLayout = view!!.findViewById(R.id.relativeLayout)
        relativeLayout.setBackgroundColor(colorBackground)

        //setup title
        titleTv = view!!.findViewById(R.id.title)
        titleTv!!.setTextColor(colorMainText)
        titleTv!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_medium")
        )

        //setup message
        messageTv = view!!.findViewById(R.id.message)
        messageTv!!.setTextColor(colorMainText)
        messageTv!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )

        //setup second title
        pressOk = view!!.findViewById(R.id.pressOkTv)
        pressOk!!.setTextColor(colorMainText)
        pressOk!!.typeface = TypeFaceProvider.getTypeFace(
            ReferenceApplication.applicationContext(),
            ConfigFontManager.getFont("font_regular")
        )
        pressOk!!.text = ConfigStringsManager.getStringById("press_ok_to_confirm")
        pressOk!!.visibility = View.INVISIBLE

        defaultPinTextView = view!!.findViewById(R.id.default_pin_text_view)
        setupDefaultPinTextView()

        //setup lock icon
        lockedIv = view!!.findViewById(R.id.lockIv)

        pinAdapter = EnterPinAdapter(getPinItems(NUMBER_OF_PIN_ITEMS)!!)

        //setup recycler and adapter
        pinRecycler = view!!.findViewById(R.id.new_pin_items)
        pinRecycler!!.visibility = View.VISIBLE
        var layoutManager = LinearLayoutManager(context)
        if(!(sceneListener as PinSceneListener).isAccessibilityEnabled()) {
            layoutManager = BaseLinearLayoutManager(context)
        }
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        pinRecycler.layoutManager = layoutManager
        pinRecycler.adapter = pinAdapter
        if((sceneListener as PinSceneListener).isAccessibilityEnabled()) {
            pinRecycler!!.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
            pinRecycler!!.isFocusable = false
        }

        pinAdapter.registerListener(object : EnterPinAdapter.EnterPinListener {
            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                (sceneListener as PinSceneListener).setSpeechText(text = text, importance = importance)
            }

            override fun onPinConfirmed(pinCode: String?) {
                (sceneListener as PinSceneListener).checkPin(pinCode!!)
            }

            override fun getAdapterPosition(position: Int) {
                currentPinPosition = position
                if (position == 3) {
                    pressOk!!.visibility = View.VISIBLE
                    setSpeechText(
                        ConfigStringsManager.getStringById("press_ok_to_confirm"),
                        importance = SpeechText.Importance.HIGH
                    )
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
                if(!(sceneListener as PinSceneListener).isAccessibilityEnabled()) {
                    pinRecycler.getChildAt(currentPinPosition).requestFocus()
                }
            }

            override fun validationEnabled() {
            }

            override fun isAccessibilityEnabled(): Boolean {
                return (sceneListener as PinSceneListener).isAccessibilityEnabled()
            }

        })

        if(!(sceneListener as PinSceneListener).isAccessibilityEnabled()) {
            pinRecycler.requestFocus()
        }
    }

    /**
     * Sets up the TextView for displaying the default PIN.
     * If the PIN is not default, this method does nothing.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun setupDefaultPinTextView() {
        // Check if the PIN is not default, and return if true.
        if ((sceneListener as PinSceneListener).isParentalPinChanged()) return
        if (!(sceneListener as PinSceneListener).isDefaultPinNotificationRequired()) return // if flavor is not Base this hint MUSTN'T be displayed.
        // Following code is executed only if the PIN is not default.

        // Initialize the message with a prefix.
        var message = ConfigStringsManager.getStringById("default_pin_message")

        // Retrieve the actual PIN.
        val pin = (sceneListener as PinSceneListener).getParentalPin()

        // Concatenate the PIN to the message.
        message += pin

        // Set up the TextView properties.
        defaultPinTextView.apply {
            text = message
            setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
            typeface = TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular")
            )
            visibility = View.VISIBLE
        }

    }

    /**
     * Get pin items
     *
     * @return Pin items
     */
    private fun getPinItems(numberOfItems: Int): MutableList<PinItem>? {
        val pinItems = mutableListOf<PinItem>()
        for (i in 0 until numberOfItems) {
            pinItems.add(PinItem(i, PinItem.TYPE_PASSWORD))
        }
        return pinItems
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {

    }
}