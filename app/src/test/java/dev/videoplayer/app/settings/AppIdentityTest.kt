package dev.videoplayer.app.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppIdentityTest {
    @Test
    fun aliasesMatchManifestNames() {
        assertEquals("dev.videoplayer.app.Launcher_play_VideoPlayer", AppIdentity.alias("play", "Video Player"))
        assertEquals("dev.videoplayer.app.Launcher_film_Movies", AppIdentity.alias("film", "Movies"))
        assertEquals("dev.videoplayer.app.Launcher_shield_Calculator", AppIdentity.alias("shield", "Calculator"))
        assertEquals("dev.videoplayer.app.Launcher_star_Clock", AppIdentity.alias("star", "Clock"))
    }

    @Test
    fun eachChoiceMapsToItsOwnLauncherIcon() {
        assertEquals("ic_launcher", AppIdentity.launcherIcon("play"))
        assertEquals("ic_launcher_film", AppIdentity.launcherIcon("film"))
        assertEquals("ic_launcher_shield", AppIdentity.launcherIcon("shield"))
        assertEquals("ic_launcher_star", AppIdentity.launcherIcon("star"))
    }
}
