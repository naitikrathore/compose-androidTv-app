package com.example.tvscratch

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tvscratch.data.Movie
import com.example.tvscratch.data.MovieRow
import com.example.tvscratch.databinding.FragmentDetailBinding
import com.example.tvscratch.databinding.FragmentRecentBinding

class RecentFragment : Fragment(),GridAdapter.OnItemClickListener {
    private lateinit var binding:FragmentRecentBinding
    private lateinit var adapter: VerticalAdapter
    private lateinit var viewModel: AppViewModel
    override fun onItemClick(movie: Movie) {
        (requireActivity() as MainActivity).openDetailsFragment(movie)
    }

    override fun onNavigateUp(sourceFragment: String) {
        (requireActivity() as MainActivity).onNavigateUp(sourceFragment)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding= FragmentRecentBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter=VerticalAdapter(emptyList(),"RecentFrag",)
        binding.rvRecent.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecent.adapter = adapter
        initializeUi()

    }
    private fun initializeUi() {
        viewModel = ViewModelProvider(requireActivity()).get(AppViewModel::class.java)
        viewModel.latest_recent.observe(viewLifecycleOwner, { entries ->
            val rows = createMovieRows(entries, 4) // Specify the number of items per row
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


}