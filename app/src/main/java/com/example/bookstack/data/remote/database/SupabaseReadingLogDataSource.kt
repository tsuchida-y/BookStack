package com.example.bookstack.data.remote.database

import android.util.Log
import com.example.bookstack.data.model.ReadingLog
import com.example.bookstack.data.model.ReadingLogDto
import com.example.bookstack.data.model.toReadingLog
import com.example.bookstack.data.model.toReadingLogDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

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
}
