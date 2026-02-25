package com.example.bookstack.ui.heatmap

import kotlinx.datetime.LocalDate

/**
 * 読書ヒートマップ画面のUI状態。
 */
sealed interface ReadingHeatmapUiState {
    /**
     * ローディング中。
     */
    data object Loading : ReadingHeatmapUiState

    /**
     * データ読み込み成功。
     * @param dailyStats 日付ごとの読書ページ数マップ
     * @param totalPages 期間内の総読書ページ数
     * @param totalDays 読書した日数
     */
    data class Success(
        val dailyStats: Map<LocalDate, Int>,
        val totalPages: Int,
        val totalDays: Int
    ) : ReadingHeatmapUiState

    /**
     * エラー発生。
     * @param message エラーメッセージ
     */
    data class Error(val message: String) : ReadingHeatmapUiState
}
