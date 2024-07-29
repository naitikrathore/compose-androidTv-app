package com.example.appbuttomnavi

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

class Frag1 : Fragment() {


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_frag1, container, false)
    }
//    This fun which is overriding Oncreateview is actually first it checkes whether a previous view of fragment is available in bundle or if not then it
//    it is infating the frag1 layout to this class meaning attaching xml file
//    and in activity lifecycle first onreate() is called then we have the access to views of layout but in this first Oncrietview() is created so if we try to get views of xml in on createview irst then it is not possibe


//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//    }
////
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//    }

}