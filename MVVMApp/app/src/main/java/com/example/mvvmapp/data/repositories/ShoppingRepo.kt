package com.example.mvvmapp.data.repositories

import com.example.mvvmapp.data.db.ShoppingDatabase
import com.example.mvvmapp.data.db.entities.ShoppingItem

class ShoppingRepo (private val db: ShoppingDatabase
){
    suspend fun upsert(item: ShoppingItem) = db.getShoppingDao().upsert(item)
    suspend fun delete(item: ShoppingItem) = db.getShoppingDao().delete(item)

    fun getAllItem() =db.getShoppingDao().getAllitem()

}