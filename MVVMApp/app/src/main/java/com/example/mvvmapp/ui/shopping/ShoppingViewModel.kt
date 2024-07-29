package com.example.mvvmapp.ui.shopping

import androidx.lifecycle.ViewModel
import com.example.mvvmapp.data.db.entities.ShoppingItem
import com.example.mvvmapp.data.repositories.ShoppingRepo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ShoppingViewModel (
    private val repository:ShoppingRepo
):ViewModel(){
    fun upsert(item :ShoppingItem) = CoroutineScope(Dispatchers.Main).launch {
        repository.upsert(item)
    }

    fun delete(item :ShoppingItem) = CoroutineScope(Dispatchers.Main).launch {
        repository.delete(item)
    }
    fun getAllItems()=repository.getAllItem()
}