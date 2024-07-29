import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.videoplayer.databinding.FragmentSearchFragmentBinding
import com.example.videoplayer.ui.AppViewModel


class SearchFrag : Fragment() {
    private lateinit var binding: FragmentSearchFragmentBinding
    private lateinit var viewModel: AppViewModel
    private lateinit var adapter: EntryAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSearchFragmentBinding.inflate(inflater, container, false)
        Log.d(TAG, "onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(requireActivity()).get(AppViewModel::class.java)
        Log.d(TAG, "onViewCreated")
        adapter = EntryAdapter(emptyList(), viewModel)
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
//        initializeUi()


        binding.etSearch.addTextChangedListener(object : TextWatcher {
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
    }

    //    private fun initializeUi() {
//        viewModel.entries.observe(viewLifecycleOwner, { entries ->
//            entries?.let {
//                adapter.updateData(it)
//                Log.e("HomeFragment", it.toString())
//            }
//        })
//    }
    private fun filterVideo(query: String) {
        viewModel.entries.value?.let { entries ->
            val filteredList = if (query.isBlank()) {
                emptyList()  // Return an empty list when query is empty or blank
            } else {
                entries.filter { it.name.contains(query, ignoreCase = true) }
            }
            adapter.updateData(filteredList)
        }
    }






    private val TAG = "actlife"
    override fun onAttach(context: Context) {
        super.onAttach(context)
        Log.d(TAG, "onAttachHOME")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate")
    }

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
        super.onDetach()
        Log.d(TAG, "onDetach")
    }


}