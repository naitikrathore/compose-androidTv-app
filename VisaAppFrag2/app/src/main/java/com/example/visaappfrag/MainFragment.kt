package com.example.visaappfrag

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.visaapp.MyData
import com.example.visaappfrag.databinding.FragmentMainBinding

class MainFragment : Fragment() {
    private var dataPassListener:DataPassListener?=null
    private val entries = mutableListOf<MyData>()
    private lateinit var binding: FragmentMainBinding

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is DataPassListener) {
            dataPassListener = context
        } else {
            throw RuntimeException("$context must implement DataPassListener")
        }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentMainBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btappply.setOnClickListener{
             updatelist(it)
        }
    }
    private fun updatelist(view: View){
        var firstName = binding.etFname.text.toString()
        var lastName = binding.etLname.text.toString()
        var country = binding.etCn.text.toString()
        if (firstName.isEmpty() || lastName.isEmpty() || country.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill in all fields", Toast.LENGTH_SHORT).show()
        } else {
            entries.add(MyData(firstName, lastName, country))
            Log.e("nai","$entries")
            binding.etFname.text.clear()
            binding.etLname.text.clear()
            binding.etCn.text.clear()
            passEntries()
        }

    }
    private fun passEntries() {
        if (entries.isNotEmpty()) {
            dataPassListener?.onDataPassed(entries.toList())
            Log.e("nai","$entries")
            entries.clear()
        }
    }

}