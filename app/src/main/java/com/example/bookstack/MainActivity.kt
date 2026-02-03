package com.example.bookstack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.bookstack.ui.auth.AuthScreen
import com.example.bookstack.ui.auth.AuthViewModel
import com.example.bookstack.ui.theme.BookStackTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

/**
 * アプリのメイン画面を表示するActivity。
 * Koin経由でViewModelを取得し、依存性注入を活用する。
 */
class MainActivity : ComponentActivity() {

    // ✅ Koin経由でAuthViewModelを取得（依存関係は自動注入される）
    private val authViewModel: AuthViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookStackTheme {
                // 認証画面（状態に応じてHomeへ切り替わる）を表示
                AuthScreen(authViewModel)
            }
        }
    }
}
