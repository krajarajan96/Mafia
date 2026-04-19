package com.mafia.ui

import android.media.AudioManager
import android.media.ToneGenerator

actual class SoundPlayer actual constructor() {
    private val toneGen = try {
        ToneGenerator(AudioManager.STREAM_MUSIC, 55)
    } catch (_: Exception) { null }

    actual fun play(event: SoundEvent) {
        val gen = toneGen ?: return
        try {
            when (event) {
                SoundEvent.NIGHT_START       -> gen.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 500)
                SoundEvent.DAY_START         -> gen.startTone(ToneGenerator.TONE_PROP_BEEP2, 300)
                SoundEvent.PLAYER_ELIMINATED -> gen.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 700)
                SoundEvent.VOTE_CAST         -> gen.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
                SoundEvent.GAME_WIN          -> gen.startTone(ToneGenerator.TONE_CDMA_CALLDROP_LITE, 1000)
                SoundEvent.GAME_LOSE         -> gen.startTone(ToneGenerator.TONE_CDMA_NETWORK_BUSY, 1000)
                SoundEvent.PHASE_CHANGE      -> gen.startTone(ToneGenerator.TONE_PROP_BEEP2, 150)
            }
        } catch (_: Exception) {}
    }

    actual fun release() {
        try { toneGen?.release() } catch (_: Exception) {}
    }
}
