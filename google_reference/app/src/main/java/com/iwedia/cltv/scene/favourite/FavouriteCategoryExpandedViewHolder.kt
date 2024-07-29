package com.iwedia.cltv.scene.favourite

import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.iwedia.cltv.R
import com.iwedia.cltv.ReferenceDrawableButton


/**
 * Favourite channels expanded view holder
 *
 * @author Aleksandar Lazic
 */
class FavouriteCategoryExpandedViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    //Root view
    var rootView: ConstraintLayout? = null

    //categoryTextLayout
    var categoryTextLayout : RelativeLayout? = null

    //Category text
    var categoryText: TextView? = null

    //Delete number
    var deleteButton: ImageView? = null

    //Rename button
    var renameButton: ImageView? = null

    init {

        //Set references
        rootView = view.findViewById(R.id.favourites_category_item_root_view)
        categoryTextLayout = view.findViewById(R.id.text_layout)
        categoryText = view.findViewById(R.id.favourite_category_item_category_text)
        deleteButton = view.findViewById(R.id.delete_button)
        renameButton = view.findViewById(R.id.edit_button)

        //Set root view to be focusable and clickable
        categoryTextLayout!!.focusable = View.FOCUSABLE
        deleteButton!!.focusable = View.FOCUSABLE
        renameButton!!.focusable = View.FOCUSABLE
        categoryTextLayout!!.isClickable = true
        deleteButton!!.isClickable = true
        renameButton!!.isClickable = true
    }
}