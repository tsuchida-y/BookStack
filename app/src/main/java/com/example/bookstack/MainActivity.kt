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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.bookstack.data.AuthRepository
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    // 本来はDI(Hiltなど)を使うのが良いですが、今回は手動で作成
    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppContent(authRepository)
                }
            }
        }
    }
}

@Composable
fun AppContent(repository: AuthRepository) {
    // ログイン状態を監視
    val sessionStatus by repository.sessionStatus.collectAsState()
    var isError by remember { mutableStateOf(false) }

    // 起動時にチェック＆ログイン試行
    LaunchedEffect(Unit) {
        // まだログインしていなければ（NotAuthenticatedなら）
        if (sessionStatus !is SessionStatus.Authenticated) {
            try {
                Log.d("Auth", "匿名ログインを開始します...")
                repository.signInAnonymously()
                Log.d("Auth", "匿名ログイン成功！")
            } catch (e: Exception) {
                Log.e("Auth", "ログインエラー: ${e.message}")
                isError = true
            }
        }
    }

    // 状態に応じた画面表示
    when (sessionStatus) {
        is SessionStatus.Authenticated -> {
            // ★ログイン成功時の画面（本棚画面など）
            HomeScreen(userId = repository.getCurrentUserId() ?: "Unknown")
        }
        else -> {
            // ロード中、またはエラー画面
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                if (isError) {
                    Text("ログインに失敗しました。ネット接続を確認してください。")
                } else {
                    CircularProgressIndicator() // くるくるローディング
                }
            }
        }
    }
}

// 仮のホーム画面
@Composable
fun HomeScreen(userId: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text("ようこそ！\nあなたのID: $userId")
    }
}