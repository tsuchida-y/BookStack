package com.example.bookstack.di

import com.example.bookstack.BuildConfig
import com.example.bookstack.data.remote.auth.AuthDataSource
import com.example.bookstack.data.remote.auth.SupabaseAuthDataSource
import com.example.bookstack.data.remote.book.BookDataSource
import com.example.bookstack.data.remote.book.GoogleBooksDataSource
import com.example.bookstack.data.remote.book.OpenBdDataSource
import com.example.bookstack.data.repository.AuthRepository
import com.example.bookstack.data.repository.BookRepository
import com.example.bookstack.ui.auth.AuthViewModel
import com.example.bookstack.ui.scan.BookScanViewModel
import io.github.jan.supabase.SupabaseClient
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
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * アプリ全体のDI（依存性注入）を管理するKoinモジュール。
 * アプリ全体で一つのインスタンスを共有。
 * ずっと同じインスタンスを共有するため、メモリ効率がよく、設定の一貫性が保たれる
 */
val appModule = module {
    // ===== Network Layer =====

    // Ktor HttpClient の定義（OpenBD、Google Books API用）
    single<HttpClient> {
        HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                    isLenient = true
                })
            }
        }
    }

    // Supabase Client の定義（認証、DB操作用）
    single<SupabaseClient> {
        createSupabaseClient(
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

    // ===== Data Layer: DataSource =====

    // Auth DataSource
    single<AuthDataSource> {
        SupabaseAuthDataSource(supabaseClient = get())
    }

    // Book DataSource
    single { OpenBdDataSource(client = get()) }
    single { GoogleBooksDataSource(client = get()) }

    // 後方互換性のため、BookDataSourceインターフェースとしてOpenBdDataSourceを提供
    single<BookDataSource> { get<OpenBdDataSource>() }

    // ===== Data Layer: Repository =====

    // Auth Repository
    single {
        AuthRepository(authDataSource = get())
    }

    // Book Repository
    single {
        BookRepository(
            openBdDataSource = get(),
            googleBooksDataSource = get()
        )
    }

    // ===== UI Layer: ViewModel =====

    // Auth ViewModel
    viewModel {
        AuthViewModel(repository = get())
    }

    // BookScan ViewModel
    viewModel {
        BookScanViewModel(bookRepository = get())
    }
}