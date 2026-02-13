package com.example.bookstack.ui.booklist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookstack.data.model.Book
import com.example.bookstack.data.repository.BookDatabaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 本棚画面のUI状態を表すSealed Interface。
 */
sealed interface BookListUiState {
    /**
     * 初期状態。
     */
    data object Initial : BookListUiState

    /**
     * ローディング中。
     */
    data object Loading : BookListUiState

    /**
     * 書籍データの読み込み成功。
     * @param books 書籍リスト
     */
    data class Success(val books: List<Book>) : BookListUiState

    /**
     * 書籍が1冊もない状態。
     */
    data object Empty : BookListUiState

    /**
     * エラー発生。
     * @param message エラーメッセージ
     */
    data class Error(val message: String) : BookListUiState
}

/**
 * 本棚画面のViewModel。
 *
 * 責務:
 * - BookDatabaseRepositoryから書籍データを取得
 * - UI状態の管理（Loading, Success, Error, Empty）
 * - エラー時のリトライ処理
 *
 * @param bookDatabaseRepository 書籍データベースリポジトリ
 */
class BookListViewModel(
    private val bookDatabaseRepository: BookDatabaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BookListUiState>(BookListUiState.Initial)
    val uiState: StateFlow<BookListUiState> = _uiState.asStateFlow()

    init {
        // ViewModelの初期化時に書籍データを読み込む
        loadBooks()
    }

    /**
     * Supabaseから全書籍データを取得する。
     */
    fun loadBooks() {
        viewModelScope.launch {
            _uiState.value = BookListUiState.Loading

            bookDatabaseRepository.getAllBooks()
                .onSuccess { books ->
                    if (books.isEmpty()) {
                        _uiState.value = BookListUiState.Empty
                    } else {
                        _uiState.value = BookListUiState.Success(books)
                    }
                }
                .onFailure { exception ->
                    _uiState.value = BookListUiState.Error(
                        exception.message ?: "書籍の読み込みに失敗しました"
                    )
                }
        }
    }

    /**
     * エラー時のリトライ処理。
     * loadBooks()を再度呼び出す。
     */
    fun retry() {
        loadBooks()
    }
}
