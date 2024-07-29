package com.iwedia.cltv.components

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import java.util.*

class PreferencesSystemInformationAdapter : RecyclerView.Adapter<PreferencesSystemInformationViewHolder>() {

    //Items
    private var items: MutableList<PreferencesSystemInformationData> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreferencesSystemInformationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.preferences_system_information_text_container, parent, false)
        return PreferencesSystemInformationViewHolder(view)
    }

    override fun onBindViewHolder(holder: PreferencesSystemInformationViewHolder, position: Int) {
        val item = items[position]

        holder.titleText?.setTextColor(Color.parseColor(ConfigColorManager.getColor(ConfigColorManager.getColor("color_main_text"), 0.8)))
        holder.titleText?.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )

        holder.contentText?.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        holder.contentText?.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )

        holder.titleText?.text = item.titleString
        holder.contentText?.text = item.contentString
    }

    fun refresh(adapterItems: MutableList<PreferencesSystemInformationData>) {
        this.items.clear()
        this.items.addAll(adapterItems)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return items.size
    }
}