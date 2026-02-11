package com.example.bookstack.data.remote.database

import com.example.bookstack.data.model.Book

/**
 * 書籍データベース操作のためのデータソースインターフェース。
 * Supabaseなどのバックエンドとの通信を抽象化する。
 */
interface BookDatabaseDataSource {
    /**
     * 書籍を新規登録する。
     * @param userId 所有ユーザーのID
     * @param book 登録する書籍情報
     * @return 成功時はResult.success(登録されたBook)、失敗時はResult.failure(Exception)
     */
    suspend fun insertBook(userId: String, book: Book): Result<Book>

    /**
     * ユーザーの全書籍を取得する。
     * @param userId 取得対象のユーザーID
     * @return 成功時はResult.success(書籍リスト)、失敗時はResult.failure(Exception)
     */
    suspend fun getAllBooks(userId: String): Result<List<Book>>

    /**
     * 特定のISBNの書籍を取得する。
     * @param userId ユーザーID
     * @param isbn 取得対象のISBN
     * @return 成功時はResult.success(Book)、失敗時はResult.failure(Exception)
     */
    suspend fun getBookByIsbn(userId: String, isbn: String): Result<Book?>

    /**
     * 書籍情報を更新する。
     * @param userId ユーザーID
     * @param book 更新する書籍情報（idが必須）
     * @return 成功時はResult.success(更新されたBook)、失敗時はResult.failure(Exception)
     */
    suspend fun updateBook(userId: String, book: Book): Result<Book>

    /**
     * 書籍を削除する。
     * @param userId ユーザーID
     * @param bookId 削除する書籍のID
     * @return 成功時はResult.success(Unit)、失敗時はResult.failure(Exception)
     */
    suspend fun deleteBook(userId: String, bookId: String): Result<Unit>
}
