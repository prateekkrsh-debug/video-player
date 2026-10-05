# Javascript interface used by the YouTube page bridge.
-keepclassmembers class dev.videoplayer.app.youtube.VideoBridge {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class dev.videoplayer.app.youtube.VideoBridge { *; }

# Filter assets and line parsing must keep their names stable in crash logs.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
