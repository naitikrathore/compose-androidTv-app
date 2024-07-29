package com.iwedia.cltv.scene.fti.selectInput

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.tv.TvInputInfo
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.utils.Utils

/**
 * Select input list adapter
 *
 * @author Aleksandar Lazic
 */
class SelectInputListAdapter : RecyclerView.Adapter<SelectInputItemViewHolder>() {

    private var items = mutableListOf<TvInputInfo>()
    var adapterListener: AdapterClickListener? = null
    val TAG = javaClass.simpleName

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectInputItemViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.select_input_item, parent, false)

        val inputTitle: TextView = view.findViewById(R.id.inputTitle)
        inputTitle.setText(ConfigStringsManager.getStringById("not_available"))
        inputTitle.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        return SelectInputItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: SelectInputItemViewHolder, position: Int) {
        val item = items[position]
        holder.itemTitle!!.text = Utils.getTvInputName(item)

        holder.rootView!!.onFocusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {

                var selectorColor = ConfigColorManager.getColor("color_selector")
                var selectorDrawable = ContextCompat.getDrawable(
                        getContext(),
                        R.drawable.focus_shape
                )

                DrawableCompat.setTint(selectorDrawable!!, Color.parseColor(selectorColor))
                holder!!.itemTitle!!.background = selectorDrawable

                try {
                    val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color_context $color_context")
                    holder.itemTitle!!.setTextColor(color_context)
                } catch(ex: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color rdb $ex")
                }
                holder.itemTitle!!.typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_medium")
                )
            } else {
                holder.itemTitle!!.background = ContextCompat.getDrawable(
                        getContext(),
                        R.drawable.reference_button_non_focus_shape
                )
                holder.itemTitle!!.setBackgroundResource(0)
//                holder.itemTitle!!.backgroundTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text").replace("#",ConfigColorManager.alfa_light)))

                try {
                    val color_context = Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color_context $color_context")
                    holder.itemTitle!!.setTextColor(color_context)
                } catch(ex: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color rdb $ex")
                }
                holder.itemTitle!!.typeface = TypeFaceProvider.getTypeFace(
                        ReferenceApplication.applicationContext(),
                        ConfigFontManager.getFont("font_regular")
                )
            }
        }

        holder.rootView!!.setOnClickListener {
            adapterListener!!.onItemClicked(position ,item)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    private fun getContext(): Context {
        return ReferenceApplication.applicationContext()
    }

    fun setListener(adapterListener: AdapterClickListener) {
        this.adapterListener = adapterListener
    }

    interface AdapterClickListener {
        fun onItemClicked(position: Int , inputInfo: TvInputInfo)
    }

    fun refresh(adapterItems: MutableList<TvInputInfo>) {
        this.items.clear()
        this.items.addAll(adapterItems)
        notifyDataSetChanged()
    }
}