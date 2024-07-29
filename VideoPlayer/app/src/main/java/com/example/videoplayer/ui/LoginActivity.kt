package com.example.videoplayer.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.videoplayer.R
import com.example.videoplayer.databinding.ActivityLoginBinding
import com.example.videoplayer.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
//     lateinit var viewModel: AppViewModel
     val viewModel by viewModels<AppViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        initializeUi()

        binding.loginButton.setOnClickListener{
            val username= binding.loginUsername.text.toString()
            val password=binding.loginPassword.text.toString()
            viewModel.login(username,password)
        }
        binding.signupRedirect.setOnClickListener{
            val intent=Intent(this,SignUpActivity::class.java)
            startActivity(intent)
            finish()
        }
        observeLogin()

    }
    private fun observeLogin(){
        viewModel.loginResult.observe(this, Observer {success ->
            if(success){
                Toast.makeText(this,"Login Sucessful",Toast.LENGTH_SHORT).show()
                val intent=Intent(this,MainActivity::class.java)
                startActivity(intent)
                finish()
            }else{
                Toast.makeText(this,"Login Failed",Toast.LENGTH_SHORT).show()
            }

        })
    }
//    private fun initializeUi(){
////        val viewModelFactory = AppViewModelFactory(applicationContext)
//        viewModel = ViewModelProvider(this).get(AppViewModel::class.java)
//    }
}