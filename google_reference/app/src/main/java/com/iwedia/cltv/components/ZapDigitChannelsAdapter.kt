package com.iwedia.cltv.components

import android.graphics.Color
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.isDigitsOnly
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.TvChannel

class ZapDigitChannelsAdapter : RecyclerView.Adapter<ZapDigitChannelViewHolder>() {

    val items = mutableListOf<TvChannel>()
    var adapterPosition = -1
    var zapDigitChannelsAdapterListener: ZapDigitChannelsAdapterListener? = null
    val TAG = javaClass.simpleName

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ZapDigitChannelViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.zap_digit_channel_item, parent, false)
        return ZapDigitChannelViewHolder(view, zapDigitChannelsAdapterListener!!)
    }

    override fun onBindViewHolder(holder: ZapDigitChannelViewHolder, position: Int) {
        var item = items[position]


        //add channel logo image to button

//            if (item.logoImagePath != null) {
//                Utils.loadImage(
//                    item.logoImagePath!!,
//                    holder.referenceDrawableButton!!.drawableView!!,
//                    object : AsyncReceiver {
//                        override fun onFailed(error: Error?) {
//                        }
//
//                        override fun onSuccess() {
//                        }
//                    })
//            }

        holder.referenceDrawableButton!!.shouldAnimate = false


//        holder.referenceDrawableButton!!.setText(
//            item.displayNumber + " " + item.name,
//
//        )

        var channelType = zapDigitChannelsAdapterListener?.getChannelSourceType(item)
        var channelTypeData = ""

        //TODO VASILISA this should be done in widget and not in adapter

        //for browsable channel display whole channel type
            if(item.isBrowsable) {
                channelTypeData = channelType!!
            }
        //for non browsable channel remove "analog"
            else{
                val channelTypeDataList = channelType!!.split(" ").toMutableList()
                channelTypeDataList.forEach{
                    if (it.lowercase() == "analog") {
                        return@forEach
                    }
                    channelTypeData += it + " "
                }
            }

        if(item.displayNumber.isDigitsOnly()) {
            holder.referenceDrawableButton!!.setSpannableText(
                item.getDisplayNumberText() + " " + item.name + " " + channelTypeData
            )
        }
        else {
            holder.referenceDrawableButton!!.setSpannableText(
                item.displayNumber + " " + item.name + " " + channelTypeData
            )
        }


        holder.referenceDrawableButton!!.setOnFocusChangeListener { view, hasFocus ->
            when (hasFocus) {
                true -> {
                    try {
                        val color_context =
                            Color.parseColor(ConfigColorManager.getColor("color_background"))
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color_context $color_context")
                        holder.referenceDrawableButton!!.getDrawable().setColorFilter(
                            color_context
                        )
                        adapterPosition = holder.adapterPosition
                    } catch (ex: Exception) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color rdb $ex")
                    }
                }

                false -> {
                    try {
                        val color_context =
                            Color.parseColor(ConfigColorManager.getColor("color_text_description"))
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color_context $color_context")
                        holder.referenceDrawableButton!!.getDrawable().setColorFilter(
                            color_context
                        )
                    } catch (ex: Exception) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color rdb $ex")
                    }
                }
            }
        }

        holder.referenceDrawableButton!!.setOnClickListener {
            zapDigitChannelsAdapterListener!!.getItem(item.id)
        }

        holder.referenceDrawableButton!!.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(p0: View?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
                zapDigitChannelsAdapterListener!!.onKey()
                if (keyEvent!!.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        if (zapDigitChannelsAdapterListener != null) {
                            return zapDigitChannelsAdapterListener!!.onKeyUp(holder.adapterPosition)
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if (zapDigitChannelsAdapterListener != null) {
                            return zapDigitChannelsAdapterListener!!.onKeyDown(holder.adapterPosition)
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        return true
                    }
                }
                return false
            }
        })
    }

    override fun getItemCount(): Int {
        return items.size
    }

    interface ZapDigitChannelsAdapterListener: TTSSetterInterface {
        fun getItem(itemId: Int)
        fun onKeyUp(position: Int): Boolean
        fun onKeyDown(position: Int): Boolean
        fun onKey()
        fun getChannelSourceType(item: TvChannel): String
    }

    fun refresh(list: MutableList<TvChannel>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }


}