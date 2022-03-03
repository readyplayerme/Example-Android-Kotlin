package com.kotlin.readyplayerme

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import android.widget.Toast
import org.json.JSONObject

class WebViewInterface(private val context: Context) {

    @JavascriptInterface
    fun receiveData(text: String){
        // extract avatar url from received message
        var url = ""

        if(text.endsWith(".glb"))   // post message v1, will be deprecated
        {
            url = text;
        }
        else    // post message v2
        {
            val json = JSONObject(text);
            url = json.getJSONObject("data").getString("url");
        }

        // copy to clipboard
        val data = ClipData.newPlainText("Ready Player Me", url)
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.setPrimaryClip(data)
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
