package com.kotlin.androidplayerme

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel

class MainActivityViewModel: ViewModel() {
    val viewState = MutableLiveData(ViewState.MAIN)
    val hasCookie = MutableLiveData(false)


//    val updateButtonDisplayed = MediatorLiveData<Boolean>().apply {
//        addSource(viewState){
//            Transformations.switchMap(hasCookie){
//
//            }
//            postValue(it == ViewState.MAIN)
//        }
//        addSource(hasCookie){
//            postValue(it)
//        }
//    }

//    val updateButtonDisplayed = MutableLiveData(false)
}