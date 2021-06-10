package com.kotlin.androidplayerme

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.kotlin.androidplayerme.databinding.ActivityWebViewBinding

class WebViewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityWebViewBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpWebView()
        setUpWebViewClient()

    }

    private fun setUpWebViewClient() {
        with(binding.webview){
            webViewClient = object: WebViewClient(){
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    handleAvatarCreated()
                }
            }
        }
    }


    private fun handleAvatarCreated() {
        // TODO add handler to display android dialog
        with(binding.webview){
            evaluateJavascript("""
                window.addEventListener("message", receiveMessage, false)
                function receiveMessage(event){
                    if (typeof event.data === "string" && event.data.indexOf("https:") !== -1) {
                        var content = document.querySelector(".content")
                        if (content) content.remove()
                        Android.showToast(event.data)
                    }
                }
            """.trimIndent(), null)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setUpWebView() {
        with(binding.webview.settings){
            javaScriptEnabled = true
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            databaseEnabled = true
            domStorageEnabled = true
            allowFileAccess = true

        }

        with(binding.webview){
            loadUrl("https://readyplayer.me/avatar")
            addJavascriptInterface(WebViewInterface(this@WebViewActivity), "Android")
        }
        // TODO, implement custom loader
    }
}