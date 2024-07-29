package com.example.hilt

import android.util.Log
import javax.inject.Inject

interface UserRepo{
    fun saveUser(emial:String,password:String)
}


class SQLRepo @Inject constructor():UserRepo{
    override fun saveUser(emial: String, password: String) {
      Log.e("nait","user saved")
    }
}

//class FirebaseRepo @Inject constructor():UserRepo{
//    override fun saveUser(emial: String, password: String) {
//        Log.e("nait","saved in Firebase")
//    }
//}
class FirebaseRepo :UserRepo{
    override fun saveUser(emial: String, password: String) {
        Log.e("nait","saved in Firebase")
    }
}