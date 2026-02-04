package com.example.bookstack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.bookstack.ui.auth.AuthScreen
import com.example.bookstack.ui.auth.AuthViewModel
import com.example.bookstack.ui.scan.BookScanScreen
import com.example.bookstack.ui.scan.BookScanViewModel
import com.example.bookstack.ui.theme.BookStackTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * アプリのメイン画面を表示するActivity。
 * Koin経由でViewModelを取得し、依存性注入を活用する。
 *
 * 【開発中】現在はバーコードスキャン画面を直接表示（Issue確認用）
 */
class MainActivity : ComponentActivity() {

    // ✅ Koin経由でViewModelを取得（依存関係は自動注入される）
    private val authViewModel: AuthViewModel by viewModel()
    private val bookScanViewModel: BookScanViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookStackTheme {
                // ===== 開発用: バーコードスキャン画面を直接表示 =====
                BookScanScreen(
                    viewModel = bookScanViewModel,
                    onNavigateBack = {
                        // TODO: ナビゲーション実装後に修正
                        finish() // アプリを終了
                    }
                )

                // ===== 本来の認証画面（現在はコメントアウト） =====
                // TODO: ナビゲーション実装後は以下のコメントを外す
                // AuthScreen(authViewModel)
            }
        }
    }
}
