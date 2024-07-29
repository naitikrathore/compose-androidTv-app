package com.iwedia.cltv.components.welcome_screen

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.iwedia.cltv.R

@SuppressLint("ViewConstructor")
class CustomWelcomeScreenItem(context: Context, val item: Item) : ConstraintLayout(context) {

    private val iconImageView: ImageView
    private val titleTextView: TextView
    private val descriptionTextView: TextView

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.layout_custom_item, this, true)

        // PROPERTY INITIALIZATION
        iconImageView = findViewById(R.id.icon_image_view)
        titleTextView = findViewById(R.id.title_text_view)
        descriptionTextView = findViewById(R.id.description_text_view)
        // ---

        setup()
    }

    private fun setup() {
        iconImageView.setImageResource(item.iconResource)
        titleTextView.text = item.title
        descriptionTextView.text = item.description
    }

    data class Item(val title: String, val description: String, val iconResource: Int)
}