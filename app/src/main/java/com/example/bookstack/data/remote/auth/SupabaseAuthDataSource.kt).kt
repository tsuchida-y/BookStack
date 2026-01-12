package com.example.bookstack.data.remote.auth

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.SupabaseClient // SupabaseConnectModuleから渡すように変更
import kotlinx.coroutines.flow.StateFlow

class SupabaseAuthDataSource(private val supabaseClient: SupabaseClient) : AuthDataSource {

    // SupabaseConnectModule.client.auth の代わりに supabaseClient.auth を使用
    private val auth = supabaseClient.auth

    override suspend fun signInAnonymously() {
        auth.signInAnonymously()
    }

    override val sessionStatus: StateFlow<SessionStatus>
        get() = auth.sessionStatus

    override fun getCurrentUserId(): String? {
        return auth.currentUserOrNull()?.id
    }

    override suspend fun signOut() {
        auth.signOut()
    }
}