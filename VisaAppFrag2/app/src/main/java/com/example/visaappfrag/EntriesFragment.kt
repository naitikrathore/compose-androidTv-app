package com.example.visaappfrag

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.visaapp.MyData
import com.example.visaappfrag.databinding.FragmentEntriesBinding

class EntriesFragment : Fragment() {

    private lateinit var binding: FragmentEntriesBinding

    companion object {
        private const val ARG_DATA_LIST = "data_list"

        fun newInstance(dataList: List<MyData>): EntriesFragment {
            val fragment = EntriesFragment ()
            val args = Bundle()
            args.putSerializable(ARG_DATA_LIST, ArrayList(dataList))
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentEntriesBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var entries=arguments?.getSerializable(ARG_DATA_LIST) as ArrayList<MyData>
        Log.e("naoi",entries.toString())
       var adapter = EntriesAdapter(entries)
        binding.rvVisa.adapter = adapter
        binding.rvVisa.layoutManager = LinearLayoutManager(requireContext())


//        var  entries= ArrayList<MyData>()
//        entries.add(MyData("Nait","ff","fddf"))
//               var adapter = EntriesAdapter(entries)
//        binding.rvVisa.adapter = adapter
//        binding.rvVisa.layoutManager = LinearLayoutManager(requireContext())

    }




}




