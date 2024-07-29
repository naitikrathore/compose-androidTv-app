package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemListHoriBinding

class HorizontalAdapter(private val items:List<String>)
    :RecyclerView.Adapter<HorizontalAdapter.ViewHolder>() {
    inner class ViewHolder(private var binding: ItemListHoriBinding):RecyclerView.ViewHolder(binding.root) {
        fun bind(item:String){
            binding.text1.text=item
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HorizontalAdapter.ViewHolder {
        val binding=ItemListHoriBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HorizontalAdapter.ViewHolder, position: Int) {
        val posi=items[position]
        holder.bind(posi)
    }

    override fun getItemCount(): Int {
       return items.size
    }
}