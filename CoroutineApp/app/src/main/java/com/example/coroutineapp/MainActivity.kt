package com.example.coroutineapp

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.coroutineapp.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis
import kotlin.time.measureTime

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        GlobalScope.launch(Dispatchers.IO) {
//            val netwokcall=networkdelay()
//            val networkcall2=networkdelay2()
//            Log.e("Naitik","starting coroutine from ${Thread.currentThread().name}")
//            withContext(Dispatchers.Main){
//                Log.e("Naitik","updating ui from coroutine which is from ${Thread.currentThread().name}")
//            }

        //these all will print after 6 seconds only beacuse both functions delay will add up it not like first aafter 3 sec then anpther after 3sec i
//            Log.e("Naitik","corotine says hello from ${Thread.currentThread().name}")
//            Log.e("Naitik",netwokcall)
//            Log.e("Naitik",networkcall2)
//        runBlocking {
//            launch(Dispatchers.IO){
//                delay(1000L)
//                Log.e("nai","finished coro 1 ${Thread.currentThread().name}")
//            }
//            launch (Dispatchers.IO){
//                delay(5000L)
//                Log.e("nai","finished coro 2 ${Thread.currentThread().name}")
//
//            }
//            Log.e("naitik","main thread blocked ${Thread.currentThread().name}")
//            delay(7000L)
//            Log.e("naitik","main thread released ${Thread.currentThread().name}")
//
//        }

        GlobalScope.launch {
            val result = async {
                computeResult()
            }
            println("nait result: ${result.await()}")
        }
//        Thread.sleep(2000L)


    Log.e("naitik", "hello form ${Thread.currentThread().name}")
}
    suspend fun computeResult(): Int {
        delay(1000L)
        return 42
    }

suspend fun networkdelay(): String {
    delay(5000L)
    return "this is the answer 1"
}

suspend fun networkdelay2(): String {
    delay(8000L)
    return "this is the answer 2"
}
}