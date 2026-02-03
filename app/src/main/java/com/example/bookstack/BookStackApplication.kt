package com.example.bookstack

import android.app.Application
import com.example.bookstack.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * アプリケーション全体のライフサイクルを管理するクラス。
 * AndroidManifest.xml の android:name に指定されている必要があります。
 *
 * Koin（DIフレームワーク）の初期化を行い、依存関係の注入を可能にします。
 */
class BookStackApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Koinの初期化
        startKoin {
            // Androidのコンテキストを提供
            androidContext(this@BookStackApplication)

            // デバッグビルド時のみログを有効化
            androidLogger(Level.ERROR) // 本番環境ではERRORレベルのみ

            // DIモジュールを読み込む
            modules(appModule)
        }
    }
}