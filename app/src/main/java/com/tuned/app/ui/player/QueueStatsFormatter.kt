package com.tuned.app.ui.player

import com.tuned.app.playback.PlayableTrack

object QueueStatsFormatter {
    /**
     * The queue's song count alone: "0 songs", "1 song", "58 songs".
     */
    fun formatQueueStats(songs: List<PlayableTrack>): String =
        if (songs.size == 1) "1 song" else "${songs.size} songs"
}
