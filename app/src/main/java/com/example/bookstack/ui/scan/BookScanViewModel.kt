package com.example.bookstack.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookstack.data.model.Book
import com.example.bookstack.data.repository.BookDatabaseRepository
import com.example.bookstack.data.repository.BookRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * バーコードスキャン画面のUI状態
 */
sealed interface BookScanUiState {
    data object Idle : BookScanUiState
    data object Scanning : BookScanUiState
    data object Loading : BookScanUiState
    data class BookFound(val book: Book) : BookScanUiState
    data class Error(val message: String) : BookScanUiState
    data object Saved : BookScanUiState
}

/**
 * バーコードスキャン機能のViewModel。
 *
 * 責務:
 * 1. ISBNコードから書籍情報を取得（BookRepository経由）
 * 2. 取得した書籍情報の確認画面表示
 * 3. Supabaseへの保存処理（BookDatabaseRepository経由）
 */
class BookScanViewModel(
    private val bookRepository: BookRepository,
    private val bookDatabaseRepository: BookDatabaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<BookScanUiState>(BookScanUiState.Idle)
    val uiState: StateFlow<BookScanUiState> = _uiState.asStateFlow()

    /**
     * ISBNコードから書籍情報を取得する。
     * OpenBD API → Google Books API の順にフォールバックして検索。
     */
    fun searchBookByIsbn(isbn: String) {
        if (isbn.isEmpty()) return

        viewModelScope.launch {
            _uiState.value = BookScanUiState.Loading

            try {
                val book = bookRepository.getBookDetails(isbn)

                if (book != null) {
                    _uiState.value = BookScanUiState.BookFound(book)
                } else {
                    _uiState.value = BookScanUiState.Error("書籍情報が見つかりませんでした（ISBN: $isbn）")
                }
            } catch (e: Exception) {
                _uiState.value = BookScanUiState.Error(
                    e.message ?: "ネットワークエラーが発生しました"
                )
            }
        }
    }

    /**
     * 取得した書籍情報をSupabaseに保存する。
     */
    fun saveBook(book: Book) {
        viewModelScope.launch {
            _uiState.value = BookScanUiState.Loading

            try {
                // Supabaseに書籍情報を保存
                val result = bookDatabaseRepository.insertBook(book)

                result.onSuccess {
                    _uiState.value = BookScanUiState.Saved
                }.onFailure { exception ->
                    _uiState.value = BookScanUiState.Error(
                        "保存に失敗しました: ${exception.message}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = BookScanUiState.Error(
                    "保存に失敗しました: ${e.message}"
                )
            }
        }
    }

    /**
     * スキャン画面に戻る
     */
    fun resetToScanning() {
        _uiState.value = BookScanUiState.Scanning
    }

    /**
     * アイドル状態に戻る
     * エラーが出た後や別の本を読み込みたい時に使用予定
     */
    fun resetToIdle() {
        _uiState.value = BookScanUiState.Idle
    }
}
