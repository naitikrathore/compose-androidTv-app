package com.iwedia.cltv.anoki_fast

import android.os.CountDownTimer
import android.view.*
import android.widget.LinearLayout
import com.bosphere.fadingedgelayout.FadingEdgeLayout
import com.iwedia.cltv.components.ButtonType
import com.iwedia.cltv.components.CategoryItem
import com.iwedia.cltv.components.FadeAdapter
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface

/**
 * @author Boris Tirkajla
 */
class FastCategoryAdapter(
    private val toSpeechTextSetterInterface: TTSSetterInterface
) : FadeAdapter<FastCategoryItemViewHolder>() {

    override val itemsCount: Int
        get() = items.size

    private var items = mutableListOf<CategoryItem>()

    fun init(fadingEdgeLayout: FadingEdgeLayout?){
        this.fadeAdapterType = FadeAdapterType.VERTICAL
        this.fadingEdgeLayout = fadingEdgeLayout
    }

    /**
     * [selectedItemPosition] is variable used to save index of the last focused item.
     *
     * It is not the same as the [activeItemPosition] because [activeItemPosition] is updated only
     * after timer has finished.
     */
    var selectedItemPosition = -1

    /**
     * [activeItemPosition] is variable used to save index of the last ACTIVE item.
     *
     * Item is ACTIVE only after timer has finished (when user stays long enough on one item in Grid)
     * and onItemSelected is triggered.
     */
    var activeItemPosition = 0 // initially 1 st item will be selected

    //Force focus on active item after list update
    var forceActiveItemRequest = true

    var adapterListener: ChannelListCategoryAdapterListener? = null

    var holders: HashMap<Int, FastCategoryItemViewHolder> = HashMap()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FastCategoryItemViewHolder {
        return FastCategoryItemViewHolder(
            LinearLayout(parent.context),
            toSpeechTextSetterInterface
        )
    }

    override fun onViewAttachedToWindow(holder: FastCategoryItemViewHolder) {
        if (holder.bindingAdapterPosition != selectedItemPosition)
            holder.customButton.setTransparentBackground(true)
        super.onViewAttachedToWindow(holder)
    }

    override fun onBindViewHolder(holder: FastCategoryItemViewHolder, position: Int) {
        val categoryItem = items[position]
        holders[position] = holder

        holder.customButton.setTransparentBackground(true) // crucial for setting button's background to transparent to follow design

        // used to initially set focus on activeItemPosition
        if (position == activeItemPosition && forceActiveItemRequest) {
            holder.customButton.forceFocus()
        }

        holder.customButton.update(
            buttonType = ButtonType.CUSTOM_TEXT, // this enables setting custom text in the button
            text = categoryItem.name // text that button will show
        )

        holder.customButton.setOnFocusChanged {hasFocus ->
            if (!hasFocus) return@setOnFocusChanged // Skip further execution if focus is not on the button.
            selectedItemPosition = position
            onItemSelected(position)
        }

        holder.customButton.setOnKeyListener { _, keyCode, keyEvent ->

            if (holder.customButton.isAnimationInProgress) return@setOnKeyListener true // when user clicks on the button animation is triggered, until it's finished keys are disabled.

            if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    return@setOnKeyListener true // disabling DPAD_LEFT
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (selectedItemTimer!=null) return@setOnKeyListener true // prevent right click when user changing filter
                    if (!adapterListener!!.isDataLoading()) {
                        holder.customButton.keepFocus()
                    }
                    adapterListener?.onKeyRight()
                    return@setOnKeyListener true
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    if (position == 0) {
                        holder.customButton.keepFocus()
                    }
                    adapterListener?.onKeyUp(isFromFirstItem = position == 0)
                }
            }

            if (keyEvent.action == KeyEvent.ACTION_UP) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    holder.customButton.keepFocus()
                    adapterListener?.onBackPressed(position)
                    return@setOnKeyListener true
                }
            }
            return@setOnKeyListener false
        }

        holder.customButton.setOnClick {
            holder.customButton.keepFocus()
            activeItemPosition = position
            adapterListener?.onItemClicked(position)
        }

    }

    // TODO BORIS LAST ONE
    fun setActiveFilter(holder: FastCategoryItemViewHolder) {
        selectedItemPosition = holder.adapterPosition
//        selectedItemViewHolder = holder
    }

    fun getSelectedItem():String{
        return items[selectedItemPosition].name
    }

    /**
     * Changes the selected Category based on the specified direction.
     *
     * @param direction The direction of the change (UP or DOWN).
     */
    fun changeCategory(direction: Direction, onTimerFinished: () -> Unit) {
        when (direction) {
            Direction.UP -> {
                // Check if the user is at the first Channel Filter (no filters before this one).
                if (selectedItemPosition == 0) return
            }
            Direction.DOWN -> {
                // Check if the user is at the last Channel Filter (no filters after this one).
                if (selectedItemPosition == itemCount-1) return
            }
        }
        // Additional code executed for all cases after category change.
        holders[selectedItemPosition]?.customButton?.releaseFocus()

        when (direction) {
            Direction.UP -> {
                selectedItemPosition --
            }
            Direction.DOWN -> {
                selectedItemPosition ++
            }
        }

        onItemSelected(position = selectedItemPosition, isDelayEnabled = false, afterItemSelected = onTimerFinished)
        holders[selectedItemPosition]?.customButton?.forceFocus()

    }

    private fun stopItemSelectedTimer() {
        selectedItemTimer?.cancel()
        selectedItemTimer = null
    }
    /**
     * [triggerItemSelectedTimer] triggers adapterListener?.onItemSelected(position) with 500ms delay
     * Added in order to avoid multiple selected item details info refreshing during fast scrolling
     */
    private var selectedItemTimer: CountDownTimer?= null

    private fun onItemSelected(position: Int, isDelayEnabled: Boolean = true, afterItemSelected: () -> Unit = {}) {

        stopItemSelectedTimer()

        if (isDelayEnabled) {
            selectedItemTimer = object :
                CountDownTimer(
                    500,
                    500
                ) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    selectedItemTimer= null
                    if (position != activeItemPosition) { // if user hasn't selected the same item which is already active
                        activeItemPosition = position // update active item
                        adapterListener?.onItemSelected(position,afterItemSelected)
                    }
                }
            }
            selectedItemTimer!!.start()
        } else if (position != activeItemPosition){
            activeItemPosition = position // update active item
            adapterListener?.onItemSelected(position,afterItemSelected)
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun refresh(adapterItems: MutableList<CategoryItem>) {
        this.items.clear()
        this.items.addAll(adapterItems)
        notifyDataSetChanged()
    }

    /**
     * Set selected item by position
     *
     * @param position item position inside the list
     */
    fun setSelected(position: Int) {
        if (holders.containsKey(position)) {
            selectedItemPosition = position
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

    interface ChannelListCategoryAdapterListener: TTSSetterInterface {
        fun onKeyLeft(currentPosition: Int): Boolean
        fun onKeyRight()
        /**
         * [onKeyUp] the "up" key press event based on the specified condition.
         *
         * @param isFromFirstItem Indicates whether the event originates from the first item in the adapter.
         * If true, it performs actions specific to the first item, such as requesting the top menu focus.
         * If false, it performs generic actions that apply to items other than the first one.
         */
        fun onKeyUp(isFromFirstItem: Boolean)
        fun onItemClicked(position: Int)

        /**
         * [onBackPressed] is used to handle pressing back from the Category Item.
         * @param position can be saved in order to know from which position user pressed Back button.
         */
        fun onBackPressed(position: Int)
        fun onItemSelected(position: Int, onRefreshFinished: () -> Unit = {})
        fun isDataLoading(): Boolean
    }

    enum class Direction {
        UP, DOWN
    }
}