package com.example.picosbsvrplayer.vr

import android.annotation.SuppressLint
import android.app.Presentation
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Display
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.ConsoleMessage
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

class VrPresentation(
  context: Context,
  display: Display,
) : Presentation(context, display) {
  lateinit var rotatingLayout: RotatingLayout
    private set
  private var webView: WebView? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    window?.setBackgroundDrawableResource(android.R.color.black)
    window?.decorView?.systemUiVisibility =
      View.SYSTEM_UI_FLAG_FULLSCREEN or
        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      window?.insetsController?.hide(WindowInsets.Type.systemBars())
    }
    rotatingLayout = RotatingLayout(context).apply { setBackgroundColor(Color.BLACK) }
    setContentView(rotatingLayout)
  }

  fun showContent(view: View) {
    destroyWebView()
    rotatingLayout.replaceContent(view)
  }

  @SuppressLint("SetJavaScriptEnabled")
  fun showYouTube(videoId: String) {
    destroyWebView()
    val player =
      WebView(context).apply {
        setBackgroundColor(Color.BLACK)
        // JavaScript is required by the official YouTube IFrame API. The page
        // contains only our fixed template and a validated 11-character ID.
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.loadsImagesAutomatically = true
        // Let YouTube size the player for the external display instead of
        // choosing a stream for a small mobile viewport and enlarging it.
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = false
        settings.setSupportZoom(false)
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        webViewClient =
          object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
              super.onPageFinished(view, url)
              view?.evaluateJavascript(FULL_SCREEN_VIDEO_SCRIPT, null)
            }

            override fun onReceivedError(
              view: WebView?,
              request: WebResourceRequest?,
              error: WebResourceError?,
            ) {
              Log.e(TAG, "YouTube WebView error: ${error?.errorCode} ${error?.description}")
            }
          }
        webChromeClient =
          object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
              Log.d(TAG, "YouTube: ${consoleMessage?.message()}")
              return true
            }
          }
        // Android WebView embeds can be rejected by YouTube's app-integrity
        // checks (the player currently reports 152-4 on this device). Loading
        // the normal watch page keeps playback in the official YouTube web
        // client while still rendering it on the PICO presentation display.
        val watchUrl = "https://www.youtube.com/watch?v=$videoId"
        loadUrl(watchUrl)
      }
    webView = player
    rotatingLayout.replaceContent(player)
  }

  fun playYouTube() {
    webView?.evaluateJavascript("document.querySelector('video')?.play();", null)
  }

  fun pauseYouTube() {
    webView?.evaluateJavascript("document.querySelector('video')?.pause();", null)
  }

  private fun destroyWebView() {
    webView?.run {
      stopLoading()
      loadUrl("about:blank")
      destroy()
    }
    webView = null
  }

  override fun dismiss() {
    destroyWebView()
    super.dismiss()
  }

  companion object {
    private const val TAG = "PicoYouTube"
    private const val FULL_SCREEN_VIDEO_SCRIPT =
      "javascript:(()=>{" +
        "let lastInfo='';" +
        "const preferred=['highres','hd2160','hd1440','hd1080','hd720'];" +
        "const fit=()=>{" +
          "const v=document.querySelector('video');if(!v)return;" +
          "document.documentElement.style.background='#000';" +
          "document.body.style.background='#000';document.body.style.overflow='hidden';" +
          "v.style.setProperty('position','fixed','important');" +
          "v.style.setProperty('inset','0','important');" +
          "v.style.setProperty('width','100vw','important');" +
          "v.style.setProperty('height','100vh','important');" +
          "v.style.setProperty('max-width','none','important');" +
          "v.style.setProperty('max-height','none','important');" +
          "v.style.setProperty('object-fit','contain','important');" +
          "v.style.setProperty('z-index','2147483647','important');" +
          "v.style.setProperty('background','#000','important');" +
          "const p=document.getElementById('movie_player');" +
          "let levels=[];try{levels=p?.getAvailableQualityLevels?.()||[];}catch(e){}" +
          "const target=preferred.find(q=>levels.includes(q));" +
          "if(target&&p){" +
            "try{p.setPlaybackQualityRange?.(target,target);p.setPlaybackQuality?.(target);}catch(e){}" +
          "}" +
          "let current='';try{current=p?.getPlaybackQuality?.()||'';}catch(e){}" +
          "const info='decoded='+v.videoWidth+'x'+v.videoHeight+' quality='+current+' target='+(target||'auto')+' available='+levels.join(',');" +
          "if(info!==lastInfo){lastInfo=info;console.log('PicoQuality '+info);}" +
        "};" +
        "fit();setInterval(fit,1500);" +
      "})()"
  }
}
