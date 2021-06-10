package com.kotlin.androidplayerme

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MainActivityViewModel: ViewModel() {
    val viewState = MutableLiveData(ViewState.MAIN)
    val updateButtonDisplayed = MutableLiveData(false)
}