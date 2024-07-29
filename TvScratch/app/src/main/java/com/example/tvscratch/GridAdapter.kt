package com.example.tvscratch

import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tvscratch.data.Movie
import com.example.tvscratch.databinding.SingleItemBinding
import kotlin.math.acos

class GridAdapter(private var entries: List<Movie>, private val sourceFragment: String, private val listener: OnItemClickListener) :
    RecyclerView.Adapter<GridAdapter.MyViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(movie: Movie)
        fun onNavigateUp(sourceFragment: String)
    }

    inner class MyViewHolder(private val binding: SingleItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    itemView.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).start()
//                    itemView.setBackgroundResource(R.drawable.button_focused)
                } else {
                    itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
                }
            }
            itemView.setOnKeyListener { v, keyCode, event ->
//                val activity = itemView.context as MainActivity
                if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER -> {
                            val movie = entries[position]
                            listener.onItemClick(movie)
                            return@setOnKeyListener true
                        }

                        KeyEvent.KEYCODE_DPAD_UP -> {
                            Log.e("lates", "$adapterPosition")
                            val currentPosition = adapterPosition
                            if (currentPosition in 0..3) {
                                listener.onNavigateUp(sourceFragment)
                                return@setOnKeyListener true
                            }
                            val layout = (itemView.parent as RecyclerView).layoutManager
                            val itemViewFocus = layout?.findViewByPosition(currentPosition)
                                ?.focusSearch(View.FOCUS_UP)
                            if (itemViewFocus == null)
                                return@setOnKeyListener true
                            val newPosition = layout.getPosition(itemViewFocus)
                            navigateToPosition(newPosition)

                            return@setOnKeyListener true
                        }

                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            val nextFocus = itemView.focusSearch(View.FOCUS_RIGHT)
                            nextFocus?.requestFocus()
                            return@setOnKeyListener true
                        }

                        KeyEvent.KEYCODE_BACK -> {
                            listener.onNavigateUp(sourceFragment)
                            return@setOnKeyListener true
                        }

                        else -> false
                    }

                } else {
                    Log.e("DPAD", "no press")
                    false
                }

            }
        }

        fun bind(data: Movie) {
            binding.textView.text = data.title
            binding.imageView.setImageResource(data.img)
            Log.e("adapter", "gridadapter")
        }


        private fun navigateToPosition(newPosition: Int) {
            (itemView.parent as RecyclerView).smoothScrollToPosition(newPosition)
            // Or, if focusable items, set focus programmatically
            Log.e("Life", "newpos")
            (itemView.parent as RecyclerView).layoutManager?.findViewByPosition(newPosition)
                ?.requestFocus()
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val binding = SingleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MyViewHolder(binding)
        Log.e("adapter", "onCre")
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val entry = entries[position]
        holder.bind(entry)
//        if (position == 0) {
//            holder.itemView.requestFocus()
//        }
    }

    override fun getItemCount(): Int {
        return entries.size
    }

    fun updateData(it: List<Movie>) {
        entries = it
        notifyDataSetChanged()
    }
}