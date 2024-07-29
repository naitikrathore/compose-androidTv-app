package com.iwedia.cltv.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import android.view.*
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.bosphere.fadingedgelayout.FadingEdgeLayout
import com.iwedia.cltv.*
import com.iwedia.cltv.ReferenceApplication.Companion.downActionBackKeyDone
import com.iwedia.cltv.config.ConfigColorManager
import com.iwedia.cltv.config.ConfigFontManager
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.Constants
import com.iwedia.cltv.utils.AnimationListener
import com.iwedia.cltv.utils.Utils
import com.iwedia.cltv.utils.Utils.Companion.checkIfDigit

class CategoryAdapter(private val isRefPrefWidget : Boolean = false, fadingEdgeLayout: FadingEdgeLayout? = null) : FadeAdapter<CategoryItemViewHolder>(fadingEdgeLayout, FadeAdapterType.HORIZONTAL) {

    // this variable is important for FadeAdapter.
    override var itemsCount: Int
        get() = items.size
        set(value) {}

    val TAG = javaClass.simpleName
    //Items
    private var items = mutableListOf<CategoryItem>()

    //Selected item
    var selectedItem = -1

    private var selectedItemViewHolder: CategoryItemViewHolder? = null
    private var focusedItemViewHolder: CategoryItemViewHolder? = null
    var clickAnimationProgress = false

    //Keep focus flag
    var keepFocus = false
    var shoudKeepFocusOnClick = false
    var focusPosition = -1
    var selectedItemEnabled = true

    //Adapter listener
    var adapterListener: ChannelListCategoryAdapterListener? = null

    //Holders hash map
    var holders: HashMap<Int, CategoryItemViewHolder> = HashMap()
    var isCenterAligned = false

    var isCategoryFocus = false


    lateinit var recyclerView: RecyclerView

    override fun onAttachedToRecyclerView(RecyclerView: RecyclerView) {
        this.recyclerView = RecyclerView
        super.onAttachedToRecyclerView(RecyclerView)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CategoryItemViewHolder {

        if(isCenterAligned){


            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.category_item_centered, parent, false)
            return CategoryItemViewHolder(view)

        }
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.category_item, parent, false)

        val channelCategoryItemCategoryText: TextView = view.findViewById(R.id.channel_category_item_category_text)
        channelCategoryItemCategoryText.text = ConfigStringsManager.getStringById("encrypted")

        return CategoryItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryItemViewHolder, position: Int) {
        val categoryItem = items[position]
        holders[position] = holder
        if (position == selectedItem) {
            selectedItemViewHolder = holder
        }

        holder.categoryText!!.text = categoryItem.name

        holder.categoryText!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )

        clearFocus(holder)

        if (position == selectedItem) {
            setSelected(holder)
        }

        holder.rootView!!.onFocusChangeListener =
            View.OnFocusChangeListener { view, hasFocus ->
                if (keepFocus) {
                    return@OnFocusChangeListener
                }
                if (hasFocus) {
                    adapterListener?.setSpeechText(holder.getSpeechText())
                    if(adapterListener != null)
                        adapterListener?.getAdapterPosition(position)
                    triggerItemSelected(position)
                    focusPosition = holder.absoluteAdapterPosition
                    setFocused(holder)

                    holder.categoryText!!.animate().scaleY(1.06f).scaleX(1.06f).setDuration(0)
                        .start()
                }
                else {
                    Utils.unFocusAnimation(holder.rootView!!)
                    if(!isCategoryFocus) {
                        if (selectedItem != holder.absoluteAdapterPosition) {
                            clearFocus(holder)

                            holder.categoryText!!.animate().scaleY(1f).scaleX(1f).setDuration(0)
                                .start()

                        } else {
                            setSelected(holder)
                        }
                    }
                    else{
                        if (focusPosition != selectedItem) {
                            clearFocus(holder)

                            holder.categoryText!!.animate().scaleY(1f).scaleX(1f).setDuration(0)
                                .start()
                        }
                    }
                }

            }

        holder.rootView!!.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(view: View?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
                if (!ReferenceApplication.worldHandler?.isEnableUserInteraction!! || clickAnimationProgress) {
                    return true
                }
                keepFocus = false
                if (keyEvent!!.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_BACK ){
                        downActionBackKeyDone = true
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        if (adapterListener != null) {
                            return if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                                adapterListener!!.onKeyLeft(holder.absoluteAdapterPosition)
                            } else {
                                adapterListener!!.onKeyRight(holder.absoluteAdapterPosition)
                            }
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        if (adapterListener != null) {
                            return if (ViewCompat.getLayoutDirection(view!!) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                                if (holder.absoluteAdapterPosition == itemsCount - 1) {
                                    setSelected(holder.absoluteAdapterPosition)
                                }
                                adapterListener!!.onKeyRight(holder.absoluteAdapterPosition)
                            } else {
                                adapterListener!!.onKeyLeft(holder.absoluteAdapterPosition)
                            }
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if (adapterListener != null) {
                            return adapterListener!!.onKeyDown(holder.absoluteAdapterPosition)
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        if (adapterListener != null) {
                            return adapterListener!!.onKeyUp(holder.absoluteAdapterPosition)
                        }
                    }else if (checkIfDigit(keyCode)){
                            if(adapterListener!=null){
                                val digit = if (keyEvent.keyCode<KeyEvent.KEYCODE_NUMPAD_0){
                                    keyEvent.keyCode - KeyEvent.KEYCODE_0
                                }else{
                                    keyEvent.keyCode - KeyEvent.KEYCODE_NUMPAD_0
                                }
                                adapterListener!!.digitPressed(digit)
                                return true
                            }
                        }
                }

                if (keyEvent.action == KeyEvent.ACTION_UP) {
                    if (keyCode == KeyEvent.KEYCODE_BACK && downActionBackKeyDone) {
                        return adapterListener!!.onBackPressed(holder.absoluteAdapterPosition)
                    }
                }
                return false
            }
        })
        holder.rootView?.setOnClickListener {

            clickAnimationProgress = true
            Utils.viewClickAnimation(holder.rootView!!, object : AnimationListener {
                override fun onAnimationEnd() {
                    clickAnimationProgress = false
                    if (selectedItem != holder.absoluteAdapterPosition && selectedItemEnabled) {
                        selectedItem = holder.absoluteAdapterPosition
                        selectedItemViewHolder?.rootView?.onFocusChangeListener?.onFocusChange(
                            selectedItemViewHolder?.rootView,
                            false
                        )
                        selectedItemViewHolder = holder
                        setSelected(holder)
                        if (shoudKeepFocusOnClick) {
                            setFocused(holder)
                        }
                    }
                    if (adapterListener != null) {
                        adapterListener?.onItemClicked(holder.layoutPosition)
                    }
                    holder.rootView?.clearAnimation()
                }
            })
        }
    }



    fun clearPreviousFocus() {
        clearFocus(selectedItem)
    }


    fun setPreviousFocus(holder: CategoryItemViewHolder) {
        setSelected(holder)
    }

    fun setActiveFilter(holder: CategoryItemViewHolder) {
        selectedItem = holder.absoluteAdapterPosition
        selectedItemViewHolder = holder
        setSelected(holder)
    }

    fun getSelectedItem():String{
        return  items[selectedItem].name
    }

    fun setSelected(holder: CategoryItemViewHolder?) {

        if (holder!!.absoluteAdapterPosition == -1 || !selectedItemEnabled) {
            return
        }
        holder!!.rootView!!.background =
            ContextCompat.getDrawable(
                getContext(),
                R.drawable.reference_button_selected
            )
        //todo color
        holder!!.rootView!!.backgroundTintList  = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text").replace("#",ConfigColorManager.alfa_light)))
        holder.categoryText!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )

        holder.categoryText!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
    }

    @SuppressLint("ResourceType")
    fun setFocused(holder: CategoryItemViewHolder?) {
        Utils.focusAnimation(holder!!.itemView,isRefPrefWidget)
        var selectorColor = ConfigColorManager.getColor("color_selector")
        var selectorDrawable = ContextCompat.getDrawable(
            getContext(),
            R.drawable.focus_shape
        )

        DrawableCompat.setTint(selectorDrawable!!, Color.parseColor(selectorColor))
        holder!!.rootView!!.background = selectorDrawable
        holder!!.rootView!!.backgroundTintList  = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_selector")))

        holder.categoryText!!.typeface =
            TypeFaceProvider.getTypeFace(
                ReferenceApplication.applicationContext(),
                ConfigFontManager.getFont("font_regular")
            )
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

    /**
     * Item selected count down timer
     * Triggers getAdapterPosition listener with 500ms delay
     * Added in order to avoid multiple selected item details info refreshing during fast scrolling
     */
    private var itemSelectedTimer: CountDownTimer?= null
    var isItemSelectedTimerInProgress = false
    private fun triggerItemSelected(position: Int) {
        isItemSelectedTimerInProgress = true
        itemSelectedTimer?.cancel()
        itemSelectedTimer = null
        itemSelectedTimer = object :
            CountDownTimer(
                500,
                500
            ) {
            override fun onTick(millisUntilFinished: Long) {}
            @RequiresApi(Build.VERSION_CODES.R)
            override fun onFinish() {
                if (::recyclerView.isInitialized) {
                    if(!recyclerView.hasFocus()){
                        itemSelectedTimer?.cancel() // Cancelling timers when detached
                        itemSelectedTimer = null
                        return
                    }
                    isItemSelectedTimerInProgress = false
                }
                itemSelectedTimer = null
                adapterListener?.onItemSelected(position)
            }
        }
        itemSelectedTimer!!.start()
    }

    fun isWaiting(): Boolean {
        return itemSelectedTimer!=null
    }

    private fun clearFocus(holder: CategoryItemViewHolder) {
        Utils.unFocusAnimation(holder.itemView)

        if(isCenterAligned){
            var selectorColor = ConfigColorManager.getColor("color_background")
            var selectorDrawable = ContextCompat.getDrawable(
                getContext(),
                R.drawable.focus_shape
            )

            DrawableCompat.setTint(selectorDrawable!!, Color.parseColor(selectorColor))
            holder.rootView!!.background = selectorDrawable
            holder.rootView!!.backgroundTintList  = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_not_selected")))
        }else{
            holder.rootView!!.background =
                ContextCompat.getDrawable(
                    ReferenceApplication.applicationContext(),
                    R.drawable.transparent_shape
                )
        }
        holder.categoryText!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun refresh(adapterItems: MutableList<CategoryItem>) {
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
        if (holders.containsKey(position)) {
            selectedItem = position
            holders[position]!!.rootView!!.background =
                ContextCompat.getDrawable(
                    getContext(),
                    R.drawable.reference_button_selected
                )
            holders[position]!!.rootView!!.backgroundTintList  = ColorStateList.valueOf(Color.parseColor(ConfigColorManager.getColor("color_main_text").replace("#",ConfigColorManager.alfa_light)))

            holders[position]!!.categoryText!!.typeface =
                TypeFaceProvider.getTypeFace(
                    ReferenceApplication.applicationContext(),
                    ConfigFontManager.getFont("font_regular")
                )

            holders[position]!!.categoryText!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        }
    }

    /**
     * Clear focus on item by position
     *
     * @param position item position inside the list
     */
    fun clearFocus(position: Int) {
        if (holders.containsKey(position)) {
            holders[position]!!.rootView!!.background =
                ContextCompat.getDrawable(
                    ReferenceApplication.applicationContext(),
                    R.drawable.transparent_shape
                )

            holders[position]!!.categoryText!!.setTextColor(Color.parseColor(ConfigColorManager.getColor("color_main_text")))
        }
    }

    /**
     * Update adapter items list
     */
    fun update(list: MutableList<CategoryItem>) {
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
        if (holders.containsKey(position)) {
            if (focusedItemViewHolder != null) {
                clearFocus(focusedItemViewHolder!!)
            }
            focusedItemViewHolder = holders[position]
            setFocused(focusedItemViewHolder)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        itemSelectedTimer?.cancel() // Cancelling timers when detached
        itemSelectedTimer = null
    }
    
    fun dispose() {
        adapterListener = null
        selectedItemViewHolder = null
        focusedItemViewHolder = null

        itemSelectedTimer?.cancel()
        itemSelectedTimer = null

        if (items.isNotEmpty()) items.clear()
        if (holders.isNotEmpty()) holders.clear()
    }

    interface ChannelListCategoryAdapterListener: TTSSetterInterface {
        fun getAdapterPosition(position: Int)
        fun onKeyLeft(currentPosition: Int): Boolean
        fun onKeyRight(currentPosition: Int): Boolean
        fun onKeyUp(currentPosition: Int): Boolean
        fun onKeyDown(currentPosition: Int): Boolean
        fun onItemClicked(position: Int)
        fun onBackPressed(position: Int) :Boolean
        fun onItemSelected(position: Int)
        fun digitPressed(digit:Int)
    }
}