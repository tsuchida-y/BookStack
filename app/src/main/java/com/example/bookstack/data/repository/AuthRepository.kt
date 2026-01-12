package com.example.bookstack.data.repository

import com.example.bookstack.data.remote.auth.AuthDataSource // 新しいデータソースをインポート
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow

// AuthDataSourceインターフェースに依存するように変更
class AuthRepository(private val authDataSource: AuthDataSource) {

    suspend fun signInAnonymously() {
        authDataSource.signInAnonymously() // データソースに委譲
    }

    val sessionStatus: StateFlow<SessionStatus> = authDataSource.sessionStatus // データソースから取得

    fun getCurrentUserId(): String? {
        return authDataSource.getCurrentUserId() // データソースから取得
    }

    suspend fun signOut() {
        authDataSource.signOut() // データソースに委譲
    }
}