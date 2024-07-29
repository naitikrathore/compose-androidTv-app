package com.iwedia.cltv.scene.parental_control.change_pin

import android.content.Context
import android.graphics.Color
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication.Companion.applicationContext
import com.iwedia.cltv.ReferenceWorldHandler
import com.iwedia.cltv.TypeFaceProvider.Companion.getTypeFace
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager.Companion.getFont
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.config.SceneConfig
import com.iwedia.cltv.platform.`interface`.TTSInstructionsInterface
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.scene.ReferenceScene
import com.iwedia.cltv.scene.parental_control.EnterPinAdapter
import com.iwedia.cltv.scene.parental_control.PinItem
import com.iwedia.guide.android.tools.GAndroidSceneFragment
import com.iwedia.guide.android.tools.GAndroidSceneFragmentListener
import com.iwedia.guide.android.widgets.helpers.BaseLinearLayoutManager

/**
 * Parental pin scene
 *
 * @author Aleksandar Lazic
 */
class ParentalPinScene(context: Context, sceneListener: ParentalPinSceneListener) :

    ReferenceScene(
        context,
        ReferenceWorldHandler.SceneId.PARENTAL_PIN,
        ReferenceWorldHandler.getSceneName(ReferenceWorldHandler.SceneId.PARENTAL_PIN),
        sceneListener
    ), TTSInstructionsInterface {

    var titleTv: TextView? = null
    var messageTv: TextView? = null
    lateinit var pinAdapter : EnterPinAdapter
    lateinit var pinRecycler : RecyclerView
    private val NUMBER_OF_PIN_ITEMS = 4
    var currentPinPosition = -1

    //distinguish scenes which are the same, just text is switching
    //0 - enter current pin
    //1 - enter new pin
    //2 - confirm new pin

    private var phaseNumber = 0

    override fun createView() {
        super.createView()
        view = GAndroidSceneFragment(name, R.layout.layout_scene_parental_pin, object :
            GAndroidSceneFragmentListener {

            override fun onCreated() {
                findRefs()
                sceneListener.onSceneInitialized()
            }
        })
    }

    override fun refresh(data: Any?) {

        if (data is Int) {
            when (data) {
                1 -> {
                    phaseNumber++
                    titleTv!!.text = ConfigStringsManager.getStringById("change_pin")
                    messageTv!!.text = ConfigStringsManager.getStringById("enter_new_pin")
                    speakInstructions()
                    pinAdapter.reset()
                }
                2 -> {
                    phaseNumber++
                    titleTv!!.text = ConfigStringsManager.getStringById("confirm_change")
                    messageTv!!.text = ConfigStringsManager.getStringById("enter_new_pin")
                    speakInstructions()
                    pinAdapter.reset()
                }
                -1 -> {
                    pinAdapter.reset()
                }
            }
        } else {
            pinAdapter.registerListener(object : EnterPinAdapter.EnterPinListener {

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    (sceneListener as ParentalPinSceneListener).setSpeechText(text = text, importance = importance)
                }

                override fun onPinConfirmed(pinCode: String?) {
                    when (phaseNumber) {
                        0 -> {
                            (sceneListener as ParentalPinSceneListener).onCurrentPinEntered(pinCode!!)
                        }
                        1 -> {
                            (sceneListener as ParentalPinSceneListener).onNewPinEntered(pinCode!!)
                        }
                        2 -> {
                            phaseNumber = 0
                            (sceneListener as ParentalPinSceneListener).onConfirmNewPinEntered(pinCode!!)
                        }
                    }
                }

                override fun getAdapterPosition(position: Int) {
                    currentPinPosition = position
                }

                override fun previous() {
                }

                override fun next() {
                    currentPinPosition+=1
                    if (currentPinPosition > NUMBER_OF_PIN_ITEMS - 1) {
                        currentPinPosition = NUMBER_OF_PIN_ITEMS - 1
                    }
                    if(!(sceneListener as ParentalPinSceneListener).isAccessibilityEnabled()) {
                        pinRecycler.getChildAt(currentPinPosition).requestFocus()
                    }
                }

                override fun validationEnabled() {
                }

                override fun isAccessibilityEnabled(): Boolean {
                    return (sceneListener as ParentalPinSceneListener).isAccessibilityEnabled()
                }
            })
        }

        super.refresh(data)
    }

    override fun parseConfig(sceneConfig: SceneConfig?) {

    }

    private fun findRefs() {
        val relativeLayout: ConstraintLayout = view!!.findViewById(R.id.relativeLayout)
        relativeLayout.setBackgroundColor(Color.parseColor(ConfigColorManager.getColor("color_background")))

        //setup title
        titleTv = view!!.findViewById(R.id.title)
        titleTv!!.typeface = getTypeFace(applicationContext(), getFont("font_medium"))
        titleTv!!.text = ConfigStringsManager.getStringById("change_pin")
        titleTv!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        //setup message
        messageTv = view!!.findViewById(R.id.message)
        messageTv!!.typeface = getTypeFace(applicationContext(), getFont("font_regular"))

        messageTv!!.text = ConfigStringsManager.getStringById("enter_current_pin")
        messageTv!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        //setup recycler and adapter
        pinRecycler = view!!.findViewById(R.id.new_pin_items)
        var layoutManager = LinearLayoutManager(context)
        if(!(sceneListener as ParentalPinSceneListener).isAccessibilityEnabled()) {
            layoutManager = BaseLinearLayoutManager(context)
        }
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        pinRecycler.layoutManager = layoutManager
        pinAdapter = EnterPinAdapter(getPinItems(NUMBER_OF_PIN_ITEMS)!!)
        pinRecycler.adapter = pinAdapter

        pinRecycler.requestFocus()

        speakInstructions()
    }

    override fun speakInstructions() {
        (sceneListener as ParentalPinSceneListener).setSpeechText(
            titleTv!!.text.toString(),
            messageTv!!.text.toString(),
            importance = SpeechText.Importance.HIGH
        )
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
}