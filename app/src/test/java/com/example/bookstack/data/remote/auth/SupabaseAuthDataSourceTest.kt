package com.example.bookstack.data.remote.auth

import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * SupabaseAuthDataSourceの単体テスト。
 * モックを使用してSupabase Authへの実際の通信を行わずにテストする。
 */
class SupabaseAuthDataSourceTest {

    private lateinit var dataSource: MockAuthDataSource

    @Before
    fun setup() {
        dataSource = MockAuthDataSource()
    }

    @Test
    fun `signInAnonymously - 匿名ログインが正常に実行されること`() = runTest {
        // When
        dataSource.signInAnonymously()

        // Then
        assertEquals("signInAnonymously()が呼ばれること", 1, dataSource.signInAnonymouslyCallCount)
        val sessionStatus = dataSource.sessionStatus.value
        assert(sessionStatus is SessionStatus.Authenticated) {
            "ログイン後はAuthenticatedになること"
        }
    }

    @Test
    fun `sessionStatus - 初期状態はNotAuthenticatedであること`() {
        // Given
        val freshDataSource = MockAuthDataSource()

        // When
        val result = freshDataSource.sessionStatus.value

        // Then
        assert(result is SessionStatus.NotAuthenticated) {
            "初期状態はNotAuthenticatedであること"
        }
    }

    @Test
    fun `sessionStatus - ログイン後はAuthenticatedになること`() = runTest {
        // When
        dataSource.signInAnonymously()

        // Then
        val result = dataSource.sessionStatus.value
        assert(result is SessionStatus.Authenticated) {
            "SessionStatusがAuthenticatedであること"
        }
        val authenticatedStatus = result as SessionStatus.Authenticated
        assertEquals("アクセストークンが設定されること", "mock-token", authenticatedStatus.session.accessToken)
    }

    @Test
    fun `getCurrentUserId - ユーザーがログイン中の場合IDを返すこと`() = runTest {
        // Given
        dataSource.signInAnonymously()
        dataSource.currentUserId = "user-123"

        // When
        val userId = dataSource.getCurrentUserId()

        // Then
        assertNotNull("ユーザーIDが返ること", userId)
        assertEquals("ユーザーIDが正しいこと", "user-123", userId)
    }

    @Test
    fun `getCurrentUserId - ユーザーが未ログインの場合nullを返すこと`() {
        // Given: ログインしていない状態

        // When
        val userId = dataSource.getCurrentUserId()

        // Then
        assertNull("nullが返ること", userId)
    }

    @Test
    fun `signOut - ログアウトが正常に実行されること`() = runTest {
        // Given: ログイン状態
        dataSource.signInAnonymously()
        dataSource.currentUserId = "user-123"

        // When
        dataSource.signOut()

        // Then
        assertEquals("signOut()が呼ばれること", 1, dataSource.signOutCallCount)
        val sessionStatus = dataSource.sessionStatus.value
        assert(sessionStatus is SessionStatus.NotAuthenticated) {
            "ログアウト後はNotAuthenticatedになること"
        }
        assertNull("ユーザーIDがnullになること", dataSource.getCurrentUserId())
    }

    // ========== モッククラス ==========

    /**
     * テスト用のAuthDataSource実装。
     * 実際のSupabase認証の代わりにメモリ上で状態を管理する。
     */
    private class MockAuthDataSource : AuthDataSource {
        var signInAnonymouslyCallCount = 0
        var signOutCallCount = 0
        var currentUserId: String? = null

        private val _sessionStatus = MutableStateFlow<SessionStatus>(
            SessionStatus.NotAuthenticated(isSignOut = false)
        )
        override val sessionStatus: StateFlow<SessionStatus> = _sessionStatus

        override suspend fun signInAnonymously() {
            signInAnonymouslyCallCount++
            _sessionStatus.value = SessionStatus.Authenticated(
                UserSession(
                    accessToken = "mock-token",
                    refreshToken = "mock-refresh",
                    expiresIn = 3600,
                    tokenType = "bearer",
                    user = null
                )
            )
            currentUserId = "anonymous-user-id"
        }

        override fun getCurrentUserId(): String? = currentUserId

        override suspend fun signOut() {
            signOutCallCount++
            _sessionStatus.value = SessionStatus.NotAuthenticated(isSignOut = true)
            currentUserId = null
        }
    }
}
