package com.example.mvvmapp.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.mvvmapp.data.repositories.ShoppingRepo

class ShoppingViewModelFactory(
    private val repo:ShoppingRepo
): ViewModelProvider.NewInstanceFactory() {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShoppingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShoppingViewModel(repo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}