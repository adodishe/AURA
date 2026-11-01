package com.auratune.player.service

import android.content.Intent
import android.media.audiofx.Equalizer
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Нативный фоновый сервис воспроизведения на базе AndroidX Media3 (ExoPlayer).
 * Обеспечивает:
 * 1. Воспроизведение при выключенном экране и управление из системной шторки Android (MediaStyle).
 * 2. Пакетную буферизацию (Batch Buffering 60–120 сек), чтобы радиомодуль уходил в Doze Mode и не сажал батарею.
 * 3. Подключение аппаратного 5-полосного эквалайзера (android.media.audiofx.Equalizer) напрямую к audioSessionId.
 */
class AuraPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    lateinit var player: ExoPlayer
    var hardwareEqualizer: Equalizer? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        // Энергоэффективный буфер: накапливаем 90 секунд аудио в ОЗУ, чтобы реже будить модем
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                30_000,  // minBufferMs (30 сек)
                90_000,  // maxBufferMs (90 сек пакетной подгрузки)
                1_500,   // старт воспроизведения
                3_000    // ребуферизация
            )
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true) // Пауза при отключении наушников
            .setWakeMode(C.WAKE_MODE_NETWORK)  // Держим CPU и сеть только когда нужно
            .build()

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                setupHardwareEqualizer(audioSessionId)
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
        InstanceHolder.service = this
    }

    private fun setupHardwareEqualizer(sessionId: Int) {
        if (sessionId == C.AUDIO_SESSION_ID_UNSET) return
        try {
            hardwareEqualizer?.release()
            hardwareEqualizer = Equalizer(0, sessionId).apply {
                enabled = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setBandGain(bandIndex: Short, decibels: Float) {
        try {
            val eq = hardwareEqualizer ?: return
            if (bandIndex < eq.numberOfBands) {
                // В Android AudioFX уровень задается в миллибелах (1 дБ = 100 мБ)
                val millibels = (decibels * 100).toInt().toShort()
                val min = eq.bandLevelRange[0]
                val max = eq.bandLevelRange[1]
                eq.setBandLevel(bandIndex, millibels.coerceIn(min, max))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        hardwareEqualizer?.release()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        InstanceHolder.service = null
        super.onDestroy()
    }

    object InstanceHolder {
        var service: AuraPlaybackService? = null
    }
}
