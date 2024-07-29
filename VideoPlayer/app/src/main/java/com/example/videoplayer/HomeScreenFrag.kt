package com.example.videoplayer

import EntryAdapter
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.videoplayer.databinding.FragmentHomeScreenFragBinding
import com.example.videoplayer.ui.AppViewModel
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
@AndroidEntryPoint
class HomeScreenFrag : Fragment() {
    private lateinit var binding: FragmentHomeScreenFragBinding

    val viewModel by viewModels<AppViewModel>()
    lateinit var adapter: EntryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeScreenFragBinding.inflate(inflater, container, false)
        Log.d("actlife", "onCreateViewHOME")
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        viewModel = ViewModelProvider(requireActivity()).get(AppViewModel::class.java)

        adapter = EntryAdapter(emptyList(), viewModel)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        initializeUi()
        Log.d("actlife", "onViewCreatedHOME")

//        utility class
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val entry = adapter.getEntryAtPosition(position)
                viewModel.deleteEntry(entry.id)
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).apply {
            attachToRecyclerView(binding.recyclerView)
        }
    }

    private fun initializeUi() {
        viewModel.entries.observe(viewLifecycleOwner, { entries ->
            entries?.let {
                if(it.isEmpty()){
                    binding.emptyImage.visibility= View.VISIBLE
                }else {
                    binding.emptyImage.visibility= View.GONE
                    adapter.updateData(it)
                    Log.e("nait", it.toString())
                }
            }
        })
    }
    private val TAG = "actlife"


    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "onAttachHOME")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreatefragHOME")
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStartfragHOME")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResumefragHOME")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPausefragHOME")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStopfragHOME")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.d(TAG, "onDestroyViewfragHOME")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroyfragHOME")
    }

    override fun onDetach() {
        super.onDetach()
        Log.d(TAG, "onDetachfragHOME")
    }


}
