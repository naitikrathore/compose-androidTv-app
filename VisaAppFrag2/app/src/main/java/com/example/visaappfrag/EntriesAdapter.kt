package com.example.visaappfrag

import android.util.Log
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.visaapp.MyData
import com.example.visaappfrag.databinding.SingleEntryBinding

class EntriesAdapter(var allitem:ArrayList<MyData>
):RecyclerView.Adapter<EntriesAdapter.EntriesViewHolder>() {

    inner class EntriesViewHolder(private val binding: SingleEntryBinding):RecyclerView.ViewHolder(binding.root){
      fun bind(data: MyData){
          binding.tvFirstn.text=data.FirstName
          binding.tvLastn.text=data.LastName
          binding.tvCountr.text=data.Country
          binding.cbselect.isChecked=false

      }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int): EntriesAdapter.EntriesViewHolder {
        val binding= SingleEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return EntriesViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EntriesViewHolder, position: Int) {
        Log.e("nao",allitem.toString())
        holder.bind(allitem[position])
    }

    override fun getItemCount(): Int {
     return allitem.size
    }
}
