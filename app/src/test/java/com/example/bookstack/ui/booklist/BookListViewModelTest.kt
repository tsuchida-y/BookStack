package com.example.bookstack.ui.booklist

import com.example.bookstack.data.model.Book
import com.example.bookstack.data.model.BookSize
import com.example.bookstack.data.remote.auth.AuthDataSource
import com.example.bookstack.data.remote.database.BookDatabaseDataSource
import com.example.bookstack.data.repository.AuthRepository
import com.example.bookstack.data.repository.BookDatabaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * BookListViewModelの単体テスト。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BookListViewModelTest {

    private lateinit var viewModel: BookListViewModel
    private lateinit var testDataSource: TestBookDatabaseDataSource
    private lateinit var testRepository: BookDatabaseRepository

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        testDataSource = TestBookDatabaseDataSource()

        val testAuthDataSource = TestAuthDataSource()
        val testAuthRepository = AuthRepository(testAuthDataSource)
        testRepository = BookDatabaseRepository(testDataSource, testAuthRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `初期化時にLoadingとなり、書籍データ取得成功時にSuccessとなる`() = runTest {
        // Given: テストデータソースに2冊の本を設定
        val books = listOf(
            Book(
                id = "1",
                isbn = "9784873119038",
                title = "リーダブルコード",
                author = "Dustin Boswell",
                coverImageUrl = "https://example.com/cover1.jpg",
                pageCount = 260,
                bookSize = BookSize.M
            ),
            Book(
                id = "2",
                isbn = "9784297124830",
                title = "達人プログラマー",
                author = "David Thomas",
                coverImageUrl = "https://example.com/cover2.jpg",
                pageCount = 350,
                bookSize = BookSize.L
            )
        )
        testDataSource.setBooks(books)

        // When: ViewModelを初期化（コンストラクタでloadBooks()が呼ばれる）
        viewModel = BookListViewModel(testRepository)
        advanceUntilIdle()

        // Then: UI状態がSuccessとなり、書籍リストが取得できる
        val state = viewModel.uiState.first()
        assertTrue(state is BookListUiState.Success)
        assertEquals(2, (state as BookListUiState.Success).books.size)
    }

    @Test
    fun `書籍が0冊の場合、Emptyとなる`() = runTest {
        // Given: テストデータソースに空のリストを設定
        testDataSource.setBooks(emptyList())

        // When: ViewModelを初期化
        viewModel = BookListViewModel(testRepository)
        advanceUntilIdle()

        // Then: UI状態がEmptyとなる
        val state = viewModel.uiState.first()
        assertTrue(state is BookListUiState.Empty)
    }

    @Test
    fun `書籍取得に失敗した場合、Errorとなる`() = runTest {
        // Given: テストデータソースがエラーを返すように設定
        testDataSource.setShouldFail(true)

        // When: ViewModelを初期化
        viewModel = BookListViewModel(testRepository)
        advanceUntilIdle()

        // Then: UI状態がErrorとなり、エラーメッセージが設定される
        val state = viewModel.uiState.first()
        assertTrue(state is BookListUiState.Error)
        assertNotNull((state as BookListUiState.Error).message)
    }

    @Test
    fun `retry()を呼ぶと再度loadBooks()が実行される`() = runTest {
        // Given: 最初はエラーを返す設定
        testDataSource.setShouldFail(true)
        viewModel = BookListViewModel(testRepository)
        advanceUntilIdle()

        // 最初はエラー状態
        var state = viewModel.uiState.first()
        assertTrue(state is BookListUiState.Error)

        // When: エラーを解除してretry()を呼ぶ
        testDataSource.setShouldFail(false)
        val books = listOf(
            Book(
                id = "1",
                isbn = "9784873119038",
                title = "リーダブルコード",
                author = "Dustin Boswell",
                coverImageUrl = null,
                pageCount = 260,
                bookSize = BookSize.M
            )
        )
        testDataSource.setBooks(books)
        viewModel.retry()
        advanceUntilIdle()

        // Then: UI状態がSuccessに変わる
        state = viewModel.uiState.first()
        assertTrue(state is BookListUiState.Success)
        assertEquals(1, (state as BookListUiState.Success).books.size)
    }
}

/**
 * テスト用のBookDatabaseDataSource。
 * Supabaseへの接続なしで、メモリ上でデータを管理する。
 */
class TestBookDatabaseDataSource : BookDatabaseDataSource {
    private var books: List<Book> = emptyList()
    private var shouldFail: Boolean = false

    fun setBooks(books: List<Book>) {
        this.books = books
    }

    fun setShouldFail(shouldFail: Boolean) {
        this.shouldFail = shouldFail
    }

    override suspend fun insertBook(userId: String, book: Book): Result<Book> {
        return if (shouldFail) {
            Result.failure(Exception("Test error"))
        } else {
            Result.success(book.copy(id = "test-id"))
        }
    }

    override suspend fun getAllBooks(userId: String): Result<List<Book>> {
        return if (shouldFail) {
            Result.failure(Exception("Test error"))
        } else {
            Result.success(books)
        }
    }

    override suspend fun getBookByIsbn(userId: String, isbn: String): Result<Book?> {
        return if (shouldFail) {
            Result.failure(Exception("Test error"))
        } else {
            Result.success(books.find { it.isbn == isbn })
        }
    }

    override suspend fun updateBook(userId: String, book: Book): Result<Book> {
        return if (shouldFail) {
            Result.failure(Exception("Test error"))
        } else {
            Result.success(book)
        }
    }

    override suspend fun deleteBook(userId: String, bookId: String): Result<Unit> {
        return if (shouldFail) {
            Result.failure(Exception("Test error"))
        } else {
            Result.success(Unit)
        }
    }
}

/**
 * テスト用のAuthDataSource。
 * 常に認証済みの状態を返す。
 */
class TestAuthDataSource : AuthDataSource {
    override fun getCurrentUserId(): String = "test-user-id"

    override val sessionStatus: kotlinx.coroutines.flow.StateFlow<io.github.jan.supabase.auth.status.SessionStatus>
        get() = kotlinx.coroutines.flow.MutableStateFlow(
            io.github.jan.supabase.auth.status.SessionStatus.Authenticated(
                session = io.github.jan.supabase.auth.user.UserSession(
                    accessToken = "test-token",
                    refreshToken = "test-refresh-token",
                    expiresIn = 3600,
                    tokenType = "bearer",
                    user = null
                )
            )
        )

    override suspend fun signInAnonymously() {
        // 何もしない（常に認証済み）
    }

    override suspend fun signOut() {
        // 何もしない
    }
}
