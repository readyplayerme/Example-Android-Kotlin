package com.kotlin.readyplayerme

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.kotlin.readyplayerme.databinding.ActivityWebViewBinding

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
                            if (hasPermissionAccess()) {
                                openCameraResultContract.launch(null)
                            } else {
                                requestPermission.launch(arrayOf(
                                        Manifest.permission.CAMERA,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ))
                            }
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


    private val openCameraResultContract  = registerForActivityResult(ActivityResultContracts.TakePicturePreview()){
        it?.let {
            Log.d("ON RESULT", "no data bitmap: ${it}")
            val path = MediaStore.Images.Media.insertImage(contentResolver, it, "fromCamera.jpeg", "")
            filePathCallback?.onReceiveValue(arrayOf(Uri.parse(path)))
        } ?: Toast.makeText(this, "No Image captured !!", Toast.LENGTH_SHORT).show()
    }

    private val openDocumentContract = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ){
        it?.let {
            filePathCallback?.onReceiveValue(arrayOf(it))
        } ?: Toast.makeText(this, "No Image Selected !!", Toast.LENGTH_SHORT).show()
    }

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ){ permissionMap ->

        if (!permissionMap.values.all { it }){
            Toast.makeText(this, "Camera permission not granted.", Toast.LENGTH_SHORT).show()
        } else {
            openCameraResultContract.launch(null)
        }

    }

    private fun hasPermissionAccess(): Boolean{
        return arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun handleAvatarCreated() {
        with(binding.webview){
            evaluateJavascript("""
                window.addEventListener("message", receiveMessage, false)
                function receiveMessage(event){                        
                    // url display, used for displaying any string that starts with "https:"                    
                    if (typeof event.data === "string" && event.data.indexOf("https:") !== -1) {
                        var content = document.querySelector(".content")
                        content.remove()
                        MyScript.openDialog(event.data)
                    }
                    else{
                        // catches only RPM related messages, displays stickers object
                        // parse incoming message into JSON, and if object has readyPlayerMe and sticker
                        // fields take stickers and print it as a string
                          
                        const data = JSON.parse(event.data)
                        
                        if(data.readyPlayerMe && data.readyPlayerMe.stickers)
                        {
                            var content = document.querySelector(".content")
                            content.remove()
                            MyScript.openDialog(JSON.stringify(data.readyPlayerMe.stickers))
                        }
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
            val url = getString(R.string.partner_url)
            loadUrl(url)
        }
    }
}