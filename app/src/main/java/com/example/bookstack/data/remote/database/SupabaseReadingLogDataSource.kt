package com.example.bookstack.data.remote.database

import android.util.Log
import com.example.bookstack.data.model.ReadingLog
import com.example.bookstack.data.model.ReadingLogDto
import com.example.bookstack.data.model.toReadingLog
import com.example.bookstack.data.model.toReadingLogDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.datetime.LocalDate

/**
 * Supabase Postgrestを使用した読書記録データベース操作の実装。
 */
class SupabaseReadingLogDataSource(
    private val supabaseClient: SupabaseClient
) : ReadingLogDataSource {

    companion object {
        private const val TABLE_NAME = "reading_logs"
        private const val TAG = "SupabaseReadingLog"
    }

    override suspend fun insertReadingLog(userId: String, readingLog: ReadingLog): Result<ReadingLog> {
        return try {
            Log.d(TAG, "insertReadingLog: Starting insert for userId=$userId, bookId=${readingLog.bookId}")

            val readingLogDto = readingLog.toReadingLogDto(userId)
            Log.d(TAG, "insertReadingLog: ReadingLogDto created: $readingLogDto")

            val insertedDto = supabaseClient
                .from(TABLE_NAME)
                .insert(readingLogDto) {
                    select()
                }
                .decodeSingle<ReadingLogDto>()

            Log.d(TAG, "insertReadingLog: Success - inserted log with id=${insertedDto.id}")
            Result.success(insertedDto.toReadingLog())
        } catch (e: Exception) {
            Log.e(TAG, "insertReadingLog: Failed", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getReadingLogsByBookId(userId: String, bookId: String): Result<List<ReadingLog>> {
        return try {
            Log.d(TAG, "getReadingLogsByBookId: Getting logs for bookId=$bookId, userId=$userId")

            val readingLogDtos = supabaseClient
                .from(TABLE_NAME)
                .select {
                    filter {
                        eq("book_id", bookId)
                    }
                }
                .decodeList<ReadingLogDto>()

            Log.d(TAG, "getReadingLogsByBookId: Success - found ${readingLogDtos.size} logs")
            val logs = readingLogDtos.map { it.toReadingLog() }
            Result.success(logs)
        } catch (e: Exception) {
            Log.e(TAG, "getReadingLogsByBookId: Failed", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * 指定された期間の読書ログを取得する
     * @param userId ユーザーID
     * @param startDate 期間の開始日（この日を含む）
     * @param endDate 期間の終了日（この日を含む）
     * @return 指定期間内の読書ログリスト
     */
    override suspend fun getReadingLogsByDateRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<ReadingLog>> {
        return try {
            Log.d(TAG, "getReadingLogsByDateRange: Getting logs from $startDate to $endDate for userId=$userId")

            val readingLogDtos = supabaseClient
                .from(TABLE_NAME)
                .select {
                    filter {
                        eq("user_id", userId)
                        gte("read_date", startDate.toString())
                        lte("read_date", endDate.toString())
                    }
                }
                .decodeList<ReadingLogDto>()

            Log.d(TAG, "getReadingLogsByDateRange: Success - found ${readingLogDtos.size} logs")
            val logs = readingLogDtos.map { it.toReadingLog() }
            Result.success(logs)
        } catch (e: Exception) {
            Log.e(TAG, "getReadingLogsByDateRange: Failed", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
