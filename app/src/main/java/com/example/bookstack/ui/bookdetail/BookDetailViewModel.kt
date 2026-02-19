package com.example.bookstack.ui.bookdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookstack.data.model.Book
import com.example.bookstack.data.repository.BookDatabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
 * - ページ数の手動修正
 * - Supabaseへの更新処理
 *
 * @param bookDatabaseRepository 書籍データベースリポジトリ
 */
class BookDetailViewModel(
    private val bookDatabaseRepository: BookDatabaseRepository
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
}
