package com.example.bookstack.data.remote.auth

import android.util.Log
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.flow.StateFlow

/**
 * Supabase Authを使用した認証データソース実装。
 * 匿名ユーザーによるサインインと現在のユーザーID取得を提供する。
 */
class SupabaseAuthDataSource(private val supabaseClient: SupabaseClient) : AuthDataSource {

    companion object {
        private const val TAG = "SupabaseAuthDataSource"
    }

    private val auth = supabaseClient.auth

    /**
     * 匿名ユーザーとしてサインインする。
     * RLS（Row Level Security）により、このユーザーIDでデータベースにアクセスできる。
     */
    override suspend fun signInAnonymously() {
        Log.d(TAG, "signInAnonymously: Starting anonymous sign-in")
        try {
            auth.signInAnonymously()
            Log.d(TAG, "signInAnonymously: Success")
        } catch (e: Exception) {
            Log.e(TAG, "signInAnonymously: Failed", e)
            throw e
        }
    }

    /**
     * 現在のセッション状態を監視するStateFlow。
     * Authenticated / NotAuthenticated の状態を通知する。
     */
    override val sessionStatus: StateFlow<SessionStatus>
        get() = auth.sessionStatus

    /**
     * 現在ログイン中のユーザーIDを取得する。
     * 未ログインの場合はnullを返す。
     *
     * @return ユーザーID（UUID形式）、またはnull
     */
    override fun getCurrentUserId(): String? {
        val userId = auth.currentUserOrNull()?.id
        Log.d(TAG, "getCurrentUserId: $userId")
        return userId
    }

    /**
     * ログアウトする。
     * セッションをクリアし、NotAuthenticated状態に遷移する。
     */
    override suspend fun signOut() {
        Log.d(TAG, "signOut: Starting sign out")
        try {
            auth.signOut()
            Log.d(TAG, "signOut: Success")
        } catch (e: Exception) {
            Log.e(TAG, "signOut: Failed", e)
            throw e
        }
    }
}
