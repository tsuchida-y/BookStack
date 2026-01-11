package com.example.bookstack

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.serializer.KotlinXSerializer
import kotlinx.serialization.json.Json
import io.github.jan.supabase.auth.SettingsSessionManager

object SupabaseConnectModule {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_KEY
    ) {
        install(Postgrest) {
            serializer = KotlinXSerializer(Json {
                ignoreUnknownKeys = true
            })
        }
        install(Auth) {
            // トークンをどこに保存するかを管理するクラス
            // 自動でSharedPreferences(Android標準の物理ストレージ)に保存
            // v3系では 'storage' ではなく 'sessionManager' 変数を使用します
            sessionManager = SettingsSessionManager()
            // 匿名認証を有効にする設定
            alwaysAutoRefresh = true
        }
    }
}