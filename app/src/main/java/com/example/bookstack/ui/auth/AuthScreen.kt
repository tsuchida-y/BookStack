package com.example.bookstack.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.jan.supabase.auth.status.SessionStatus

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    // ViewModelからUIの状態を購読 (Lifecycleを考慮した最新の書き方)
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sessionStatus by viewModel.sessionStatus.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.signInIfNeeded()
    }

    // 1. まず認証済みかどうかをチェック
    if (sessionStatus is SessionStatus.Authenticated) {
        HomeScreen(userId = viewModel.getUserId())
    } else {
        // 2. 認証されていない場合、通信状態（uiState）に応じて表示を切り替える
        when (val state = uiState) {
            is AuthUiState.Loading -> {
                LoadingScreen(message = "ログイン中...")
            }
            is AuthUiState.Error -> {
                ErrorScreen(
                    message = state.message,
                    onRetry = { viewModel.signInIfNeeded() }
                )
            }
            else -> {
                // 初期状態などはローディングを表示
                LoadingScreen()
            }
        }
    }
}

@Composable
fun LoadingScreen(message: String = "読み込み中...") {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = message)
        }
    }
}

@Composable
fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "エラーが発生しました",
                style = MaterialTheme.typography.titleLarge,
                color = Color.Red
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("再試行する")
            }
        }
    }
}

@Composable
fun HomeScreen(userId: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text("ようこそ！\nあなたのID: $userId", textAlign = TextAlign.Center)
    }
}