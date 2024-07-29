package com.iwedia.cltv.anoki_fast.epg

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.`interface`.TTSSpeakTextForFocusedViewInterface
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.platform.model.text_to_speech.Type

/**
 * [FastLiveTabChannelListViewHolder] is used in [FastLiveTabChannelListAdapter] to hoist information
 * about channel (it's logo) and if there is no image for logo, channel's title will be displayed.
 *
 * @author Boris Tirkajla
 */
class FastLiveTabChannelListViewHolder(
    view: View,
    private val ttsSetterForSelectableViewInterface: TTSSetterForSelectableViewInterface
) : RecyclerView.ViewHolder(view), TTSSpeakTextForFocusedViewInterface {

    private val heartIconEmpty = R.drawable.heart_empty_icon
    private val heartIconFull = R.drawable.heart_full_icon

    private lateinit var tvChannel: TvChannel

    var rootView: MaterialCardView

    var channelLogoImageView: ImageView

    var channelNameTextView: TextView

    var favoriteImageView: ImageView

    /**
     * [isFavorite] is label used to distinguish whether channel is favorite or not.
     */
    var isFavorite = false

    init {
        rootView = view.findViewById(R.id.root_view)
        channelLogoImageView = view.findViewById(R.id.channel_image_view)
        channelNameTextView = view.findViewById(R.id.channel_name_text_view)
        favoriteImageView = view.findViewById(R.id.favorite_image_view)

        if (FastLiveTabDataProvider.isAccessibilityEnabled()) {
            channelNameTextView.isFocusable = true
            channelNameTextView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
            channelLogoImageView.isFocusable = true
            channelLogoImageView.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }

        channelNameTextView.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_bold")
            )

        updateFavoriteIcon() // initially set resource to empty heart
    }

    private fun updateFavoriteIcon() {
        favoriteImageView.setImageResource(
            if (isFavorite) heartIconFull else heartIconEmpty
        )
    }

    /**
     * [switchFavoriteState] is method called to switch favorite state. It should be called from corresponding
     * adapter whenever user clicks on Channel to switch resource of the icon in ImageView.
     */
    fun switchFavoriteState() {
        isFavorite = !isFavorite
        updateFavoriteIcon()
    }

    fun bindData(tvChannel: TvChannel) {
        this.tvChannel = tvChannel
    }

    override fun speakTextForFocusedView() {
        ttsSetterForSelectableViewInterface.setSpeechTextForSelectableView(
            tvChannel.name,
            type = Type.FAVORITE,
            isChecked = isFavorite
        )
    }
}