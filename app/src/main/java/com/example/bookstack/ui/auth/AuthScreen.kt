package com.example.bookstack.ui.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.jan.supabase.auth.status.SessionStatus

@Composable
fun AuthScreen(viewModel: AuthViewModel) {
    val sessionStatus by viewModel.sessionStatus.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.signInIfNeeded()
    }

    when (sessionStatus) {
        is SessionStatus.Authenticated -> {
            HomeScreen(userId = viewModel.getUserId())
        }
        else -> {
            LoadingScreen()
        }
    }
}

@Composable
fun LoadingScreen() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        CircularProgressIndicator()
    }
}

@Composable
fun HomeScreen(userId: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Text("ようこそ！\nあなたのID: $userId")
    }
}
