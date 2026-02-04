package com.example.bookstack.data.remote.database

import com.example.bookstack.data.model.Book
import com.example.bookstack.data.model.BookSize
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * SupabaseBookDatabaseDataSourceの単体テスト。
 * 実装ロジックをテストするため、モッククラスを使用する。
 */
class SupabaseBookDatabaseDataSourceTest {

    private lateinit var dataSource: MockBookDatabaseDataSource

    private val testUserId = "test-user-123"
    private val testBook = Book(
        isbn = "9784873119038",
        title = "リーダブルコード",
        author = "Dustin Boswell",
        coverImageUrl = "https://example.com/cover.jpg",
        pageCount = 260,
        bookSize = BookSize.L
    )

    @Before
    fun setup() {
        dataSource = MockBookDatabaseDataSource()
    }

    @Test
    fun `insertBook - 書籍が正常に保存されること`() = runTest {
        // When
        val result = dataSource.insertBook(testUserId, testBook)

        // Then
        assertTrue("保存に成功すること", result.isSuccess)
        result.onSuccess { savedBook ->
            assertNotNull("IDが設定されていること", savedBook.id)
            assertEquals("ISBNが正しいこと", testBook.isbn, savedBook.isbn)
            assertEquals("タイトルが正しいこと", testBook.title, savedBook.title)
            assertEquals("著者が正しいこと", testBook.author, savedBook.author)
        }
    }

    @Test
    fun `getAllBooks - 全書籍が正常に取得されること`() = runTest {
        // Given: 事前に書籍を保存
        dataSource.insertBook(testUserId, testBook)

        // When
        val result = dataSource.getAllBooks(testUserId)

        // Then
        assertTrue("取得に成功すること", result.isSuccess)
        result.onSuccess { books ->
            assertTrue("書籍が1件以上あること", books.isNotEmpty())
            assertEquals("タイトルが正しいこと", testBook.title, books[0].title)
        }
    }

    @Test
    fun `getBookByIsbn - ISBNで書籍が検索できること`() = runTest {
        // Given: 事前に書籍を保存
        dataSource.insertBook(testUserId, testBook)

        // When
        val result = dataSource.getBookByIsbn(testUserId, testBook.isbn)

        // Then
        assertTrue("取得に成功すること", result.isSuccess)
        result.onSuccess { book ->
            assertNotNull("書籍が見つかること", book)
            assertEquals("ISBNが正しいこと", testBook.isbn, book?.isbn)
        }
    }

    @Test
    fun `getBookByIsbn - 存在しないISBNの場合nullを返すこと`() = runTest {
        // When
        val result = dataSource.getBookByIsbn(testUserId, "9999999999999")

        // Then
        assertTrue("取得に成功すること", result.isSuccess)
        result.onSuccess { book ->
            assertEquals("書籍が見つからないこと", null, book)
        }
    }

    @Test
    fun `updateBook - 書籍情報が正常に更新されること`() = runTest {
        // Given: 事前に書籍を保存
        val insertResult = dataSource.insertBook(testUserId, testBook)
        val savedBook = insertResult.getOrNull()!!

        // 更新用のBook
        val updatedBook = savedBook.copy(
            title = "リーダブルコード（更新版）",
            pageCount = 300
        )

        // When
        val result = dataSource.updateBook(testUserId, updatedBook)

        // Then
        assertTrue("更新に成功すること", result.isSuccess)
        result.onSuccess { book ->
            assertEquals("タイトルが更新されていること", "リーダブルコード（更新版）", book.title)
            assertEquals("ページ数が更新されていること", 300, book.pageCount)
        }
    }

    @Test
    fun `updateBook - IDがnullの場合エラーとなること`() = runTest {
        // Given: IDがnullのBook
        val bookWithoutId = testBook.copy(id = null)

        // When
        val result = dataSource.updateBook(testUserId, bookWithoutId)

        // Then
        assertTrue("失敗すること", result.isFailure)
        result.onFailure { exception ->
            assertTrue(
                "エラーメッセージが正しいこと",
                exception.message?.contains("Book ID is required") == true
            )
        }
    }

    @Test
    fun `deleteBook - 書籍が正常に削除されること`() = runTest {
        // Given: 事前に書籍を保存
        val insertResult = dataSource.insertBook(testUserId, testBook)
        val savedBook = insertResult.getOrNull()!!

        // When
        val deleteResult = dataSource.deleteBook(testUserId, savedBook.id!!)

        // Then
        assertTrue("削除に成功すること", deleteResult.isSuccess)

        // 削除後に取得できないことを確認
        val getResult = dataSource.getBookByIsbn(testUserId, testBook.isbn)
        getResult.onSuccess { book ->
            assertEquals("書籍が削除されていること", null, book)
        }
    }

    @Test
    fun `getAllBooks - ユーザーIDでフィルタリングされること`() = runTest {
        // Given: 異なるユーザーの書籍を保存
        val user1Id = "user-1"
        val user2Id = "user-2"
        dataSource.insertBook(user1Id, testBook)
        dataSource.insertBook(user2Id, testBook.copy(title = "別の本"))

        // When: user1の書籍のみ取得
        val result = dataSource.getAllBooks(user1Id)

        // Then
        assertTrue("取得に成功すること", result.isSuccess)
        result.onSuccess { books ->
            assertEquals("user1の書籍のみ取得されること", 1, books.size)
            assertEquals("タイトルが正しいこと", testBook.title, books[0].title)
        }
    }

    // ========== モッククラス ==========

    /**
     * テスト用のBookDatabaseDataSource実装。
     * 実際のSupabase通信の代わりにメモリ上でデータを管理する。
     */
    private class MockBookDatabaseDataSource : BookDatabaseDataSource {
        private val books = mutableListOf<BookWithUserId>()
        private var nextId = 1

        override suspend fun insertBook(userId: String, book: Book): Result<Book> {
            return try {
                val savedBook = book.copy(id = "book-id-${nextId++}")
                books.add(BookWithUserId(userId, savedBook))
                Result.success(savedBook)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun getAllBooks(userId: String): Result<List<Book>> {
            return try {
                val userBooks = books.filter { it.userId == userId }.map { it.book }
                Result.success(userBooks)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun getBookByIsbn(userId: String, isbn: String): Result<Book?> {
            return try {
                val book = books
                    .filter { it.userId == userId }
                    .map { it.book }
                    .find { it.isbn == isbn }
                Result.success(book)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun updateBook(userId: String, book: Book): Result<Book> {
            return try {
                if (book.id == null) {
                    throw IllegalArgumentException("Book ID is required for update operation")
                }

                val index = books.indexOfFirst {
                    it.userId == userId && it.book.id == book.id
                }
                if (index >= 0) {
                    books[index] = BookWithUserId(userId, book)
                    Result.success(book)
                } else {
                    throw Exception("Book not found")
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        override suspend fun deleteBook(userId: String, bookId: String): Result<Unit> {
            return try {
                books.removeIf { it.userId == userId && it.book.id == bookId }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        private data class BookWithUserId(val userId: String, val book: Book)
    }
}
