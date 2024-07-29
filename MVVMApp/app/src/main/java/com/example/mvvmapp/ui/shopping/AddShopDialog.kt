package com.example.mvvmapp.ui.shopping

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatDialog
import com.example.mvvmapp.data.db.entities.ShoppingItem
import com.example.mvvmapp.databinding.DialogBinding// Import your generated binding class

class AddShopDialog(context: Context,var addDialogListener:AddDialogListner) : AppCompatDialog(context) {

    private lateinit var binding: DialogBinding // Declare binding variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using the generated binding class
        binding = DialogBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvAdd.setOnClickListener{
            val name= binding.etName.text.toString()
            val amount=binding.etAmount.text.toString()
            if(name.isEmpty() || amount.isEmpty()){
                Toast.makeText(context,"please fill all",Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val item=ShoppingItem(name,amount.toInt())

        }

    }
}
