package com.iwedia.cltv.anoki_fast.epg

import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.utils.AnimationListener
import com.iwedia.cltv.utils.Utils
import core_entities.Error
import listeners.AsyncReceiver
import kotlin.collections.HashMap

/**
 * [FastLiveTabChannelListAdapter] is used as adapter for items that represents channels having information
 * about channel's logo and title (title will be loaded if there is no image which can be inserted as logo).
 *
 * When item in adapter is focused border around logo image is shown.
 *
 * @author Boris Tirkajla
 */
class FastLiveTabChannelListAdapter(
    private var favoritesCheck: (tvChannel: TvChannel)->Boolean,
    private val ttsSetterForSelectableViewInterface: TTSSetterForSelectableViewInterface
    ) : RecyclerView.Adapter<FastLiveTabChannelListViewHolder>() {

    private var isParentalLockEnabled: Boolean = false

    //Items
    private var items = mutableListOf<TvChannel>()
    //View holders map
    private var holders = HashMap<Int, FastLiveTabChannelListViewHolder>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FastLiveTabChannelListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.guide_channel_list_item_fast, parent, false)

        if (FastLiveTabDataProvider.isAccessibilityEnabled()) {
            var holder = FastLiveTabChannelListViewHolder(view, ttsSetterForSelectableViewInterface)
            holder.setIsRecyclable(false)
            return holder
        }else{
            return FastLiveTabChannelListViewHolder(view, ttsSetterForSelectableViewInterface)
        }
    }

    override fun onBindViewHolder(holder: FastLiveTabChannelListViewHolder, position: Int) {
        val item = items[position]

        if (FastLiveTabDataProvider.isAccessibilityEnabled()) {
            holder.channelLogoImageView.setOnClickListener {
                holders[position]?.let {
                    Utils.viewClickAnimation(it.rootView, object : AnimationListener {
                        override fun onAnimationEnd() {
                            it.switchFavoriteState()
                        }
                    })
                }
            }
            holder.channelLogoImageView.setOnLongClickListener {
                FastLiveTabDataProvider.onChannelClickedFromTalkback(items[position])
                true
            }
        }

        holders[position] = holder

        holders[position]?.bindData(item)
        holder.channelNameTextView.text = ""

         holder.channelNameTextView.visibility = View.GONE
         holder.channelLogoImageView.visibility = View.VISIBLE

        var isFavorite = favoritesCheck(item)
        if (holder.isFavorite != isFavorite) {
            holder.switchFavoriteState()
        }

        Utils.loadImage(item.logoImagePath, holder.channelLogoImageView, object : AsyncReceiver {
            override fun onFailed(error: Error?) {
                setChannelName(holder, item)
            }

            override fun onSuccess() {
                holder.channelLogoImageView.visibility = View.VISIBLE
            }
        })
    }

    private fun setChannelName(holder: FastLiveTabChannelListViewHolder, item: TvChannel) {
        holder.channelLogoImageView.visibility = View.GONE
        holder.channelNameTextView.visibility = View.VISIBLE
        holder.channelNameTextView.text = item.name
        holder.channelNameTextView.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        holder.channelNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
            ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_13))
        holder.channelNameTextView.post {
            if (holder.channelNameTextView.lineCount == 1) {
//                holder.channelName!!.setPadding(0, Utils.convertDpToPixel(5.0).toInt(), 0,0 )
            } else if (holder.channelNameTextView.lineCount == 2) {
                var maxLength = getTextWordMaxLength(holder.channelNameTextView.toString().split(" "))
                if (maxLength > 10) {
                    setChannelNameTextSize(maxLength, holder.channelNameTextView, item.displayNumber)
                }
            } else if (holder.channelNameTextView.lineCount > 2) {
                val maxLength = getTextWordMaxLength(holder.channelNameTextView.toString().split(" "))
                if (maxLength > 10) {
                    setChannelNameTextSize(maxLength, holder.channelNameTextView,item.displayNumber)
                } else {
                    val textSize = ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_12)
                    holder.channelNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        textSize)
                }
            }
        }
    }

    // Returns max length of the word inside the string list
    private fun getTextWordMaxLength(text: List<String>): Int {
        var length = 0
        for (word in text) {
            if (word.length > length) {
                length = word.length
            }
        }
        return length
    }

    // Sets channel name text size depending on the text length and channel number
    private fun setChannelNameTextSize(maxLength: Int, textView: TextView, channelNumber: String) {
        if (maxLength in 11..12) {
            val textSize = if (channelNumber.length > 5)
                ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_10)
            else
                ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_12)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                textSize)
        } else {
            val textSize = if (channelNumber.length > 5)
                ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_8)
            else
                ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_10)
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                textSize)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    /**
     * Hide channel list items inside the list above the position
     *
     * @param position item position
     */
    fun hideItemsAbove(position: Int) {
        showItems()
        for (index in position downTo 0) {
            holders[index]?.channelLogoImageView?.visibility = View.INVISIBLE
        }
    }

    /**
     * Show all items inside the list
     */
    fun showItems() {
        holders.values.forEach {
            it.channelLogoImageView.visibility = View.VISIBLE
        }
    }


    fun removeChannel(position: Int){
        items.removeAt(position)
        notifyDataSetChanged()
    }

    /**
     * [setStrokeColor] is used to set the stroke color of the MaterialCardView used to display the
     * image of the channel in order to create a UX that indicates the card is being focused (even if it's not actually).
     */
    fun setStrokeColor(position: Int) {
        holders[position]?.rootView?.strokeColor = Color.parseColor("#CCE8F0FE")
        holders[position]?.speakTextForFocusedView()
    }

    /**
     * [removeStrokeColor] is used to remove the stroke color of the MaterialCardView used to display the
     * image of the channel in order to create a UX that indicates the card is NOT being focused.
     */
    fun removeStrokeColor(position: Int) {
        holders[position]?.rootView?.strokeColor = Color.TRANSPARENT
    }

    /**
     * [onItemClicked] is method used when user presses on the Channel MaterialCardView.
     */
    fun onItemClicked(position: Int, onClick: () -> Unit) {
        holders[position]?.let {
            Utils.viewClickAnimation(it.rootView, object : AnimationListener {
                override fun onAnimationEnd() {
                    it.switchFavoriteState()
                    holders[position]!!.speakTextForFocusedView()
                    onClick.invoke()
                }
            })

        }
    }

    //Refresh
    fun refresh(adapterItems: MutableList<TvChannel>, onRefreshFinished: () -> Unit = {}) {
        this.holders.clear()
        this.items.clear()
        this.items.addAll(adapterItems)
        notifyDataSetChanged()
        onRefreshFinished.invoke()
    }

    fun addNewChannelsToEnd(adapterItems: MutableList<TvChannel>){
        this.items.addAll(adapterItems)
        notifyItemRangeInserted(itemCount,adapterItems.size)
    }
    fun addNewChannelsToStart(adapterItems: MutableList<TvChannel>){
        items.addAll(0,adapterItems)
        val holders = java.util.HashMap<Int, FastLiveTabChannelListViewHolder>()

        this.holders.forEach { (key, value) ->
            holders[adapterItems.size+ key] = value
        }
        this.holders = holders
        notifyItemRangeInserted(0,adapterItems.size)
    }

    fun isParentalEnabled(enabled : Boolean){
        isParentalLockEnabled = enabled
    }

}