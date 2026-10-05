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
import dev.videoplayer.app.blocker.PlayerAdSignals
import dev.videoplayer.app.blocker.YoutubePlayerAdDetector
import dev.videoplayer.app.blocker.YoutubeCosmetic
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

class VideoBridge(
    private val onState: (Boolean, Long, String) -> Unit,
    private val onAd: (String, Int) -> Unit
) {
    @android.webkit.JavascriptInterface
    fun onVideoState(playing: Boolean, seconds: Double, title: String) {
        onState(playing, (seconds * 1000).toLong(), title)
    }

    @android.webkit.JavascriptInterface
    fun onPlayerSignals(videoId: String, flags: Int) {
        onAd(videoId, flags)
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
    private val adDetector = YoutubePlayerAdDetector()
    private var watchdogOn = false
    var state = PageState()
        private set

    @SuppressLint("SetJavaScriptEnabled")
    val webView: WebView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.setSupportMultipleWindows(false)
        settings.javaScriptCanOpenWindowsAutomatically = false
        settings.userAgentString = settings.userAgentString.replace("; wv", "")
        settings.loadsImagesAutomatically = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.safeBrowsingEnabled = true
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
        addJavascriptInterface(
            VideoBridge(
                { playing, position, title ->
                    state = state.copy(playing = playing, positionMs = position, title = title.ifBlank { state.title })
                    PlaybackController.update(playing, position, state.title)
                    host.onPageState(state)
                },
                { videoId, flags -> onPlayerSignals(videoId, flags) }
            ),
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

    private fun onPlayerSignals(videoId: String, flags: Int) {
        val decision = adDetector.onSignals(
            PlayerAdSignals(
                videoId = videoId,
                playerPresent = flags and 1 != 0,
                adShowing = flags and 2 != 0,
                adInterrupting = flags and 4 != 0,
                overlayVisible = flags and 8 != 0,
                adTextVisible = flags and 16 != 0,
                countdownVisible = flags and 32 != 0,
                skipButtonVisible = flags and 64 != 0,
                adModulePresent = flags and 128 != 0
            )
        )
        webView.post {
            if (decision.clickSkip) {
                webView.evaluateJavascript("window.__vpClickSkip&&window.__vpClickSkip()", null)
            }
            if (decision.watchdog != watchdogOn) {
                watchdogOn = decision.watchdog
                webView.evaluateJavascript("window.__vpWatchAd&&window.__vpWatchAd(${decision.watchdog})", null)
            }
            if (decision.hideAdChrome) {
                webView.evaluateJavascript(
                    "var p=document.querySelector('#movie_player,.html5-video-player');if(p){var n=p.querySelectorAll('.ytp-ad-player-overlay,.ytp-ad-image-overlay');for(var i=0;i<n.length;i++)n[i].style.setProperty('display','none','important');}",
                    null
                )
            }
        }
    }

    private fun publish() {
        state = state.copy(canGoBack = webView.canGoBack(), canGoForward = webView.canGoForward())
        host.onPageState(state)
    }

    private var hooksInstalledFor = ""

    private fun applyPageScripts() {
        val css = (engine.cosmeticCss(documentHost) + YoutubeCosmetic.styleBlock())
            .replace("\\", "\\\\")
            .replace("'", "\\'")
        val installHooks = hooksInstalledFor != state.url
        if (installHooks) hooksInstalledFor = state.url
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
                if (!$installHooks) return;
                var selectors = '${YoutubeCosmetic.selectors.joinToString(",")}';
                var hide = function(node){
                  if (!node || node.nodeType !== 1) return;
                  if (node.matches && node.matches(selectors)) node.style.setProperty('display','none','important');
                  if (node.querySelectorAll) {
                    var found = node.querySelectorAll(selectors);
                    for (var i = 0; i < found.length; i++) found[i].style.setProperty('display','none','important');
                  }
                };
                hide(document.documentElement);
                if (!window.__vpAds) {
                  window.__vpAds = new MutationObserver(function(mutations){
                    for (var i = 0; i < mutations.length; i++) {
                      var added = mutations[i].addedNodes;
                      for (var j = 0; j < added.length; j++) hide(added[j]);
                    }
                  });
                  if (document.documentElement) {
                    window.__vpAds.observe(document.documentElement, {childList:true, subtree:true});
                  }
                }
                var report = function(video){
                  if (!video || !window.VPBridge) return;
                  VPBridge.onVideoState(!video.paused && !video.ended, video.currentTime || 0, document.title || '');
                };
                var bind = function(video){
                  if (!video || video.__vpBound) return;
                  video.__vpBound = true;
                  video.playbackRate = $speed;
                  ['play','pause','ended','emptied'].forEach(function(name){
                    video.addEventListener(name, function(){ report(video); });
                  });
                  video.addEventListener('timeupdate', function(){
                    var now = Date.now();
                    if (now - (video.__vpAt || 0) < 2000) return;
                    video.__vpAt = now;
                    report(video);
                  });
                  report(video);
                };
                bind(document.querySelector('video'));
                if (!window.__vpWatch) {
                  window.__vpWatch = new MutationObserver(function(mutations){
                    for (var i = 0; i < mutations.length; i++) {
                      var added = mutations[i].addedNodes;
                      for (var j = 0; j < added.length; j++) {
                        var node = added[j];
                        if (!node || node.nodeType !== 1) continue;
                        if (node.tagName === 'VIDEO') bind(node);
                        else if (node.querySelectorAll) {
                          var videos = node.querySelectorAll('video');
                          for (var k = 0; k < videos.length; k++) bind(videos[k]);
                        }
                      }
                    }
                  });
                  window.__vpWatch.observe(document.documentElement, {childList:true, subtree:true});
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
                ${YoutubePlayerAdDetector.installScript()}
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
            hooksInstalledFor = ""
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
        ): Boolean = false

        override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
            result.cancel()
            return true
        }
    }
}
