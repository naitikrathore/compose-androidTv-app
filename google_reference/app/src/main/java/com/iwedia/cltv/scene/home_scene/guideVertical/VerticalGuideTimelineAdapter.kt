package com.iwedia.cltv.scene.home_scene.guideVertical

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
 * Vertical guide timeline adapter
 *
 * @author Thanvandh Natarajan
 */
class VerticalGuideTimelineAdapter: RecyclerView.Adapter<VerticalGuideTimelineViewHolder>() {

    //Items
    private var items = mutableListOf<String>()

    //Guide timeline  adapter listener
    var guideTimelineAdapterListener: GuideTimelineAdapterListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerticalGuideTimelineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.guide_timeline_item_vertical, parent, false)

        val guide_timeline_text: TextView = view!!.findViewById(R.id.guide_timeline_text)
        guide_timeline_text.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_text_description")))

        return VerticalGuideTimelineViewHolder(view)
    }

    override fun onBindViewHolder(holder: VerticalGuideTimelineViewHolder, position: Int) {
        val item = items[position]
        holder.timeTextView!!.text = item

        var layoutParams = holder.rootView?.layoutParams

        var startOffset = position * VerticalGuideSceneWidget.GUIDE_TIMELINE_ITEM_SIZE
        var endOffset = (position+1) * VerticalGuideSceneWidget.GUIDE_TIMELINE_ITEM_SIZE


        if (items.lastIndex == position) {
            endOffset += Utils.getDimens(R.dimen.custom_dim_69)
        }


        var height = endOffset.toInt() - startOffset.toInt()


        layoutParams?.height = height
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

    //Pagination
    fun extendTimeline(adapterItems: MutableList<String>, extendingPreviousDay: Boolean) {
        val insertPosition = if(!extendingPreviousDay) this.items.size else 0
        if(!extendingPreviousDay) notifyItemChanged(items.lastIndex)
        this.items.addAll(insertPosition,adapterItems)
        notifyItemRangeInserted(insertPosition,adapterItems.size)
    }

    //Guide timeline adapter listener
    interface GuideTimelineAdapterListener {
        fun dispatchKey(keyCode: Int, keyEvent: KeyEvent): Boolean
    }
}