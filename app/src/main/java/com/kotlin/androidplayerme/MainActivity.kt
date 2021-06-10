package com.kotlin.androidplayerme

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.lifecycle.ViewModelProvider
import com.kotlin.androidplayerme.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainActivityViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        registerViewModel()


        setUpWebView()
        setUpWebViewClient()

        binding.createButton.setOnClickListener {
            openWebViewPage()
        }
    }

    private fun registerViewModel() {
        viewModel = ViewModelProvider(this, ViewModelProvider.NewInstanceFactory()).get(MainActivityViewModel::class.java)
        viewModel.viewState.observe(this){
            if (it == ViewState.MAIN){
                binding.webview.visibility = View.INVISIBLE
                binding.createButton.visibility = View.VISIBLE
            } else {
                binding.webview.visibility = View.VISIBLE
                binding.updateButton.visibility = View.INVISIBLE
                binding.createButton.visibility = View.INVISIBLE
            }
        }

        viewModel.updateButtonDisplayed.observe(this){
            if(it){
                binding.updateButton.visibility = View.VISIBLE
            } else {
                binding.updateButton.visibility = View.INVISIBLE
            }
        }
    }

    private fun openWebViewPage() {
        viewModel.viewState.postValue(ViewState.WEBVIEW)
        CookieManager.getInstance().removeAllCookies{}
        CookieManager.getInstance().removeSessionCookies{}
    }


    private fun setUpWebViewClient() {
        with(binding.webview){
            webViewClient = object: WebViewClient(){
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    handleAvatarCreated()
                    if (
                        CookieManager.getInstance().hasCookies()
                    ) viewModel.updateButtonDisplayed.postValue(true)
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

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun setUpWebView() {
        with(binding.webview.settings){
            javaScriptEnabled = true
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            databaseEnabled = true
            domStorageEnabled = true
            allowFileAccess = true

        }

        with(binding.webview){
            addJavascriptInterface(WebViewInterface(this@MainActivity), "Android")
            loadUrl("https://readyplayer.me/avatar")
        }
        // TODO, implement custom loader
    }
}