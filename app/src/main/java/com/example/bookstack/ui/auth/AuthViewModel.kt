// ui/auth/AuthViewModel.kt
package com.example.bookstack.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookstack.data.repository.AuthRepository
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    // RepositoryのStateFlowをそのまま公開
    val sessionStatus: StateFlow<SessionStatus> = repository.sessionStatus

    fun signInIfNeeded() {
        viewModelScope.launch {
            // ログインしていない場合のみ匿名ログインを実行
            if (sessionStatus.value !is SessionStatus.Authenticated) {
                try {
                    repository.signInAnonymously()
                } catch (e: Exception) {
                    // エラー処理（本来はUiStateを作ってエラーメッセージを通知するのが理想）
                }
            }
        }
    }

    fun getUserId(): String = repository.getCurrentUserId() ?: "Unknown"
}
