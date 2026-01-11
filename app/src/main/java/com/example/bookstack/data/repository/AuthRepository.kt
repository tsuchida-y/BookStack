package com.example.bookstack.data.repository

import com.example.bookstack.SupabaseConnectModule
import io.github.jan.supabase.auth.auth

class AuthRepository {
    private val auth = SupabaseConnectModule.client.auth

    // 匿名でサインイン
    suspend fun signInAnonymously() {
        auth.signInAnonymously()
    }

    // すでにサインイン済みかチェック
    // sessionStatus が Authenticated ならログイン済み
    val sessionStatus = auth.sessionStatus

    // 現在のユーザーIDを取得（デバッグ用など）
    fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }

    // ログアウト
    suspend fun signOut() {
        auth.signOut()
    }
}