package com.example.tvscratch

import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.Presenter
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.example.tvscratch.data.Movie
import com.example.tvscratch.databinding.SingleItemBinding

class CardPresenter(
    private val activity: MainActivity,
    private val listener: FocusChangeListener?
) : Presenter() {

    private var ListInstance:updateView?=null
    private var curPos = 0
    override fun onCreateViewHolder(parent: ViewGroup?): ViewHolder {
        val binding = SingleItemBinding.inflate(LayoutInflater.from(parent?.context), parent, false)
        return ViewHolder(binding.root)

    }


    override fun onBindViewHolder(viewHolder: ViewHolder?, item: Any?) {
        val binding = viewHolder?.let { SingleItemBinding.bind(it.view) }
        val movie = item as Movie
        binding?.textView?.text = movie.title
        binding?.imageView?.setImageResource(movie.img)
//
        viewHolder?.view?.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                v.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).start()
                ListInstance?.updateData(movie)
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
            }
        }

        viewHolder?.view?.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN) {
                Log.e("detail", "n")
                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        Log.e("Naitik", "onkecode: ")
                        if (curPos == 0) {
                            activity.TopNavAll()
                            return@setOnKeyListener true
                        }
                        curPos -= 1
                        return@setOnKeyListener false

                    }

                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        curPos += 1
                        return@setOnKeyListener false

                    }

                    KeyEvent.KEYCODE_DPAD_CENTER -> {
                        activity.openDetailsFragment(movie)

                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_BACK ->{
                        (activity as MainActivity).onFocusChangeToFavItem()
                        return@setOnKeyListener true

                    }
                }
            }
            false
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder?) {
        //ss
    }
    fun setInstance(callback:updateView){
        ListInstance=callback
    }

    interface updateView {
        fun updateData(movie: Movie)
    }
}