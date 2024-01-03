package com.kotlin.readyplayerme

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import com.kotlin.readyplayerme.databinding.ActivityAvatarLoadedBinding
import com.kotlin.readyplayerme.databinding.ActivityMainBinding
import com.squareup.picasso.Picasso

class AvatarLoaded : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityAvatarLoadedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val imageView = findViewById<ImageView>(R.id.user_avatar)
        val imageUrl = intent.getStringExtra("user_avatar_url")

        if(imageUrl != null){
            Picasso.get().load(imageUrl).into(imageView)
        }

        binding.menu.setOnClickListener{
            openMainMenu()
        }
    }

    private fun openMainMenu(){
        val intent = Intent(this, MainActivity::class.java);
        startActivity(intent);
    }
}