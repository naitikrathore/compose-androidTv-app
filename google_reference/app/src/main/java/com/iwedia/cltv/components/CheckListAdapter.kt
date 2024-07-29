package com.iwedia.cltv.components

import android.annotation.SuppressLint
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.bosphere.fadingedgelayout.FadingEdgeLayout
import com.iwedia.cltv.platform.`interface`.TTSSetterForSelectableViewInterface
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.ReferenceApplication
import com.iwedia.cltv.ReferenceApplication.Companion.downActionBackKeyDone


class CheckListAdapter(
    fadingEdgeLayout: FadingEdgeLayout,
    fadeAdapterType: FadeAdapterType,
    preventClip : Boolean? = false
): FadeAdapter<HorizontalButtonsAdapterViewHolder>(
    fadingEdgeLayout = fadingEdgeLayout,
    fadeAdapterType = fadeAdapterType
) {

    /**
     * itemsCount is abstract variable from FadeAdapter that needs to be implemented here. If needed look into the FadeAdapter's documentation.
     */
    override val itemsCount: Int
        get() = items.size

    /**
     * item that is sometime used and if used, it is displayed as the last one in VerticalGridView. It is used to send callback to Widget/Scene that user has pressed "Off" button.
     */
    private lateinit var offCheckListItem: CheckListItem

    /**
     * this variable is important for knowing how many "standard" items Adapter contains. Standard item is evey item passed through adapter's refresh method without "Off" button.
     */
    private var numberOfItemsWithoutOffItem: Int = -1

    private var items = mutableListOf<CheckListItem>()

    /**
     * selectedItemViewHolder is important when unchecking the last checked item.
     */
    private var checkedItemViewHolder: HorizontalButtonsAdapterViewHolder? = null

    var adapterListener: CheckListAdapterListener? = null
    private var preventClip : Boolean = preventClip ?: false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HorizontalButtonsAdapterViewHolder {
        return HorizontalButtonsAdapterViewHolder(
            ConstraintLayout(parent.context),
            preventClip = preventClip,
            ttsSetterInterface = adapterListener!!
        )
    }

    override fun onBindViewHolder(holder: HorizontalButtonsAdapterViewHolder, position: Int) {
        val name = items[position].name
        val isChecked = items[position].isChecked

        //items are cutting at the edges, so giving padding prevent them
        if (preventClip) holder.customButton.setPadding(10, 10, 10, 10)

        holder.customButton.update(ButtonType.CHECK, name) // used to initialise Button

        if (isChecked) {
            holder.customButton.switchCheckedState() // this is used to initially set check on button that should be checked (by default all buttons are unchecked)
            checkedItemViewHolder = holder // remember checked button in order to be able to uncheck it later in onClick method.
        }

        //check data property to add additional image resources to the button
        // sometimes buttons have Dolby, AD or HOH indication.
        items[position].data?.let {
            holder.customButton.addIcons(it)
        }

        holder.customButton.setTransparentBackground(true)

        holder.customButton.setOnClick {
            // if user clicks the button that has already been checked nothing should happen.
            if (checkedItemViewHolder == holder) return@setOnClick

            // if button is one of the "standard" buttons (this doesn't stand for "Off" button added at the end in some cases as in Subtitle list in Info Banner and Details scene)
            if (position < numberOfItemsWithoutOffItem) {
                adapterListener!!.onItemClicked(position)
            } else { // this is "Off" button - sometimes adapter doesn't contain "Off" button and then this else clause doesn't do anything.
                adapterListener!!.onAdditionalItemClicked()
            }

            // code common for both "standard" items and "off" button (if there is "off" button)
            checkedItemViewHolder?.customButton?.switchCheckedState() // switch state of the last checked item
            holder.customButton.switchCheckedState() // switch state of the clicked button
            checkedItemViewHolder = holder // update reference to the last checked/clicked item
        }

        holder.customButton.textToSpeechHandler.setupTextToSpeechTextSetterInterface(adapterListener!!)
        holder.customButton.textToSpeechHandler.setupTTSSetterForSelectableViewInterface(adapterListener!!)

        holder.customButton.setOnKeyListener { _, keyCode, keyEvent ->
            if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_DPAD_LEFT -> {
                        return@setOnKeyListener true // user is not allowed to clicks right and left while in CheckListAdapter.
                    }

                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (adapterListener!!.onUpPressed(position)) {
                            return@setOnKeyListener true
                        }
                    }

                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (adapterListener!!.onDownPressed(position)) {
                            return@setOnKeyListener true
                        }
                    }
                    KeyEvent.KEYCODE_BACK -> {
                        downActionBackKeyDone = true
                        return@setOnKeyListener true
                    }
                }
            } else if (keyEvent.action == KeyEvent.ACTION_UP) {
                if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                    if (!downActionBackKeyDone) return@setOnKeyListener true
                    return@setOnKeyListener adapterListener?.onBackPressed() ?: false
                }
            }
            return@setOnKeyListener false
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    /**
     * this is standard refresh method when only items that are passed as parameter should be displayed in Adapter.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun refresh(adapterItems: MutableList<CheckListItem>) {
        items.clear()
        items.addAll(adapterItems)
        numberOfItemsWithoutOffItem = itemsCount // must be set here in
        notifyDataSetChanged()
    }

    /**
     * this is refresh method used to add "Off" button on the bottom of the Adapter's list.
     * VerticalGrid will display all adapterItems passed as parameter, and than "Off" button will be added.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun refreshWithAdditionalItem(adapterItems: MutableList<CheckListItem>, name: String, isChecked: Boolean) {
        items.clear()
        items.addAll(adapterItems)
        numberOfItemsWithoutOffItem = itemsCount

        offCheckListItem = CheckListItem(
            name = name,
            isChecked = isChecked
        )

        items.add(offCheckListItem) // insert "Off" button at the end of the list
        notifyDataSetChanged()
    }

    interface CheckListAdapterListener: TTSSetterInterface, TTSSetterForSelectableViewInterface {
        /**
         * called when user clicks to the "standard" items passed as parameter in refresh() method. This method WON'T be called when "Off" button is pressed.
         */
        fun onItemClicked(position: Int)

        /**
         * This method is called if list of items inside CheckListAdapter contains "Off" button.
         *
         * For example, in Audio or Subtitle VerticalGridView (Info banner or Details scene) last item is "Off" button and it's used for disabling tracks.
         */
        fun onAdditionalItemClicked()

        /**
         * called when user presses back button while focus is on one of the Adapter's item.
         */
        fun onBackPressed(): Boolean

        /**
         * called when user presses up button while focus is on one of the Adapter's item.
         */
        fun onUpPressed(position: Int): Boolean

        /**
         * called when user presses down button while focus is on one of the Adapter's item.
         */
        fun onDownPressed(position: Int): Boolean
    }

}