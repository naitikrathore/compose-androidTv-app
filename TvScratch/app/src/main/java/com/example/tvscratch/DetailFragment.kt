package com.example.tvscratch

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.tvscratch.data.Movie
import com.example.tvscratch.databinding.FragmentDetailBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DetailFragment : Fragment() {
    private lateinit var binding: FragmentDetailBinding
    private lateinit var viewModel: AppViewModel
    private var selectedMovie: Movie? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDetailBinding.inflate(layoutInflater)
        selectedMovie?.let { movie ->
            binding.movieTitle.text = movie.title
            binding.backgroundImage.setImageResource(movie.img)
        }

        return binding.root

    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        super.onViewCreated(view, savedInstanceState)
        selectedMovie?.let { movie ->
            if (movie.isFav == 1) {
                binding.buttonWatchlist.text = "Unfavourite"
            } else {
                binding.buttonWatchlist.text = "Favourite"
            }
        }
        Log.d(TAG, "onView Created")
        binding.buttonWatch.isFocusable = true
        binding.buttonWatch.requestFocus()
        binding.buttonWatch.setOnKeyListener { v, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_CENTER -> {
                        selectedMovie?.let { movie ->
                            movie.isRecent = 1
                            viewModel.updateData(movie)
                        }
                        var videoUrl =
                            "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
                            putExtra("VIDEO_URL", videoUrl)
                        }
                        startActivity(intent)
                        return@setOnKeyListener true
                    }

                    android.view.KeyEvent.KEYCODE_BACK -> {
                        val activity = requireActivity() as MainActivity
                        activity.backNavi()
//                        activity.launchFrag(HomeFragment())
                        return@setOnKeyListener true
                    }
                    else -> false
                }
            } else {
                false
            }

        }

        binding.buttonWatch.setOnFocusChangeListener { v, hasFocus ->
            changeAppear(v as Button, hasFocus)

        }
        binding.buttonWatchlist.setOnFocusChangeListener { v, hasFocus ->
            changeAppear(v as Button, hasFocus)

        }
//        selectedMovie?.let { movie ->
//            binding.movieTitle.text = movie.title
//            binding.backgroundImage.setImageResource(movie.img)
//            // Update other UI components as needed
//        }
        viewModel = ViewModelProvider(requireActivity()).get(AppViewModel::class.java)


        binding.buttonWatchlist.setOnKeyListener { v, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_CENTER -> {
                        if(binding.buttonWatchlist.text=="Favourite"){
                            selectedMovie?.let { movie ->
                                movie.isFav = 1
                                viewModel.updateData(movie)
                            }
                            binding.buttonWatchlist.text="Unfavourite"
                        }else if ( binding.buttonWatchlist.text=="Unfavourite"){
                            selectedMovie?.let { movie ->
                                movie.isFav = 0
                                viewModel.updateData(movie)
                            }
                            binding.buttonWatchlist.text="Favourite"
                        }
                        return@setOnKeyListener true
                    }

                    android.view.KeyEvent.KEYCODE_BACK -> {
                        val activity = requireActivity() as MainActivity
                        activity.backNavi()
                        return@setOnKeyListener true
                    }
                    else -> false
                }
            } else {
                false
            }

        }
    }

    fun setSelectedMovie(movie: Movie) {
        selectedMovie = movie
    }

    private fun changeAppear(button: Button, hasFocus: Boolean) {
        if (hasFocus) {
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
            button.setBackgroundResource(R.drawable.button_focused)
        } else {
            button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
            button.setBackgroundResource(R.drawable.button_unfocused)
        }
    }


    val TAG="DetailWatch"
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyView")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy")
    }
}
