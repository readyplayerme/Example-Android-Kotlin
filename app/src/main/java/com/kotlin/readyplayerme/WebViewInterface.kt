package com.kotlin.readyplayerme

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import android.widget.Toast

class WebViewInterface(private val context: Context) {

    @JavascriptInterface
    fun openDialog(text: String){
        // add to clipboard
        val data = ClipData.newPlainText("Ready Player Me", text)
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(data)
        Toast.makeText(context, "Url copied into clipboard.", Toast.LENGTH_SHORT).show()

        val builder = AlertDialog.Builder(context).apply {
            setTitle("Result")
            setMessage(text)
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
