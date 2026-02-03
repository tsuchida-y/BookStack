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
 * ⚠️ **このファイルは非推奨です。削除予定。**
 *
 * 以前はシングルトンobjectとして実装されていましたが、
 * Koinによる依存性注入のメリット（テスト容易性、結合度の低下）を
 * 活かすため、AppModule.kt にクライアント生成ロジックを移動しました。
 *
 * **このファイルは削除してください。**
 *
 * 移行先: com.example.bookstack.di.appModule
 * - HttpClient は appModule で single<HttpClient> { ... } として定義
 * - SupabaseClient は appModule で single<SupabaseClient> { ... } として定義
 */
@Deprecated(
    message = "Use appModule instead. This object will be removed in future versions.",
    replaceWith = ReplaceWith("appModule", "com.example.bookstack.di.appModule"),
    level = DeprecationLevel.ERROR
)
object SupabaseConnectModule {

    // ⚠️ これらのプロパティは使用しないでください
    // AppModule から Koin 経由で取得してください

    val ktorClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                isLenient = true
            })
        }
    }

    val supabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
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