package com.fastiptv.player

sealed interface PlayerState {
    data object Idle : PlayerState

    data class Loading(
        val streamId: Int,
        val title: String? = null
    ) : PlayerState

    data class Playing(
        val streamId: Int,
        val title: String? = null
    ) : PlayerState

    data class Buffering(
        val streamId: Int,
        val bufferPercentage: Int = 0
    ) : PlayerState

    data class Error(
        val streamId: Int,
        val message: String,
        val canRetry: Boolean = true,
        val stage: Int = 4
    ) : PlayerState
}
