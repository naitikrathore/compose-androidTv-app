package com.example.flows

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        Log.e("naik","oncreate")

        GlobalScope.launch(Dispatchers.Main) {
//            val data: Flow<Int> =producer()
//            delay(1000)
//            data.collect{
//                Log.e("naik"," rec1 ${it.toString()}")
//                Log.e("naitk","rcv2${Thread.currentThread().name}")
            producer()
                .onStart {

                    Log.e("nak","starting")
                }

                .onEach {
                    Log.e("nak","About to emit")
                }
                .onCompletion {
                    Log.e("nak"," completed")
                }
                .collect{
                    Log.e("nak","${it.toString()}")
                }



        }

//        }
//        GlobalScope.launch {
//            val data: Flow<Int> =producer()
//            delay(1000)
//            data.collect{
//                Log.e("naik"," rec2 ${it.toString()}")
//                Log.e("naitk","rcv1 ${Thread.currentThread().name}")
//            }
//
//        }
//        GlobalScope.launch {
//            val data: Flow<Int> =producer()
//            delay(1000)
//            data.collect{
//                Log.e("naik"," rec3 ${it.toString()}")
//                Log.e("naitk","rcv3 ${Thread.currentThread().name}")
//            }
//
//        }

//        GlobalScope.launch {
//            delay(2000)
//            job.cancel()
//        }

    }
    fun producer() = flow<Int> {
        val list = listOf(1, 2, 3, 4, 5, 6,7,8,)
        for (it in list ){
//            Log.e("naik",it.toString())
            delay(1000)
            emit(it)
        }
        Log.e("naitk","sender ${Thread.currentThread().name}")
    }
}

//check 1->what if 1 producer 2 consumer
// check2-> what if rec 2 sarts late