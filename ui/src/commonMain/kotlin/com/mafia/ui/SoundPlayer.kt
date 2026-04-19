package com.mafia.ui

enum class SoundEvent {
    NIGHT_START,
    DAY_START,
    PLAYER_ELIMINATED,
    VOTE_CAST,
    GAME_WIN,
    GAME_LOSE,
    PHASE_CHANGE
}

expect class SoundPlayer() {
    fun play(event: SoundEvent)
    fun release()
}
