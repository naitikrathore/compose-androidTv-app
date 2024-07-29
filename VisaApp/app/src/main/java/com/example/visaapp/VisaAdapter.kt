package com.example.visaapp

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.util.SparseArray
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.view.menu.MenuView.ItemView
import androidx.core.util.contains
import androidx.core.util.containsKey
import androidx.recyclerview.widget.RecyclerView
import com.example.visaapp.databinding.SingleEntryBinding

class VisaAdapter(var allitem: ArrayList<MyData>) :
    RecyclerView.Adapter<VisaAdapter.VisaViewHolder>() {
    private var selected = SparseBooleanArray()

    inner class VisaViewHolder(private val binding: SingleEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(data: MyData) {
            binding.tvFirstn.text = data.FirstName
            binding.tvLastn.text = data.LastName
            binding.tvCountr.text = data.Country
            binding.cbselect.isChecked = selected[adapterPosition, false]
            binding.cbselect.setOnCheckedChangeListener { _, isChecked ->
                toggleSelection(adapterPosition, isChecked)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VisaViewHolder {
        val binding = SingleEntryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VisaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VisaViewHolder, position: Int) {
        holder.bind(allitem[position])
    }

    override fun getItemCount(): Int {
        return allitem.size
    }

    private fun toggleSelection(position: Int, isChecked: Boolean) {
        if (isChecked) {
            selected.put(position, true)
        } else {
            selected.delete(position)
        }
    }

    fun isAnyItemSelected(): Boolean {
        return selected.size() > 0
    }

    fun deleteselected() {
        val positionsToRemove = mutableListOf<Int>()
        for (i in selected.size() - 1 downTo 0) {
            val position = selected.keyAt(i)
            allitem.removeAt(position)
            positionsToRemove.add(position)
        }
        selected.clear()
        notifyDataSetChanged()
    }
}