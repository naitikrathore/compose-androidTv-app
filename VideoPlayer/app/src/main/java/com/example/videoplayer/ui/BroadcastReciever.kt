package com.example.videoplayer.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.widget.Toast

    class BroadcastReciever : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.e("memoo","broadcast receiver action: "+intent?.action)
          if(intent?.action =="DATA_LOADED"){

                val data=intent.getStringExtra("data")
                Log.e("memoo","data")

                Toast.makeText(context, "Data Refreshed ${data}", Toast.LENGTH_LONG).show()
            }

        }
}