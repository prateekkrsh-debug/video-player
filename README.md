# Video Player

Local Android video player. It scans videos already on the device, groups them by folder, and plays them. It is not a YouTube client and it does not download streams.

## What it does

- Lists device videos by folder, with a recent-count badge.
- Search and sort.
- Plays the folder queue with ExoPlayer, including common formats such as MP4, MKV, WebM, and 3GP when the device can decode them.
- Portrait and landscape controls. Controls hide after 2 seconds and return on tap.
- Brightness, volume, seek, previous, next, repeat, shuffle, speed, and lock.
- Rename and delete from the folder list. Android may ask you to confirm.
- Pencil opens the clip editor. Save writes an MP4 clip to Movies/Clips.
- Opens `video/*` files shared from other apps.

## Build

```bash
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

The debug APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Limits

- Clipping uses Android's media transformer and saves MP4. Some containers cannot be clipped even if they can be played.
- Codec support depends on the device. The app does not bundle a software decoder for every format.
