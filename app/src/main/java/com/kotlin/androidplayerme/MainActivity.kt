package com.kotlin.androidplayerme

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.kotlin.androidplayerme.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.createButton.setOnClickListener {
            openWebViewPage()
        }
    }

    private fun openWebViewPage() {
    }
}