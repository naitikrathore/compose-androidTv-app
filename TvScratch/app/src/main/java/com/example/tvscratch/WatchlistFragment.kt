package com.example.tvscratch

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.size
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.tvscratch.data.Movie
import com.example.tvscratch.data.MovieRow
import com.example.tvscratch.databinding.FragmentWishlistBinding


class WatchlistFragment : Fragment(),FocusChangeListener{
    private lateinit var binding: FragmentWishlistBinding
    private lateinit var adapter: VerticalAdapter
    private lateinit var viewModel: AppViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d("VANDANA","on create viewww")
        binding = FragmentWishlistBinding.inflate(layoutInflater)
        return binding.root
    }
    override fun onFocusChangeToFavItem() {
        (activity as MainActivity).TopNavFav()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Log.d("Watch", "ViewCreate")
        super.onViewCreated(view, savedInstanceState)
        adapter = VerticalAdapter(emptyList(),"WatchList")
        binding.outerRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.outerRecyclerView.adapter = adapter
        intializeUI()
   }

    @SuppressLint("SuspiciousIndentation")
    private fun intializeUI() {
        viewModel = ViewModelProvider(requireActivity()).get(AppViewModel::class.java)
        viewModel.latest_fav.observe(viewLifecycleOwner, { entries ->
            val rows = createMovieRows(entries, 6) // Specify the number of items per row
            adapter.updateData(rows)
        })
        adapter.setMainActivityCall(activity as MainActivity)
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

    val TAG="VANDANA"
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

    override fun onDetach() {
        Log.d(TAG, "onDestach")
        super.onDetach()
    }

}