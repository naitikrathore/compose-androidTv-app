package com.example.tvscratch

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.example.tvscratch.data.Movie
import com.example.tvscratch.databinding.FragmentSplashBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SplashFragment : Fragment() {
     private lateinit var binding: FragmentSplashBinding
    private val viewModel by viewModels<AppViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding=FragmentSplashBinding.inflate(layoutInflater)

        return binding.root
//        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val homeFragment=HomeFragment()
//        requireActivity().supportFragmentManager.beginTransaction()
//            .replace(R.id.fragment_container, homeFragment)
//            .commit()

        Handler(Looper.getMainLooper()).postDelayed({
            observeViewModel()
        }, 2000) // 4
    }
    private fun observeViewModel() {
        viewModel.latest_data.observe(viewLifecycleOwner, { entries ->
            entries?.let {
                if (it.isNotEmpty()) {
                    val homeFragment = HomeFragment()
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, homeFragment)
                        .commit()

                }else{
                    loadData()
                }
            }
        })
    }
    @SuppressLint("SuspiciousIndentation")
    private fun loadData(){
        val movies = listOf(
            Movie(1, "Batman", img = R.drawable.srambled_poster),
            Movie(2, "Superman", img = R.drawable.movie2),
            Movie(3, "Spiderman", img = R.drawable.movie3),
            Movie(4, "Iron Man", img = R.drawable.movie4),
            Movie(5, "Wonder Woman", img = R.drawable.movie5),
            Movie(6, "Black Panther", img = R.drawable.movie1),
            Movie(7, "Thor", img = R.drawable.movie2),
            Movie(8, "Captain America", img = R.drawable.movie3),
            Movie(9, "Deadpool", img = R.drawable.movie4),
            Movie(10, "Guardians of the Galaxy", img = R.drawable.movie5),
            Movie(11, "The Avengers", img = R.drawable.movie4),
            Movie(12, "Aquaman", img = R.drawable.movie3),
            Movie(13, "The Dark Knight", img = R.drawable.movie2),
            Movie(14, "Avatar", img = R.drawable.movie1),
            Movie(15, "Jurassic Park", img = R.drawable.movie2),
            Movie(16, "Avengers: End Game", img = R.drawable.movie8),
            Movie(17, "A Wednesday", img = R.drawable.movie9),
            Movie(18, "Thor", img = R.drawable.movie7),
            Movie(19, "Marshion", img = R.drawable.movie8),
            Movie(20, "Oblivioun", img = R.drawable.movie9),
            Movie(21, "Movie 21", img = R.drawable.movie1),
            Movie(22, "Movie 22", img = R.drawable.movie2),
            Movie(23, "Movie 23", img = R.drawable.movie3),
            Movie(24, "Movie 24", img = R.drawable.movie4),
            Movie(25, "Movie 25", img = R.drawable.movie5),
            Movie(26, "Movie 26", img = R.drawable.movie1),
            Movie(27, "Movie 27", img = R.drawable.movie2),
            Movie(28, "Movie 28", img = R.drawable.movie3),
            Movie(29, "Movie 29", img = R.drawable.movie4),
            Movie(30, "Movie 30", img = R.drawable.movie5),
            Movie(31, "Movie 31", img = R.drawable.movie4),
            Movie(32, "Movie 32", img = R.drawable.movie3),
            Movie(33, "Movie 33", img = R.drawable.movie2),
            Movie(34, "Movie 34", img = R.drawable.movie1),
            Movie(35, "Movie 35", img = R.drawable.movie2),
            Movie(36, "Movie 36", img = R.drawable.movie8),
            Movie(37, "Movie 37", img = R.drawable.movie9),
            Movie(38, "Movie 38", img = R.drawable.movie7),
            Movie(39, "Movie 39", img = R.drawable.movie8),
            Movie(40, "Movie 40", img = R.drawable.movie9)
        )

                movies.forEach{ movie->
            viewModel.insertData(movie)
//            viewModel.updateData(movie)
        }


    }
}