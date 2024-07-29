package com.example.tvscratch

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tvscratch.data.Movie
import com.example.tvscratch.data.MovieRow
import com.example.tvscratch.databinding.FragmentSearchBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchFragment : Fragment(), VerticalAdapter.SearchFrag, MainActivity.SearchCall {
    private lateinit var binding: FragmentSearchBinding
    private lateinit var viewModel: AppViewModel
    private lateinit var adapter: VerticalAdapter
    private var flag =0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding=FragmentSearchBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        MainActivity().setHomeforSearch(this)
        adapter=VerticalAdapter(emptyList(),"SearchFragment")
        binding.recyclerViewSrh.layoutManager=LinearLayoutManager(requireContext())
        binding.recyclerViewSrh.adapter=adapter
        viewModel = ViewModelProvider(requireActivity()).get(AppViewModel::class.java)

        binding.searchView.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // No implementation needed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterVideo(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {
                // No implementation needed
            }
        })
        binding.searchView.requestFocus()
        adapter.setMainActivityCall(activity as MainActivity)
        adapter.setSearchFrag(this)
        adapter.setFocusVal((activity as MainActivity).getPositionData())
        binding.searchView.setOnKeyListener { v, keyCode, event ->
            if(event.action==KeyEvent.ACTION_DOWN){
                when(keyCode){
                    KeyEvent.KEYCODE_DPAD_RIGHT ->{
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT ->{
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_UP ->{
                        Log.e("SrhEr","dpad up hereee")
                        (requireActivity() as MainActivity).TopNavSrh()
                        return@setOnKeyListener true
                    }
                    KeyEvent.KEYCODE_DPAD_DOWN ->{
                        Log.e("SrhEr","dpad up hereee")
                       if(flag==0){
                           return@setOnKeyListener true
                       }
                        return@setOnKeyListener false

                    }
                    KeyEvent.KEYCODE_BACK ->{
                        (requireActivity() as MainActivity).TopNavSrh()
                        return@setOnKeyListener true
                    }
                    else ->false
                }
            }else{
                false
            }
        }


    }

    private fun filterVideo(query: String) {
        if(query.isBlank()){
            adapter.updateData(emptyList())
            flag=0
        }else{
            viewModel = ViewModelProvider(requireActivity()).get(AppViewModel::class.java)
            viewModel.latest_data.observe(viewLifecycleOwner, { entries ->
                var entry=entries.filter { it.title.contains(query, ignoreCase = true) }
                val rows = createMovieRows(entry, 5) // Specify the number of items per row
                adapter.updateData(rows)
                flag=1
            })
        }
    }
    private fun createMovieRows(movies: List<Movie>, itemsPerRow: Int): List<MovieRow> {
        val rows = mutableListOf<MovieRow>()
        for (i in movies.indices step itemsPerRow) {
            val end = minOf(i + itemsPerRow, movies.size)
            rows.add(MovieRow(movies.subList(i, end)))
        }
        return rows
    }
    override fun onFocusUp() {
//        Log.e("SrhEr", "callupSrh : "+binding.searchView.visibility +" has focus: "+binding.searchView.focusable)
        binding.searchView.requestFocus()
    }

    override fun handleFocus() {
        Log.e("PoError","srch")
        binding.searchView.requestFocus()
    }

}

//
//viewModel = ViewModelProvider(requireActivity()).get(AppViewModel::class.java)
//viewModel.latest_data.observe(viewLifecycleOwner, { entries ->
//    entries.filter { it.title.contains(query, ignoreCase = true) }
//    val rows = createMovieRows(entries, 6) // Specify the number of items per row
//    adapter.updateData(rows)
//})