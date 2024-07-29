package com.iwedia.cltv.scene.favourite

import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.components.DiffUtilItemCallback
import com.iwedia.cltv.platform.model.TvChannel

/**
 * Favourite channels adapter
 *
 * @author Aleksandar Lazic
 */
class FavouriteChannelsAdapter : RecyclerView.Adapter<FavouriteChannelsViewHolder>() {

    //Items
    private var items: MutableList<TvChannel> = mutableListOf()

    //keep focus on item when buttons are focused
    var shouldKeepFocus = false

    //adapter listener
    var adapterListener: AdapterListener? = null

    //selected filter category
    var selectedCategory = "Favorite 1"

    private val difCallback = DiffUtilItemCallback<TvChannel>()
    private val differ = AsyncListDiffer(this, difCallback)

    var focusedItemPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavouriteChannelsViewHolder {
        val linearLayout = LinearLayout(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        return FavouriteChannelsViewHolder(linearLayout)
    }

    override fun onBindViewHolder(holder: FavouriteChannelsViewHolder, position: Int) {
        val item = differ.currentList[position]

        holder.rootView.updateData(item = item, selectedCategory = selectedCategory)

        holder.rootView.setOnClick {
            adapterListener!!.onItemClicked(item)
        }

        holder.rootView.setOnLongClick {
            adapterListener!!.onItemClicked(item)
        }

        holder.rootView.setOnKeyListener { _, keyCode, keyEvent ->
            if (holder.rootView.isAnimationInProgress) return@setOnKeyListener true

            if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                if (keyEvent.repeatCount != 0 && keyEvent.repeatCount % 3 == 0) {
                    return@setOnKeyListener  true
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    if (position in 0..4) { //first row
                        adapterListener!!.requestFocusOnFilters()
                        return@setOnKeyListener true
                    }
                }
                if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    if (position == items.count() - 1) return@setOnKeyListener true // this is used for the last element in Favorite list to avoid focusing Views that shouldn't be focused.
                }
            } else if (keyEvent.action == KeyEvent.ACTION_UP) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    adapterListener!!.requestFocusOnFilters()
                    focusedItemPosition = -1
                    return@setOnKeyListener true
                }
            }
            false
        }

        holder.rootView.setOnFocusChanged { hasFocus ->
            if (hasFocus) {
                focusedItemPosition = position
                adapterListener!!.getAdapterPosition(position)
            }
        }
    }

    fun refreshSelectedCategory(category: String) {
        selectedCategory = category
        notifyItemRangeChanged(0, itemCount)
    }

    fun refreshFocusedItem() {
        notifyItemChanged(focusedItemPosition)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    fun refresh(adapterItems: MutableList<TvChannel>) {
        this.items.clear()
        this.items.addAll(adapterItems)
        differ.submitList(adapterItems)
    }

    interface AdapterListener {
        fun onItemClicked(tvChannel: TvChannel)
        fun getAdapterPosition(position: Int)
        fun requestFocusOnFilters()

    }

    fun setListener(adapterListener: AdapterListener) {
        this.adapterListener = adapterListener
    }
}