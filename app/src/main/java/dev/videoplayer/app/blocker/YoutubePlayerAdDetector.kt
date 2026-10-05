package dev.videoplayer.app.blocker

/**
 * Decides whether the YouTube player is in an active advertisement.
 * Permanent nodes such as .ytp-ad-module, Open App, and Up Next are not enough.
 */
class YoutubePlayerAdDetector {
    var state: YoutubeAdState = YoutubeAdState.NORMAL
        private set
    var videoId: String = ""
        private set

    private var skipAttemptedForEpisode = false

    fun onSignals(signals: PlayerAdSignals): AdDecision {
        var reset = false
        if (signals.videoId.isNotBlank() && videoId.isNotBlank() && signals.videoId != videoId) {
            videoId = signals.videoId
            state = YoutubeAdState.NORMAL
            skipAttemptedForEpisode = false
            reset = true
        } else if (signals.videoId.isNotBlank()) {
            videoId = signals.videoId
        }

        val active = isActiveAd(signals)
        if (!active) {
            val finished = state == YoutubeAdState.AD_DETECTED ||
                state == YoutubeAdState.AD_WAITING_FOR_SKIP ||
                state == YoutubeAdState.AD_SKIP_AVAILABLE
            skipAttemptedForEpisode = false
            state = if (finished) YoutubeAdState.AD_FINISHED else YoutubeAdState.NORMAL
            return AdDecision(state = state, reset = reset)
        }

        if (signals.skipButtonVisible && !skipAttemptedForEpisode) {
            skipAttemptedForEpisode = true
            state = YoutubeAdState.AD_SKIP_AVAILABLE
            return AdDecision(
                state = state,
                clickSkip = true,
                hideAdChrome = true,
                watchdog = true,
                reset = reset
            )
        }
        state = if (signals.countdownVisible && !signals.skipButtonVisible) {
            YoutubeAdState.AD_WAITING_FOR_SKIP
        } else {
            YoutubeAdState.AD_DETECTED
        }
        return AdDecision(state = state, hideAdChrome = true, watchdog = true, reset = reset)
    }

    fun isActiveAd(signals: PlayerAdSignals): Boolean {
        if (!signals.playerPresent && !signals.adShowing && !signals.adInterrupting) return false
        if (signals.adShowing || signals.adInterrupting) return true
        val liveOverlay = signals.overlayVisible &&
            (signals.adTextVisible || signals.countdownVisible || signals.skipButtonVisible)
        return liveOverlay
    }

    companion object {
        fun installScript(): String = """
            (function(){
              if (window.__vpPlayerAd) return;
              window.__vpPlayerAd = true;
              var watchdog = 0;
              var last = 0;
              var player = function(){
                return document.querySelector('#movie_player, .html5-video-player, .ytp-embed');
              };
              var visible = function(node){
                if (!node) return false;
                var style = window.getComputedStyle(node);
                return style.display !== 'none' && style.visibility !== 'hidden' && node.offsetParent !== null;
              };
              var videoId = function(){
                var link = location.search || '';
                var match = link.match(/[?&]v=([^&]+)/);
                if (match) return match[1];
                var node = document.querySelector('[video-id], ytm-watch, ytd-watch-flexy');
                return (node && (node.getAttribute('video-id') || '')) || '';
              };
              var collect = function(){
                var root = player();
                var skip = root && root.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button');
                var overlay = root && root.querySelector('.ytp-ad-player-overlay');
                var text = root && root.querySelector('.ytp-ad-text, .ytp-ad-preview-text');
                var countdown = root && root.querySelector('.ytp-ad-duration-remaining, .ytp-ad-preview-container');
                var flags = 0;
                if (root) flags |= 1;
                if (root && root.classList.contains('ad-showing')) flags |= 2;
                if (root && root.classList.contains('ad-interrupting')) flags |= 4;
                if (visible(overlay)) flags |= 8;
                if (visible(text)) flags |= 16;
                if (visible(countdown)) flags |= 32;
                if (visible(skip)) flags |= 64;
                if (root && root.querySelector('.ytp-ad-module, #player-ads')) flags |= 128;
                if (window.VPBridge && VPBridge.onPlayerSignals) VPBridge.onPlayerSignals(videoId(), flags);
              };
              var schedule = function(){
                var now = Date.now();
                if (now - last < 180) return;
                last = now;
                collect();
              };
              window.__vpClickSkip = function(){
                var root = player();
                var skip = root && root.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button');
                if (skip) skip.click();
              };
              window.__vpWatchAd = function(on){
                if (watchdog) { clearInterval(watchdog); watchdog = 0; }
                if (on) watchdog = setInterval(collect, 400);
              };
              var root = player();
              if (root && window.MutationObserver) {
                new MutationObserver(schedule).observe(root, {attributes:true, childList:true, subtree:true, attributeFilter:['class','style']});
              }
              collect();
            })();
        """.trimIndent()
    }
}
