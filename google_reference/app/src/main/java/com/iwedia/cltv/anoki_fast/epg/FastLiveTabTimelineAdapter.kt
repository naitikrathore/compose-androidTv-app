package com.iwedia.cltv.anoki_fast.epg

import android.graphics.Color
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnKeyListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.utils.Utils

/**
 * Guide timeline adapter
 *
 * @author Dejan Nadj
 */
class FastLiveTabTimelineAdapter: RecyclerView.Adapter<FastLiveTabTimelineViewHolder>() {

    //Items
    private var items = mutableListOf<String>()

    //Guide timeline  adapter listener
    var guideTimelineAdapterListener: GuideTimelineAdapterListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FastLiveTabTimelineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.guide_timeline_item, parent, false)

        if (FastLiveTabDataProvider.isAccessibilityEnabled()) {
            view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
            view.isFocusable = false
        }

        val guide_timeline_text: TextView = view!!.findViewById(R.id.guide_timeline_text)
        guide_timeline_text.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))

        return FastLiveTabTimelineViewHolder(view)
    }

    override fun onBindViewHolder(holder: FastLiveTabTimelineViewHolder, position: Int) {
        val item = items[position]

        if (FastLiveTabDataProvider.isAccessibilityEnabled()) {
            holder.itemView.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
            holder.itemView.isFocusable = false
        }

        holder.timeTextView!!.text = item

        //Set item width
        var layoutParams = holder.rootView?.layoutParams
        var width = FastLiveTab.GUIDE_TIMELINE_ITEM_SIZE
        if (items.lastIndex == position) {
            width = Utils.getDimensInPixelSize(R.dimen.custom_dim_81_5)
        }

        layoutParams?.width = width
        holder.rootView?.layoutParams = layoutParams
        holder.rootView?.invalidate()

        holder.rootView!!.setOnKeyListener(object: OnKeyListener {
            override fun onKey(view: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if(guideTimelineAdapterListener != null) {
                    if (guideTimelineAdapterListener?.dispatchKey(keyCode,event!!)!!) {
                        return true
                    }
                }
                return true
            }
        })
    }

    override fun getItemCount(): Int {
        return items.size
    }

    //Refresh
    fun refresh(adapterItems: MutableList<String>) {
        this.items.clear()
        this.items.addAll(adapterItems)
        notifyDataSetChanged()
    }

    fun getItems(): MutableList<String> {
        return items
    }

    //Pagination
    fun extendTimeline(
        adapterItems: MutableList<String>,
        extendingPreviousDay: Boolean,
        dayCount: Int,
    ) {

        val insertPosition = if(!extendingPreviousDay) this.items.size else 0
        if(!extendingPreviousDay) notifyItemChanged(items.lastIndex)
        this.items.addAll(insertPosition,adapterItems)

        if(dayCount>1){
            var flag = false
            if(insertPosition == 0){
                this.items.reversed().forEach {
                    if (!flag ){
                        this.items.remove(it)
                    }
                    if(it == "12:00AM" || it == "00:00"){
                        flag = true
                        return@forEach
                    }
                }
            }else{
                this.items.toMutableList().forEach {
                    if (!flag ){
                        this.items.remove(it)
                    }
                    if(it == "11:30PM" || it == "23:30"){
                        flag = true
                        return@forEach
                    }

                }

            }
        }

        notifyDataSetChanged()
    }

    //Guide timeline adapter listener
    interface GuideTimelineAdapterListener {
        fun dispatchKey(keyCode: Int, keyEvent: KeyEvent): Boolean
    }
}