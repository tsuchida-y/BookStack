package com.example.bookstack.data.remote.database

import com.example.bookstack.data.model.ReadingLog

/**
 * 読書記録データベース操作のためのデータソースインターフェース。
 */
interface ReadingLogDataSource {
    /**
     * 読書記録を新規登録する。
     * @param userId 所有ユーザーのID
     * @param readingLog 登録する読書記録
     * @return 成功時はResult.success(登録されたReadingLog)、失敗時はResult.failure(Exception)
     */
    suspend fun insertReadingLog(userId: String, readingLog: ReadingLog): Result<ReadingLog>

    /**
     * 特定の書籍の読書記録を取得する。
     * @param userId ユーザーID
     * @param bookId 書籍ID
     * @return 成功時はResult.success(読書記録リスト)、失敗時はResult.failure(Exception)
     */
    suspend fun getReadingLogsByBookId(userId: String, bookId: String): Result<List<ReadingLog>>
}
