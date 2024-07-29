package com.example.mvvmapp.data.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.temporal.TemporalAmount

@Entity(tableName="shopping item")

data class ShoppingItem (
    @ColumnInfo(name = "item_name")
    var name: String,
    @ColumnInfo(name ="item_amount ")
    var amount: Int
){
    @PrimaryKey(autoGenerate = true)
    var id:Int?=null
}
//When a ShoppingItem is created or queried, Room uses this class to map between the database rows and the object fields. The id field, being nullable and auto-generated, allows the database to handle its assignment.