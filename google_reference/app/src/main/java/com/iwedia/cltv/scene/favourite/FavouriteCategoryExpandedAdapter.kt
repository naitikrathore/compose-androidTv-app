package com.iwedia.cltv.scene.favourite

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.utils.AnimationListener
import com.iwedia.cltv.utils.Utils

/**
 * Favourite channels adapter with additional buttons
 *
 * @author Aleksandar Lazic
 */
class FavouriteCategoryExpandedAdapter : RecyclerView.Adapter<FavouriteCategoryExpandedViewHolder>() {

    val TAG = javaClass.simpleName
    //Items
    private var items = mutableListOf<String>()

    //Selected item
    var selectedItem = -1

    private var selectedItemViewHolder: FavouriteCategoryExpandedViewHolder? = null
    private var focusedItemViewHolder: FavouriteCategoryExpandedViewHolder? = null

    //Keep focus flag
    var keepFocus = false
    var shoudKeepFocusOnClick = false
    var focusPosition = -1

    //are additional buttons presented
    var buttonsAreVisible = false
    var positionOfButtons  = -1

    //Adapter listener
    var adapterListener: AdapterListener? = null

    //Holders hash map
    var holders: HashMap<Int, FavouriteCategoryExpandedViewHolder> = HashMap()
    var clickAnimationInProgress = false;
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FavouriteCategoryExpandedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.favourite_category_item_expanded, parent, false)
        return FavouriteCategoryExpandedViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavouriteCategoryExpandedViewHolder, position: Int) {
        val categoryItem = items[position]
        holders[position] = holder
        if (position == selectedItem) {
            selectedItemViewHolder = holder
        }
        holder.categoryText!!.text = categoryItem
        holder.categoryText!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )

        holder.deleteButton!!.setBackgroundResource(R.drawable.ic_delete)
        holder.deleteButton!!.focusable = View.FOCUSABLE

        val selectorDrawable = ContextCompat.getDrawable(getContext(), R.drawable.reference_button_focus_shape)
        DrawableCompat.setTint(selectorDrawable!!, Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        holder.deleteButton!!.imageTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        holder.deleteButton!!.backgroundTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        holder.deleteButton!!.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                holder.deleteButton!!.setBackgroundResource(R.drawable.reference_button_focus_shape)
                holder.deleteButton!!.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_delete_focused))
                holder.deleteButton!!.imageTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_background")))
                try {
                    val color_context = Color.parseColor(ConfigColorManager.getColor("color_selector"))
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color_context $color_context")
                    holder.deleteButton!!.backgroundTintList = ColorStateList.valueOf(color_context)

                } catch(ex: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color rdb $ex")
                }
            } else {
                holder.deleteButton!!.setBackgroundResource(0)
                holder.deleteButton!!.setBackgroundResource(R.drawable.ic_delete)
                holder.deleteButton!!.imageTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
//                holder!!.deleteButton!!.backgroundTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text").replace("#",ConfigColorManager.alfa_light)))

                if (!holder.renameButton!!.hasFocus()) {
                    hideButtons(position)
                }
            }
        }

        holder.deleteButton!!.setOnClickListener { adapterListener!!.onDeleteButtonClicked(holders[positionOfButtons]!!.categoryText!!.text as String) }

        holder.deleteButton!!.setOnKeyListener(object: View.OnKeyListener {
            override fun onKey(view: View?, p1: Int, p2: KeyEvent?): Boolean {
                if (p2!!.action == KeyEvent.ACTION_DOWN) {
                    if (p1 == KeyEvent.KEYCODE_BACK) {
                        holder.categoryTextLayout!!.requestFocus()
                        return true
                    }
                }
                return false
            }
        })

        holder.renameButton!!.setBackgroundResource(R.drawable.ic_rename)
        holder.renameButton!!.focusable = View.FOCUSABLE
        holder.renameButton!!.imageTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        holder.renameButton!!.backgroundTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))

        holder.renameButton!!.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                holder.renameButton!!.setBackgroundResource(R.drawable.reference_button_focus_shape)
                holder.renameButton!!.setImageDrawable(ContextCompat.getDrawable(getContext(), R.drawable.ic_rename_focused))
                holder.renameButton!!.imageTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_background")))
                try {
                    val color_context = Color.parseColor(ConfigColorManager.getColor("color_selector"))
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color_context $color_context")
                    holder.renameButton!!.backgroundTintList = ColorStateList.valueOf(color_context)

                } catch(ex: Exception) {
                    Log.d(Constants.LogTag.CLTV_TAG + TAG, "onBindViewHolder: Exception color rdb $ex")
                }
            } else {
                holder.renameButton!!.setBackgroundResource(0)
                holder.renameButton!!.setBackgroundResource(R.drawable.ic_rename)
                holder.renameButton!!.imageTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
//                holder!!.renameButton!!.backgroundTintList = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text").replace("#",ConfigColorManager.alfa_light)))

                if (!holder.deleteButton!!.hasFocus()) {
                    hideButtons(position)
                }
            }
        }

        holder.renameButton!!.setOnClickListener { adapterListener!!.onRenameButtonClicked(holders[positionOfButtons]!!.categoryText!!.text as String) }

        holder.renameButton!!.setOnKeyListener(object: View.OnKeyListener {
            override fun onKey(view: View?, p1: Int, p2: KeyEvent?): Boolean {
                if (p2!!.action == KeyEvent.ACTION_UP) {
                    if (p1 == KeyEvent.KEYCODE_BACK) {
                        holder.categoryTextLayout!!.requestFocus()
                        return true
                    }
                }
            return false
            }
        })

        holder.renameButton!!.visibility = View.GONE
        holder.deleteButton!!.visibility = View.GONE

        clearFocus(holder)

        if (position == selectedItem) {
            setSelected(holder)
        }

        holder.categoryTextLayout!!.onFocusChangeListener =
            View.OnFocusChangeListener { view, hasFocus ->
                if (keepFocus) {
                    return@OnFocusChangeListener
                }
                if (holder.adapterPosition == -1) {
                    return@OnFocusChangeListener
                }
                if (hasFocus) {
                    holder.categoryTextLayout!!.backgroundTintList  = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_selector")))
                    focusPosition = holder.adapterPosition
                    adapterListener?.getAdapterPosition(holder.adapterPosition)
                    setFocused(holder)

                    if (focusPosition != selectedItem) {
                        if (buttonsAreVisible) {
                            hideButtons(positionOfButtons)
                        }
                    }

                    holder.categoryText!!.animate().scaleY(1.06f).scaleX(1.06f).setDuration(0)
                        .start()
                    Utils.focusAnimation(holder.categoryTextLayout!!, true)
                } else if (focusPosition != selectedItem) {
                    clearFocus(holder)
                    holder.categoryText!!.animate().scaleY(1f).scaleX(1f).setDuration(0).start()
                }
            }

        holder.categoryTextLayout!!.setOnLongClickListener {

            if (selectedItem != holder.adapterPosition) {
                selectedItem = holder.adapterPosition
                selectedItemViewHolder?.categoryTextLayout?.onFocusChangeListener?.onFocusChange(
                    selectedItemViewHolder?.categoryTextLayout,
                    false
                )
                selectedItemViewHolder = holder
                setSelected(holder)
                if (shoudKeepFocusOnClick) {
                    setFocused(holder)
                }
            }
            if (adapterListener != null) {
                adapterListener?.onItemLongClicked(holder.adapterPosition)
            }
            showButtons(position)
            positionOfButtons = position
            true
        }

        holder.categoryTextLayout!!.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(view: View?, p1: Int, p2: KeyEvent?): Boolean {
                if (!ReferenceApplication.worldHandler?.isEnableUserInteraction!! || clickAnimationInProgress) {
                    return true
                }
                keepFocus = false
                if (p2!!.action == KeyEvent.ACTION_DOWN) {
                    holder.categoryTextLayout!!.backgroundTintList  = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_selector")))
                    if (p1 == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if (adapterListener != null) {
                            return if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                                adapterListener!!.onKeyLeft(holder.adapterPosition)
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

                            if (buttonsAreVisible) {
                                hideButtons(positionOfButtons)
                            }
                            return adapterListener!!.onKeyDown(holder.adapterPosition)
                        }
                    } else if (p1 == KeyEvent.KEYCODE_DPAD_UP) {
                        if (adapterListener != null) {
                            if (buttonsAreVisible) {
                                hideButtons(positionOfButtons)
                            }
                            return adapterListener!!.onKeyUp(holder.adapterPosition)
                        }
                    }
                }

                    if (p2.action == KeyEvent.ACTION_UP) {
                        if (p1 == KeyEvent.KEYCODE_BACK || p1 == KeyEvent.KEYCODE_ESCAPE ) {
                            return adapterListener!!.onBackPressed(holder.adapterPosition)
                        }
                }
                return false
            }
        })
        holder.categoryTextLayout?.setOnClickListener {
                    if (selectedItem != holder.adapterPosition) {
                        selectedItem = holder.adapterPosition
                        selectedItemViewHolder?.categoryTextLayout?.onFocusChangeListener?.onFocusChange(
                            selectedItemViewHolder?.categoryTextLayout,
                            false
                        )
                        selectedItemViewHolder = holder
                        setSelected(holder)
                        if (shoudKeepFocusOnClick) {
                            setFocused(holder)
                        }
                    }
            clickAnimationInProgress = true
            Utils.viewClickAnimation(holder.rootView, object :
                AnimationListener {
                override fun onAnimationEnd() {
                    clickAnimationInProgress = false
                    if (adapterListener != null) {
                        adapterListener?.onItemClicked(holder.adapterPosition)
                    }
                }
            })

                }
    }


    fun setSelected(holder: FavouriteCategoryExpandedViewHolder?) {
        if (holder!!.adapterPosition == -1) {
            return
        }

        holder.categoryTextLayout!!.background =
            ContextCompat.getDrawable(
                getContext(),
                R.drawable.reference_button_selected
            )
        holder.categoryTextLayout!!.backgroundTintList  = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text").replace("#",ConfigColorManager.alfa_light)))
        holder.categoryText!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )

        holder.categoryText!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
    }

    fun clearPreviousFocus() {
        holders[selectedItem]?.let { clearFocus(it) }
    }
    fun setActiveFilter(holder: FavouriteCategoryExpandedViewHolder) {
        selectedItem = holder.adapterPosition
        selectedItemViewHolder = holder
        setSelected(holder)
    }
    fun setFocused(holder: FavouriteCategoryExpandedViewHolder?) {

        var selectorColor = ConfigColorManager.getColor("color_selector")
        var selectorDrawable = ContextCompat.getDrawable(
            getContext(),
            R.drawable.focus_shape
        )

        DrawableCompat.setTint(selectorDrawable!!, Color.parseColor(selectorColor))
        holder!!.categoryTextLayout!!.background = selectorDrawable

        holder.categoryText!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )
        Utils.focusAnimation(holder.categoryTextLayout!!, true)


        try {
            val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setFocused: Exception color_context $color_context")
            holder.categoryText!!.setTextColor(
                color_context
            )
        } catch(ex: Exception) {
            Log.d(Constants.LogTag.CLTV_TAG + TAG, "setFocused: Exception color rdb $ex")
        }

    }

    private fun clearFocus(holder: FavouriteCategoryExpandedViewHolder) {
        holder.categoryTextLayout!!.background =
            ContextCompat.getDrawable(
                ReferenceApplication.applicationContext(),
                R.drawable.transparent_shape
            )
        Utils.unFocusAnimation(holder.categoryTextLayout!!)

        holder.categoryText!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun refresh(adapterItems: MutableList<String>) {
        this.items.clear()
        this.items.addAll(adapterItems)
        notifyDataSetChanged()
    }

    private fun setDimension(dimension: Int): Int {
        return getContext().resources.getDimensionPixelSize(dimension)
    }

    private fun getContext(): Context {
        return ReferenceApplication.applicationContext()
    }

    /**
     * Update adapter items list
     */
    fun update(list: ArrayList<String>) {
        if ((!items.containsAll(list)) ||
            (list.isNotEmpty() && items.containsAll(list) && items.size != list.size)
        ) {
            notifyItemRangeRemoved(0, items.size);
            items.clear()
            items.addAll(list)
            notifyItemRangeChanged(0, items.size)
        }
    }

    fun requestFocus(position: Int) {
        if (holders.containsKey(position)) {
            if (focusedItemViewHolder != null) {
                clearFocus(focusedItemViewHolder!!)
            }
            focusedItemViewHolder = holders[position]
            setFocused(focusedItemViewHolder)
        }
    }

    fun hideButtons(position: Int){
        holders[position]!!.renameButton!!.visibility = View.GONE
        holders[position]!!.deleteButton!!.visibility = View.GONE
        buttonsAreVisible = false
    }

    fun showButtons(position: Int) {
        positionOfButtons = position
        holders[position]!!.deleteButton!!.visibility = View.VISIBLE
        holders[position]!!.renameButton!!.visibility = View.VISIBLE
        buttonsAreVisible = true
        holders[position]!!.renameButton!!.requestFocus()
        Utils.focusAnimation(holders[position]!!.renameButton!!,true)
    }

    interface AdapterListener {
        fun getAdapterPosition(position: Int)
        fun onKeyLeft(currentPosition: Int): Boolean
        fun onKeyRight(currentPosition: Int): Boolean
        fun onKeyUp(currentPosition: Int): Boolean
        fun onKeyDown(currentPosition: Int): Boolean
        fun onItemClicked(position: Int)
        fun onItemLongClicked(position: Int)
        fun onRenameButtonClicked(listName: String)
        fun onDeleteButtonClicked(listName: String)
        fun onBackPressed(position: Int): Boolean
    }
}