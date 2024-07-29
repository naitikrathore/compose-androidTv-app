package com.example.anotherapp

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter

class CardPresenter : Presenter() {
    private var mDefaultCardImage: Drawable? = null

    companion object {
        private const val CARD_WIDTH = 313
        private const val CARD_HEIGHT = 176

    }

    inner class ViewHolder(view: View) : Presenter.ViewHolder(view) {
        var movie: Movie? = null
        val cardView: ImageCardView = view as ImageCardView
    }

    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        mDefaultCardImage = ContextCompat.getDrawable(parent!!.context, R.drawable.movie)
        val cardView = ImageCardView(parent.context)
        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        cardView.setBackgroundColor(
            ContextCompat.getColor(
                parent.context,
                R.color.fastlane_background
            )
        )
        return ViewHolder(cardView)

    }

    override fun onBindViewHolder(viewHolder: Presenter.ViewHolder?, item: Any?) {
        val movie = item as Movie
        val cardView = viewHolder?.view as ImageCardView
        cardView.titleText = movie.title
        cardView.contentText = movie.studio
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        cardView.mainImage=mDefaultCardImage
    }

    override fun onUnbindViewHolder(viewHolder: Presenter.ViewHolder?) {
        Log.e("n","h")
//        val holder=viewHolder as ViewHolder
//        holder.movie=null
//            holder.cardView.apply {
//                titleText=null
//                contentText=null
//                mainImage=null
//            }
//

    }

}