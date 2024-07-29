package com.iwedia.cltv.scene.home_scene.guideVertical

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.iwedia.cltv.*
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.model.DateTimeFormat
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.utils.Utils
import core_entities.Error
import listeners.AsyncReceiver
import java.util.*
import kotlin.collections.ArrayList

/**
 * Vertical guide event details view
 *
 * @author Thanvandh Natarajan
 */
class VerticalGuideEventDetailsView : RelativeLayout {
    var mContext: Context? = null
    private var title: TextView? = null
    private var description: TextView? = null
    private var parental: TextView? = null
    private var timeInfo: TimeTextView? = null
    private var image: ImageView? = null
    private var buttonsContainer: LinearLayout? = null
    private var detailsBackground: View? = null
    private var buttons: ArrayList<ReferenceDrawableButton> = ArrayList()
    private var selectedButton = -1
    private var tvEvent: TvEvent? = null

    constructor(context: Context?) : super(context) {
        mContext = context
        setup()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        mContext = context

        setup()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
            context,
            attrs,
            defStyleAttr
    ) {
        mContext = context
        setup()
    }

    @SuppressLint("ResourceType")
    private fun setup() {
        var view = LayoutInflater.from(context).inflate(R.layout.guide_event_details_view_vertical, this, true)
        title = view.findViewById(R.id.guide_event_details_title)
        title!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        description = view.findViewById(R.id.guide_event_details_text)
        description!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))

        parental = view.findViewById(R.id.guide_event_details_parental)
        parental!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        timeInfo = view.findViewById(R.id.guide_event_details_time_info)
        timeInfo!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))

        image = view.findViewById(R.id.guide_event_details_image)
        buttonsContainer = view.findViewById(R.id.buttons_horizontal_grid_view)
        detailsBackground = view.findViewById(R.id.guide_event_details_background)

        //Set typeface
        title!!.typeface =
                TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_medium"))
        description!!.typeface =
                TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))
        parental!!.typeface =
                TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))
        timeInfo!!.typeface =
                TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_regular"))

        Utils.makeGradient(
            detailsBackground!!,
            GradientDrawable.LINEAR_GRADIENT,
            GradientDrawable.Orientation.TOP_BOTTOM,
            Color.parseColor(
                ConfigColorManager.getColor(
                    ConfigColorManager.getColor("color_background"),
                    0.0
                )
            ),
            Color.parseColor(
                ConfigColorManager.getColor(
                    ConfigColorManager.getColor("color_background"),
                    1.0
                )
            ),
            Color.parseColor(
                ConfigColorManager.getColor(
                    ConfigColorManager.getColor("color_text_description"),
                    0.0
                )
            ),
            0.0F,
            0.0F
        )
    }

    /**
     * Set event details data
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun setDetails(tvEvent: TvEvent, parentalRating: String, dateTimeFormat: DateTimeFormat) {
        if (tvEvent != null) {
            this.tvEvent = tvEvent
            title?.text = tvEvent.name

            title!!.typeface =
                TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_medium"))
            title!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
            description?.text = Utils.getUTF8String(tvEvent.shortDescription!!)
            description!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
            parental!!.text = parentalRating
            timeInfo?.setDateTimeFormat(dateTimeFormat)
            timeInfo?.time = tvEvent
            timeInfo!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))
            Utils.loadImage(tvEvent.imagePath!!, image!!, object : AsyncReceiver {
                override fun onFailed(error: Error?) {
                }

                override fun onSuccess() {
                }

            })

            var buttonLayoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
            buttonLayoutParams.bottomMargin = Utils.convertDpToPixel(13.5).toInt()
            var currentTime = Date()
            var eventStartTime = Date(tvEvent.startTime)
            var eventEndTime = Date(tvEvent.endTime)
            if (eventStartTime.before(currentTime) && eventEndTime.after(currentTime)) {
                // Current event
                var buttonWatch = ReferenceDrawableButton(ReferenceApplication.applicationContext(), R.drawable.watch_icon_focused, R.drawable.watch_icon)
                buttonWatch.setText(ConfigStringsManager.getStringById("watch"))
                buttonWatch.layoutParams = buttonLayoutParams
                buttonsContainer?.addView(buttonWatch)
                buttonWatch.tag = CURRENT_EVENT_WATCH_BUTTON_ID
                buttons.add(buttonWatch)
                if (ReferenceApplication.IS_CATCH_UP_SUPPORTED) {
                    var buttonStartOver = ReferenceDrawableButton(
                            ReferenceApplication.applicationContext(),
                            R.drawable.catchup_icon_focused,
                            R.drawable.catchup_icon
                    )
                    buttonStartOver.setText(ConfigStringsManager.getStringById("start_over"))
                    buttonStartOver.layoutParams = buttonLayoutParams
                    buttonsContainer?.addView(buttonStartOver)
                    buttonStartOver.tag = START_OVER_BUTTON_ID
                    buttons.add(buttonStartOver)
                }
                var buttonRecord = ReferenceDrawableButton(ReferenceApplication.applicationContext(), R.drawable.record_icon_focused, R.drawable.record_icon)
                buttonRecord.setText(ConfigStringsManager.getStringById("record"))
                buttonRecord.layoutParams = buttonLayoutParams
                buttonsContainer?.addView(buttonRecord)
                buttonRecord.tag = RECORD_BUTTON_ID
                buttons.add(buttonRecord)

//                if (GeneralConfigManager.getGeneralSettingsInfo("pvr")) {
//                    buttonRecord!!.visibility = View.VISIBLE
//                }else{
//                    buttonRecord!!.visibility = View.GONE
//                    Log.d(Constants.LogTag.CLTV_TAG + "VerticalGuideEvent", "rightButton (record) is invisible")
//                }

            } else if (eventStartTime.before(currentTime) && eventEndTime.before(currentTime)) {
                //Past event
                if (ReferenceApplication.IS_CATCH_UP_SUPPORTED) {
                    var buttonWatch = ReferenceDrawableButton(
                            ReferenceApplication.applicationContext(),
                            R.drawable.watch_icon_focused,
                            R.drawable.watch_icon
                    )
                    buttonWatch.setText(ConfigStringsManager.getStringById("watch"))
                    buttonWatch.layoutParams = buttonLayoutParams
                    buttonsContainer?.addView(buttonWatch)
                    buttonWatch.tag = PAST_EVENT_WATCH_BUTTON_ID
                    buttons.add(buttonWatch)
                }
            } else {
                //Future event

                //TODO !!!! Use color filter for resource color
                var watchList = ReferenceDrawableButton(ReferenceApplication.applicationContext(), R.drawable.watchlist_focused, R.drawable.watchlist)

                //TODO Commented due to pending refactor
/*
                val watchListText = if (ReferenceSdk.watchlistHandler!!.isInWatchlist(tvEvent))
                    ConfigStringsManager.getStringById("watchlist_remove")
                else
                    ConfigStringsManager.getStringById("watchlist")
                watchList.setText(watchListText)
*/

                watchList.layoutParams = buttonLayoutParams
                buttonsContainer?.addView(watchList)
                watchList.tag = WATCHLIST_BUTTON_ID
                buttons.add(watchList)
                var buttonRecord = ReferenceDrawableButton(ReferenceApplication.applicationContext(), R.drawable.record_icon_focused, R.drawable.record_icon)
                buttonRecord.setText(ConfigStringsManager.getStringById("record"))
                buttonRecord.layoutParams = buttonLayoutParams
                buttonsContainer?.addView(buttonRecord)
                buttonRecord.tag = RECORD_BUTTON_ID
                buttons.add(buttonRecord)


//                if (GeneralConfigManager.getGeneralSettingsInfo("pvr")) {
//                    buttonRecord!!.visibility = View.VISIBLE
//                }else{
//                    buttonRecord!!.visibility = View.GONE
//                    Log.d(Constants.LogTag.CLTV_TAG + "VerticalGuideEvent", "rightButton (record) is invisible")
//                }
            }

            var buttonAddToFavorites = ReferenceDrawableButton(
                    ReferenceApplication.applicationContext(),
                    R.drawable.favorite_icon_focused,
                    R.drawable.favorite_icon
            )
            buttonAddToFavorites.layoutParams = buttonLayoutParams
            buttonsContainer?.addView(buttonAddToFavorites)
            buttons.add(buttonAddToFavorites)
            buttonAddToFavorites.tag = ADD_TO_FAVORITES_BUTTON_ID
            if (tvEvent.tvChannel.favListIds.isNotEmpty()) {
                buttonAddToFavorites.setText(ConfigStringsManager.getStringById("edit_favorites"))
            } else {
                buttonAddToFavorites.setText(ConfigStringsManager.getStringById("add_to_favorites"))
            }

            var buttonMoreInfo = ReferenceDrawableButton(
                    ReferenceApplication.applicationContext()
            )
            buttonMoreInfo.setText(ConfigStringsManager.getStringById("more_info"))
            buttonMoreInfo.layoutParams = buttonLayoutParams
            buttonsContainer?.addView(buttonMoreInfo)
            buttons.add(buttonMoreInfo)
            buttonMoreInfo.tag = MORE_INFO_BUTTON_ID

            selectedButton = 0
            buttons[selectedButton].onFocusChange(true)
        }
    }

    /**
     * Refresh favorite button
     */
    fun refreshFavoriteButton() {
        if (tvEvent != null) {
            for (button in buttons) {
                if (button.tag == ADD_TO_FAVORITES_BUTTON_ID) {
                    if (tvEvent!!.tvChannel.favListIds.isNotEmpty()) {
                        button.setText(ConfigStringsManager.getStringById("edit_favorites"))
                    } else {
                        button.setText(ConfigStringsManager.getStringById("add_to_favorites"))
                    }
                    break
                }
            }
        }
    }

    /**
     * Refresh record button
     */
    fun refreshRecordButton() {
        //TODO Commented due to pending refactor

        /*if (tvEvent != null) {
            for (button in buttons) {
                if (button.tag == GuideEventDetailsView.CANCEL_RECORD_BUTTON_ID || button.tag == GuideEventDetailsView.RECORD_BUTTON_ID) {
                    if (Utils.isCurrentEvent(tvEvent!!)) {
                        var isRecordingInProgress = (ReferenceSdk.pvrHandler as ReferencePVRHandler).isRecordingInProgress()
                        if (isRecordingInProgress) {
                            button.setText(
                                ConfigStringsManager.getStringById("cancel")
                            )
                            button.tag = GuideEventDetailsView.CANCEL_RECORD_BUTTON_ID
                        } else {
                            button.setText(
                                ConfigStringsManager.getStringById("record")
                            )
                            button.tag = GuideEventDetailsView.RECORD_BUTTON_ID
                        }
                    } else {
                        // Future recording
                        if (ReferenceSdk.pvrSchedulerHandler!!.isInReclist(tvEvent!!)) {
                            button.setText(
                                ConfigStringsManager.getStringById("cancel")
                            )
                            button.tag = GuideEventDetailsView.CANCEL_RECORD_BUTTON_ID
                        } else {
                            button.setText(
                                ConfigStringsManager.getStringById("record")
                            )
                            button.tag = GuideEventDetailsView.RECORD_BUTTON_ID
                        }
                    }
                    break
                }
            }
        }*/
    }

    fun refreshWatchlistButton() {
        //TODO Commented due to pending refactor

        /* if (tvEvent != null) {
             for (button in buttons) {
                 if (button.tag == WATCHLIST_BUTTON_ID) {
                     ReferenceSdk.watchlistHandler!!.hasScheduledReminder(tvEvent, object :
                             AsyncDataReceiver<Boolean> {
                         override fun onFailed(error: Error?) {

                         }

                         override fun onReceive(data: Boolean) {
                             if (data) {
                                 button.setText(ConfigStringsManager.getStringById("watchlist_remove"))
                             } else {
                                 button.setText(ConfigStringsManager.getStringById("watchlist"))
                             }
                         }
                     })
                 }
             }
         }*/
    }

    /**
     * Select favorite button
     */
    fun selectFavoriteButton(hasFocus: Boolean) {
        buttons[selectedButton].onFocusChange(hasFocus)
    }

    fun dispatchKey(keyCode: Int, keyEvent: KeyEvent): Boolean {
        if (keyEvent.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (selectedButton < buttons.size - 1) {
                        buttons[selectedButton].onFocusChange(false)
                        selectedButton += 1
                        buttons[selectedButton].onFocusChange(true)
                    }
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (selectedButton > 0) {
                        buttons[selectedButton].onFocusChange(false)
                        selectedButton -= 1
                        buttons[selectedButton].onFocusChange(true)
                    }
                }
                KeyEvent.KEYCODE_ESCAPE,
                KeyEvent.KEYCODE_BACK -> {
                    return false
                }
            }
        }
        return true
    }

    /**
     * Get selected button id
     */
    fun getSelectedButtonId(): Int {
        return buttons[selectedButton].tag as Int
    }
    //Button ids
    companion object {
        const val PAST_EVENT_WATCH_BUTTON_ID = 0
        const val CURRENT_EVENT_WATCH_BUTTON_ID = 1
        const val START_OVER_BUTTON_ID = 2
        const val RECORD_BUTTON_ID = 3
        const val WATCHLIST_BUTTON_ID = 4
        const val ADD_TO_FAVORITES_BUTTON_ID = 5
        const val MORE_INFO_BUTTON_ID = 6
    }
}