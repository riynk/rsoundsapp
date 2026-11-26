package com.rsounds.core.player

import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Owns the playback engine and exposes a system media session.
 * Queue policy, source resolution, notification customization and UI controllers
 * remain separate integration tasks.
 */
@UnstableApi
class RsoundsPlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        val player = mediaSession?.player
        mediaSession?.release()
        player?.release()
        mediaSession = null
        super.onDestroy()
    }
}
