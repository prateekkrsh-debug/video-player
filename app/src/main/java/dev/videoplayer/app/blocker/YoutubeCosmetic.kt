package dev.videoplayer.app.blocker

/**
 * Cosmetic selectors for known YouTube ad slots. Applied once via a stylesheet
 * and a MutationObserver. Not a document-wide text scan.
 */
object YoutubeCosmetic {
    val selectors: List<String> = listOf(
        ".ytp-ad-module",
        ".ytp-ad-overlay-container",
        ".ytp-ad-player-overlay",
        ".ytp-ad-text",
        ".ytp-ad-image-overlay",
        ".video-ads",
        "#player-ads",
        "ytd-ad-slot-renderer",
        "ytd-banner-promo-renderer",
        "ytd-in-feed-ad-layout-renderer",
        "ytd-companion-slot-renderer",
        "ytd-display-ad-renderer",
        "ytd-action-companion-ad-renderer",
        "ytd-promoted-sparkles-web-renderer",
        "ytm-companion-slot",
        "ytm-companion-ad-renderer",
        "ytm-promoted-sparkles-web-renderer",
        "ytm-promoted-sparkles-text-search-renderer",
        "ytm-promoted-video-renderer",
        "ytm-paid-content-overlay-renderer",
        ".ytm-companion-slot",
        ".ytm-promoted-sparkles-click-wrapper",
        ".ytd-mealbar-promo-renderer",
        "#masthead-ad"
    )

    fun styleBlock(): String =
        selectors.joinToString(",") + "{display:none!important;visibility:hidden!important;}"
}
