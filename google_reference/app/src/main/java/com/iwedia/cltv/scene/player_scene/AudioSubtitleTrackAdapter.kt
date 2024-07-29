package com.iwedia.cltv.scene.player_scene

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.language.LanguageMapperInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.player.track.IAudioTrack
import com.iwedia.cltv.platform.model.player.track.ISubtitle
import com.iwedia.cltv.platform.model.player.track.ITrack
import com.iwedia.cltv.scene.channel_list.ChannelListSortViewHolder
import com.iwedia.cltv.utils.AnimationListener
import com.iwedia.cltv.utils.Utils

class AudioSubtitleTrackAdapter(private var languageMapper: LanguageMapperInterface) : RecyclerView.Adapter<ChannelListSortViewHolder>() {

    private val TAG = "AudioSubtitleTrackAdapter"

    enum class AdapterType {
        eAudioAdapter,
        eSubtitlesAdapter
    }
    private val UNDEFINED_LANGUAGE_STR = "Undefined"
    //Items
    private var items = mutableListOf<ITrack>()

    //Selected item
    var selectedItem: ITrack? = null
    var adapterType = AdapterType.eAudioAdapter

    var isSelected = false
    //check if click animation is progress or not
    var animationInProgress = false

    private var selectedItemViewHolder: ChannelListSortViewHolder? = null

    //Adapter listener
    var adapterListener: AudioSubtitleTrackAdapterListener? = null

    var fadeListener: AudioSubtitleTrackFadeListener? = null // TODO BORIS this should be in new class

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelListSortViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.sort_by_item, parent, false)
        return ChannelListSortViewHolder(view,adapterListener!!)
    }

    // TODO BORIS this should be in new class
    override fun onViewAttachedToWindow(holder: ChannelListSortViewHolder) {
        super.onViewAttachedToWindow(holder)
        val position = holder.adapterPosition
        fadeListener?.let { fadeListener -> // only if fadeListener has been initialised (in some scenes or widgets there is no need for handling fade in VerticalGridView and there fadeListener hasn't been implemented)
            if (position == 0) { // when first item is attached remove top fade
                fadeListener.removeTopFade()
            }
            if (position == items.size -1) { // when last item is attached remove bottom fade
                fadeListener.removeBottomFade()
            }
        }
    }

    // TODO BORIS this should be in new class
    override fun onViewDetachedFromWindow(holder: ChannelListSortViewHolder) {
        super.onViewDetachedFromWindow(holder)
        val position = holder.adapterPosition
        fadeListener?.let { fadeListener -> // only if fadeListener has been initialised (in some scenes or widgets there is no need for handling fade in VerticalGridView and there fadeListener hasn't been implemented)
            if (position == 0) { // when first item is detached add top fade
                fadeListener.addTopFade()
            }
            if (position == items.size -1) { // when last item is detached add bottom fade
                fadeListener.addBottomFade()
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.R)
    override fun onBindViewHolder(holder: ChannelListSortViewHolder, position: Int) {

        var item: ITrack? = items[position]

        holder.sortItemDrawableButton!!.setDrawable(null)
        holder.sortItemDrawableButton!!.setText("")
        selectedItemViewHolder?.let {
            if (it.adapterPosition == position) {
                setSelected(holder)
            }
        }

        if (adapterType == AdapterType.eAudioAdapter) {
            var languageName = getLanguageCodeMapper( item!!.languageName)

            if (languageName == UNDEFINED_LANGUAGE_STR) {

                var undefiedLanguagePos = 0
                items.forEach {
                    if(getLanguageCodeMapper(it.languageName) == UNDEFINED_LANGUAGE_STR){
                        undefiedLanguagePos++
                        if(it==item){
                            languageName = "Audio $undefiedLanguagePos"
                        }
                    }

                }

                if(item is IAudioTrack) {
                    if(item.isAnalogTrack) {
                        languageName = item.analogName!!
                    }
                }

                Log.i(TAG, "Audio language name: = $languageName")
                adapterListener!!.updateUndefinedCount()
            }

//            var audioInfo = item!!.getTrackInfo()
//            Log.i(TAG, "Audio Info -> $audioInfo ")
//            if(!audioInfo.isEmpty()){
//                holder.sortItem!!.setText( languageName.plus(audioInfo) )
//            }else{
//                holder.sortItem!!.setText(languageName)
//            }

            var audioTrack: ITrack? = null
            if (adapterListener != null && adapterListener?.getCurrentAudioTrack() != null) {
                audioTrack = adapterListener?.getCurrentAudioTrack() as ITrack
            }

            if(audioTrack == null){
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getActiveAudioTrack failed")
            }else{
                selectedItem = audioTrack
                Log.i(TAG, "selectedAudioItem = " + selectedItem?.trackId)
            }
        }

        if (adapterType == AdapterType.eSubtitlesAdapter) {
            holder.sortItemDrawableButton!!.post {
                var languageName = getLanguageCodeMapper( item!!.languageName)
                if (languageName == UNDEFINED_LANGUAGE_STR) {
                    Log.i(TAG, "Subtitle : getUndefinedCountValue() = ${adapterListener!!.getUndefinedCount()}")
                    holder.sortItemDrawableButton!!.setText("Subtitle ${adapterListener!!.getUndefinedCount()}")
                    adapterListener!!.updateUndefinedCount()
                } else {
//                    var hohInfo = item!!.getTrackInfo()
//                    Log.i(TAG, "Subtitle info -> $hohInfo ")
//                    if(!hohInfo.isEmpty()){
//                        holder.sortItem!!.setText(languageName.plus(hohInfo))
//                    }else{
//                        holder.sortItem!!.setText( languageName )
//                    }
                }
            }
            var subtitleTrack: ITrack? = null
            if (adapterListener != null && adapterListener?.getCurrentSubtitleTrack() != null ) {
                subtitleTrack = adapterListener?.getCurrentSubtitleTrack() as ITrack
            }

            if(subtitleTrack == null){
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "getActiveAudioTrack failed")
            }else{
                selectedItem = subtitleTrack
                Log.i(TAG, "selectedSubtitleItem = " + selectedItem?.trackId)
            }
        }


        holder.sortItemDrawableButton!!.setDrawable(null)

        holder.sortItemDrawableButton!!.background =
            ContextCompat.getDrawable(
                ReferenceApplication.applicationContext(),
                R.drawable.transparent_shape
            )

        holder.sortItemDrawableButton!!.getTextView().setTextColor(
            Color.parseColor(ConfigColorManager.getColor("color_main_text"))
        )

        holder.rootView!!.onFocusChangeListener =
            View.OnFocusChangeListener { view, hasFocus ->
                if (holder.adapterPosition == -1) {
                    return@OnFocusChangeListener
                }

                holder.sortItemDrawableButton!!.onFocusChange(hasFocus)
                if (hasFocus) {
                    if (selectedItem == items[holder.adapterPosition]) {
                        holder.sortItemDrawableButton!!.setDrawable(
                            ContextCompat.getDrawable(
                                ReferenceApplication.applicationContext(),
                                R.drawable.check_focused
                            )
                        )
                        try {
                            val color_context =
                                Color.parseColor(ConfigColorManager.getColor("color_background"))
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color_context $color_context")
                            holder.sortItemDrawableButton!!.getDrawable().imageTintList =
                                ColorStateList.valueOf(color_context)

                        } catch (ex: Exception) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color rdb $ex")
                        }
                    }
                    else {
                        holder.sortItemDrawableButton!!.setDrawable(null)
                    }
                    holder.sortItemDrawableButton!!.getTextView().animate().scaleY(1.06f)
                        .scaleX(1.06f).duration =
                        0
                    holder.sortItemDrawableButton!!.background =
                        ConfigColorManager.generateButtonBackground()

                    try {
                        val color_context =
                            Color.parseColor(ConfigColorManager.getColor("color_background"))
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color_context $color_context")
                        holder.sortItemDrawableButton!!.getTextView().setTextColor(
                            color_context
                        )
                    } catch (ex: Exception) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color rdb $ex")
                    }
                    Utils.fadeInAnimationForEmptyButtons(holder.sortItemDrawableButton!!,100)
                }
                else if (selectedItem != items[holder.adapterPosition]) {
                    holder.sortItemDrawableButton!!.getTextView().animate().scaleY(1f).scaleX(1f).duration = 0
                    holder.sortItemDrawableButton!!.background =
                        ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.transparent_shape
                        )
                    holder.sortItemDrawableButton!!.setDrawable(null)
                    holder.sortItemDrawableButton!!.getTextView().setTextColor(
                        Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                    )

                }
                else {
                    setSelected(holder)
                    holder.sortItemDrawableButton!!.getTextView().animate().scaleY(1f).scaleX(1f).duration = 0
                    holder.sortItemDrawableButton!!.background =
                        ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.transparent_shape
                        )

                    holder.sortItemDrawableButton!!.setDrawable(
                        ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.check_unfocused
                        )
                    )
                    try {
                        val color_context =
                            Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color_context $color_context")
                        holder.sortItemDrawableButton!!.getDrawable().imageTintList =
                            ColorStateList.valueOf(color_context)

                    } catch (ex: Exception) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color rdb $ex")
                    }
                    holder.sortItemDrawableButton!!.getTextView().setTextColor(
                        Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                    )
                }
            }

        holder.rootView?.setOnClickListener {
            animationInProgress = true
            Utils.viewClickAnimation(it, object : AnimationListener{
                override fun onAnimationEnd() {
                    animationInProgress= false
                    if (adapterType == AdapterType.eAudioAdapter) {

                        if (selectedItem != items[holder.adapterPosition]) {
                            selectedItem = items[holder.adapterPosition]

                            selectedItemViewHolder?.rootView?.onFocusChangeListener?.onFocusChange(
                                selectedItemViewHolder?.rootView,
                                false
                            )
                            setSelected(holder)
                            selectedItemViewHolder = holder
                            if (adapterListener != null) {
                                adapterListener?.onAudioItemClicked(selectedItem as IAudioTrack)
                            }
                        } else {
                            //Do nothing since audio track is already active
                        }
                    }
                    else {


                        if (selectedItem != items[holder.adapterPosition]) {
                            selectedItem = items[holder.adapterPosition]

                            selectedItemViewHolder?.rootView?.onFocusChangeListener?.onFocusChange(
                                selectedItemViewHolder?.rootView,
                                false
                            )
                            setSelected(holder)
                            selectedItemViewHolder = holder
                            if (adapterListener != null) {
                                adapterListener?.onSubtitleItemClicked(selectedItem!! as ISubtitle)
                            }
                        } else {
                            selectedItem = null
                            holder.sortItemDrawableButton!!.setDrawable(null)
                            selectedItemViewHolder = null
                            if (adapterListener != null) {
                                adapterListener?.onSubtitleItemClicked(null)
                            }
                        }
                    }
                }
            })
        }

        holder.rootView!!.setOnKeyListener( object : View.OnKeyListener {
            override fun onKey(view: View?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
                if (adapterListener != null) {
                    if (animationInProgress)return true
                    return adapterListener!!.onKeyPressed(keyEvent!!)
                }
                return false
            }
        })
    }

    private fun getLanguageCodeMapper(language: String) : String{
        Log.i(TAG, "getLanguageCodeMapper() : language ->  $language ")
        if (language.length <= 1)
            return UNDEFINED_LANGUAGE_STR

        if (language.length == 2) {
            if (languageMapper!!.getLanguageName(language) == null) {
                return UNDEFINED_LANGUAGE_STR
            }
            return languageMapper!!.getLanguageName(language)!!
        } else {
            if (languageMapper!!.getLanguageName(language.substring(0, 3)) == null) {
                return UNDEFINED_LANGUAGE_STR
            }
            return languageMapper!!.getLanguageName(language.substring(0, 3))!!
        }
        return ""
    }

    fun setSelected(holder: ChannelListSortViewHolder?) {
        selectedItemViewHolder = holder
        selectedItem = items[holder!!.adapterPosition]
        holder!!.sortItemDrawableButton!!.setDrawable(
            ContextCompat.getDrawable(
                ReferenceApplication.applicationContext(),
                R.drawable.check_focused
            )
        )
        try {
            val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setSelected: Exception color_context $color_context")
            holder.sortItemDrawableButton!!.getDrawable().imageTintList = ColorStateList.valueOf(color_context)

        } catch (ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setSelected: Exception color rdb $ex")
        }
        try {
            val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setSelected: Exception color_context $color_context")
            holder.sortItemDrawableButton!!.getTextView().setTextColor(
                color_context
            )
        } catch (ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setSelected: Exception color rdb $ex")
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    //Refresh
    fun refresh(adapterItems: MutableList<ITrack>) {
        selectedItem = null
        selectedItemViewHolder = null
        this.items.clear()
        this.items.addAll(adapterItems)
        notifyDataSetChanged()
    }

    @JvmName("setAdapterType1")
    fun setAdapterType(type: AdapterType) {
        adapterType = type
    }

    interface AudioSubtitleTrackAdapterListener: TTSSetterInterface {
        fun onAudioItemClicked(obj: IAudioTrack?)
        fun onSubtitleItemClicked(obj: ISubtitle?)
        fun onKeyPressed(keyEvent: KeyEvent): Boolean
        fun getCurrentAudioTrack(): IAudioTrack?
        fun getCurrentSubtitleTrack() :  ISubtitle?
        fun getUndefinedCount(): Int
        fun updateUndefinedCount()
    }

    // TODO BORIS remove this into separate class which will inherit Adapter from RecyclerView.
    interface AudioSubtitleTrackFadeListener {
        /**
         * callback used to ADDING TOP FADE in VerticalGridView when first item is not visible any more.
         */
        fun addTopFade()
        /**
         * callback used to REMOVING TOP FADE in VerticalGridView when first item item start being visible.
         */
        fun removeTopFade()
        /**
         * callback used for ADDING BOTTOM FADE in VerticalGridView when last item is not visible any more.
         */
        fun addBottomFade()
        /**
         * callback used for REMOVING BOTTOM FADE in VerticalGridView when last item start being visible.
         */
        fun removeBottomFade()
    }

}