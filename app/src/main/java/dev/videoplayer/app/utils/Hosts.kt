package dev.videoplayer.app.utils

import android.net.Uri
import dev.videoplayer.app.blocker.ResourceKind

object Hosts {
    fun hostOf(url: String): String =
        runCatching { Uri.parse(url).host?.lowercase().orEmpty() }.getOrDefault("")

    fun registrable(host: String): String {
        val parts = host.split(".").filter { it.isNotBlank() }
        if (parts.size <= 2) return host
        val last = parts.last()
        val second = parts[parts.size - 2]
        val multi = last.length == 2 && second in setOf("co", "com", "org", "net", "ac", "gov")
        val keep = if (multi) 3 else 2
        return parts.takeLast(keep).joinToString(".")
    }

    fun isThirdParty(requestHost: String, documentHost: String): Boolean {
        if (requestHost.isBlank() || documentHost.isBlank()) return false
        return registrable(requestHost) != registrable(documentHost)
    }

    fun inferKind(url: String, accept: String?, isMainFrame: Boolean): ResourceKind {
        if (isMainFrame) return ResourceKind.DOCUMENT
        val path = url.lowercase().substringBefore("?")
        val a = accept.orEmpty().lowercase()
        return when {
            path.contains("videoplayback") || path.endsWith(".mp4") || path.endsWith(".m3u8") ||
                path.endsWith(".webm") || a.contains("video") -> ResourceKind.MEDIA
            path.endsWith(".js") || a.contains("javascript") -> ResourceKind.SCRIPT
            path.endsWith(".css") || a.contains("text/css") -> ResourceKind.STYLESHEET
            path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") ||
                path.endsWith(".gif") || path.endsWith(".webp") || path.endsWith(".svg") ||
                a.startsWith("image/") -> ResourceKind.IMAGE
            path.endsWith(".woff") || path.endsWith(".woff2") || path.endsWith(".ttf") -> ResourceKind.FONT
            a.contains("json") || a.contains("xml") -> ResourceKind.XHR
            else -> ResourceKind.OTHER
        }
    }
}
