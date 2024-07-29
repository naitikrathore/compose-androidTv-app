package com.example.mvvmapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.mvvmapp.data.db.entities.ShoppingItem


//Declares this class as a Room database. It lists the entities (tables) that belong to the database and the version of the database schema.
@Database(
    entities = [ShoppingItem::class],
    version = 1
)
abstract class ShoppingDatabase: RoomDatabase() {

//   This method provides access to the DAO (Data Access Object), ShoppingDao, which contains methods to interact with the database.
    abstract fun getShoppingDao(): ShoppingDao


//    companion object: Ensures a single instance of the database is created throughout the app's lifecycle. create a instance of Databse of our databse a single instance only be created at a time
//    only one thread at one time will interact with db writeoperraton

    companion object{
        @Volatile
        private var instance: ShoppingDatabase?=null
        private var LOCK=Any()

//        The app initializes the database using ShoppingDatabase.invoke(context), ensuring a single instance is created and accessed throughout the app's lifecycle.
        operator fun invoke(context: Context)= instance ?: synchronized(LOCK){
            instance ?: createDatabase(context).also { instance =it }
        }

        private fun createDatabase(context: Context) =
            Room.databaseBuilder(context.applicationContext,
                ShoppingDatabase::class.java,"Shopping.db").build()
    }

//    When the app needs to interact with the database, it will call ShoppingDatabase(context).getShoppingDao() to get an instance of ShoppingDao. This ensures all database operations are funneled through a single instance, providing thread safety and reducing resource usage.


}