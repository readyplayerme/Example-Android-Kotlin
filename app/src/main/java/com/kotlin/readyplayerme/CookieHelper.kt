package com.kotlin.readyplayerme

import android.content.Context

class CookieHelper(context: Context) {
    private val preference = context.getSharedPreferences("PlayerMe", Context.MODE_PRIVATE)

    fun getUpdateState() = preference.getBoolean("hasCookie", false)
    fun setUpdateState(state: Boolean){
        preference.edit().putBoolean("hasCookie", state).apply()
    }
}