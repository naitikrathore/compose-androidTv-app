package com.iwedia.cltv.scene.home_scene.guideVertical

import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.utils.InvalidDataTracker
import com.iwedia.cltv.utils.Utils
import core_entities.Error
import listeners.AsyncReceiver
import java.util.*
import kotlin.collections.HashMap

/**
 * Vertical guide channel list adapter
 *
 * @author Thanvandh Natarajan
 */
class VerticalGuideChannelListAdapter : RecyclerView.Adapter<VerticalGuideChannelListViewHolder>() {

    //Items
    private var items = mutableListOf<TvChannel>()
    //View holders map
    private var holders = HashMap<Int, VerticalGuideChannelListViewHolder>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerticalGuideChannelListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.guide_channel_list_item_horizontal, parent, false)
        return VerticalGuideChannelListViewHolder(view)
    }

    override fun onBindViewHolder(holder: VerticalGuideChannelListViewHolder, position: Int) {
        val item = items[position]
        holders[position] = holder

        holder.channelIndex!!.text = item.getDisplayNumberText()
        holder.channelIndex!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        holder.channelName!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        holder.isSkipped!!.visibility= if(item.isSkipped)View.VISIBLE else View.GONE
        holder.isLocked!!.visibility= if(item.isLocked)View.VISIBLE else View.GONE



        if (InvalidDataTracker.hasValidData(item)) {
            holder.channelName!!.visibility = View.GONE
            holder.channelLogo!!.visibility = View.VISIBLE
            Utils.loadImage(item.logoImagePath!!, holder.channelLogo!!, object : AsyncReceiver {
                override fun onFailed(error: Error?) {
                    InvalidDataTracker.setInvalidData(item)
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

    private fun setChannelName(holder: VerticalGuideChannelListViewHolder, item: TvChannel) {
        holder.channelLogo!!.visibility = View.GONE
        holder.channelName!!.visibility = View.VISIBLE
        holder.channelName!!.text = item.name

        holder.channelName!!.typeface =
                TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_bold"))

        holder.channelName!!.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_15))

        holder.channelName!!.doOnPreDraw {
            if(holder.channelName!!.lineCount>2){
                holder.channelName!!.typeface =
                        TypeFaceProvider.getTypeFace(ReferenceApplication.applicationContext(), ConfigFontManager.getFont("font_medium"))

                holder.channelName!!.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        ReferenceApplication.applicationContext().resources.getDimension(R.dimen.font_12))
            }
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
        } else {
            holders[index]?.separatorView?.visibility = View.GONE
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

    //Refresh
    fun refresh(adapterItems: MutableList<TvChannel>) {
        this.items.clear()
        this.items.addAll(adapterItems)
        notifyDataSetChanged()
    }
}