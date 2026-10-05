package dev.videoplayer.app.youtube

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import dev.videoplayer.app.blocker.BlockerEngine
import dev.videoplayer.app.blocker.RequestContext
import dev.videoplayer.app.downloads.DownloadPolicy
import dev.videoplayer.app.utils.Hosts
import dev.videoplayer.app.player.PlaybackController
import java.io.ByteArrayInputStream

data class PageState(
    val url: String = YoutubeUrls.HOME,
    val title: String = "YouTube",
    val progress: Int = 0,
    val loading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val error: String? = null,
    val playing: Boolean = false,
    val positionMs: Long = 0L
)

interface BrowserHost {
    fun onPageState(state: PageState)
    fun onShowCustomView(view: View, callback: WebChromeClient.CustomViewCallback)
    fun onHideCustomView()
    fun openExternal(url: String)
}

object YoutubeUrls {
    const val HOME = "https://m.youtube.com/"
    const val SUBSCRIPTIONS = "https://m.youtube.com/feed/subscriptions"
    const val LIBRARY = "https://m.youtube.com/feed/library"
    const val TRENDING = "https://m.youtube.com/feed/trending"

    fun search(query: String): String {
        val encoded = Uri.encode(query)
        return "https://m.youtube.com/results?search_query=$encoded"
    }

    fun fromExternal(raw: String): String? {
        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
        if (uri.scheme == "vnd.youtube") {
            val id = uri.schemeSpecificPart?.substringAfterLast("/").orEmpty()
            return if (id.isNotBlank()) "https://m.youtube.com/watch?v=$id" else null
        }
        val host = uri.host?.lowercase().orEmpty()
        val allowed = host == "youtu.be" || host.endsWith("youtube.com") || host.endsWith("youtube-nocookie.com")
        if (!allowed) return null
        if (uri.scheme != "https" && uri.scheme != "http") return null
        return uri.buildUpon().scheme("https").build().toString()
    }

    fun isInAppHost(host: String): Boolean {
        val h = host.lowercase()
        return h == "youtu.be" ||
            h.endsWith("youtube.com") ||
            h.endsWith("youtube-nocookie.com") ||
            h.endsWith("google.com") ||
            h.endsWith("gstatic.com") ||
            h.endsWith("googleusercontent.com") ||
            h.endsWith("ggpht.com")
    }
}

class VideoBridge(private val onState: (Boolean, Long, String) -> Unit) {
    @android.webkit.JavascriptInterface
    fun onVideoState(playing: Boolean, seconds: Double, title: String) {
        onState(playing, (seconds * 1000).toLong(), title)
    }
}

/**
 * Owns a single WebView for the process so tab switches do not destroy the YouTube session.
 */
class YoutubeSession(
    context: Context,
    private val engine: BlockerEngine,
    private val host: BrowserHost
) {
    private val appContext = context.applicationContext
    private var speed = 1f
    private var documentHost = "m.youtube.com"
    var state = PageState()
        private set

    @SuppressLint("SetJavaScriptEnabled")
    val webView: WebView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.setSupportMultipleWindows(true)
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.userAgentString = settings.userAgentString.replace("; wv", "")
        settings.loadsImagesAutomatically = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.safeBrowsingEnabled = true
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
        addJavascriptInterface(
            VideoBridge { playing, position, title ->
                state = state.copy(playing = playing, positionMs = position, title = title.ifBlank { state.title })
                PlaybackController.update(playing, position, state.title)
                host.onPageState(state)
            },
            "VPBridge"
        )
        webViewClient = FilteringClient()
        webChromeClient = Chrome()
        setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            DownloadPolicy.handle(appContext, url, userAgent, contentDisposition, mimeType)
        }
    }

    init {
        PlaybackController.attach(this)
    }

    fun control(action: String) {
        val js = when (action) {
            "play" -> "try{var v=document.querySelector('video');if(v)v.play();}catch(e){}"
            "pause" -> "try{var v=document.querySelector('video');if(v)v.pause();}catch(e){}"
            "forward" -> "try{var v=document.querySelector('video');if(v)v.currentTime=Math.min((v.duration||0),v.currentTime+10);}catch(e){}"
            "back" -> "try{var v=document.querySelector('video');if(v)v.currentTime=Math.max(0,v.currentTime-10);}catch(e){}"
            else -> "void 0"
        }
        webView.post { webView.evaluateJavascript(js, null) }
        Unit
    }

    fun holdInBackground(hold: Boolean) {
        webView.post {
            webView.onResume()
            webView.evaluateJavascript(
                "window.__vpHold=$hold;try{var v=document.querySelector('video');if($hold&&v&&v.paused)v.play();}catch(e){}",
                null
            )
        }
    }

    fun load(url: String) {
        state = state.copy(error = null, loading = true)
        host.onPageState(state)
        webView.loadUrl(url)
    }

    fun goBack() { if (webView.canGoBack()) webView.goBack() }
    fun goForward() { if (webView.canGoForward()) webView.goForward() }
    fun reload() { webView.reload() }
    fun home() { load(YoutubeUrls.HOME) }

    fun setPlaybackSpeed(value: Float) {
        speed = value
        webView.evaluateJavascript("try{var v=document.querySelector('video');if(v)v.playbackRate=$value;}catch(e){}", null)
    }

    fun clearBrowsingData() {
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        android.webkit.WebStorage.getInstance().deleteAllData()
    }

    fun release() {
        PlaybackController.detach(this)
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.destroy()
    }

    private fun publish() {
        state = state.copy(canGoBack = webView.canGoBack(), canGoForward = webView.canGoForward())
        host.onPageState(state)
    }

    private fun applyPageScripts() {
        val css = engine.cosmeticCss(documentHost)
            .replace("\\", "\\\\")
            .replace("'", "\\'")
        val js = """
            (function(){
              try {
                var style = document.getElementById('vp-cosmetic');
                if (!style) {
                  style = document.createElement('style');
                  style.id = 'vp-cosmetic';
                  (document.documentElement || document.head || document.body).appendChild(style);
                }
                style.textContent = '$css';
                var v = document.querySelector('video');
                if (v) { v.playbackRate = $speed; }
                if (!window.__vpTimer) {
                  window.__vpTimer = setInterval(function(){
                    var video = document.querySelector('video');
                    if (video && window.VPBridge) {
                      VPBridge.onVideoState(!video.paused && !video.ended, video.currentTime || 0, document.title || '');
                    }
                  }, 1000);
                }
                if (!window.__vpVis) {
                  window.__vpHold = false;
                  document.addEventListener('visibilitychange', function(e){
                    if (!window.__vpHold) return;
                    e.stopImmediatePropagation();
                    var video = document.querySelector('video');
                    if (video && video.paused) video.play().catch(function(){});
                  }, true);
                  window.__vpVis = true;
                }
                if (!window.__vpAds) {
                  var hideAds = function(){
                    var selectors = [
                      '.ytp-ad-module','.ytp-ad-overlay-container','.ytp-ad-player-overlay',
                      '.video-ads','#player-ads','.ytp-ad-text','.ytp-ad-image-overlay',
                      'ytd-ad-slot-renderer','ytd-banner-promo-renderer','ytd-in-feed-ad-layout-renderer',
                      'ytd-companion-slot-renderer','ytd-display-ad-renderer','ytd-action-companion-ad-renderer',
                      'ytd-promoted-sparkles-web-renderer','ytm-companion-slot','ytm-companion-ad-renderer',
                      'ytm-promoted-sparkles-web-renderer','ytm-promoted-sparkles-text-search-renderer',
                      'ytm-promoted-video-renderer','ytm-paid-content-overlay-renderer',
                      '.ytm-companion-slot','.ytm-promoted-sparkles-click-wrapper',
                      '.ytd-mealbar-promo-renderer','#masthead-ad'
                    ];
                    var nodes = document.querySelectorAll(selectors.join(','));
                    for (var i = 0; i < nodes.length; i++) {
                      nodes[i].style.setProperty('display', 'none', 'important');
                    }
                    var cards = document.querySelectorAll('span, div, yt-formatted-string, button');
                    for (var j = 0; j < cards.length; j++) {
                      var text = (cards[j].innerText || '').trim();
                      if (text !== 'Sponsored' && text.indexOf('Sponsored') !== 0) continue;
                      if (text.length > 90) continue;
                      var card = cards[j];
                      for (var depth = 0; depth < 8 && card && card !== document.body; depth++) {
                        var h = card.offsetHeight || 0;
                        if (h > 36 && h < 320) {
                          card.style.setProperty('display', 'none', 'important');
                          break;
                        }
                        card = card.parentElement;
                      }
                    }
                    var skip = document.querySelector('.ytp-ad-skip-button, .ytp-skip-ad-button, .ytp-ad-skip-button-modern');
                    if (skip) skip.click();
                  };
                  hideAds();
                  window.__vpAds = new MutationObserver(hideAds);
                  if (document.documentElement) {
                    window.__vpAds.observe(document.documentElement, {childList: true, subtree: true});
                  }
                  setInterval(hideAds, 700);
                }
              } catch (e) {}
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private inner class FilteringClient : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
            val url = request.url?.toString().orEmpty()
            if (url.isBlank() || url.startsWith("data:") || url.startsWith("blob:") || url.startsWith("about:")) {
                return null
            }
            val host = request.url.host?.lowercase().orEmpty()
            val accept = request.requestHeaders?.get("Accept")
            val kind = Hosts.inferKind(url, accept, request.isForMainFrame)
            val context = RequestContext(
                url = url,
                host = host,
                documentHost = documentHost,
                kind = kind,
                isMainFrame = request.isForMainFrame,
                isThirdParty = Hosts.isThirdParty(host, documentHost)
            )
            val decision = runCatching { engine.decide(context) }.getOrNull()
            if (decision?.block == true) {
                val mime = if (kind == dev.videoplayer.app.blocker.ResourceKind.SCRIPT) "text/javascript" else "text/plain"
                return WebResourceResponse(mime, "utf-8", ByteArrayInputStream(ByteArray(0)))
            }
            return null
        }

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url ?: return true
            val scheme = uri.scheme?.lowercase().orEmpty()
            if (scheme != "http" && scheme != "https" && scheme != "about") {
                return true
            }
            val host = uri.host?.lowercase().orEmpty()
            if (engine.shouldBlockPopup(host, documentHost) && !request.isForMainFrame) {
                return true
            }
            if (!YoutubeUrls.isInAppHost(host)) {
                this@YoutubeSession.host.openExternal(uri.toString())
                return true
            }
            return false
        }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            documentHost = Hosts.hostOf(url).ifBlank { documentHost }
            state = state.copy(url = url, loading = true, progress = 10, error = null)
            publish()
        }

        override fun onPageFinished(view: WebView, url: String) {
            documentHost = Hosts.hostOf(url).ifBlank { documentHost }
            state = state.copy(
                url = url,
                title = view.title?.takeIf { it.isNotBlank() && it != "about:blank" } ?: state.title,
                loading = false,
                progress = 100
            )
            applyPageScripts()
            publish()
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: android.webkit.WebResourceError
        ) {
            if (!request.isForMainFrame) return
            state = state.copy(
                loading = false,
                error = error.description?.toString() ?: "YouTube could not be loaded."
            )
            publish()
        }
    }

    private inner class Chrome : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            state = state.copy(progress = newProgress, loading = newProgress < 100)
            if (newProgress > 40) applyPageScripts()
            publish()
        }

        override fun onReceivedTitle(view: WebView, title: String?) {
            if (!title.isNullOrBlank()) {
                state = state.copy(title = title)
                publish()
            }
        }

        override fun onShowCustomView(view: View, callback: CustomViewCallback) {
            host.onShowCustomView(view, callback)
        }

        override fun onHideCustomView() {
            host.onHideCustomView()
        }

        override fun onCreateWindow(
            view: WebView,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: android.os.Message
        ): Boolean {
            if (engine.popupsEnabled) return false
            val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
            transport.webView = webView
            resultMsg.sendToTarget()
            return true
        }

        override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
            result.cancel()
            return true
        }
    }
}
