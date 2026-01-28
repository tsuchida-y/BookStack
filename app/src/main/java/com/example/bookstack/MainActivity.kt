package com.example.bookstack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.bookstack.data.remote.auth.SupabaseAuthDataSource
import com.example.bookstack.di.SupabaseConnectModule
import com.example.bookstack.data.repository.AuthRepository
import com.example.bookstack.ui.auth.AuthScreen
import com.example.bookstack.ui.auth.AuthViewModel
import com.example.bookstack.ui.theme.BookStackTheme

class MainActivity : ComponentActivity() {
    // SupabaseClientはSupabaseConnectModuleから取得
    private val supabaseClient = SupabaseConnectModule.supabaseClient

    // データソースとリポジトリをここでインスタンス化
    private val authDataSource = SupabaseAuthDataSource(supabaseClient)
    private val authRepository = AuthRepository(authDataSource)
    private val authViewModel = AuthViewModel(authRepository)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookStackTheme { // 作成したテーマを適用
                // 認証画面（状態に応じてHomeへ切り替わる）を表示
                AuthScreen(authViewModel)
            }
        }
    }
}
