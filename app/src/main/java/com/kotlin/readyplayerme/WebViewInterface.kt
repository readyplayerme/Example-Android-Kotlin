package com.kotlin.readyplayerme

import android.content.Context
import android.os.Parcelable
import android.webkit.JavascriptInterface
import com.google.gson.Gson


class WebViewInterface(private val context: Context, private val callback: (WebMessage) -> Unit) {

    private var isCallbackAdded = false

    data class WebMessage(
        val type: String = "",
        val source: String = "readyplayerme",
        val eventName: String = "event",
        val data: Map<String, String>
    )

    object WebViewEvents {
        const val AVATAR_EXPORT = "v1.avatar.exported"
        const val USER_SET = "v1.user.set"
        const val USER_UPDATED = "v1.user.updated"
        const val USER_AUTHORIZED = "v1.user.authorized"
        const val ASSET_UNLOCK = "v1.asset.unlock"
    }

    data class AssetRecord(
        val userId: String,
        val assetId: String
    )

    @JavascriptInterface
    fun receiveData(json: String){
        val gson = Gson()
        val webMessage = gson.fromJson(json, WebMessage::class.java)
        callback(webMessage)
    }
}
