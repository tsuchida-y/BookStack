package com.example.bookstack.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookstack.data.repository.AuthRepository
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * UIの状態を表現するシールクラス
 */
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    // UI状態を管理するStateFlow
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // RepositoryのStateFlowをそのまま公開
    val sessionStatus: StateFlow<SessionStatus> = repository.sessionStatus

    fun signInIfNeeded() {
        viewModelScope.launch {
            // すでにログイン中なら何もしない
            if (sessionStatus.value is SessionStatus.Authenticated) return@launch

            _uiState.value = AuthUiState.Loading
            try {
                // 匿名ログインを実行
                repository.signInAnonymously()
                _uiState.value = AuthUiState.Success
            } catch (e: Exception) {
                // ログに出力し、UI状態をErrorに更新する
                e.printStackTrace()
                _uiState.value = AuthUiState.Error(
                    e.message ?: "ネットワーク接続に失敗しました。設定を確認してください。"
                )
            }
        }
    }

    fun getUserId(): String = repository.getCurrentUserId() ?: "Unknown"
}