package com.kotlin.androidplayerme

import android.content.Context
import android.widget.Toast

class WebViewInterface(private val context: Context) {
    fun showToast(text: String){
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }
}
