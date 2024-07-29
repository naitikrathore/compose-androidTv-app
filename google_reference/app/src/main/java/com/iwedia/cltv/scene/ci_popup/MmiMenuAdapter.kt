package com.iwedia.cltv.components

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.text.TextUtils
import android.view.*
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.TypeFaceProvider
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager

class MmiMenuAdapter : RecyclerView.Adapter<PreferenceMmiMenuItemViewHolder>() {

    var isCategoryFocus= false

    //Items
    private var items = mutableListOf<PreferenceCategoryItem>()

    //Selected item
    var selectedItem = -1

    private var selectedItemViewHolder: PreferenceMmiMenuItemViewHolder? = null
    private var focusedItemViewHolder: PreferenceMmiMenuItemViewHolder? = null

    //Keep focus flag
    var keepFocus = false
    var shoudKeepFocusOnClick = false
    var focusPosition = -1
    var selectedItemEnabled = true

    //Adapter listener
    var adapterListener: PrefrenceSubcategoryAdapterListener? = null

    //Holders hash map
    var holders: HashMap<Int, PreferenceMmiMenuItemViewHolder> = HashMap()


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PreferenceMmiMenuItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.preference_mmi_menu_item, parent, false)
        return PreferenceMmiMenuItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: PreferenceMmiMenuItemViewHolder, position: Int) {
        val categoryItem = items[position]

        holder.categoryText1!!.text = categoryItem.name

        if(categoryItem.showArrow){
            holder.arrowRight!!.visibility = View.VISIBLE
        }


        holder.categoryText1!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
            )

        setBackground(holder)
        holders[position] = holder
        if (position == selectedItem) {
            println("MMiM2 selectedItem $selectedItem")
            selectedItemViewHolder = holder
            setActiveFilter(holder)

        }

        holder.rootView!!.onFocusChangeListener =
            View.OnFocusChangeListener { view, hasFocus ->
                holder.categoryText1!!.setEllipsize(if(hasFocus)TextUtils.TruncateAt.MARQUEE else TextUtils.TruncateAt.END);

                println("MMiM2 holder.rootView!!.onFocusChangeListener selectedItem $selectedItem")

                if(hasFocus) holder.categoryText1!!.setSingleLine(true);

                if(hasFocus) holder.categoryText1!!.setSelected(true);

//                if (keepFocus) {
//                    return@OnFocusChangeListener
//                }
                if (hasFocus) {
                    adapterListener?.getAdapterPosition(position)
                    focusPosition = holder.adapterPosition
                    setFocused(holder)

                    holder.categoryText1!!.animate().scaleY(1.06f).scaleX(1.06f).setDuration(0)
                        .start()
                    holder.arrowRight!!.animate().scaleY(1.06f).scaleX(1.06f).setDuration(0)
                        .start()


                }else{
                    if(isCategoryFocus) {
                        if (selectedItem != holder.adapterPosition) {
                            setBackground(holder)
                            holder.categoryText1!!.animate().scaleY(1f).scaleX(1f).setDuration(0).start()
                            holder.arrowRight!!.animate().scaleY(1f).scaleX(1f).setDuration(0).start()

                        } else {
                            setActiveFilter(holder)
                        }
                    }
                    else{
                        if (focusPosition != selectedItem) {
                            setBackground(holder)
                            holder.categoryText1!!.animate().scaleY(1f).scaleX(1f).setDuration(0).start()
                            holder.arrowRight!!.animate().scaleY(1f).scaleX(1f).setDuration(0).start()
                        }
                    }

                }

            }

        holder.rootView!!.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(view: View?, p1: Int, p2: KeyEvent?): Boolean {
                if (!ReferenceApplication.worldHandler?.isEnableUserInteraction!!) {
                    return true
                }
                keepFocus = false
                if (p2!!.action == KeyEvent.ACTION_DOWN) {
                    if (p1 == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if (adapterListener != null) {
                            return if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                                adapterListener!!.onKeyLeft(holder.adapterPosition)
                            } else {
                                adapterListener!!.onKeyRight(holder.adapterPosition)
                            }
                        }
                    } else if (p1 == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        selectedItem = holder.adapterPosition
                        if (adapterListener != null) {
                            return if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                                adapterListener!!.onKeyRight(holder.adapterPosition)
                            } else {
                                adapterListener!!.onKeyLeft(holder.adapterPosition)
                            }
                        }
                    } else if (p1 == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if (adapterListener != null) {
                            selectedItem=-1
                            return adapterListener!!.onKeyDown(holder.adapterPosition)

                        }

                    } else if (p1 == KeyEvent.KEYCODE_DPAD_UP) {
                        if (adapterListener != null) {
                            selectedItem=-1
                            return adapterListener!!.onKeyUp(holder.adapterPosition)
                        }
                    }
                }

                if (p2.action == KeyEvent.ACTION_UP) {
                    if (p1 == KeyEvent.KEYCODE_BACK) {
                        selectedItem = holder.adapterPosition
                        if(adapterListener != null) {
                            return adapterListener!!.onBackPressed(holder.adapterPosition)
                        }else{
                            return false
                        }
                    }
                }
                return false
            }
        })
        holder.rootView?.setOnClickListener {
            if (adapterListener != null) {
                selectedItem = holder.adapterPosition
                adapterListener?.onItemClicked(holder.adapterPosition)
            }
        }
    }

    fun setActiveFilter(holder: PreferenceMmiMenuItemViewHolder) {
        println("MMiM3 setActiveFilter holder.adapterPosition ${holder.adapterPosition}")
        selectedItem = holder.adapterPosition
        selectedItemViewHolder = holder
        setSelected(holder)
    }

    fun setSelected(holder: PreferenceMmiMenuItemViewHolder?) {
        println("MMiM3 setSelected holder.adapterPosition ${holder?.adapterPosition}")
        if (holder!!.adapterPosition == -1 || !selectedItemEnabled) {
            return
        }
        holder!!.rootView!!.background =
            ContextCompat.getDrawable(
                getContext(),
                R.drawable.preference_subcategory_selected
            )

        holder.categoryText1!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
            )

        holder.categoryText1!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        holder!!.arrowRight!!.setColorFilter(
            Color.parseColor(ConfigColorManager.getColor("color_main_text"))
        )
        val gdseB = GradientDrawable()
        gdseB.setShape(GradientDrawable.RECTANGLE)
        gdseB.setColor(Color.parseColor((ConfigColorManager.getColor("color_main_text")).replace("#",ConfigColorManager.alfa_light)))//todo
        gdseB.cornerRadius = 17f
        holder!!.rootView!!.background = gdseB

    }

    fun setFocused(holder: PreferenceMmiMenuItemViewHolder?) {
        println("MMiM3 setFocused holder.adapterPosition ${holder?.adapterPosition}")
        val gdseB = GradientDrawable()
        gdseB.setShape(GradientDrawable.RECTANGLE)
        gdseB.setColor(Color.parseColor((ConfigColorManager.getColor("color_selector"))))//todo
        gdseB.cornerRadius = 17f
        holder!!.rootView!!.background = gdseB

        holder.categoryText1!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_medium")
            )

        try {
            val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
            println("Exception color_context ${color_context}")
            holder.categoryText1!!.setTextColor(
                color_context
            )
        } catch(ex: Exception) {
            println("Exception color rdb $ex")
        }

        try {
            val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
            println("Exception color_context ${color_context}")
        } catch(ex: Exception) {
            println("Exception color rdb $ex")
        }

        try {
            val color_context = Color.parseColor(ConfigColorManager.getColor("color_background"))
            println("Exception color_context ${color_context}")
            holder.arrowRight!!.setColorFilter(
                color_context
            )
        } catch(ex: Exception) {
            println("Exception color rdb $ex")
        }
    }

    private fun setBackground(holder: PreferenceMmiMenuItemViewHolder) {

        holder.categoryText1!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        holder.arrowRight!!.setColorFilter(
            Color.parseColor(ConfigColorManager.getColor("color_main_text"))
        )

        val drawableChannelLogo = GradientDrawable()
        drawableChannelLogo.setShape(GradientDrawable.RECTANGLE)
        drawableChannelLogo.setCornerRadius(17F)
        val colorStartLight = Color.parseColor(ConfigColorManager.getColor("color_main_text").replace("#",ConfigColorManager.alfa_ten_per))
        drawableChannelLogo.setColors(
            intArrayOf(
                colorStartLight,
                colorStartLight,
                colorStartLight
            )
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            holder.rootView!!.setBackground(drawableChannelLogo)
        } else{
            holder.rootView!!.setBackgroundDrawable(drawableChannelLogo)
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun refresh(adapterItems: MutableList<PreferenceCategoryItem>) {
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
     * Set selected item by position
     *
     * @param position item position inside the list
     */
    fun setSelected(position: Int) {
        println("MMiM3 setSelected")
        if (holders.containsKey(position)) {
            println("MMiM3 setSelected containsKey")
            selectedItem = position

            holders[position]!!.categoryText1!!.typeface =
                TypeFaceProvider.getTypeFace(
                    ReferenceApplication.applicationContext(),
                    ConfigFontManager.getFont("font_medium")
                )

            holders[position]!!.categoryText1!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
            holders[position]!!.arrowRight!!.setColorFilter(
                Color.parseColor(ConfigColorManager.getColor("color_main_text"))
            )

            val drawableChannelLogo1 = GradientDrawable()
            drawableChannelLogo1.setShape(GradientDrawable.RECTANGLE)
            drawableChannelLogo1.setCornerRadius(17F)
            val colorStartLight = Color.parseColor(ConfigColorManager.getColor("color_main_text").replace("#",ConfigColorManager.alfa_ten_per))
            drawableChannelLogo1.setColors(
                intArrayOf(
                    colorStartLight,
                    colorStartLight,
                    colorStartLight
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                holders[position]!!.rootView!!.setBackground(drawableChannelLogo1)
            } else{
                holders[position]!!.rootView!!.setBackgroundDrawable(drawableChannelLogo1)
            }
        }
    }

    /**
     * sets the background color of the item
     */
    fun setBackground(position: Int) {
        if (holders.containsKey(position)) {

            holders[position]!!.categoryText1!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
            holders[position]!!.arrowRight!!.setColorFilter(
                Color.parseColor(ConfigColorManager.getColor("color_main_text"))
            )

            val drawableChannelLogo3 = GradientDrawable()
            drawableChannelLogo3.setShape(GradientDrawable.RECTANGLE)
            drawableChannelLogo3.setCornerRadius(17F)
            val colorStartLight = Color.parseColor(ConfigColorManager.getColor("color_main_text").replace("#",ConfigColorManager.alfa_ten_per))
            drawableChannelLogo3.setColors(
                intArrayOf(
                    colorStartLight,
                    colorStartLight,
                    colorStartLight
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                holders[position]!!.rootView!!.setBackground(drawableChannelLogo3)
            } else{
                holders[position]!!.rootView!!.setBackgroundDrawable(drawableChannelLogo3)
            }
        }
    }

    /**
     * Update adapter items list
     */
    fun update(list: MutableList<PreferenceCategoryItem>) {
        if ((!items.containsAll(list)) ||
            (list.isNotEmpty() && items.containsAll(list) && items.size != list.size)
        ) {
            var difference =  list.size - items.size

            if(difference!=0){
                //new added
                if(difference>0){
                    notifyItemRangeInserted(items.size,Math.abs(difference))
                }
                //removed
                if(difference<0){
                    notifyItemRangeRemoved(list.size,Math.abs(difference))
                }
            }

            if(list.size!=0) {
                //changed
                notifyItemRangeChanged(0, list.size)
            }

            items.clear()
            items.addAll(list)
        }
    }

    fun requestFocus(position: Int) {
        println("MMiM3 requestFocus")
        if (holders.containsKey(position)) {
            if (focusedItemViewHolder != null) {
                setBackground(focusedItemViewHolder!!)
            }
            focusedItemViewHolder = holders[position]
            setFocused(focusedItemViewHolder)
        }
    }

    fun clearFocus(position: Int) {
        println("MMiM3 clearFocus")
        if (holders.containsKey(position)) {
            focusedItemViewHolder = holders[position]
            val gdseB = GradientDrawable()
            gdseB.setShape(GradientDrawable.RECTANGLE)
            gdseB.setColor(Color.parseColor((ConfigColorManager.getColor("color_main_text").replace("#",ConfigColorManager.alfa_ten_per))))//todo
            gdseB.cornerRadius = 17f
            focusedItemViewHolder!!.rootView!!.background = gdseB
            try {
                val color_context = Color.parseColor(ConfigColorManager.getColor("color_main_text"))
                println("Exception color_context ${color_context}")
                focusedItemViewHolder!!.categoryText1!!.setTextColor(
                    color_context
                )
            } catch(ex: Exception) {
                println("Exception color rdb $ex")
            }
        }
    }
    interface PreferenceListener {
        fun onUpdate(value: String)
    }

    interface PrefrenceSubcategoryAdapterListener {
        fun getAdapterPosition(position: Int)
        fun onKeyLeft(currentPosition: Int): Boolean
        fun onKeyRight(currentPosition: Int): Boolean
        fun onKeyUp(currentPosition: Int): Boolean
        fun onKeyDown(currentPosition: Int): Boolean
        fun onItemClicked(position: Int)
        fun onBackPressed(position: Int):Boolean
    }
}