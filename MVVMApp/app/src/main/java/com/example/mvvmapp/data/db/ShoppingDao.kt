package com.example.mvvmapp.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.mvvmapp.data.db.entities.ShoppingItem

@Dao //Marks the interface as a Data Access Object. DAOs are responsible for defining methods to interact with the database.
interface ShoppingDao {
    //mix of update and insert =upsert means if data is already available update it else insert it
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ShoppingItem){
    }

    @Delete
    suspend fun delete(item: ShoppingItem)

    @Query(value = "SELECT * FROM `shopping item`")
    fun getAllitem():LiveData<List<ShoppingItem>>
//    Returns a LiveData object that observes changes in the database and updates the UI accordingly.
}