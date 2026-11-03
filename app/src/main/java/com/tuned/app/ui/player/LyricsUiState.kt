package com.tuned.app.ui.player

import com.tuned.app.data.lyrics.LyricLine
import com.tuned.app.data.lyrics.isRtlText

sealed interface LyricsUiState {
    data object Idle : LyricsUiState
    data object Loading : LyricsUiState
    data class Success(
        val lines: List<LyricLine>,
        val isSynced: Boolean,
        val isWordSynced: Boolean = false,
        val plainLyrics: String? = null,
        val isInstrumental: Boolean = false,
        val source: String? = null,
    ) : LyricsUiState {
        // Computed once at construction: the getter used to rescan every line
        // (and the plain text) on each access, and views read it per frame.
        val isRtl: Boolean = lines.any { it.isRtl } || isRtlText(plainLyrics)
    }
    data object Empty : LyricsUiState
    data class Error(val message: String) : LyricsUiState
}
