package com.iwedia.cltv.scene.home_scene.guide

import android.animation.Animator
import android.graphics.Color
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.utils.InvalidDataTracker
import com.iwedia.cltv.utils.Utils
import core_entities.Error
import listeners.AsyncReceiver

/**
 * Guide channel list adapter
 *
 * @author Dejan Nadj
 */
class GuideChannelListAdapter : RecyclerView.Adapter<GuideChannelListViewHolder>() {

    private var isParentalLockEnabled: Boolean = false

    //Items
    private var items = mutableListOf<TvChannel>()

    //View holders map
    private var holders = HashMap<Int, GuideChannelListViewHolder>()

    var adapterListener: GuideChannelListAdapterListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GuideChannelListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.guide_channel_list_item, parent, false)
        return GuideChannelListViewHolder(view)
    }

    override fun onViewAttachedToWindow(holder: GuideChannelListViewHolder) {
        super.onViewAttachedToWindow(holder)
        holders[holder.adapterPosition] = holder
    }

    override fun onViewDetachedFromWindow(holder: GuideChannelListViewHolder) {
        super.onViewDetachedFromWindow(holder)
        holders.remove(holder.adapterPosition)
    }

    override fun onBindViewHolder(holder: GuideChannelListViewHolder, position: Int) {
        val item = items[position]
        holders[position] = holder
        holder.tvChannel = item
        holder.channelIndex!!.text = item.getDisplayNumberText()
        holder.channelName!!.text = ""
        holder.channelIndex!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        holder.channelIndex!!.alpha=0.48F
        holder.channelSource?.text = adapterListener?.getChannelSourceType(tvChannel = item)
        holder.channelSource?.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        holder.channelSource?.alpha=0.48F
        holder.channelIndex!!.alpha = 0.48F


        holder.isSkipped!!.alpha = 0.49F
        holder.isSkipped!!.visibility = if (item.isSkipped) View.VISIBLE else View.GONE

        if (adapterListener!!.isAccessibilityEnabled()) {
            holder.itemView.setOnClickListener {
                adapterListener?.changeChannelFromTalkback(items[position])
            }
        }

        holder.isLocked!!.alpha = 0.49F
        holder.isLocked!!.visibility =
            if (item.isLocked && isParentalLockEnabled) View.VISIBLE else View.GONE

        holder.isScrambled?.alpha = 0.49F
        holder.isScrambled!!.visibility =
            if (adapterListener!!.isScrambled() && adapterListener!!.activeChannel()
                    .getUniqueIdentifier() == item.getUniqueIdentifier()
            ) View.VISIBLE else View.GONE


        var channelLogoPath = item.logoImagePath

        holder.channelName!!.visibility = View.GONE
        holder.channelLogo!!.visibility = View.VISIBLE
        if (channelLogoPath != null) {
            Utils.loadImage(channelLogoPath, holder.channelLogo!!, object : AsyncReceiver {
                override fun onFailed(error: Error?) {
                    setChannelName(holder, item)
                }

                override fun onSuccess() {
                    InvalidDataTracker.setValidData(item)
                }
            })
        } else {
            setChannelName(holder, item)
        }
    }

    private fun setChannelName(holder: GuideChannelListViewHolder, item: TvChannel) {
        holder.channelLogo!!.visibility = View.GONE
        holder.channelName!!.visibility = View.VISIBLE
        holder.channelName!!.text = item.name
        holder.channelName!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        holder.channelName!!!!.setTextSize(
            TypedValue.COMPLEX_UNIT_PX,
            ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_13)
        )
        holder.channelName!!.post {
            if (holder.channelName!!.lineCount == 1) {
//                holder.channelName!!.setPadding(0, Utils.convertDpToPixel(5.0).toInt(), 0,0 )
            } else if (holder.channelName!!.lineCount == 2) {
                var maxLength = getTextWordMaxLength(holder.channelName.toString().split(" "))
                if (maxLength > 10) {
                    setChannelNameTextSize(maxLength, holder.channelName!!, item.displayNumber)
                }
            } else if (holder.channelName!!.lineCount > 2) {
                var maxLength = getTextWordMaxLength(holder.channelName.toString().split(" "))
                if (maxLength > 10) {
                    setChannelNameTextSize(maxLength, holder.channelName!!, item.displayNumber)
                } else {
                    val textSize =
                        ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_12)
                    holder.channelName!!.setTextSize(
                        TypedValue.COMPLEX_UNIT_PX,
                        textSize
                    )
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
            textView.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                textSize
            )
        } else {
            val textSize = if (channelNumber.length > 5)
                ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_8)
            else
                ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_10)
            textView.setTextSize(
                TypedValue.COMPLEX_UNIT_PX,
                textSize
            )
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }


    /**
     * Show channel list separator view
     *
     * @param index item index
     * @param show show or hide separator view
     */
    fun showSeparator(index: Int, show: Boolean) {
        holders.values.forEach { holder ->
            holder.separatorView?.visibility = View.GONE
        }
        if (show) {
            holders[index]?.separatorView?.visibility = View.VISIBLE

            for (i in index + 1 until index + 8) {
                var holder = holders[i]
                holder?.let {
                    it.itemView.translationY = -Utils.getDimens(R.dimen.custom_dim_186)
                    it.itemView.animate().setListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(p0: Animator) {
                        }

                        override fun onAnimationEnd(p0: Animator) {
                            it.itemView.translationY = 0f
                        }

                        override fun onAnimationCancel(p0: Animator) {
                            it.itemView.translationY = 0f
                        }

                        override fun onAnimationRepeat(p0: Animator) {
                        }
                    }).translationY(0f).duration = 200
                }
            }
        } else {
            for (i in index + 1 until index + 8) {
                var holder = holders[i]
                holder.let {
                    it?.itemView?.translationY = Utils.getDimens(R.dimen.custom_dim_186)
                    it?.itemView?.animate()?.setListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(p0: Animator) {
                        }

                        override fun onAnimationEnd(p0: Animator) {
                            it.itemView.translationY = 0f
                            holders[index]?.separatorView?.visibility = View.GONE
                        }

                        override fun onAnimationCancel(p0: Animator) {
                            it.itemView.translationY = 0f
                            holders[index]?.separatorView?.visibility = View.GONE
                        }

                        override fun onAnimationRepeat(p0: Animator) {
                        }
                    })?.translationY(0f)?.duration = 200
                }
            }
        }
    }

    /**
     * Hide channel list separator instantly
     */
    fun resetSeparator() {
        holders.values.forEach { holder ->
            holder.let {
                it.separatorView?.visibility = View.GONE
                it.itemView.clearAnimation()
                it.itemView.translationY = 0f
            }
        }
    }

    /**
     * Hide channel list items inside the list above the position
     *
     * @param position item position
     */
    fun hideItemsAbove(position: Int) {
        showItems()
        for (index in position downTo 0) {
            holders[index]?.channelIndex?.visibility = View.INVISIBLE
            holders[index]?.channelLogo?.visibility = View.INVISIBLE
        }
    }

    /**
     * Show all items inside the list
     */
    fun showItems() {
        holders.values.forEach {
            it?.channelIndex?.visibility = View.VISIBLE
            it?.channelLogo?.visibility = View.VISIBLE
        }
    }


    fun removeChannel(position: Int) {
        items.removeAt(position)
        notifyDataSetChanged()
    }

    //Refresh
    fun refresh(adapterItems: MutableList<TvChannel>) {
        this.holders.clear()
        this.items.clear()
        this.items.addAll(adapterItems)
        notifyDataSetChanged()
    }

    fun addNewChannelsToEnd(adapterItems: MutableList<TvChannel>) {
        this.items.addAll(adapterItems)
        notifyItemRangeInserted(itemCount, adapterItems.size)
    }

    fun addNewChannelsToStart(adapterItems: MutableList<TvChannel>) {
        items.addAll(0, adapterItems)
        val holders = java.util.HashMap<Int, GuideChannelListViewHolder>()

        this.holders.forEach { (key, value) ->
            holders[adapterItems.size + key] = value
        }
        this.holders = holders
        notifyItemRangeInserted(0, adapterItems.size)
    }

    fun isParentalEnabled(enabled: Boolean) {
        isParentalLockEnabled = enabled
    }

    fun getVisibleChannels(): MutableList<TvChannel> {
        val channelList = mutableListOf<TvChannel>()
        holders.forEach { item ->
            if (item.value.itemView.isAttachedToWindow) {
                channelList.add(item.value.tvChannel!!)
            }
        }
        return channelList
    }

    interface GuideChannelListAdapterListener {
        fun changeChannelFromTalkback(tvChannel: TvChannel)
        fun isAccessibilityEnabled(): Boolean
        fun getChannelSourceType(tvChannel: TvChannel):String
        fun isScrambled(): Boolean
        fun activeChannel(): TvChannel
    }

}