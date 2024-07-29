package com.iwedia.cltv.components

import androidx.recyclerview.widget.DiffUtil
import com.iwedia.cltv.platform.model.TvEvent
import com.iwedia.cltv.platform.model.foryou.RailItem
import com.iwedia.cltv.platform.model.recording.Recording

/**
 * Custom implementation of [DiffUtil.ItemCallback] designed for use with the AsyncListDiffer.
 * It provides flexibility for comparing and identifying changes between different item types within a RecyclerView.
 *
 * * Website: https://developer.android.com/reference/androidx/recyclerview/widget/AsyncListDiffer
 *
 * @param T The type of items to be compared.
 * @author Boris Tirkajla
 */
class AsyncDiffUtil<T> :
    DiffUtil.ItemCallback<T>() {

    /**
     * Checks whether the items represented by the old and new items are the same.
     *
     * @param oldItem The old item.
     * @param newItem The new item.
     * @return `true` if the items are the same; `false` otherwise.
     * @throws Exception If the item types are unhandled and not properly specified in the 'when' expression.
     */
    override fun areItemsTheSame(oldItem: T & Any, newItem: T & Any): Boolean {
        val result = when {
            oldItem is RailItem && newItem is RailItem -> {
                oldItem.id == newItem.id
            }

            oldItem is TvEvent && newItem is TvEvent -> {
                oldItem.id == newItem.id
            }

            oldItem is Recording && newItem is Recording -> {
                oldItem.id == newItem.id
            }

            else -> {
                throw Exception("Unhandled item type $oldItem. Make sure to handle this case in the 'when' expression.")
            }
        }
        return result
    }

    /**
     * Checks whether the contents of the items represented by the old and new items are the same.
     *
     * @param oldItem The old item.
     * @param newItem The new item.
     * @return `true` if the contents of the items are the same; `false` otherwise.
     * @throws Exception If the item types are unhandled and not properly specified in the 'when' expression.
     */
    override fun areContentsTheSame(oldItem: T & Any, newItem: T & Any): Boolean {
        val result = when {
            oldItem is RailItem && newItem is RailItem -> {
                (oldItem as RailItem) == (newItem as RailItem)
            }

            oldItem is TvEvent && newItem is TvEvent -> {
                (oldItem as TvEvent) == (newItem as TvEvent)
            }

            oldItem is Recording && newItem is Recording -> {
                (oldItem as Recording) == (newItem as Recording)
            }

            else -> {
                throw Exception("Unhandled item type. Make sure to handle this case in the 'when' expression.")
            }
        }
        return result
    }
}