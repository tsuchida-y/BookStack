package com.example.bookstack.ui.bookdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookstack.data.model.Book
import com.example.bookstack.data.model.ReadingLog
import com.example.bookstack.data.repository.BookDatabaseRepository
import com.example.bookstack.data.repository.ReadingLogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * 本の詳細画面のUI状態を表すSealed Interface。
 */
sealed interface BookDetailUiState {
    /**
     * ローディング中。
     */
    data object Loading : BookDetailUiState

    /**
     * 書籍データの読み込み成功。
     * @param book 書籍情報
     */
    data class Success(val book: Book) : BookDetailUiState

    /**
     * エラー発生。
     * @param message エラーメッセージ
     */
    data class Error(val message: String) : BookDetailUiState
}

/**
 * 読書ステータスを表すEnumクラス。
 */
enum class ReadingStatus(val displayName: String, val value: String) {
    UNREAD("未読", "unread"),
    READING("読書中", "reading"),
    COMPLETED("読了", "completed");

    companion object {
        fun fromValue(value: String?): ReadingStatus {
            return entries.find { it.value == value } ?: UNREAD
        }
    }
}

/**
 * 本の詳細画面のViewModel。
 *
 * 責務:
 * - 書籍情報の取得と表示
 * - 読書ステータスの変更（未読/読書中/読了）
 * - 読書進捗の記録（current_pageの更新とreading_logsへの記録）
 * - ページ数の手動修正
 * - Supabaseへの更新処理
 *
 * @param bookDatabaseRepository 書籍データベースリポジトリ
 * @param readingLogRepository 読書記録リポジトリ
 */
class BookDetailViewModel(
    private val bookDatabaseRepository: BookDatabaseRepository,
    private val readingLogRepository: ReadingLogRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BookDetailUiState>(BookDetailUiState.Loading)
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()

    /**
     * 書籍情報を読み込む。
     *
     * @param bookId 書籍のID
     */
    fun loadBook(bookId: String) {
        viewModelScope.launch {
            _uiState.value = BookDetailUiState.Loading

            bookDatabaseRepository.getAllBooks()
                .onSuccess { books ->
                    val book = books.find { it.id == bookId }
                    if (book != null) {
                        _uiState.value = BookDetailUiState.Success(book)
                    } else {
                        _uiState.value = BookDetailUiState.Error("書籍が見つかりませんでした")
                    }
                }
                .onFailure { exception ->
                    _uiState.value = BookDetailUiState.Error(
                        exception.message ?: "書籍の読み込みに失敗しました"
                    )
                }
        }
    }

    /**
     * 読書ステータスを変更する。
     *
     * @param newStatus 新しいステータス
     */
    fun updateReadingStatus(newStatus: ReadingStatus) {
        val currentState = _uiState.value
        if (currentState !is BookDetailUiState.Success) return

        viewModelScope.launch {
            val updatedBook = currentState.book.copy(
                status = newStatus.value
            )

            bookDatabaseRepository.updateBook(updatedBook)
                .onSuccess { book ->
                    _uiState.value = BookDetailUiState.Success(book)
                }
                .onFailure { exception ->
                    _uiState.value = BookDetailUiState.Error(
                        "ステータスの更新に失敗しました: ${exception.message}"
                    )
                }
        }
    }

    /**
     * ページ数を手動で修正する。
     *
     * @param newPageCount 新しいページ数
     */
    fun updatePageCount(newPageCount: Int) {
        val currentState = _uiState.value
        if (currentState !is BookDetailUiState.Success) return

        viewModelScope.launch {
            val updatedBook = currentState.book.copy(
                pageCount = newPageCount
            )

            bookDatabaseRepository.updateBook(updatedBook)
                .onSuccess { book ->
                    _uiState.value = BookDetailUiState.Success(book)
                }
                .onFailure { exception ->
                    _uiState.value = BookDetailUiState.Error(
                        "ページ数の更新に失敗しました: ${exception.message}"
                    )
                }
        }
    }

    /**
     * 書籍を削除する。
     *
     * @param bookId 削除する書籍のID
     * @param onSuccess 削除成功時のコールバック
     */
    fun deleteBook(bookId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            bookDatabaseRepository.deleteBook(bookId)
                .onSuccess {
                    onSuccess()
                }
                .onFailure { exception ->
                    _uiState.value = BookDetailUiState.Error(
                        "書籍の削除に失敗しました: ${exception.message}"
                    )
                }
        }
    }

    /**
     * 読書進捗を記録する。
     * 
     * 処理内容:
     * 1. books.current_pageを更新
     * 2. reading_logsに今日の読書記録を追加
     * 3. 総ページ数に達した場合、statusをcompletedに自動更新
     *
     * @param newCurrentPage 新しい現在ページ数
     */
    fun updateReadingProgress(newCurrentPage: Int) {
        val currentState = _uiState.value
        if (currentState !is BookDetailUiState.Success) return

        val book = currentState.book
        val bookId = book.id ?: return
        val pageCount = book.pageCount ?: return

        // 入力値のバリデーション
        if (newCurrentPage < 0 || newCurrentPage > pageCount) {
            _uiState.value = BookDetailUiState.Error(
                "ページ数は0〜${pageCount}の範囲で入力してください"
            )
            return
        }

        viewModelScope.launch {
            // 前回のページ数との差分を計算
            val pagesRead = newCurrentPage - book.currentPage
            
            // 読了判定
            val isCompleted = newCurrentPage >= pageCount
            val newStatus = if (isCompleted) "completed" else {
                // まだ読み始めていない場合はreadingに更新
                if (book.status == "unread" && newCurrentPage > 0) "reading" else book.status
            }

            // 1. books.current_pageとstatusを更新
            val updatedBook = book.copy(
                currentPage = newCurrentPage,
                status = newStatus
            )

            bookDatabaseRepository.updateBook(updatedBook)
                .onSuccess { savedBook ->
                    // 2. reading_logsに記録（進捗があった場合のみ）
                    if (pagesRead > 0) {
                        val today = Clock.System.now()
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .date

                        val readingLog = ReadingLog(
                            bookId = bookId,
                            readDate = today,
                            pagesRead = pagesRead
                        )

                        readingLogRepository.insertReadingLog(readingLog)
                            .onSuccess {
                                _uiState.value = BookDetailUiState.Success(savedBook)
                            }
                            .onFailure { exception ->
                                // 読書記録の保存に失敗してもbooksの更新は成功しているので警告のみ
                                _uiState.value = BookDetailUiState.Error(
                                    "進捗は更新されましたが、読書記録の保存に失敗しました: ${exception.message}"
                                )
                            }
                    } else {
                        // ページ数が減った場合や変化がない場合
                        _uiState.value = BookDetailUiState.Success(savedBook)
                    }
                }
                .onFailure { exception ->
                    _uiState.value = BookDetailUiState.Error(
                        "進捗の更新に失敗しました: ${exception.message}"
                    )
                }
        }
    }
}
