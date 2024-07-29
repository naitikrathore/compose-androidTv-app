package com.example.videoplayer.ui

import EntryAdapter
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.videoplayer.R
import com.example.videoplayer.databinding.FragmentFavBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
@AndroidEntryPoint
class FavFragment : Fragment() {
    private lateinit var binding: FragmentFavBinding
    private lateinit var adapter: EntryAdapter
    private lateinit var viewModel: AppViewModel
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFavBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(AppViewModel::class.java)
        adapter = EntryAdapter(emptyList(), viewModel)
        binding.recyclerView2.adapter = adapter
        binding.recyclerView2.layoutManager = LinearLayoutManager(requireContext())
        initializeUi()
    }

    private fun initializeUi() {
        viewModel = ViewModelProvider(requireActivity()).get(AppViewModel::class.java)
        viewModel.getfav.observe(viewLifecycleOwner, { entries ->
            entries?.let {
                adapter.updateData(it)
                Log.e("nait", it.toString())
            }
        })
    }


}