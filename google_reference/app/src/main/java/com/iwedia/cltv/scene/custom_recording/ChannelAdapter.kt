package com.iwedia.cltv.scene.custom_recording


import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.platform.model.TvChannel
import com.iwedia.cltv.utils.InvalidDataTracker
import com.iwedia.cltv.utils.Utils
import core_entities.Error
import listeners.AsyncReceiver


class ChannelAdapter : RecyclerView.Adapter<ChannelAdapterViewHolder>() {

    val TAG = javaClass.simpleName
    //Items
    private var items = mutableListOf<TvChannel>()

    //Selected item
    var selectedItem = -1

    var focusPosition = -1

    //Adapter listener
    var adapterListener: ChannelAdapterListener? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChannelAdapterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.channel_cr_item, parent, false)

        val channelNum: TextView = view.findViewById(R.id.channel_num)
        channelNum.setText(ConfigStringsManager.getStringById("encrypted"))
        return ChannelAdapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelAdapterViewHolder, position: Int) {
        clearFocus(holder)

        if (position == selectedItem) {
            setSelected(holder)
        }else{
            clearSelected(holder)
        }

        holder.rootView!!.onFocusChangeListener =
            View.OnFocusChangeListener { view, hasFocus ->
                if (selectedItem != holder.adapterPosition && !hasFocus) {

                    clearFocus(holder)
                    holder.checkBox!!.visibility = View.GONE


                } else if (selectedItem != holder.adapterPosition && hasFocus) {
                    adapterListener?.getAdapterPosition(position)
                    focusPosition = holder.adapterPosition
                    adapterListener!!.setFocusedPosition(focusPosition)
                    setFocused(holder)
                    holder.checkBox!!.visibility = View.GONE

                } else if (selectedItem == holder.adapterPosition && hasFocus) {
                    adapterListener?.getAdapterPosition(position)
                    focusPosition = holder.adapterPosition
                    adapterListener!!.setFocusedPosition(focusPosition)

                    setSelected(holder)
                    setFocused(holder)

                } else if (selectedItem == holder.adapterPosition && !hasFocus) {
                    selectedItem = holder.adapterPosition
                    setSelected(holder)

                    clearFocus(holder)
                }
            }
        holder.rootView!!.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(view: View?, p1: Int, p2: KeyEvent?): Boolean {
                if (!ReferenceApplication.worldHandler?.isEnableUserInteraction!!) {
                    return true
                }
                if (p2!!.action == KeyEvent.ACTION_DOWN) {
                    if (p1 == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if (adapterListener != null) {
                            return if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                                adapterListener!!.onKeyLeft(selectedItem)
                            } else {
                                adapterListener!!.onKeyRight(holder.adapterPosition)
                            }
                        }
                    } else if (p1 == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        if (adapterListener != null) {
                            return if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                                adapterListener!!.onKeyRight(holder.adapterPosition)
                            } else {
                                adapterListener!!.onKeyLeft(holder.adapterPosition)
                            }
                        }
                    } else if (p1 == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if (adapterListener != null) {
                            return adapterListener!!.onKeyDown(holder.adapterPosition)
                        }
                    } else if (p1 == KeyEvent.KEYCODE_DPAD_UP) {
                        if (adapterListener != null) {
                            return adapterListener!!.onKeyUp(holder.adapterPosition)
                        }
                    }
                }


                return false
            }
        })
        holder.rootView?.setOnClickListener {
            if (selectedItem != holder.adapterPosition) {
                val lastSelectedPosition = selectedItem
                selectedItem = holder.adapterPosition
                notifyItemChanged(lastSelectedPosition)
                holder.rootView?.onFocusChangeListener?.onFocusChange(
                    holder.rootView,
                    false
                )
                setSelected(holder)
                setFocused(holder)
            }
            if (adapterListener != null) {
                adapterListener?.onItemClicked(holder.adapterPosition)
            }
        }


        holder.channelNum!!.text = items[holder.adapterPosition].getDisplayNumberText()
        holder.channelNum!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))


        if (InvalidDataTracker.hasValidData(items[holder.adapterPosition])) {
            holder.channelName!!.visibility = View.GONE
            holder.channelImage!!.visibility = View.VISIBLE
            Utils.loadImage(
                items[holder.adapterPosition].logoImagePath!!,
                holder.channelImage!!,
                object : AsyncReceiver {
                    override fun onFailed(error: Error?) {

                        if (holder.adapterPosition == -1) {
                            return
                        }

                        InvalidDataTracker.setInvalidData(items[holder.adapterPosition])
                        holder.channelImage!!.visibility = View.GONE
                        holder.channelName!!.visibility = View.VISIBLE
                        holder.channelName!!.text = items[holder.adapterPosition].name
                        holder!!.checkBox!!.setImageResource(R.drawable.check_unfocused)
                        try {
                            val color_context = Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: Exception color_context $color_context")
                            holder.checkBox!!.imageTintList = ColorStateList.valueOf(color_context)

                        } catch(ex: Exception) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onFailed: Exception color rdb $ex")
                        }
                        holder.channelName!!.setTextColor(
                            Color.parseColor(
                                ConfigColorManager.getColor(
                                    "color_main_text"
                                )
                            )
                        )
                    }

                    override fun onSuccess() {
                        InvalidDataTracker.setValidData(items[holder.adapterPosition])
                    }
                })
        } else {
            holder.channelImage!!.visibility = View.GONE
            holder.channelName!!.visibility = View.VISIBLE
            holder.channelName!!.text = items[holder.adapterPosition].name
            holder.channelName!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
            holder!!.checkBox!!.setImageResource(R.drawable.check_unfocused)
            try {
                val color_context = Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color_context $color_context")
                holder.checkBox!!.imageTintList = ColorStateList.valueOf(color_context)

            } catch(ex: Exception) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color rdb $ex")
            }

        }

    }


    fun setSelected(holder: ChannelAdapterViewHolder?) {
        selectedItem = holder!!.adapterPosition
        holder!!.checkBox!!.visibility = View.VISIBLE
        if (focusPosition == holder.adapterPosition && !InvalidDataTracker.hasValidData(items[holder!!.adapterPosition])) {
            holder!!.checkBox!!.setImageResource(R.drawable.check_focused)
            try {
                val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "setSelected: Exception color_context $color_context")
                holder.checkBox!!.imageTintList = ColorStateList.valueOf(color_context)

            } catch(ex: Exception) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "setSelected: Exception color rdb $ex")
            }
        }
        if (focusPosition == holder.adapterPosition && InvalidDataTracker.hasValidData(items[holder!!.adapterPosition])) {
            holder!!.checkBox!!.setImageResource(R.drawable.check_unfocused)
            try {
                val color_context = Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "setSelected: Exception color_context $color_context")
                holder.checkBox!!.imageTintList = ColorStateList.valueOf(color_context)

            } catch(ex: Exception) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "setSelected: Exception color rdb $ex")
            }
        }


    }

    fun clearSelected(holder: ChannelAdapterViewHolder?) {
        holder?.checkBox!!.visibility = View.GONE
    }

    fun setFocused(holder: ChannelAdapterViewHolder?) {
        Log.i(
            "Kaustubh",
            "setFocused: ${InvalidDataTracker.hasValidData(items[holder!!.adapterPosition])}"
        )
        items.forEach {
            if (!InvalidDataTracker.hasValidData(items[holder!!.adapterPosition])) {
                var selectorColor = ConfigColorManager.getColor("color_selector")
                var selectorDrawable = ContextCompat.getDrawable(
                    ReferenceApplication.applicationContext(),
                    R.drawable.focus_shape
                )


                DrawableCompat.setTint(selectorDrawable!!, Color.parseColor(selectorColor))
                holder!!.rootView!!.background = selectorDrawable
                holder!!.channelNum!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_background")))
                holder!!.channelName!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_background")))
                if (holder!!.adapterPosition == selectedItem) {

                    holder!!.checkBox!!.setImageResource(R.drawable.check_focused)

                }
            } else {
                val border = GradientDrawable()
                border.setStroke(3, Color.parseColor(ConfigColorManager.getColor("color_selector")))
                border.cornerRadius = 1000F


                holder?.rootView!!.background = border
            }
        }


    }

    private fun clearFocus(holder: ChannelAdapterViewHolder) {
        if (selectedItem == holder.adapterPosition) {
            Log.i("kaustubh", "clearFocus: $selectedItem ${holder.adapterPosition}")


        }
        holder.rootView!!.background =
            ContextCompat.getDrawable(
                ReferenceApplication.applicationContext(),
                R.drawable.transparent_shape
            )
        holder!!.channelNum!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        holder!!.channelName!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        holder!!.checkBox!!.setImageResource(R.drawable.check_unfocused)
        try {
            val color_context = Color.parseColor(ConfigColorManager.getColor("color_main_text"))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "clearFocus: Exception color_context $color_context")
            holder.checkBox!!.imageTintList = ColorStateList.valueOf(color_context)

        } catch(ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "clearFocus: Exception color rdb $ex")
        }



    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun refresh(adapterItems: MutableList<TvChannel>) {
        this.items.clear()
        this.items.addAll(adapterItems)
        notifyDataSetChanged()
    }

    interface ChannelAdapterListener {
        fun getAdapterPosition(position: Int)
        fun onKeyLeft(currentPosition: Int): Boolean
        fun onKeyRight(currentPosition: Int): Boolean
        fun onKeyUp(currentPosition: Int): Boolean
        fun onKeyDown(currentPosition: Int): Boolean
        fun onItemClicked(position: Int)
        fun setFocusedPosition(position: Int)
    }


}