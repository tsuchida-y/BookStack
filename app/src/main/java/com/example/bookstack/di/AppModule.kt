package com.example.bookstack.di

import com.example.bookstack.BuildConfig
import com.example.bookstack.data.remote.auth.AuthDataSource
import com.example.bookstack.data.remote.auth.SupabaseAuthDataSource
import com.example.bookstack.data.remote.book.BookDataSource
import com.example.bookstack.data.remote.book.GoogleBooksDataSource
import com.example.bookstack.data.remote.book.OpenBdDataSource
import com.example.bookstack.data.remote.database.BookDatabaseDataSource
import com.example.bookstack.data.remote.database.ReadingLogDataSource
import com.example.bookstack.data.remote.database.SupabaseBookDatabaseDataSource
import com.example.bookstack.data.remote.database.SupabaseReadingLogDataSource
import com.example.bookstack.data.repository.AuthRepository
import com.example.bookstack.data.repository.BookDatabaseRepository
import com.example.bookstack.data.repository.BookRepository
import com.example.bookstack.data.repository.ReadingLogRepository
import com.example.bookstack.ui.auth.AuthViewModel
import com.example.bookstack.ui.bookdetail.BookDetailViewModel
import com.example.bookstack.ui.booklist.BookListViewModel
import com.example.bookstack.ui.heatmap.ReadingHeatmapViewModel
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

    // Book DataSource (外部API用)
    single { OpenBdDataSource(client = get()) }
    single { GoogleBooksDataSource(client = get()) }

    // 後方互換性のため、BookDataSourceインターフェースとしてOpenBdDataSourceを提供
    single<BookDataSource> { get<OpenBdDataSource>() }

    // Book Database DataSource (Supabase DB操作用)
    single<BookDatabaseDataSource> {
        SupabaseBookDatabaseDataSource(supabaseClient = get())
    }

    // Reading Log DataSource (Supabase DB操作用)
    single<ReadingLogDataSource> {
        SupabaseReadingLogDataSource(supabaseClient = get())
    }

    // ===== Data Layer: Repository =====

    // Auth Repository
    single {
        AuthRepository(authDataSource = get())
    }

    // Book Repository (外部API用)
    single {
        BookRepository(
            openBdDataSource = get(),
            googleBooksDataSource = get()
        )
    }

    // Book Database Repository (Supabase DB操作用)
    single {
        BookDatabaseRepository(
            bookDatabaseDataSource = get(),
            authRepository = get()
        )
    }

    // Reading Log Repository (読書記録用)
    single {
        ReadingLogRepository(
            readingLogDataSource = get(),
            authRepository = get()
        )
    }

    // ===== UI Layer: ViewModel =====

    // Auth ViewModel
    viewModel<AuthViewModel> {
        AuthViewModel(repository = get())
    }

    // BookScan ViewModel
    viewModel<BookScanViewModel> {
        BookScanViewModel(
            bookRepository = get(),
            bookDatabaseRepository = get()
        )
    }

    // BookList ViewModel (本棚画面用)
    viewModel<BookListViewModel> {
        BookListViewModel(
            bookDatabaseRepository = get()
        )
    }

    // BookDetail ViewModel (本の詳細画面用)
    viewModel<BookDetailViewModel> {
        BookDetailViewModel(
            bookDatabaseRepository = get(),
            readingLogRepository = get()
        )
    }

    // ReadingHeatmap ViewModel (ヒートマップ画面用)
    viewModel<ReadingHeatmapViewModel> {
        ReadingHeatmapViewModel(
            readingLogRepository = get()
        )
    }
}