package com.example.bookstack

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.bookstack.ui.auth.AuthViewModel
import com.example.bookstack.ui.bookdetail.BookDetailScreen
import com.example.bookstack.ui.bookdetail.BookDetailViewModel
import com.example.bookstack.ui.booklist.BookListViewModel
import com.example.bookstack.ui.booklist.BookshelfScreen
import com.example.bookstack.ui.heatmap.ReadingHeatmapScreen
import com.example.bookstack.ui.heatmap.ReadingHeatmapViewModel
import com.example.bookstack.ui.scan.BookScanScreen
import com.example.bookstack.ui.scan.BookScanViewModel
import com.example.bookstack.ui.theme.BookStackTheme
import io.github.jan.supabase.auth.status.SessionStatus
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * アプリのメイン画面を表示するActivity。
 * Koin経由でViewModelを取得し、依存性注入を活用する。
 *
 * 【重要】アプリ起動時に自動的に匿名ユーザーとしてサインインします。
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    // ✅ Koin経由でViewModelを取得（依存関係は自動注入される）
    private val authViewModel: AuthViewModel by viewModel()
    private val bookListViewModel: BookListViewModel by viewModel()
    private val bookScanViewModel: BookScanViewModel by viewModel()
    private val bookDetailViewModel: BookDetailViewModel by viewModel()
    private val readingHeatmapViewModel: ReadingHeatmapViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate: Starting app")

        setContent {
            BookStackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 画面遷移の状態管理
                    var currentScreen by remember { mutableStateOf<Screen>(Screen.Bookshelf) }

                    // 認証状態を監視
                    val sessionStatus by authViewModel.sessionStatus.collectAsState()

                    // ✅ アプリ起動時に匿名ユーザーとしてサインイン
                    LaunchedEffect(Unit) {
                        Log.d(TAG, "LaunchedEffect: Calling signInIfNeeded")
                        authViewModel.signInIfNeeded()
                    }

                    // 認証状態に応じて画面を表示
                    when (sessionStatus) {
                        is SessionStatus.Authenticated -> {
                            Log.d(TAG, "SessionStatus: Authenticated")
                            // ✅ 認証完了後に画面を表示
                            when (val screen = currentScreen) {
                                is Screen.Bookshelf -> {
                                    BookshelfScreen(
                                        viewModel = bookListViewModel,
                                        onAddBookClick = {
                                            currentScreen = Screen.Scan
                                        },
                                        onBookClick = { bookId ->
                                            currentScreen = Screen.Detail(bookId)
                                        },
                                        onHeatmapClick = {
                                            currentScreen = Screen.Heatmap
                                        }
                                    )
                                }
                                is Screen.Scan -> {
                                    BookScanScreen(
                                        viewModel = bookScanViewModel,
                                        onNavigateBack = {
                                            currentScreen = Screen.Bookshelf
                                            // スキャン画面から戻ったら本棚をリロード
                                            bookListViewModel.loadBooks()
                                        }
                                    )
                                }
                                is Screen.Detail -> {
                                    BookDetailScreen(
                                        viewModel = bookDetailViewModel,
                                        bookId = screen.bookId,
                                        onNavigateBack = {
                                            currentScreen = Screen.Bookshelf
                                            // 詳細画面から戻ったら本棚をリロード
                                            bookListViewModel.loadBooks()
                                        }
                                    )
                                }
                                is Screen.Heatmap -> {
                                    ReadingHeatmapScreen(
                                        viewModel = readingHeatmapViewModel,
                                        onNavigateBack = {
                                            currentScreen = Screen.Bookshelf
                                        }
                                    )
                                }
                            }
                        }
                        is SessionStatus.NotAuthenticated -> {
                            Log.d(TAG, "SessionStatus: NotAuthenticated - showing loading")
                            // 認証処理中はローディング表示
                            LoadingScreen()
                        }
                        else -> {
                            Log.d(TAG, "SessionStatus: Other - showing loading")
                            // その他の状態（Network接続中など）もローディング表示
                            LoadingScreen()
                        }
                    }
                }
            }
        }
    }
}

/**
 * 画面遷移を管理するシールドクラス。
 */
sealed class Screen {
    data object Bookshelf : Screen()
    data object Scan : Screen()
    data class Detail(val bookId: String) : Screen()
    data object Heatmap : Screen()
}

/**
 * ローディング画面
 */
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
