package com.zakaria.muhasaba

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.webkit.*
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.webkit.WebViewAssetLoader

class MainActivity : ComponentActivity() {
    private lateinit var web: WebView
    private var chooser: ValueCallback<Array<Uri>>? = null
    private val picker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        chooser?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(r.resultCode, r.data)); chooser = null
    }

    inner class Bridge {
        @JavascriptInterface fun save(name: String, text: String) {
            try {
                val v = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, name); put(MediaStore.Downloads.MIME_TYPE, "application/json")
                }
                val u = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)
                contentResolver.openOutputStream(u!!)?.use { it.write(text.toByteArray()) }
                runOnUiThread { Toast.makeText(this@MainActivity, "تم الحفظ في مجلد التنزيلات: $name", Toast.LENGTH_LONG).show() }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@MainActivity, "تعذّر الحفظ", Toast.LENGTH_LONG).show() }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this); setContentView(web)
        val loader = WebViewAssetLoader.Builder().addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        web.settings.apply { javaScriptEnabled = true; domStorageEnabled = true; allowFileAccess = false }
        web.addJavascriptInterface(Bridge(), "Android")
        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(v: WebView, r: WebResourceRequest) = loader.shouldInterceptRequest(r.url)
        }
        web.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(w: WebView, cb: ValueCallback<Array<Uri>>, p: FileChooserParams): Boolean {
                chooser?.onReceiveValue(null); chooser = cb
                picker.launch(p.createIntent()); return true
            }
            override fun onJsAlert(v: WebView, u: String, m: String, r: JsResult): Boolean {
                AlertDialog.Builder(this@MainActivity).setMessage(m).setPositiveButton("حسنًا") { _, _ -> r.confirm() }
                    .setOnCancelListener { r.cancel() }.show(); return true
            }
            override fun onJsConfirm(v: WebView, u: String, m: String, r: JsResult): Boolean {
                AlertDialog.Builder(this@MainActivity).setMessage(m).setPositiveButton("نعم") { _, _ -> r.confirm() }
                    .setNegativeButton("إلغاء") { _, _ -> r.cancel() }.setOnCancelListener { r.cancel() }.show(); return true
            }
            override fun onJsPrompt(v: WebView, u: String, m: String, d: String?, r: JsPromptResult): Boolean {
                val e = EditText(this@MainActivity).apply { setText(d) }
                AlertDialog.Builder(this@MainActivity).setMessage(m).setView(e)
                    .setPositiveButton("موافق") { _, _ -> r.confirm(e.text.toString()) }
                    .setNegativeButton("إلغاء") { _, _ -> r.cancel() }.setOnCancelListener { r.cancel() }.show(); return true
            }
        }
        if (savedInstanceState == null) web.loadUrl("https://appassets.androidplatform.net/assets/index.html") else web.restoreState(savedInstanceState)
    }
    override fun onSaveInstanceState(o: Bundle) { super.onSaveInstanceState(o); web.saveState(o) }
}
