package com.example.bookstack.ui.booklist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow
import com.example.bookstack.data.model.Book
import com.example.bookstack.ui.components.BookSpineCard

/**
 * 本棚画面（メイン画面）。
 *
 * 機能:
 * - Supabaseに保存された書籍を背表紙風に一覧表示
 * - LazyVerticalGridによるグリッドレイアウト
 * - 書籍追加ボタン（スキャン画面へ遷移）
 * - ローディング、エラー、空状態の表示
 *
 * @param viewModel BookListViewModel
 * @param onAddBookClick 書籍追加ボタンクリック時のコールバック
 * @param onBookClick 書籍タップ時のコールバック
 * @param onHeatmapClick ヒートマップボタンクリック時のコールバック
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookshelfScreen(
    viewModel: BookListViewModel,
    onAddBookClick: () -> Unit,
    onBookClick: (String) -> Unit = {},
    onHeatmapClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("本棚") },
                actions = {
                    // ヒートマップボタン
                    IconButton(onClick = onHeatmapClick) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "読書ヒートマップ"
                        )
                    }
                    // 書籍追加ボタン
                    IconButton(onClick = onAddBookClick) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "本を追加"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is BookListUiState.Initial,
                is BookListUiState.Loading -> {
                    LoadingContent()
                }
                is BookListUiState.Success -> {
                    BookshelfContent(
                        books = state.books,
                        onBookClick = onBookClick
                    )
                }
                is BookListUiState.Empty -> {
                    EmptyContent(onAddBookClick = onAddBookClick)
                }
                is BookListUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.retry() }
                    )
                }
            }
        }
    }
}

/**
 * ローディング表示。
 */
@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "本棚を読み込んでいます...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * 書籍一覧を本棚風に表示（改善版：Adaptive Grid）。
 *
 * @param books 表示する書籍リスト
 * @param onBookClick 書籍タップ時のコールバック
 */
@Composable
private fun BookshelfContent(
    books: List<Book>,
    onBookClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8D4B8)) // 濃いベージュの背景（本棚の壁）
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 80.dp), // 画面幅に応じて自動調整
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = books,
                key = { book -> book.id ?: book.isbn }
            ) { book ->
                // 棚板と背表紙を含むコンポーネント
                BookWithShelf(
                    book = book,
                    onClick = { onBookClick(book.id ?: "") }
                )
            }
        }
    }
}

/**
 * 棚板付きの背表紙コンポーネント。
 *
 * @param book 表示する書籍情報
 * @param onClick クリック時のコールバック
 */
@Composable
private fun BookWithShelf(
    book: Book,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 棚板（上部）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .shadow(elevation = 2.dp, shape = RectangleShape)
                .background(Color(0xFF654321)) // より濃い茶色
        )

        // 背表紙部分（棚の背景色）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFC19A6B)) // 濃いタンの棚背景
                .padding(horizontal = 4.dp, vertical = 8.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            BookSpineCard(
                book = book,
                onClick = onClick
            )
        }

        // 棚板（下部）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .shadow(elevation = 4.dp, shape = RectangleShape)
                .background(Color(0xFF4A2F1A)) // さらに濃い茶色
        )
    }
}

/**
 * 空状態の表示（書籍が1冊もない場合）。
 *
 * @param onAddBookClick 書籍追加ボタンクリック時のコールバック
 */
@Composable
private fun EmptyContent(onAddBookClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "📚",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "まだ本が登録されていません",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = "右上の「+」ボタンから\n本を追加してみましょう",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(
                onClick = onAddBookClick,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("本を追加する")
            }
        }
    }
}

/**
 * エラー表示。
 *
 * @param message エラーメッセージ
 * @param onRetry リトライボタンクリック時のコールバック
 */
@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "⚠️",
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = "エラーが発生しました",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("再試行")
            }
        }
    }
}
