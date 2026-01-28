package com.example.bookstack.di

import com.example.bookstack.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * ネットワーク通信（SupabaseおよびKtor）に関連する道具（依存関係）を定義する場所。
 */
object SupabaseConnectModule {

    // 1. Ktor HttpClient の定義 (OpenBDなどの外部API用)
    // これを定義することで、AppModule から参照可能になります
    val ktorClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                isLenient = true
            })
        }
    }

    // 2. Supabase Client の定義
    val supabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        // v3系の推奨される記述方法
        install(Postgrest) {
            serializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
            })
        }
        install(Auth) {
            sessionManager = SettingsSessionManager()
            alwaysAutoRefresh = true
        }
    }
}