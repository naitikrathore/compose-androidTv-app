package com.example.myapplication

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.ItemListHoriBinding
import com.example.myapplication.databinding.ItemListVertBinding

class VerticalAdapter(private val items:List<String>)
:RecyclerView.Adapter<VerticalAdapter.ViewHolder>() {
    inner class ViewHolder(private var binding: ItemListVertBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(item:String){
            binding.text1.text=item
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerticalAdapter.ViewHolder {
        val binding= ItemListVertBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return ViewHolder(binding)
    }
    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val posi=items[position]
        holder.bind(posi)
    }
}