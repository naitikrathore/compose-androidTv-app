package com.example.tvscratch

import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.tvscratch.data.FocusValue
import com.example.tvscratch.data.Movie
import com.example.tvscratch.databinding.FragmentWishlistBinding
import com.example.tvscratch.databinding.SingleItemBinding

class HorizontalAdapter(private var movies: List<Movie>,private val sourceFragment: String) :
    RecyclerView.Adapter<HorizontalAdapter.MovieViewHolder>() {
    private var parentCall: ParentCall? = null
    private var vertPos: Int = -1
    private var focusValue : FocusValue ? = null

    inner class MovieViewHolder(private val binding: SingleItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    parentCall?.updateView(movies[adapterPosition])
                    itemView.animate().scaleX(1.2f).scaleY(1.2f).setDuration(200).start()
                } else {
                    itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
                }
            }
            itemView.setOnKeyListener { v, keyCode, event ->

                if (event.action == KeyEvent.ACTION_DOWN) {
                    when (keyCode) {
                        KeyEvent.KEYCODE_DPAD_CENTER -> {
                            Log.e("pppp","clicked")
                            //save focus point
//                            parentCall?.saveFocus(position)
                            parentCall?.saveFocus(vertPos,adapterPosition)
                            val movie = movies[adapterPosition]
                             parentCall?.onCenter(movie)
//                            activity?.openDetailsFragment(movie)
                            return@setOnKeyListener true
                        }

                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            Log.e("cjk", "${adapterPosition}")
                            if (adapterPosition < movies.size - 1) {
                                val nextFocus = itemView.focusSearch(View.FOCUS_RIGHT)
                                nextFocus?.requestFocus()
                            }
                            return@setOnKeyListener true
                        }

                        KeyEvent.KEYCODE_DPAD_LEFT -> {
                            if (adapterPosition > 0) {
                                val previousFocus = itemView.focusSearch(View.FOCUS_LEFT)
                                previousFocus?.requestFocus()
                                true
                            } else {
                                false
                            }
                        }

                        KeyEvent.KEYCODE_DPAD_UP -> {
                            if(sourceFragment=="SearchFragment"){
                                parentCall?.onNavigateUp()
                                return@setOnKeyListener true
                            }else{
                                Log.e("SrhEr", "callupHori")
                                parentCall?.onNavigateUp()
                                return@setOnKeyListener false
                            }
                        }

                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            parentCall?.onNavigateDown()
                            return@setOnKeyListener false
                        }

                        KeyEvent.KEYCODE_BACK -> {
                            parentCall?.onNavigateBack()
                            return@setOnKeyListener true
                        }

                        else -> false
                    }
                } else {
                    false
                }
            }
        }

        fun bind(movie: Movie) {
            Log.e("llkk","${focusValue?.horiPos}")
            if(focusValue!=null && adapterPosition==focusValue?.horiPos) {
                itemView.requestFocus()
                Log.e("Bindddd","${focusValue?.horiPos}")
                Log.e("Binddd","${adapterPosition}")
                Log.e("Bindd","${itemView}")
                parentCall?.saveFocus(-1,-1)
            }

            binding.textView.text = movie.title
            binding.imageView.setBackgroundResource(movie.img)
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = SingleItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MovieViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return movies.size
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position])

    }

    fun setParetnCall(callback: ParentCall) {
        parentCall = callback

    }

    fun setFocusPosi(position: Int) {
        vertPos=position
    }

    fun setFocusValues(positionData: FocusValue?) {
        if (positionData != null) {
//           Log.e("poiii22", "${positionData.verticalPos}")
            Log.e("Qwerty", "${positionData.horiPos}")

            if (focusValue == null) {
                focusValue = FocusValue()
            }
            focusValue?.horiPos = positionData.horiPos
            notifyDataSetChanged()
//            Log.e("", "${focusValue?.verticalPos}")
            Log.e("Qwert", "${focusValue?.horiPos}")
        }

    }

    interface ParentCall {
        fun onNavigateUp()
        fun onNavigateDown()
        fun onNavigateBack()
        fun onCenter(movie:Movie)
        fun saveFocus(verti:Int,hori: Int)
        fun updateView(movie:Movie)
    }
}