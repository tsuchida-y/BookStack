package com.example.bookstack

import android.app.Application

/**
 * アプリケーション全体のライフサイクルを管理するクラス。
 * AndroidManifest.xml の android:name に指定されている必要があります。
 */
class BookStackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 将来的にDI（Hiltなど）やログライブラリの初期化をここで行います
    }
}