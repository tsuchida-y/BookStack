package com.example.bookstack.ui.heatmap

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookstack.data.repository.ReadingLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn

/**
 * 読書ヒートマップ画面のViewModel。
 *
 * 責務:
 * - 指定期間の読書記録を取得
 * - 日付ごとの読書ページ数を集計
 * - UI状態の管理
 */
class ReadingHeatmapViewModel(
    private val readingLogRepository: ReadingLogRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ReadingHeatmapViewModel"
        private const val DEFAULT_DAYS = 365 // デフォルトで1年分表示
    }

    private val _uiState = MutableStateFlow<ReadingHeatmapUiState>(ReadingHeatmapUiState.Loading)
    val uiState: StateFlow<ReadingHeatmapUiState> = _uiState.asStateFlow()

    init {
        loadHeatmapData()
    }

    /**
     * ヒートマップデータを読み込む。
     *
     * @param days 過去何日分のデータを取得するか（デフォルト365日）
     */
    fun loadHeatmapData(days: Int = DEFAULT_DAYS) {
        viewModelScope.launch {
            _uiState.value = ReadingHeatmapUiState.Loading
            Log.d(TAG, "loadHeatmapData: Loading data for last $days days")

            try {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val startDate = today.minus(kotlinx.datetime.DatePeriod(days = days - 1))

                Log.d(TAG, "loadHeatmapData: Period from $startDate to $today")

                val result = readingLogRepository.getDailyReadingStats(startDate, today)

                if (result.isSuccess) {
                    val dailyStats = result.getOrNull() ?: emptyMap()
                    val totalPages = dailyStats.values.sum()
                    val totalDays = dailyStats.size

                    Log.d(TAG, "loadHeatmapData: Success - $totalDays days, $totalPages pages")

                    _uiState.value = ReadingHeatmapUiState.Success(
                        dailyStats = dailyStats,
                        totalPages = totalPages,
                        totalDays = totalDays
                    )
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e(TAG, "loadHeatmapData: Failed - $error")
                    _uiState.value = ReadingHeatmapUiState.Error("読書記録の取得に失敗しました: $error")
                }
            } catch (e: Exception) {
                Log.e(TAG, "loadHeatmapData: Exception occurred", e)
                _uiState.value = ReadingHeatmapUiState.Error("エラーが発生しました: ${e.message}")
            }
        }
    }
}
