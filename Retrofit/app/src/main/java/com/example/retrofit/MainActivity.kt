package com.example.retrofit

import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputBinding
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.retrofit.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.zip.Inflater

class MainActivity : AppCompatActivity() {
    private lateinit var binding:ActivityMainBinding
    val channel=Channel<Int>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

       val quotesAPI=RetrofitHelper.getInstane().create(QuotesAPI::class.java)
        GlobalScope.launch(Dispatchers.IO) {
           val result =quotesAPI.getQuotes(1)
            if(result!=null){

                val quoteList=result.body()
                if (quoteList!=null){
                    quoteList.results.forEach {
                        Log.d("naitik",it.content)
                    }
                }
            }
        }


        producer()
        consumer()
    }
    fun producer(){
          GlobalScope.launch(Dispatchers.IO) {
              channel.send(1)
              channel.send(2)

          }
    }
    fun consumer(){
        GlobalScope.launch (Dispatchers.Main){
            Log.e("chnl", channel.receive().toString())
            Log.e("chnl", channel.receive().toString())
            Log.e("chnl", channel.receive().toString())

        }

    }
}