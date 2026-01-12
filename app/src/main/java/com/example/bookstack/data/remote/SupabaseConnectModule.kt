package com.example.bookstack.data.remote

import com.example.bookstack.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.SettingsSessionManager
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json

object SupabaseConnectModule {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Postgrest.Companion) {
            serializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
            })
        }
        install(Auth.Companion) {
            // トークンをどこに保存するかを管理するクラス
            // 自動でSharedPreferences(Android標準の物理ストレージ)に保存
            // v3系では 'storage' ではなく 'sessionManager' 変数を使用します
            sessionManager = SettingsSessionManager()
            // 匿名認証を有効にする設定
            alwaysAutoRefresh = true
        }
    }
}