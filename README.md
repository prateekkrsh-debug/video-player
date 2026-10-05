# Video Player

Android client for browsing and watching YouTube, with a separate ad and tracker blocking engine.

The app opens on YouTube's site, keeps a normal browsing session, and filters advertising, tracking, and popup requests before they are loaded. It is not a local MP4 player and it does not download or redistribute YouTube videos.

Repository: https://github.com/prateekkrsh-debug/video-player

## What it does

- Opens YouTube home, search, subscriptions, trending, and library inside a controlled browser layer.
- Search screen with YouTube suggestion queries, then results on the home surface.
- Watch pages, fullscreen via the player chrome, portrait and landscape, and picture-in-picture.
- Leaving a playing video enters picture-in-picture. Leaving that window keeps the page player alive through a foreground media service and notification controls.
- Back, forward, and reload, plus a local history of opened YouTube pages.
- Handles `https://youtube.com`, `youtu.be`, and `vnd.youtube` links.
- Light, dark, and system themes.
- Settings for blocker lists, whitelist, playback speed, privacy clears, and block statistics.

Playback speed is applied to the page's HTML5 video element when that element exists. Quality stays on YouTube's own player menu; the app only remembers a preference. It does not call private player APIs.

## Architecture

Kotlin, Jetpack Compose, Material 3, Navigation, DataStore, coroutines, and a single retained `WebView` session.

```
app/src/main/java/dev/videoplayer/app/
  ui/            Compose shell: home, search, library, settings
  youtube/       URL policy, WebView session, request client, fullscreen chrome
  blocker/       Filter parser and decision engine (no UI)
  filters/       List catalog, download, disk cache, compile
  settings/      DataStore preferences
  history/       SQLite history
  networking/    Suggestion client
  downloads/     Refuses YouTube media; other files go to DownloadManager
  player/        Foreground media service and session bridge
  utils/         Host and resource-type helpers
```

The blocking engine does not know about Compose. The YouTube session does not parse filter lists. Either side can change without rewriting the other.

## Ad-blocking architecture

Filtering is layered:

1. Network interception in `WebViewClient.shouldInterceptRequest`.
2. URL and domain rules in Adblock Plus / EasyList syntax.
3. Tracker lists, toggled separately from general ad lists.
4. Cosmetic CSS for known ad slots, installed once per page. A MutationObserver hides matching nodes as they are added. There is no interval scan and no full-document text walk.
5. Page scripts are injected on page finish, not on every progress tick. Playback state comes from HTML5 `play`, `pause`, `ended`, and throttled `timeupdate` events.
6. YouTube compatibility allow-list. Media, `googlevideo.com`, player, browse, search, and heartbeat endpoints fail open so buffering is not broken.
7. Multiple-window pop-ups are disabled on the WebView. `onCreateWindow` does not spawn another view.

A blocked request returns an empty response and increments the counter. If a rule would hit a playback-critical URL, the engine allows it. A failed list download keeps the previous cached copy and never crashes the app.

Bundled lists work offline:

- `assets/filters/seed-ads.txt` — advertising and tracker domains
- `assets/filters/youtube-compat.txt` — YouTube ad endpoints and cosmetic slots

Optional remote lists, cached under app storage after a successful update:

- EasyList
- EasyPrivacy
- Peter Lowe's ad server list
- AdGuard Mobile Ads
- AdGuard Tracking Protection

Per-domain exceptions are stored in settings and checked before a block decision.

## Build

Requirements: Android Studio Ladybug or newer, JDK 17, Android SDK 35.

```bash
git clone https://github.com/prateekkrsh-debug/video-player.git
cd video-player
./gradlew :app:assembleDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`.

Release:

```bash
./gradlew :app:assembleRelease
```

Release builds use R8. Sign the APK in Android Studio before installing it on a device.

Unit check for the parser:

```bash
./gradlew :app:testDebugUnitTest
```

## Contribute

Issues and pull requests are welcome. Keep the blocker module independent of the UI. Add filter coverage with a parser test when you change rule matching. Do not add video download, stream ripping, DRM bypass, or account-security workarounds.

## Legal and technical limits

- This is an independent client. YouTube is a trademark of Google. The app loads YouTube's website; it is not affiliated with Google.
- Ad blocking can conflict with YouTube's terms. Users are responsible for how they use it.
- The app does not bypass DRM, sign-in, age gates, or paid content.
- Background playback uses a foreground media service. It does not extract or rehost the stream. YouTube can still pause if the site rejects background play.
- Picture-in-picture uses the activity window. The WebView is not paused while PiP or background playback is active.
- Some ads are stitched into the media stream. Request blocking and cosmetic hiding cannot remove those without breaking playback.
- Subscriptions and accounts use YouTube's own session cookies inside the WebView.
- Filter lists belong to their authors. See `NOTICE`.

## License

Apache License 2.0. See `LICENSE`.
