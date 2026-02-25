package com.example.bookstack.data.repository

import android.util.Log
import com.example.bookstack.data.model.ReadingLog
import com.example.bookstack.data.remote.database.ReadingLogDataSource
import kotlinx.datetime.LocalDate

/**
 * 読書記録操作のRepository。
 * データソースを隠蔽し、ドメイン層に対してシンプルなAPIを提供する。
 */
class ReadingLogRepository(
    private val readingLogDataSource: ReadingLogDataSource,
    private val authRepository: AuthRepository
) {
    companion object {
        private const val TAG = "ReadingLogRepository"
    }

    /**
     * 読書記録を新規登録する。
     *
     * @param readingLog 登録する読書記録
     * @return 成功時はResult.success(登録されたReadingLog)、失敗時はResult.failure(Exception)
     */
    suspend fun insertReadingLog(readingLog: ReadingLog): Result<ReadingLog> {
        val userId = authRepository.getCurrentUserId()

        if (userId == null) {
            Log.e(TAG, "insertReadingLog: User not authenticated")
            return Result.failure(Exception("User not authenticated"))
        }

        return readingLogDataSource.insertReadingLog(userId, readingLog)
    }

    /**
     * 特定の書籍の読書記録を取得する。
     *
     * @param bookId 書籍ID
     * @return 成功時はResult.success(読書記録リスト)、失敗時はResult.failure(Exception)
     */
    suspend fun getReadingLogsByBookId(bookId: String): Result<List<ReadingLog>> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not authenticated"))

        return readingLogDataSource.getReadingLogsByBookId(userId, bookId)
    }

    /**
     * 指定期間の読書記録を取得する（ヒートマップ用）。
     *
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 成功時はResult.success(読書記録リスト)、失敗時はResult.failure(Exception)
     */
    suspend fun getReadingLogsByDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<ReadingLog>> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not authenticated"))

        return readingLogDataSource.getReadingLogsByDateRange(userId, startDate, endDate)
    }

    /**
     * 日付ごとの読書ページ数を集計する（ヒートマップ用）。
     *
     * @param startDate 開始日
     * @param endDate 終了日
     * @return 成功時はResult.success(Map<日付, 総ページ数>)、失敗時はResult.failure(Exception)
     */
    suspend fun getDailyReadingStats(
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<Map<LocalDate, Int>> {
        val result = getReadingLogsByDateRange(startDate, endDate)

        return if (result.isSuccess) {
            // 日付ごとにページ数を集計
            val stats = result.getOrNull()
                ?.groupBy { it.readDate }
                ?.mapValues { (_, logs) -> logs.sumOf { it.pagesRead } }
                ?: emptyMap()

            Log.d(TAG, "getDailyReadingStats: Aggregated ${stats.size} days of data")
            Result.success(stats)
        } else {
            Log.e(TAG, "getDailyReadingStats: Failed to get logs")
            Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
        }
    }
}
