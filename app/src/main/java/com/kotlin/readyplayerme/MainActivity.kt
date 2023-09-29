package com.kotlin.readyplayerme

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import com.kotlin.readyplayerme.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), WebViewActivity.WebViewCallback {
    private lateinit var binding: ActivityMainBinding
    private var urlConfig: UrlConfig = UrlConfig()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WebViewActivity.setWebViewCallback(this)
        if (CookieHelper(this).getUpdateState()){
            binding.updateButton.visibility = View.VISIBLE
        }

        binding.createButton.setOnClickListener {
            openWebViewPage(false)
        }

        binding.updateButton.setOnClickListener{
            openWebViewPage(true)
        }
    }

    private fun openWebViewPage(clearBrowserCache: Boolean) {
        val intent = Intent(this, WebViewActivity::class.java)
        intent.putExtra(WebViewActivity.CLEAR_BROWSER_CACHE, clearBrowserCache)
        intent.putExtra(WebViewActivity.URL_KEY, UrlBuilder(urlConfig).buildUrl())
        webViewActivityResultLauncher.launch(intent)
    }

    private val webViewActivityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("RPM", "Result activity run.")
        }
    }


    override fun onAvatarExported(avatarUrl: String) {
        Log.d("RPM", "Avatar Exported - Avatar URL: $avatarUrl")
        showAlert(avatarUrl)
    }

    override fun onOnUserSet(userId: String) {
        Log.d("RPM", "User Set - User ID: $userId")
    }

    override fun onOnUserUpdated(userId: String) {
        Log.d("RPM", "User Updated - User ID: $userId")
    }

    override fun onOnUserAuthorized(userId: String) {
        println("RPM: User Authorized - User ID: $userId")
        Log.d("RPM", "User Authorized - User ID: $userId")
    }

    override fun onAssetUnlock(assetRecord: WebViewInterface.AssetRecord) {
        Log.d("RPM", "Asset Unlock - Asset Record: $assetRecord")
    }

    override fun onUserLogout() {
        Log.d("RPM", "User Logout")
    }

    private fun showAlert(url: String){
        val context = this@MainActivity
        val clipboardData = ClipData.newPlainText("Ready Player Me", url)
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(clipboardData)
        Toast.makeText(context, "Url copied into clipboard.", Toast.LENGTH_SHORT).show()

        // display modal window with the avatar url
        val builder = AlertDialog.Builder(context).apply {
            setTitle("Result")
            setMessage(url)
            setPositiveButton("Ok"){ dialog, _ ->
                dialog.dismiss()
            }
        }.create()
        builder.show()
    }
}