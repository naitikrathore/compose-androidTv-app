package com.iwedia.cltv.components

import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceApplication.Companion.downActionBackKeyDone
import com.iwedia.cltv.platform.`interface`.TTSSetterInterface
import com.iwedia.cltv.platform.model.IAsyncCallback

/**
 * Reference multi check list adapter
 *
 * @author Dejan Nadj
 */
class MultiCheckListAdapter(private val preventClip: Boolean? = false): RecyclerView.Adapter<HorizontalButtonsAdapterViewHolder>() {

    //Items
    private var items = mutableListOf<String>()

    //Focused item index
    var focusedItem = -1

    //Selected item list
    private var selectedItems = arrayListOf<Int>()

    //Adapter listener
    var adapterListener: MultiCheckListAdapterListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HorizontalButtonsAdapterViewHolder {
        return HorizontalButtonsAdapterViewHolder(
            view = ConstraintLayout(parent.context),
            ttsSetterInterface = adapterListener!!,
            preventClip = preventClip!!
        )
    }

    override fun onBindViewHolder(holder: HorizontalButtonsAdapterViewHolder, position: Int) {
        val item = items[position]

        if (selectedItems.contains(position)) {
            holder.customButton.update(ButtonType.CHECKED, item, R.drawable.ic_check)
        }
        else {
            holder.customButton.update(ButtonType.NOT_CHECKED, item, null)
        }

        holder.customButton.setTransparentBackground(true)

        holder.customButton.setOnClick {
            holder.customButton.isAnimationInProgress = true

            adapterListener!!.onItemClicked(
                item,
                object : IAsyncCallback {
                    override fun onSuccess() {
                        holder.customButton.isAnimationInProgress = false
                        if (selectedItems.contains(holder.adapterPosition)) {
                            holder.customButton.update(ButtonType.NOT_CHECKED, holder.customButton.getTextLabel(), null)
                            selectedItems.remove(holder.adapterPosition)
                        } else {
                            holder.customButton.update(ButtonType.CHECKED, holder.customButton.getTextLabel(), R.drawable.ic_check)
                            selectedItems.add(holder.adapterPosition)
                        }
                    }

                    override fun onFailed(error: Error) {
                        holder.customButton.isAnimationInProgress = false
                    }
                })
        }

        holder.customButton.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(p0: View?, keyCode: Int, keyEvent: KeyEvent?): Boolean {
                if (keyEvent!!.action == KeyEvent.ACTION_DOWN) {
                    if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                        if (adapterListener != null) {
                            return adapterListener!!.onKeyUp(holder.adapterPosition)
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                        if (adapterListener != null) {
                            return adapterListener!!.onKeyDown(holder.adapterPosition)
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                        if (adapterListener != null) {
                            return if (ViewCompat.getLayoutDirection(p0!!) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                                adapterListener!!.onKeyRight(holder.adapterPosition)
                            } else {
                                adapterListener!!.onKeyLeft(holder.adapterPosition)
                            }
                        }
                    } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                        return if (ViewCompat.getLayoutDirection(p0!!) == ViewCompat.LAYOUT_DIRECTION_LTR) {
                            adapterListener!!.onKeyLeft(holder.adapterPosition)
                        } else {
                            adapterListener!!.onKeyRight(holder.adapterPosition)
                        }
                    }
                    if (keyCode == KeyEvent.KEYCODE_BACK){
                        downActionBackKeyDone = true
                    }
                }
                else if (keyEvent.action == KeyEvent.ACTION_UP) {
                    if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ESCAPE) {
                        if (!downActionBackKeyDone) return true
                        return adapterListener!!.onBackPressed(holder.adapterPosition)
                    }
                }

                return false
            }
        })
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
        notifyDataSetChanged()
    }

    //Refresh
    fun refresh(adapterItems: MutableList<String>) {
        this.items.clear()
        this.items.addAll(adapterItems)
        notifyDataSetChanged()
    }

    fun dispose() {
        if (items.isNotEmpty()) {
            items.clear()
        }
        if (selectedItems.isNotEmpty()) {
            selectedItems.clear()
        }
        adapterListener = null
    }

    /**
     * Listener
     */
    interface MultiCheckListAdapterListener: TTSSetterInterface {
        fun onItemClicked(button: String, callback: IAsyncCallback)
        fun onKeyUp(position: Int): Boolean
        fun onKeyDown(position: Int): Boolean
        fun onKeyRight(position: Int): Boolean
        fun onKeyLeft(position: Int) : Boolean
        fun onBackPressed(position: Int): Boolean
    }
}