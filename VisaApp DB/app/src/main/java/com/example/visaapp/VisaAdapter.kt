package com.example.visaapp

import android.database.Cursor
import android.provider.BaseColumns
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.visaapp.databinding.SingleEntryBinding

class VisaAdapter(var cursor: Cursor?) :
    RecyclerView.Adapter<VisaAdapter.VisaViewHolder>() {
     var selected = SparseBooleanArray()
//    private lateinit var cursor: Cursor

    inner class VisaViewHolder(private val binding: SingleEntryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(cursor: Cursor) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow(BaseColumns._ID))
            val firstName = cursor.getString(cursor.getColumnIndexOrThrow(MyDataEntry.COLUMN_FIRST_NAME))
            val lastName = cursor.getString(cursor.getColumnIndexOrThrow(MyDataEntry.COLUMN_LAST_NAME))
            val country = cursor.getString(cursor.getColumnIndexOrThrow(MyDataEntry.COLUMN_COUNTRY))

            binding.tvFirstn.text = firstName
            binding.tvLastn.text = lastName
            binding.tvCountr.text = country
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
        cursor?.apply {
            if (moveToPosition(position)) {
                holder.bind(this)
            }
        }
    }

    override fun getItemCount(): Int {
        return cursor?.count ?: 0
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

    fun updateData(newCursor: Cursor?) {
        cursor?.close()
          cursor=newCursor
        notifyDataSetChanged()
    }
    fun closeCursor() {
        cursor?.close()
    }
}