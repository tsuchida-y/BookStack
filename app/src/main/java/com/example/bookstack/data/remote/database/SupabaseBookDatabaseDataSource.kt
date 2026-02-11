package com.example.bookstack.data.remote.database

import android.util.Log
import com.example.bookstack.data.model.Book
import com.example.bookstack.data.model.BookDto
import com.example.bookstack.data.model.toBook
import com.example.bookstack.data.model.toBookDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

/**
 * Supabase Postgrestを使用した書籍データベース操作の実装。
 * RLS（Row Level Security）により、各ユーザーは自分のデータのみアクセス可能。
 */
class SupabaseBookDatabaseDataSource(
    private val supabaseClient: SupabaseClient
) : BookDatabaseDataSource {

    companion object {
        private const val TABLE_NAME = "books"
        private const val TAG = "SupabaseBookDatabase"
    }

    override suspend fun insertBook(userId: String, book: Book): Result<Book> {
        return try {
            Log.d(TAG, "insertBook: Starting insert for userId=$userId, isbn=${book.isbn}")

            // BookドメインモデルをBookDtoに変換
            val bookDto = book.toBookDto(userId)
            Log.d(TAG, "insertBook: BookDto created: $bookDto")

            // Supabaseにデータを挿入し、挿入されたレコードを取得
            Log.d(TAG, "insertBook: Calling Supabase insert")
            val insertedDto = supabaseClient
                .from(TABLE_NAME)
                .insert(bookDto) {
                    select() // 挿入後のデータを返す
                }
                .decodeSingle<BookDto>()

            Log.d(TAG, "insertBook: Success - inserted book with id=${insertedDto.id}")

            // BookDtoをBookドメインモデルに変換して返す
            Result.success(insertedDto.toBook())
        } catch (e: Exception) {
            Log.e(TAG, "insertBook: Failed", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getAllBooks(userId: String): Result<List<Book>> {
        return try {
            Log.d(TAG, "getAllBooks: Getting books for userId=$userId")

            // RLSにより、自動的にauth.uid() = user_idでフィルタリングされる
            val bookDtos = supabaseClient
                .from(TABLE_NAME)
                .select()
                .decodeList<BookDto>()

            Log.d(TAG, "getAllBooks: Success - found ${bookDtos.size} books")

            // BookDtoリストをBookドメインモデルリストに変換
            val books = bookDtos.map { it.toBook() }
            Result.success(books)
        } catch (e: Exception) {
            Log.e(TAG, "getAllBooks: Failed", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun getBookByIsbn(userId: String, isbn: String): Result<Book?> {
        return try {
            Log.d(TAG, "getBookByIsbn: Searching for isbn=$isbn, userId=$userId")

            // ISBNで検索（RLSで自動的にuser_idもチェックされる）
            val bookDtos = supabaseClient
                .from(TABLE_NAME)
                .select {
                    filter {
                        eq("isbn", isbn)
                    }
                }
                .decodeList<BookDto>()

            // 最初の結果を返す（ISBNはユニークなので通常1件）
            val book = bookDtos.firstOrNull()?.toBook()
            Log.d(TAG, "getBookByIsbn: Found book: ${book != null}")
            Result.success(book)
        } catch (e: Exception) {
            Log.e(TAG, "getBookByIsbn: Failed", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun updateBook(userId: String, book: Book): Result<Book> {
        return try {
            if (book.id == null) {
                throw IllegalArgumentException("Book ID is required for update operation")
            }

            Log.d(TAG, "updateBook: Updating book id=${book.id}, userId=$userId")

            // BookドメインモデルをBookDtoに変換
            val bookDto = book.toBookDto(userId)

            // Supabaseでデータを更新
            val updatedDto = supabaseClient
                .from(TABLE_NAME)
                .update(bookDto) {
                    filter {
                        eq("id", book.id)
                    }
                    select() // 更新後のデータを返す
                }
                .decodeSingle<BookDto>()

            Log.d(TAG, "updateBook: Success")
            Result.success(updatedDto.toBook())
        } catch (e: Exception) {
            Log.e(TAG, "updateBook: Failed", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }

    override suspend fun deleteBook(userId: String, bookId: String): Result<Unit> {
        return try {
            Log.d(TAG, "deleteBook: Deleting bookId=$bookId, userId=$userId")

            // RLSにより、自分のデータのみ削除可能
            supabaseClient
                .from(TABLE_NAME)
                .delete {
                    filter {
                        eq("id", bookId)
                    }
                }

            Log.d(TAG, "deleteBook: Success")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteBook: Failed", e)
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
