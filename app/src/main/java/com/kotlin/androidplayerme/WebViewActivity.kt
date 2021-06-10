package com.kotlin.androidplayerme

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import com.kotlin.androidplayerme.databinding.ActivityWebViewBinding
import java.io.File

class WebViewActivity : AppCompatActivity() {
    companion object {
        const val IS_CREATE_NEW = "is create new"
    }
    private lateinit var binding: ActivityWebViewBinding
    private var isCreateNew = false
    
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestPermission.launch(Manifest.permission.CAMERA)
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
                    binding.progressBar.visibility = View.GONE
                    visibility = View.VISIBLE
                }
            }

            webChromeClient = object: WebChromeClient(){
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    this@WebViewActivity.filePathCallback = filePathCallback

                    fileChooserParams?.let {
                        if (it.isCaptureEnabled){
                            openFileResultContract.launch(null)
                        } else {
                            openDocumentContract.launch("image/*")
                        }
                    }
                    return true
                }

                override fun onPermissionRequest(request: PermissionRequest?) {
                    Log.d("PERMISSION", "onPermissionRequest: ${request?.resources} ")
                    request?.grant(arrayOf(Manifest.permission.CAMERA))

                }
            }
        }
    }


    private val openFileResultContract  = registerForActivityResult(ActivityResultContracts.TakePicturePreview()){
        val path = MediaStore.Images.Media.insertImage(contentResolver, it, "fromCamera.jpeg", "")
        filePathCallback?.onReceiveValue(arrayOf(Uri.parse(path)))
    }

    private val openDocumentContract = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ){
        filePathCallback?.onReceiveValue(arrayOf(it))
    }

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){ granted ->
        if (!granted){
            Toast.makeText(this, "Camera permission not granted.", Toast.LENGTH_SHORT).show()
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
                clearHistory()
                clearFormData()
                clearCache(true)
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().removeSessionCookies(null)
                CookieManager.getInstance().flush()
                WebStorage.getInstance().deleteAllData()

            }
            loadUrl("https://readyplayer.me/avatar")
        }
    }
}