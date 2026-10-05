package dev.videoplayer.app.filters

data class FilterSource(
    val id: String,
    val title: String,
    val url: String?,
    val asset: String?,
    val category: String,
    val enabledByDefault: Boolean,
    val attribution: String
)

object FilterCatalog {
    val sources = listOf(
        FilterSource(
            id = "youtube-compat",
            title = "YouTube compatibility and ad endpoints",
            url = null,
            asset = "filters/youtube-compat.txt",
            category = "YouTube",
            enabledByDefault = true,
            attribution = "Maintained with this project. Cosmetic rules hide known ad slots; playback hosts stay allowed."
        ),
        FilterSource(
            id = "seed-ads",
            title = "Bundled advertising and tracker domains",
            url = null,
            asset = "filters/seed-ads.txt",
            category = "Ads",
            enabledByDefault = true,
            attribution = "Curated from well-known public advertising and telemetry domains so blocking works before the first list update."
        ),
        FilterSource(
            id = "easylist",
            title = "EasyList",
            url = "https://easylist.to/easylist/easylist.txt",
            asset = null,
            category = "Ads",
            enabledByDefault = true,
            attribution = "EasyList community. https://easylist.to/"
        ),
        FilterSource(
            id = "easyprivacy",
            title = "EasyPrivacy",
            url = "https://easylist.to/easylist/easyprivacy.txt",
            asset = null,
            category = "Privacy",
            enabledByDefault = true,
            attribution = "EasyList community. https://easylist.to/"
        ),
        FilterSource(
            id = "peter-lowe",
            title = "Peter Lowe's ad server list",
            url = "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=adblockplus&showintro=1&mimetype=plaintext",
            asset = null,
            category = "Ads",
            enabledByDefault = true,
            attribution = "Peter Lowe. https://pgl.yoyo.org/adservers/"
        ),
        FilterSource(
            id = "adguard-mobile",
            title = "AdGuard Mobile Ads",
            url = "https://filters.adtidy.org/extension/android/filters/11_optimized.txt",
            asset = null,
            category = "Ads",
            enabledByDefault = false,
            attribution = "AdGuard. https://adguard.com/en/filters.html"
        ),
        FilterSource(
            id = "adguard-tracking",
            title = "AdGuard Tracking Protection",
            url = "https://filters.adtidy.org/extension/android/filters/3_optimized.txt",
            asset = null,
            category = "Privacy",
            enabledByDefault = false,
            attribution = "AdGuard. https://adguard.com/en/filters.html"
        )
    )

    fun byId(id: String): FilterSource? = sources.firstOrNull { it.id == id }
}
