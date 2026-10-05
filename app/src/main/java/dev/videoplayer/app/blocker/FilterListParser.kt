package dev.videoplayer.app.blocker

/**
 * Parses a practical subset of Adblock Plus / EasyList syntax.
 * Unsupported advanced actions (scriptlets, redirects, CSP, removeparam) are skipped
 * so a malformed or exotic rule cannot take the player down.
 */
object FilterListParser {
    private val knownOptions = setOf(
        "script", "image", "stylesheet", "xmlhttprequest", "media", "subdocument",
        "other", "font", "third-party", "match-case", "document", "xhr", "ping",
        "websocket", "object"
    )

    fun parse(text: String, listId: String): CompiledFilters {
        val network = ArrayList<NetworkRule>(256)
        val domainBlocks = HashMap<String, NetworkRule>()
        val domainExceptions = HashMap<String, NetworkRule>()
        val cosmetic = ArrayList<CosmeticRule>()

        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("!") || line.startsWith("[")) return@forEach
            try {
                if (line.contains("#@#") || line.contains("##") || line.contains("#?#")) {
                    parseCosmetic(line, listId)?.let(cosmetic::add)
                } else {
                    parseNetwork(line, listId)?.let { rule ->
                        network += rule
                        val domain = rule.domainAnchor
                        if (domain != null && rule.urlContains == null && rule.urlPrefix == null &&
                            rule.regex == null && rule.kinds.isEmpty() && rule.onlyDomains.isEmpty()
                        ) {
                            if (rule.exception) domainExceptions[domain] = rule
                            else domainBlocks[domain] = rule
                        }
                    }
                }
            } catch (_: Exception) {
                // One bad rule must never fail the list.
            }
        }
        return CompiledFilters(network, domainBlocks, domainExceptions, cosmetic, setOf(listId))
    }

    private fun parseCosmetic(line: String, listId: String): CosmeticRule? {
        val exception = line.contains("#@#")
        val marker = when {
            line.contains("#@#") -> "#@#"
            line.contains("##") -> "##"
            line.contains("#?#") -> "#?#"
            else -> return null
        }
        val parts = line.split(marker, limit = 2)
        if (parts.size != 2 || parts[1].isBlank()) return null
        val selector = parts[1].trim()
        if (selector.length > 300 || selector.contains("script", ignoreCase = true)) return null
        val domainPart = parts[0]
        val domains = LinkedHashSet<String>()
        val excluded = LinkedHashSet<String>()
        if (domainPart.isNotBlank()) {
            domainPart.split(",").forEach { token ->
                val t = token.trim().lowercase()
                if (t.startsWith("~")) excluded += t.removePrefix("~")
                else if (t.isNotEmpty()) domains += t
            }
        }
        return CosmeticRule(line, listId, domains, excluded, selector, exception)
    }

    private fun parseNetwork(line: String, listId: String): NetworkRule? {
        if (line.contains("$$") || line.contains("$" + "csp") || line.contains("$" + "redirect") ||
            line.contains("$" + "removeparam") || line.contains("$" + "replace")
        ) {
            return null
        }
        val exception = line.startsWith("@@")
        var body = if (exception) line.removePrefix("@@") else line
        var options = ""
        val dollar = body.lastIndexOf('$')
        if (dollar > 0 && !body.startsWith("/") ) {
            options = body.substring(dollar + 1)
            body = body.substring(0, dollar)
        }
        if (body.isBlank()) return null

        val kinds = LinkedHashSet<ResourceKind>()
        val onlyDomains = LinkedHashSet<String>()
        val excludedDomains = LinkedHashSet<String>()
        var thirdPartyOnly = false
        var unsupported = false
        if (options.isNotEmpty()) {
            options.split(",").forEach { optRaw ->
                val opt = optRaw.trim()
                when {
                    opt.isEmpty() -> Unit
                    opt == "third-party" -> thirdPartyOnly = true
                    opt == "~third-party" -> Unit
                    opt == "script" -> kinds += ResourceKind.SCRIPT
                    opt == "image" -> kinds += ResourceKind.IMAGE
                    opt == "stylesheet" -> kinds += ResourceKind.STYLESHEET
                    opt == "xmlhttprequest" || opt == "xhr" -> kinds += ResourceKind.XHR
                    opt == "media" -> kinds += ResourceKind.MEDIA
                    opt == "subdocument" -> kinds += ResourceKind.SUBDOCUMENT
                    opt == "font" -> kinds += ResourceKind.FONT
                    opt == "other" || opt == "object" || opt == "ping" || opt == "websocket" -> kinds += ResourceKind.OTHER
                    opt == "document" -> kinds += ResourceKind.DOCUMENT
                    opt.startsWith("domain=") -> opt.removePrefix("domain=").split("|").forEach { d ->
                        val token = d.trim().lowercase()
                        if (token.startsWith("~")) excludedDomains += token.removePrefix("~")
                        else if (token.isNotEmpty()) onlyDomains += token
                    }
                    opt.startsWith("~") -> Unit
                    opt.substringBefore("=") !in knownOptions && !opt.startsWith("domain=") -> unsupported = true
                }
            }
        }
        if (unsupported) return null

        var domainAnchor: String? = null
        var urlPrefix: String? = null
        var urlContains: String? = null
        var regex: Regex? = null

        when {
            body.startsWith("/") && body.endsWith("/") && body.length > 2 -> {
                val pattern = body.substring(1, body.length - 1)
                if (pattern.length > 180) return null
                regex = Regex(pattern, RegexOption.IGNORE_CASE)
            }
            body.startsWith("||") -> {
                val rest = body.removePrefix("||").removeSuffix("^")
                val slash = rest.indexOf('/')
                if (slash >= 0) {
                    domainAnchor = rest.substring(0, slash).lowercase().removeSuffix("^")
                    urlContains = rest.substring(slash)
                } else {
                    domainAnchor = rest.lowercase().substringBefore("^").substringBefore("*")
                    if (rest.contains("*") || rest.contains("^")) {
                        urlContains = null
                    }
                }
            }
            body.startsWith("|") -> urlPrefix = body.removePrefix("|").removeSuffix("^")
            body.contains("*") -> {
                val pattern = Regex.escape(body.removeSuffix("^")).replace("\\*", ".*")
                regex = Regex(pattern, RegexOption.IGNORE_CASE)
            }
            else -> urlContains = body.removeSuffix("^")
        }
        if (domainAnchor != null && domainAnchor.contains("*")) return null
        return NetworkRule(
            raw = line,
            listId = listId,
            exception = exception,
            domainAnchor = domainAnchor?.takeIf { it.isNotBlank() },
            urlPrefix = urlPrefix,
            urlContains = urlContains?.takeIf { it.isNotBlank() },
            regex = regex,
            thirdPartyOnly = thirdPartyOnly,
            kinds = kinds,
            onlyDomains = onlyDomains,
            excludedDomains = excludedDomains
        )
    }
}
