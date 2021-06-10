package com.kotlin.androidplayerme

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import com.kotlin.androidplayerme.databinding.ActivityWebViewBinding

class WebViewActivity : AppCompatActivity() {
    companion object {
        const val IS_CREATE_NEW = "is create new"
    }
    private lateinit var binding: ActivityWebViewBinding
    private var isCreateNew = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        isCreateNew = intent.getBooleanExtra(IS_CREATE_NEW, false)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setUpWebView(intent.getBooleanExtra(IS_CREATE_NEW, false))
        setUpWebViewClient()

    }

    private fun setUpWebViewClient() {
        with(binding.webview){
            webViewClient = object: WebViewClient(){
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    handleAvatarCreated()
                    if (
                        CookieManager.getInstance().hasCookies()
                    ) CookieHelper(this@WebViewActivity).setUpdateState(true)
                }
            }
        }
    }


    private fun handleAvatarCreated() {
        with(binding.webview){
            evaluateJavascript("""
                window.addEventListener("message", receiveMessage, false)
                function receiveMessage(event){
                    if (typeof event.data === "string" && event.data.indexOf("https:") !== -1) {
                        var content = document.querySelector(".content")
                        content.remove()
                        MyScript.openDialog(event.data)
                    }
                }
            """.trimIndent(), null)
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun setUpWebView(isCreateNew: Boolean) {
        Log.i("WEBVIEWACTIVITY", "onCreate: isCreateNew $isCreateNew")
        with(binding.webview.settings){
            javaScriptEnabled = true
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            databaseEnabled = true
            domStorageEnabled = true
            allowFileAccess = true

        }

        with(binding.webview){
            addJavascriptInterface(WebViewInterface(this@WebViewActivity), "MyScript")
            if (isCreateNew){
                WebStorage.getInstance().deleteAllData()

            }
            loadUrl("https://readyplayer.me/avatar")
        }
        // TODO, implement custom loader
    }
}