package com.example.visaappfrag

import com.example.visaapp.MyData

interface DataPassListener {
    fun onDataPassed(entries:List<MyData>)
}