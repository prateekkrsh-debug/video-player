package dev.videoplayer.app.blocker

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicLong

data class BlockEvent(val url: String, val host: String, val reason: BlockReason, val listId: String?)

/**
 * Process-wide filtering engine. UI and WebView only talk to this type.
 * Snapshots are swapped atomically after a list parse so requests never see a half-built ruleset.
 */
class BlockerEngine {
    private val blockedCount = AtomicLong(0)
    private val _blocked = MutableStateFlow(0L)
    val blocked: StateFlow<Long> = _blocked.asStateFlow()

    private val _recent = MutableStateFlow<List<BlockEvent>>(emptyList())
    val recent: StateFlow<List<BlockEvent>> = _recent.asStateFlow()
    private val recentBuffer = ArrayDeque<BlockEvent>()

    @Volatile
    private var snapshot: CompiledFilters = CompiledFilters.EMPTY

    @Volatile
    var enabled: Boolean = true

    @Volatile
    var trackersEnabled: Boolean = true

    @Volatile
    var popupsEnabled: Boolean = true

    @Volatile
    var cosmeticEnabled: Boolean = true

    @Volatile
    var whitelist: Set<String> = emptySet()

    fun install(filters: CompiledFilters) {
        snapshot = filters
    }

    fun resetCounter() {
        blockedCount.set(0)
        _blocked.value = 0
        synchronized(recentBuffer) {
            recentBuffer.clear()
            _recent.value = emptyList()
        }
    }

    fun decide(request: RequestContext): BlockDecision {
        if (!enabled) return BlockDecision(false, BlockReason.ALLOWED)
        if (request.isMainFrame && isWhitelisted(request.host)) {
            return BlockDecision(false, BlockReason.ALLOWED)
        }
        if (isWhitelisted(request.documentHost) && request.isMainFrame) {
            return BlockDecision(false, BlockReason.ALLOWED)
        }
        if (whitelist.any { request.host == it || request.host.endsWith(".$it") }) {
            return BlockDecision(false, BlockReason.ALLOWED)
        }
        if (YoutubeCompatibility.isPlaybackCritical(request)) {
            return BlockDecision(false, BlockReason.ALLOWED)
        }

        val filters = snapshot
        if (matchesException(filters, request)) {
            return BlockDecision(false, BlockReason.ALLOWED)
        }

        if (YoutubeCompatibility.isYoutubeAdEndpoint(request)) {
            return record(request, BlockDecision(true, BlockReason.TRACKER, "youtube-compat", "youtube-ad-endpoint"))
        }

        domainMatch(filters.domainExceptions, request)?.let {
            return BlockDecision(false, BlockReason.ALLOWED, it.listId, it.raw)
        }
        domainMatch(filters.domainBlocks, request)?.let { rule ->
            if (!trackersEnabled && rule.listId.contains("privacy")) {
                return BlockDecision(false, BlockReason.ALLOWED)
            }
            if (ruleApplies(rule, request)) {
                return record(request, BlockDecision(true, BlockReason.DOMAIN, rule.listId, rule.raw))
            }
        }

        for (rule in filters.network) {
            if (!rule.exception && ruleApplies(rule, request) && ruleMatches(rule, request)) {
                val reason = if (rule.listId.contains("privacy") || rule.listId.contains("tracking")) {
                    if (!trackersEnabled) continue
                    BlockReason.TRACKER
                } else {
                    BlockReason.URL_PATTERN
                }
                return record(request, BlockDecision(true, reason, rule.listId, rule.raw))
            }
        }
        return BlockDecision(false, BlockReason.ALLOWED)
    }

    fun cosmeticCss(documentHost: String): String {
        if (!enabled || !cosmeticEnabled) return ""
        if (whitelist.any { documentHost == it || documentHost.endsWith(".$it") }) return ""
        val filters = snapshot
        val selectors = LinkedHashSet<String>()
        val exceptions = HashSet<String>()
        for (rule in filters.cosmetic) {
            if (!cosmeticApplies(rule, documentHost)) continue
            if (rule.exception) exceptions += rule.selector else selectors += rule.selector
        }
        selectors.removeAll(exceptions)
        if (selectors.isEmpty()) return ""
        return selectors.joinToString(",") + "{display:none!important;visibility:hidden!important;}"
    }

    fun shouldBlockPopup(targetHost: String, documentHost: String): Boolean {
        if (!enabled || !popupsEnabled) return false
        if (targetHost.isBlank()) return true
        if (whitelist.any { targetHost == it || targetHost.endsWith(".$it") }) return false
        val sameFamily = targetHost == documentHost ||
            targetHost.endsWith("youtube.com") ||
            targetHost.endsWith("google.com") ||
            targetHost.endsWith("youtu.be")
        return !sameFamily
    }

    private fun record(request: RequestContext, decision: BlockDecision): BlockDecision {
        val total = blockedCount.incrementAndGet()
        _blocked.value = total
        val event = BlockEvent(request.url.take(180), request.host, decision.reason, decision.listId)
        synchronized(recentBuffer) {
            recentBuffer.addFirst(event)
            while (recentBuffer.size > 40) recentBuffer.removeLast()
            _recent.value = recentBuffer.toList()
        }
        return decision
    }

    private fun isWhitelisted(host: String): Boolean =
        whitelist.any { host == it || host.endsWith(".$it") }

    private fun matchesException(filters: CompiledFilters, request: RequestContext): Boolean {
        domainMatch(filters.domainExceptions, request)?.let { return true }
        for (rule in filters.network) {
            if (rule.exception && ruleApplies(rule, request) && ruleMatches(rule, request)) return true
        }
        return false
    }

    private fun domainMatch(index: Map<String, NetworkRule>, request: RequestContext): NetworkRule? {
        val host = request.host
        var cursor = host
        while (cursor.isNotEmpty()) {
            index[cursor]?.let { return it }
            val dot = cursor.indexOf('.')
            if (dot < 0) break
            cursor = cursor.substring(dot + 1)
        }
        return null
    }

    private fun ruleApplies(rule: NetworkRule, request: RequestContext): Boolean {
        if (rule.thirdPartyOnly && !request.isThirdParty) return false
        if (rule.kinds.isNotEmpty() && request.kind !in rule.kinds) return false
        val doc = request.documentHost
        if (rule.onlyDomains.isNotEmpty() && rule.onlyDomains.none { doc == it || doc.endsWith(".$it") }) return false
        if (rule.excludedDomains.any { doc == it || doc.endsWith(".$it") }) return false
        return true
    }

    private fun ruleMatches(rule: NetworkRule, request: RequestContext): Boolean {
        rule.domainAnchor?.let { anchor ->
            val hostOk = request.host == anchor || request.host.endsWith(".$anchor")
            if (!hostOk) return false
        }
        rule.urlPrefix?.let { if (!request.url.startsWith(it, ignoreCase = true)) return false }
        rule.urlContains?.let { if (!request.url.contains(it, ignoreCase = true)) return false }
        rule.regex?.let {
            return try {
                it.containsMatchIn(request.url)
            } catch (_: Exception) {
                false
            }
        }
        return rule.domainAnchor != null || rule.urlPrefix != null || rule.urlContains != null
    }

    private fun cosmeticApplies(rule: CosmeticRule, host: String): Boolean {
        if (rule.excludedDomains.any { host == it || host.endsWith(".$it") }) return false
        if (rule.domains.isEmpty()) return true
        return rule.domains.any { host == it || host.endsWith(".$it") }
    }
}
