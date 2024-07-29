package com.example.mvvmapp.other

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mvvmapp.data.db.entities.ShoppingItem
import com.example.mvvmapp.databinding.ShoppingItemBinding
import com.example.mvvmapp.ui.shopping.ShoppingViewModel

class ShoppingAdapter(
    var items: List<ShoppingItem>,
    private val viewModel: ShoppingViewModel
) : RecyclerView.Adapter<ShoppingAdapter.ShoppingViewHolder>() {

    inner class ShoppingViewHolder(val binding: ShoppingItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingViewHolder {
        val binding = ShoppingItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ShoppingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ShoppingViewHolder, position: Int) {
        val curItem = items[position]
        holder.binding.apply {
            tvName.text = curItem.name
            tvAmount.text = curItem.amount.toString()
            ivDelete.setOnClickListener{
                viewModel.delete(curItem)
            }
            ivPlus.setOnClickListener{
                curItem.amount++
                viewModel.upsert(curItem)
            }
            ivMinus.setOnClickListener{
                if(curItem.amount>0){
                    curItem.amount--
                    viewModel.upsert(curItem)
                }

            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
