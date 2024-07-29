package com.iwedia.cltv.components.welcome_screen

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.iwedia.cltv.R
import com.iwedia.cltv.anoki_fast.FastButton
import com.iwedia.cltv.anoki_fast.epg.AnimationHelper
import com.iwedia.cltv.platform.model.text_to_speech.SpeechText
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.fast_backend_utils.FastTosOptInHelper
import com.iwedia.cltv.utils.Utils
import listeners.AsyncReceiver

class CustomWelcomeScreen : ConstraintLayout {

    //--------- READ ALL TEXTS

    private val termsOfServiceText = resources.getString(R.string.terms_of_service)
    private val moreInfoTermsTextTemplate = resources.getString(R.string.more_info_terms_and_privacy_text)
    private val privacyPolicyText = resources.getString(R.string.privacy_policy)
    private val termsTextTemplate = resources.getString(R.string.terms_and_privacy_text)

    private val termsText = String.format(termsTextTemplate, termsOfServiceText, privacyPolicyText)
    //----------------------------------------------------------------------------------------------

    private var listener: Listener? = null

    private var continueButton: FastButton
    private var customSpannableLinearLayout: LinearLayout
    private var customSpannableTextView: CustomSpannableTextView? = null
    private var customWelcomeScreenWebView: CustomWelcomeScreenWebView? = null
    private lateinit var cancelButton: FastButton
    private var enableCancelBtn = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttrs: Int) : super(
        context,
        attrs,
        defStyleAttrs
    )

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.layout_custom_welcome_screen, this, true)
        // -------------------------- PROPERTIES INITIALISATION ------------------------------------
        customSpannableLinearLayout = findViewById(R.id.custom_spannable_linear_layout)
        continueButton = findViewById(R.id.continue_button)
        continueButton.textToSpeechTextSetterListener = object : TTSSetterInterface {
            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                listener!!.setSpeechText(text = text, importance = importance)
            }
        }

        cancelButton = findViewById(R.id.cancel_button)
        cancelButton.setText(resources.getString(R.string.button_cancel))
        cancelButton.textToSpeechTextSetterListener = object : TTSSetterInterface {
            override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                listener!!.setSpeechText(text = text, importance = importance)
            }
        }

        setup()
    }

    private fun setup() {
        this.alpha =
            0f // important to be set to 0f BUT VISIBLE in order for AnimationHelper to work properly when all background images are loaded successfully
        createSpannableTextView()
        setupContinueButton()
        setupAllBackgroundResources()
    }

    fun initFocus() {
        customSpannableTextView?.resetInitialState()
        continueButton.requestFocus()
    }

    private fun createSpannableTextView() {
        customSpannableTextView = CustomSpannableTextView(
            context = context,
            type = CustomSpannableTextView.Type.FOCUSED_FROM_RIGHT,
            contentText = termsText,
            listOfCustomSpannableItems = listOf(
                CustomSpannableTextView.CustomSpannableItem(
                    text = termsOfServiceText,
                    onTextPressed = {
                        openWebView(FastTosOptInHelper.getTosUrl())
                    }
                ),
                CustomSpannableTextView.CustomSpannableItem(
                    text = privacyPolicyText,
                    onTextPressed = {
                        openWebView(FastTosOptInHelper.getPrivacyPolicyUrl())
                    }
                )),
            indexOfFirstSelectableText = 1,
            listener = object : CustomSpannableTextView.Listener {
                override fun onUpPressed() { /*DO NOTHING*/
                }

                override fun onDownPressed() { /*DO NOTHING*/
                }

                override fun onLeftPressed() { /*DO NOTHING*/
                }

                override fun onRightPressed() {
                    continueButton.requestFocus()
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    listener!!.setSpeechText(text = text, importance = importance)
                }
            }
        )

        customSpannableLinearLayout.addView(customSpannableTextView)
    }

    fun enableCancelButton() {
        enableCancelBtn = true
        cancelButton.visibility = View.VISIBLE
        customSpannableLinearLayout.removeAllViews()
        customSpannableTextView = CustomSpannableTextView(
            context = context,
            type = CustomSpannableTextView.Type.FOCUSED_FROM_RIGHT,
            contentText = String.format(moreInfoTermsTextTemplate, termsOfServiceText, privacyPolicyText),
            listOfCustomSpannableItems = listOf(
                CustomSpannableTextView.CustomSpannableItem(
                    text = termsOfServiceText,
                    onTextPressed = {
                        openWebView(FastTosOptInHelper.getTosUrl())
                    }
                ),
                CustomSpannableTextView.CustomSpannableItem(
                    text = privacyPolicyText,
                    onTextPressed = {
                        openWebView(FastTosOptInHelper.getPrivacyPolicyUrl())
                    }
                )),
            indexOfFirstSelectableText = 1,
            listener = object : CustomSpannableTextView.Listener {
                override fun onUpPressed() { /*DO NOTHING*/
                }

                override fun onDownPressed() { /*DO NOTHING*/
                }

                override fun onLeftPressed() { /*DO NOTHING*/
                }

                override fun onRightPressed() {
                    cancelButton.requestFocus()
                }

                override fun setSpeechText(vararg text: String, importance: SpeechText.Importance) {
                    listener!!.setSpeechText(text = text, importance = importance)
                }
            }
        )
        customSpannableLinearLayout.addView(customSpannableTextView)
        setupCancelButton()
    }

    private fun openWebView(url: String?){
        customSpannableTextView!!.isFocusable = false
        continueButton.isFocusable = false
        customWelcomeScreenWebView = CustomWelcomeScreenWebView(context, url!!, object : CustomWelcomeScreenWebView.Listener{
            override fun onBackClicked() {
                removeWebView()
            }
        })
        customWelcomeScreenWebView!!.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        addView(customWelcomeScreenWebView)
        customWelcomeScreenWebView!!.requestFocus()
    }

    fun isWebViewShown(): Boolean {
        return customWelcomeScreenWebView != null && customWelcomeScreenWebView!!.parent != null
    }

    fun removeWebView() {
        customSpannableTextView!!.isFocusable = true
        continueButton.isFocusable = true
        removeView(customWelcomeScreenWebView)
    }

    private fun setupContinueButton() {
        continueButton.setText(resources.getString(R.string.button_continue))

        continueButton.setOnClickListener {
            listener!!.onContinueClicked()
        }

        continueButton.setOnKeyListener(object : OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (event!!.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if (enableCancelBtn) {
                            cancelButton?.requestFocus()
                        } else {
                            customSpannableTextView!!.requestFocus()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun setupCancelButton() {
        cancelButton.setOnClickListener {
            listener!!.onCancelClicked()
        }

        cancelButton.setOnKeyListener(object : OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if (event!!.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        customSpannableTextView!!.requestFocus()
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun setupAllBackgroundResources() {
        try {
            Utils.loadImage(
                context = listener?.getContext(),
                view = findViewById(R.id.bottom_image_view),
                resource = R.drawable.custom_welcome_screen_bottom_container_background,
                onComplete = {
                    AnimationHelper.fadeInAnimation(
                        this,
                        onAnimationEnded = { continueButton.requestFocus() })
                }
            )

            Utils.loadImage(
                FastTosOptInHelper.getTosSplashImageUrl(),
                findViewById(R.id.background_image_view),
                object :
                    AsyncReceiver {
                    override fun onFailed(error: core_entities.Error?) {
                        Log.d(Constants.LogTag.CLTV_TAG + "CustomWelcomeScreen", "onFailed: $error")
                    }

                    override fun onSuccess() {}
                },
                shouldCompressImage = false
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setListener(listener: Listener) {
        this.listener = listener
        listener.setSpeechText(termsText, importance = SpeechText.Importance.HIGH)
    }

    interface Listener: TTSSetterInterface {
        fun onContinueClicked()
        fun onCancelClicked()
        fun getContext(): Context
    }

}