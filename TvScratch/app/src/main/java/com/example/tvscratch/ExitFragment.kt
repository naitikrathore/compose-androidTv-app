package com.example.tvscratch

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.tvscratch.databinding.ExitBinding
import com.example.tvscratch.databinding.FragmentDetailBinding

class ExitFragment:Fragment() {
    private lateinit var binding: ExitBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
       binding=ExitBinding.inflate(layoutInflater)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnExit.requestFocus()
        binding.btnExit.setOnKeyListener{v, keyCode,event->
            if (event.action ==android.view.KeyEvent.ACTION_DOWN){
                when(keyCode){
                    android.view.KeyEvent.KEYCODE_DPAD_CENTER ->{
                       activity?.finish()
                        return@setOnKeyListener true
                    }
                    android.view.KeyEvent.KEYCODE_BACK ->{
                        val homeFragment=HomeFragment()
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, homeFragment)
                            .commit()
                        return@setOnKeyListener true
                    }
                    else ->false
                }
            }else{
                false
            }

        }
        binding.btnExit.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.btnExit.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                binding.btnExit.setBackgroundResource(R.drawable.button_focused)
            } else {
                binding.btnExit.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
               binding.btnExit.setBackgroundResource(R.drawable.button_unfocused)// Reset text color when focus is lost
            }
        }
        binding.btnStay.setOnKeyListener{v, keyCode,event->
            if (event.action ==android.view.KeyEvent.ACTION_DOWN){
                when(keyCode){
                    android.view.KeyEvent.KEYCODE_DPAD_CENTER ->{
                        val homeFragment=HomeFragment()
                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, homeFragment)
                            .commit()
                        return@setOnKeyListener true
                    }
                    android.view.KeyEvent.KEYCODE_BACK ->{
                        val nextFocus = v.focusSearch(View.FOCUS_LEFT)
                        nextFocus?.requestFocus()
                        return@setOnKeyListener true
                    }
                    else ->false
                }
            }else{
                false
            }

        }
        binding.btnStay.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                binding.btnStay.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                binding.btnStay.setBackgroundResource(R.drawable.button_focused)
            } else {
                binding.btnStay.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                binding.btnStay.setBackgroundResource(R.drawable.button_unfocused)// Reset text color when focus is lost
            }
        }
    }
}