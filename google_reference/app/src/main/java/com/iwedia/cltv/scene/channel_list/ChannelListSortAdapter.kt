package com.iwedia.cltv.scene.channel_list

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.utils.AnimationListener
import com.iwedia.cltv.utils.Utils
import com.iwedia.cltv.utils.Utils.Companion.viewClickAnimation

/**
 * ChannelListSortAdapter
 *
 * @author Aleksandar Milojevic
 */
class ChannelListSortAdapter : RecyclerView.Adapter<ChannelListSortViewHolder>() {

    //Items
    private var items = mutableListOf<String>()

    //Selected item
    var selectedItem = -1

    var isSelected = false

    private var selectedItemViewHolder: ChannelListSortViewHolder? = null

    //Adapter listener
    var adapterListener: ChannelListSortAdapterListener? = null

    //Is animation in progress flag
    var animationInProgress = false
    val TAG = javaClass.simpleName

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelListSortViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.sort_by_item, parent, false)
        return ChannelListSortViewHolder(view, textToSpeechTextSetterListener = adapterListener!!)
    }

    override fun onBindViewHolder(holder: ChannelListSortViewHolder, position: Int) {
        val item = items[position]

        holder.sortItemDrawableButton!!.setText(item)
        holder.sortItemDrawableButton!!.setDrawable(null)

        // Reset view
        holder.sortItemDrawableButton!!.background =
            ContextCompat.getDrawable(
                ReferenceApplication.applicationContext(),
                R.drawable.transparent_shape
            )

        holder.sortItemDrawableButton!!.getTextView().setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        if (selectedItem == position) {
            selectedItemViewHolder = holder
            holder.sortItemDrawableButton!!.setDrawable(
                ContextCompat.getDrawable(
                    ReferenceApplication.applicationContext(),
                    R.drawable.check_unfocused
                )
            )
            try {
                val color_context = Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color_context $color_context")
                holder.sortItemDrawableButton!!.getDrawable().imageTintList = ColorStateList.valueOf(color_context)

            } catch(ex: Exception) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color rdb $ex")
            }
        }
        holder.rootView?.setOnClickListener {
            animationInProgress = true
            viewClickAnimation(holder.sortItemDrawableButton, object : AnimationListener {
                override fun onAnimationEnd() {
                    animationInProgress = false
                    if (adapterListener != null) {
                        adapterListener?.onItemClicked(holder.adapterPosition)

                    }
                }
            })
            if (selectedItem != holder.adapterPosition) {
                selectedItem = holder.adapterPosition
                selectedItemViewHolder?.rootView?.onFocusChangeListener?.onFocusChange(
                    selectedItemViewHolder?.rootView,
                    false
                )
                selectedItemViewHolder = holder
                setSelected(holder)
            }
        }

        holder.rootView!!.onFocusChangeListener =
            View.OnFocusChangeListener { view, hasFocus ->
                holder.sortItemDrawableButton!!.onFocusChange(hasFocus)
                if (hasFocus) {
                    if (selectedItem == -1) {
                        return@OnFocusChangeListener
                    }
                    Utils.focusAnimation(holder.sortItemDrawableButton!!,true)
                    holder.sortItemDrawableButton!!.background = ConfigColorManager.generateButtonBackground()

                    try {
                        val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color_context $color_context")
                        holder.sortItemDrawableButton!!.getTextView().setTextColor(
                            color_context
                        )
                    } catch(ex: Exception) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color rdb $ex")
                    }

                    if (selectedItem == holder.adapterPosition) {
                        holder.sortItemDrawableButton!!.setDrawable(
                            ContextCompat.getDrawable(
                                ReferenceApplication.applicationContext(),
                                R.drawable.check_focused
                            )
                        )
                        try {
                            val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color_context $color_context")
                            holder.sortItemDrawableButton!!.getDrawable().imageTintList = ColorStateList.valueOf(color_context)

                        } catch(ex: Exception) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color rdb $ex")
                        }
                    } else {
                        holder.sortItemDrawableButton!!.setDrawable(null)
                    }
                } else if (selectedItem != holder.adapterPosition) {
                    holder.sortItemDrawableButton!!.getTextView().animate().scaleY(1f).scaleX(1f).duration = 0
                    holder.sortItemDrawableButton!!.background =
                        ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.transparent_shape
                        )
                    holder.sortItemDrawableButton!!.setDrawable(null)
                    holder.sortItemDrawableButton!!.getTextView().setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                } else {
                    setSelected(holder)
                    holder.sortItemDrawableButton!!.getTextView().animate().scaleY(1f).scaleX(1f).duration = 0
                    holder.sortItemDrawableButton!!.background =
                        ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.transparent_shape
                        )

                    holder.sortItemDrawableButton!!.setDrawable(
                        ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.check_unfocused
                        )
                    )
                    try {
                        val color_context = Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color_context $color_context")
                        holder.sortItemDrawableButton!!.getDrawable().imageTintList = ColorStateList.valueOf(color_context)

                    } catch(ex: Exception) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color rdb $ex")
                    }
                    holder.sortItemDrawableButton!!.getTextView().setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
                }
            }

        holder.rootView!!.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(view: View?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
                if (animationInProgress){
                    return true
                }
                if (adapterListener != null) {
                    return adapterListener!!.onKeyPressed(keyEvent!!)
                }
                return false
            }
        })
    }


    fun setSelected(holder: ChannelListSortViewHolder?) {
        selectedItemViewHolder = holder
        selectedItem = holder!!.adapterPosition
        selectedItemViewHolder!!.sortItemDrawableButton!!.setDrawable(null)
        holder.sortItemDrawableButton!!.setDrawable(
            ContextCompat.getDrawable(
                ReferenceApplication.applicationContext(),
                R.drawable.check_focused
            )
        )
        try {
            val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setSelected: Exception color_context $color_context")
            holder.sortItemDrawableButton!!.getDrawable().imageTintList = ColorStateList.valueOf(color_context)

        } catch(ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setSelected: Exception color rdb $ex")
        }
        try {
            val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setSelected: Exception color_context $color_context")
            holder.sortItemDrawableButton!!.getTextView().setTextColor(
                color_context
            )
        } catch(ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setSelected: Exception color rdb $ex")
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun getSelectedItem(): String {
        return items[selectedItem]
    }

    //Refresh
    fun refresh(adapterItems: MutableList<String>) {
        this.items.clear()
        this.items.addAll(adapterItems)
        notifyDataSetChanged()
    }

    interface ChannelListSortAdapterListener: TTSSetterInterface {
        fun onItemClicked(position: Int)
        fun onKeyPressed(keyEvent: KeyEvent): Boolean
    }
}