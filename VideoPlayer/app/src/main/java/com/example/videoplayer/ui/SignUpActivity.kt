package com.example.videoplayer.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
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
import com.example.videoplayer.databinding.ActivitySignUpBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding
    val viewModel by viewModels<AppViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        initializeUi()
        binding.signupButton.setOnClickListener{
            val username=binding.signUpUsername.text.toString()
            val password = binding.signUpPassword.text.toString()
            viewModel.signup(username,password)
        }

        binding.loginRedirect.setOnClickListener{
            val intent=Intent(this,LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
        observeSignup()
    }
    private fun observeSignup(){
        viewModel.signUpResult.observe(this, Observer { id ->
            if(id!=-1L){
                Toast.makeText(this,"SignUp Successful", Toast.LENGTH_SHORT)
                val intent=Intent(this,LoginActivity::class.java)
                startActivity(intent)
                finish()
            }else{
                Toast.makeText(this,"Signup Failed",Toast.LENGTH_SHORT)
            }
        })
    }
//    private fun initializeUi(){
////        val viewModelFactory = AppViewModelFactory(applicationContext)
//        viewModel = ViewModelProvider(this).get(AppViewModel::class.java)
//    }

}