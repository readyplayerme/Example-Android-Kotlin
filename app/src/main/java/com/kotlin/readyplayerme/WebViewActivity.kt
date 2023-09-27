package com.kotlin.readyplayerme

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
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
import com.kotlin.readyplayerme.WebViewInterface.WebMessage

class WebViewActivity : AppCompatActivity() {
    interface WebViewCallback {
        fun onAvatarExported(avatarUrl: String)
        fun onOnUserSet(userId: String)
        fun onOnUserAuthorized(userId: String)
        fun onAssetUnlock(assetRecord: WebViewInterface.AssetRecord)
    }

    companion object {
        const val IS_CREATE_NEW = "is create new"
        var callback: WebViewCallback? = null

        fun setWebViewCallback(callback: WebViewCallback) {
            this.callback = callback
        }
    }

    private lateinit var binding: ActivityWebViewBinding
    private var isCreateNew = false
    
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    public val urlConfig: UrlConfig = UrlConfig()

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
                    binding.progressBar.visibility = View.GONE
                    visibility = View.VISIBLE
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    handleAvatarCreated()
                    if (
                        CookieManager.getInstance().hasCookies()
                    ) CookieHelper(this@WebViewActivity).setUpdateState(true)
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
                var hasSentPostMessage = false;
                function subscribe(event) {
                    const json = parse(event);
                    const source = json.source;
                    
                    if (source !== 'readyplayerme') {
                      return;
                    }
                    
                    if (json.eventName === 'v1.frame.ready' && !hasSentPostMessage) {
                        window.postMessage(
                            JSON.stringify({
                                target: 'readyplayerme',
                                type: 'subscribe',
                                eventName: 'v1.**'
                            }),
                            '*'
                        );
                        hasSentPostMessage = true;
                    }

                    WebView.receiveData(event.data)
                }

                function parse(event) {
                    try {
                        return JSON.parse(event.data);
                    } catch (error) {
                        return null;
                    }
                }

                window.removeEventListener('message', subscribe);
                window.addEventListener('message', subscribe);
            """.trimIndent(), null)
        }
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun setUpWebView(isCreateNew: Boolean) {
        Log.i("WEBVIEWACTIVITY", "onCreate: isCreateNew $isCreateNew")
        with(binding.webview.settings){
            javaScriptEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            databaseEnabled = true
            domStorageEnabled = true
            allowFileAccess = true

        }

        with(binding.webview){
            addJavascriptInterface(WebViewInterface(this@WebViewActivity){ webMessage ->
                handleWebMessage(webMessage)
            }, "WebView")
            if (isCreateNew){
                clearWebViewData()
            }

            // Create an instance of UrlBuilder with the configured parameters
            val urlBuilder = UrlBuilder(urlConfig)

            loadUrl(urlBuilder.buildUrl())
        }
    }

    private fun handleWebMessage(webMessage: WebMessage) {
        // Handle the webMessage here, and invoke different events based on its content
        callback?.onOnUserSet(webMessage.eventName)
        when (webMessage.eventName) {
            WebViewInterface.WebViewEvents.AVATAR_EXPORT -> {
                var avatarUrl = webMessage.data["url"] as String
                println("Web Event: ${webMessage.eventName}, Avatar URL: $avatarUrl")
                callback?.onAvatarExported(avatarUrl)
            }
            WebViewInterface.WebViewEvents.USER_SET -> {
                var userId = webMessage.data["userId"] as String
                println("Web Event: ${webMessage.eventName}, UserId: $userId")
                callback?.onOnUserSet(userId)
            }
            WebViewInterface.WebViewEvents.USER_AUTHORIZED -> {
                var userId = webMessage.data["userId"] as String
                println("Web Event: ${webMessage.eventName}, UserId: $userId")
                callback?.onOnUserAuthorized(userId)
            }
            WebViewInterface.WebViewEvents.ASSET_UNLOCK -> {
                var assetRecord = webMessage.data["assetId"] as WebViewInterface.AssetRecord
                println("Web Event: ${webMessage.eventName}, AssetRecord: $assetRecord")
                callback?.onAssetUnlock(assetRecord)
            }
        }
    }

    public fun WebView.clearWebViewData() {
        clearHistory()
        clearFormData()
        clearCache(true)
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().removeSessionCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
    }
}
