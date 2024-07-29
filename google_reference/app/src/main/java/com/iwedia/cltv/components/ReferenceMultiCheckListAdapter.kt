package com.iwedia.cltv.components

import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceDrawableButton
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.utils.AnimationListener
import com.iwedia.cltv.utils.Utils

/**
 * Reference multi check list adapter
 *
 * @author Dejan Nadj
 */
class ReferenceMultiCheckListAdapter : RecyclerView.Adapter<ReferenceMultiCheckListViewHolder>() {

    //Items
    private var items = mutableListOf<String>()

    //Focused item index
    var focusedItem = -1

    //Selected item list
    private var selectedItems= arrayListOf<Int>()

    //Adapter listener
    var adapterListener: ReferenceMultiCheckListAdapterListener? = null

    //Is animation in progress flag
    var animationInProgress = false

    val TAG = javaClass.simpleName
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReferenceMultiCheckListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.multi_check_list_item, parent, false)
        return ReferenceMultiCheckListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReferenceMultiCheckListViewHolder, position: Int) {
        val item = items[position]
        holder.listItem!!.setText(item)
        holder.listItem!!.getTextView().typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )
        if (items.size == 1) {
            holder.listItem!!.getTextView().animate().scaleY(1.06f).scaleX(1.06f).duration =
                0
            holder.listItem!!.background = ConfigColorManager.generateButtonBackground()
            try {
                val color_context =
                    Color.parseColor(ConfigColorManager.getColor("color_background"))
                holder.listItem!!.getTextView().setTextColor(
                    color_context
                )
            } catch (ex: Exception) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color rdb $ex")
            }
            if (selectedItems.contains(position))
                holder.listItem!!.setDrawable(
                    ContextCompat.getDrawable(
                        ReferenceApplication.applicationContext(), R.drawable.check_focused
                    )
                )
            else holder.listItem!!.setDrawable(null)
        }

        holder.rootView?.setOnClickListener {
            animationInProgress = true
            Utils.viewClickAnimation(holder.listItem, object : AnimationListener {
                override fun onAnimationEnd() {
                    animationInProgress = false
                }
            })
            if (selectedItems.contains(holder.adapterPosition)) {
                setSelected(holder, false)
                selectedItems.remove(holder.adapterPosition)
            } else {
                setSelected(holder, true)
                selectedItems.add(holder.adapterPosition)
            }
            adapterListener!!.onItemClicked(position)
        }
        holder.rootView!!.onFocusChangeListener =
            View.OnFocusChangeListener { view, hasFocus ->
                holder.listItem!!.onFocusChange(hasFocus)
                if (hasFocus) {
                    focusedItem = holder.adapterPosition
                    holder.listItem!!.getTextView().animate().scaleY(1.06f).scaleX(1.06f).duration =
                        0
                    holder.listItem!!.background = ConfigColorManager.generateButtonBackground()
                    try {
                        val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color_context $color_context")
                        holder.listItem!!.getTextView().setTextColor(
                            color_context
                        )
                    } catch(ex: Exception) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color rdb $ex")
                    }

                    if (selectedItems.contains(holder.adapterPosition)) {
                        holder.listItem!!.setDrawable(
                            ContextCompat.getDrawable(
                                ReferenceApplication.applicationContext(),
                                R.drawable.check_focused
                            )
                        )
                        try {
                            val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color_context $color_context")
                            holder.listItem!!.getDrawable().imageTintList = ColorStateList.valueOf(color_context)

                        } catch(ex: Exception) {
                            Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color rdb $ex")
                        }
                    } else {
                        holder.listItem!!.setDrawable(null)
                    }
                } else if (!selectedItems.contains(holder.adapterPosition)) {
                    holder.listItem!!.getTextView().animate().scaleY(1f).scaleX(1f).duration = 0
                    holder.listItem!!.background =
                        ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.transparent_shape
                        )
                    holder.listItem!!.setDrawable(null)
                    holder.listItem!!.getTextView().setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

                } else {
                    holder.listItem!!.getTextView().animate().scaleY(1f).scaleX(1f).duration = 0
                    holder.listItem!!.background =
                        ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.transparent_shape
                        )

                    holder.listItem!!.setDrawable(
                        ContextCompat.getDrawable(
                            ReferenceApplication.applicationContext(),
                            R.drawable.check_unfocused
                        )
                    )
                    try {
                        val color_context = Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color_context $color_context")
                        holder.listItem!!.getDrawable().imageTintList = ColorStateList.valueOf(color_context)

                    } catch(ex: Exception) {
                        Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color rdb $ex")
                    }
                    holder.listItem!!.getTextView().setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
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


    private fun setSelected(holder: ReferenceMultiCheckListViewHolder , isSelected: Boolean) {
        if (isSelected) {
            holder.listItem!!.setDrawable(
                ContextCompat.getDrawable(
                    ReferenceApplication.applicationContext(),
                    R.drawable.check_focused
                )
            )
            try {
                val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "setSelected: Exception color_context $color_context")
                holder.listItem!!.getDrawable().imageTintList = ColorStateList.valueOf(color_context)

            } catch(ex: Exception) {
                Log.d(Constants.LogTag.CLTV_TAG + TAG, "setSelected: Exception color rdb $ex")
            }
            holder.listItem!!.getTextView().setTextColor(Color.parseColor(ConfigColorManager.getColor("color_background")))
        } else {
            holder.listItem!!.setDrawable(null)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    /**
     * Get selected items
     *
     * @return list of the selected items
     */
    fun getSelectedItems(): MutableList<String> {
        var retList = mutableListOf<String>()
        selectedItems.forEach { selectedIndex->
            retList.add(items[selectedIndex])
        }
        return retList
    }

    /**
     * Set selected items
     *
     * @param selectedItems list of the items that should be selected
     */
    fun setSelectedItems(selectedItems: ArrayList<String>) {
        this.selectedItems.clear()
        selectedItems.forEach { item->
            if (items.contains(item)) {
                this.selectedItems.add(items.indexOf(item))
            }
        }
        notifyItemRangeChanged(0, items.size)
    }

    //Refresh
    fun refresh(adapterItems: MutableList<String>) {
        this.items.clear()
        this.items.addAll(adapterItems)
        notifyDataSetChanged()
    }

    /**
     * Listener
     */
    interface ReferenceMultiCheckListAdapterListener {
        fun onItemClicked(position: Int)
        fun onKeyPressed(keyEvent: KeyEvent): Boolean
    }
}

/**
 * ViewHolder of the ReferenceMultiCheckListAdapter
 */
class ReferenceMultiCheckListViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    //Root view
    var rootView: ConstraintLayout? = null

    //List item
    var listItem: ReferenceDrawableButton? = null

    init {

        //Set references
        rootView = view.findViewById(R.id.multi_check_list_item_view)
        listItem = view.findViewById(R.id.multi_check_list_item)

        //Set root view to be focusable and clickable
        rootView!!.focusable = View.FOCUSABLE
        rootView!!.isClickable = true
        rootView!!.requestFocus()
    }
}