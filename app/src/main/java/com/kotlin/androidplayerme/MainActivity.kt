package com.kotlin.androidplayerme

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
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
    private var isPageLoaded = false

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

        binding.updateButton.setOnClickListener{
            openUpdateWebView()
        }
    }

    private fun openUpdateWebView() {
        viewModel.viewState.postValue(ViewState.WEBVIEW)
    }

    private fun registerViewModel() {
        // handle display and hiding update button
        var viewState = ViewState.MAIN
        var hasCookie = false

        viewModel = ViewModelProvider(this, ViewModelProvider.NewInstanceFactory()).get(MainActivityViewModel::class.java)
        viewModel.viewState.observe(this){
            viewState = it

            if (it == ViewState.MAIN){
                binding.webview.visibility = View.INVISIBLE
                if (isPageLoaded){
                    binding.createButton.visibility = View.VISIBLE
                }
                if (hasCookie){
                    binding.updateButton.visibility = View.VISIBLE
                }
            } else {
                binding.webview.visibility = View.VISIBLE
                binding.updateButton.visibility = View.INVISIBLE
                binding.createButton.visibility = View.INVISIBLE
            }
        }

        viewModel.hasCookie.observe(this){
            hasCookie = it
            if(it && viewState == ViewState.MAIN){
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
        binding.webview.reload()
    }


    private fun setUpWebViewClient() {
        with(binding.webview){
            webViewClient = object: WebViewClient(){
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    handleAvatarCreated()
                    if (
                        CookieManager.getInstance().hasCookies()
                    ) viewModel.hasCookie.postValue(true)
                    binding.progressBar.visibility = View.GONE
                    binding.createButton.visibility = View.VISIBLE
                    isPageLoaded = true
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
    private fun setUpWebView() {
        with(binding.webview.settings){
            javaScriptEnabled = true
            cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            databaseEnabled = true
            domStorageEnabled = true
            allowFileAccess = true

        }

        with(binding.webview){
            addJavascriptInterface(WebViewInterface(this@MainActivity, viewModel), "MyScript")
            loadUrl("https://readyplayer.me/avatar")
        }
        // TODO, implement custom loader
    }
}