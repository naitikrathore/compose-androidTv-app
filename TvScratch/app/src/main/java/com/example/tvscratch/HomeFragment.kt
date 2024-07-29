package com.example.tvscratch

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.tvscratch.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tvscratch.data.Movie
import com.example.tvscratch.data.MovieRow

@AndroidEntryPoint
class HomeFragment : Fragment(), VerticalAdapter.HomeFragCall {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var adapter: VerticalAdapter
    private lateinit var viewModel: AppViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.findViewById<View>(R.id.LLnavigation)?.visibility=View.VISIBLE
//        activity?.findViewById<TextView>(R.id.tvAll)?.requestFocus()
        adapter = VerticalAdapter(emptyList(),"HomeFrag")
        binding.listFragment.layoutManager = LinearLayoutManager(requireContext())
        binding.listFragment.adapter = adapter
        intializeUI()


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

// Set isFav=1 for the first 20 movies
//        for (i in 0 until 40) {
//            movies[i].isFav = 0
//}
//        for (i in 0 until 40) {
//            movies[i].isRecent = 0
//        }
////////
////////

//        movies.forEach{ movie->
//            viewModel.insertData(movie)
////            viewModel.updateData(movie)
//        }

//        val listFragment = ListFragment()
//        childFragmentManager.beginTransaction().replace(R.id.list_fragment, listFragment)
//            .commit()
//        listFragment.setMainInstance(this)
    }
    private fun intializeUI() {
        viewModel = ViewModelProvider(requireActivity()).get(AppViewModel::class.java)
        viewModel.latest_data.observe(viewLifecycleOwner, { entries ->
            val rows = createMovieRows(entries, 6) // Specify the number of items per row
            adapter.updateData(rows)

        })
        adapter.setMainActivityCall(requireActivity() as MainActivity)
        adapter.setHomeFragInstance(this)
        adapter.setFocusVal((activity as MainActivity).getPositionData())

    }
    private fun createMovieRows(movies: List<Movie>, itemsPerRow: Int): List<MovieRow> {
        val rows = mutableListOf<MovieRow>()
        for (i in movies.indices step itemsPerRow) {
            val end = minOf(i + itemsPerRow, movies.size)
            rows.add(MovieRow(movies.subList(i, end)))
        }
        return rows
    }

    override fun updateView(movie: Movie) {
        binding.imgBanner.setBackgroundResource(movie.img)
        binding.tvTitle.text = movie.title
    }
}

