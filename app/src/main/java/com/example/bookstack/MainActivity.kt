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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.bookstack.ui.auth.AuthViewModel
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
    private val bookScanViewModel: BookScanViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "onCreate: Starting app")

        setContent {
            BookStackTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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
                            // ✅ 認証完了後にスキャン画面を表示
                            BookScanScreen(
                                viewModel = bookScanViewModel,
                                onNavigateBack = {
                                    finish() // アプリを終了
                                }
                            )
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
