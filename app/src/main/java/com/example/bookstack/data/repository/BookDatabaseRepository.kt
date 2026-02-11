package com.example.bookstack.data.repository

import android.util.Log
import com.example.bookstack.data.model.Book
import com.example.bookstack.data.remote.database.BookDatabaseDataSource

/**
 * 書籍データベース操作のRepository。
 * データソースを隠蔽し、ドメイン層に対してシンプルなAPIを提供する。
 *
 * Single Source of Truth: アプリ内で書籍データにアクセスする唯一の窓口。
 */
class BookDatabaseRepository(
    private val bookDatabaseDataSource: BookDatabaseDataSource,
    private val authRepository: AuthRepository
) {
    companion object {
        private const val TAG = "BookDatabaseRepository"
    }

    /**
     * 書籍を新規登録する。
     * 現在ログイン中のユーザーIDを自動的に使用する。
     *
     * @param book 登録する書籍情報
     * @return 成功時はResult.success(登録されたBook)、失敗時はResult.failure(Exception)
     */
    suspend fun insertBook(book: Book): Result<Book> {
        val userId = authRepository.getCurrentUserId()

        Log.d(TAG, "insertBook: Attempting to get user ID")
        Log.d(TAG, "insertBook: User ID = $userId")

        if (userId == null) {
            Log.e(TAG, "insertBook: User not authenticated")
            return Result.failure(Exception("User not authenticated"))
        }

        Log.d(TAG, "insertBook: Calling bookDatabaseDataSource.insertBook with userId=$userId")
        return bookDatabaseDataSource.insertBook(userId, book)
    }

    /**
     * 現在ログイン中のユーザーの全書籍を取得する。
     *
     * @return 成功時はResult.success(書籍リスト)、失敗時はResult.failure(Exception)
     */
    suspend fun getAllBooks(): Result<List<Book>> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not authenticated"))

        return bookDatabaseDataSource.getAllBooks(userId)
    }

    /**
     * 特定のISBNの書籍を取得する。
     *
     * @param isbn 取得対象のISBN
     * @return 成功時はResult.success(Book)、失敗時はResult.failure(Exception)
     */
    suspend fun getBookByIsbn(isbn: String): Result<Book?> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not authenticated"))

        return bookDatabaseDataSource.getBookByIsbn(userId, isbn)
    }

    /**
     * 書籍情報を更新する。
     *
     * @param book 更新する書籍情報（idが必須）
     * @return 成功時はResult.success(更新されたBook)、失敗時はResult.failure(Exception)
     */
    suspend fun updateBook(book: Book): Result<Book> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not authenticated"))

        return bookDatabaseDataSource.updateBook(userId, book)
    }

    /**
     * 書籍を削除する。
     *
     * @param bookId 削除する書籍のID
     * @return 成功時はResult.success(Unit)、失敗時はResult.failure(Exception)
     */
    suspend fun deleteBook(bookId: String): Result<Unit> {
        val userId = authRepository.getCurrentUserId()
            ?: return Result.failure(Exception("User not authenticated"))

        return bookDatabaseDataSource.deleteBook(userId, bookId)
    }
}
