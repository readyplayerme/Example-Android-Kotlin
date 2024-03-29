package com.kotlin.readyplayerme

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
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
        fun onOnUserUpdated(userId: String)
        fun onOnUserAuthorized(userId: String)
        fun onAssetUnlock(assetRecord: WebViewInterface.AssetRecord)
        fun onUserLogout()
    }

    companion object {
        private const val ID_KEY = "id"
        private const val ASSET_ID_KEY = "assetId"
        const val CLEAR_BROWSER_CACHE = "clear_browser_cache"
        const val URL_KEY = "url_key"
        var callback: WebViewCallback? = null

        fun setWebViewCallback(callback: WebViewCallback) {
            this.callback = callback
        }
    }

    private lateinit var binding: ActivityWebViewBinding
    private var isCreateNew = false
    
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var webViewUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCreateNew = intent.getBooleanExtra(CLEAR_BROWSER_CACHE, false)
        webViewUrl = intent.getStringExtra(URL_KEY) ?: "https://demo.readyplayer.me/avatar"
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setUpWebView(intent.getBooleanExtra(CLEAR_BROWSER_CACHE, false))
        setUpWebViewClient()
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun setUpWebView(clearBrowserCache: Boolean) {
        Log.d("RPM", "onCreate: clearBrowserCache $clearBrowserCache")
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
            if (clearBrowserCache){
                clearWebViewData()
            }
            Log.d("RPM","setUpWebView url = $webViewUrl")
            loadUrl(webViewUrl)
        }
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
                    executeJavascript()
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
            Log.d("ON RESULT", "no data bitmap: $it")
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

    private fun executeJavascript() {
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

    private fun handleWebMessage(webMessage: WebMessage) {

        when (webMessage.eventName) {
            WebViewInterface.WebViewEvents.USER_SET -> {
                val userId = requireNotNull(webMessage.data[ID_KEY]) {
                    "RPM: 'userId' cannot be null"
                }
                callback?.onOnUserSet(userId)
            }
            WebViewInterface.WebViewEvents.USER_UPDATED -> {
                val userId = requireNotNull(webMessage.data[ID_KEY]) {
                    "RPM: 'userId' cannot be null webMessage.data"
                }
                callback?.onOnUserUpdated(userId)
            }
            WebViewInterface.WebViewEvents.USER_AUTHORIZED -> {
                val userId = requireNotNull(webMessage.data[ID_KEY]) {
                    "RPM: 'userId' cannot be null webMessage.data"
                }
                callback?.onOnUserAuthorized(userId)
            }
            WebViewInterface.WebViewEvents.ASSET_UNLOCK -> {
                val userId = requireNotNull(webMessage.data[ID_KEY]) {
                    "RPM: 'id' cannot be null webMessage.data"
                }
                val assetId = requireNotNull(webMessage.data[ASSET_ID_KEY]) {
                    "RPM: 'assetId' cannot be null webMessage.data"
                }
                var assetRecord = WebViewInterface.AssetRecord(userId, assetId)
                callback?.onAssetUnlock(assetRecord)
            }
            WebViewInterface.WebViewEvents.AVATAR_EXPORT -> {
                val avatarUrl = requireNotNull(webMessage.data["url"]) {
                    "RPM: 'url' cannot be null in webMessage.data"
                    finishActivityWithFailure("RPM: avatar 'url' property not found in event data")
                }
                callback?.onAvatarExported(avatarUrl)
                finishActivityWithResult()
            }
            WebViewInterface.WebViewEvents.USER_LOGOUT -> {
                callback?.onUserLogout()
            }
        }
    }

    private fun finishActivityWithResult() {
        val resultString = "Avatar Created Successfully"

        val data = Intent()
        data.putExtra("result_key", resultString)
        setResult(Activity.RESULT_OK, data)
        finish()
    }

    private fun finishActivityWithFailure(errorMessage: String) {
        val data = Intent()
        data.putExtra("error_key", errorMessage)
        setResult(Activity.RESULT_CANCELED, data)
        finish()
    }

    fun WebView.clearWebViewData() {
        clearHistory()
        clearFormData()
        clearCache(true)
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().removeSessionCookies(null)
        CookieManager.getInstance().flush()
        WebStorage.getInstance().deleteAllData()
    }
}
