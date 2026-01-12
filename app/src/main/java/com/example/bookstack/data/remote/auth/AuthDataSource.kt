package com.example.bookstack.data.remote.auth

import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow

interface AuthDataSource {
    suspend fun signInAnonymously()
    fun getCurrentUserId(): String?
    val sessionStatus: StateFlow<SessionStatus>
    suspend fun signOut() // ログアウト機能も追加しておくのが良いでしょう
}