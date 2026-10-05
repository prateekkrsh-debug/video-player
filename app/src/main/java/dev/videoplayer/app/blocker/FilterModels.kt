package dev.videoplayer.app.blocker

enum class ResourceKind {
    DOCUMENT,
    SCRIPT,
    IMAGE,
    STYLESHEET,
    XHR,
    MEDIA,
    SUBDOCUMENT,
    FONT,
    OTHER
}

data class RequestContext(
    val url: String,
    val host: String,
    val documentHost: String,
    val kind: ResourceKind,
    val isMainFrame: Boolean,
    val isThirdParty: Boolean
)

enum class BlockReason {
    ALLOWED,
    DOMAIN,
    URL_PATTERN,
    TRACKER,
    POPUP,
    COSMETIC
}

data class BlockDecision(
    val block: Boolean,
    val reason: BlockReason,
    val listId: String? = null,
    val rule: String? = null
)

data class NetworkRule(
    val raw: String,
    val listId: String,
    val exception: Boolean,
    val domainAnchor: String?,
    val urlPrefix: String?,
    val urlContains: String?,
    val regex: Regex?,
    val thirdPartyOnly: Boolean,
    val kinds: Set<ResourceKind>,
    val onlyDomains: Set<String>,
    val excludedDomains: Set<String>
)

data class CosmeticRule(
    val raw: String,
    val listId: String,
    val domains: Set<String>,
    val excludedDomains: Set<String>,
    val selector: String,
    val exception: Boolean
)

data class CompiledFilters(
    val network: List<NetworkRule>,
    val domainBlocks: Map<String, NetworkRule>,
    val domainExceptions: Map<String, NetworkRule>,
    val cosmetic: List<CosmeticRule>,
    val sourceIds: Set<String>
) {
    companion object {
        val EMPTY = CompiledFilters(emptyList(), emptyMap(), emptyMap(), emptyList(), emptySet())
    }
}
