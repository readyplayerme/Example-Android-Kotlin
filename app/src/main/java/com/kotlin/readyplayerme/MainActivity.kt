package com.kotlin.readyplayerme

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.kotlin.readyplayerme.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), WebViewActivity.WebViewCallback {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WebViewActivity.setWebViewCallback(this)
        if (CookieHelper(this).getUpdateState()){
            binding.updateButton.visibility = View.VISIBLE
        }

        binding.createButton.setOnClickListener {
            openWebViewPage()
        }

        binding.updateButton.setOnClickListener{
            openUpdateWebView()
        }
    }

    private fun openUpdateWebView() {
        val intent = Intent(this, WebViewActivity::class.java)
        intent.putExtra(WebViewActivity.IS_CREATE_NEW, false)
        startActivity(intent)
    }


    private fun openWebViewPage() {
        val intent = Intent(this, WebViewActivity::class.java)
        intent.putExtra(WebViewActivity.IS_CREATE_NEW, true)
        startActivity(intent)
    }

    override fun onAvatarExported(avatarUrl: String) {
        println("Web Event: Avatar Exported - Avatar URL: $avatarUrl")
    }

    override fun onOnUserSet(userId: String) {
        println("Web Event:  User Set - User ID: $userId")
    }

    override fun onOnUserAuthorized(userId: String) {
        println("Web Event: User Authorized - User ID: $userId")
    }

    override fun onAssetUnlock(assetRecord: WebViewInterface.AssetRecord) {
        println("Web Event: Asset Unlock - Asset Record: $assetRecord")
    }

    private fun showAlert(url: String){
        var context = this@WebViewActivity;
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
                context.startActivity(
                    Intent(context, MainActivity::class.java)
                )
            }
        }.create()
        builder.show()
    }
}