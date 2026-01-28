package com.example.bookstack.di

import com.example.bookstack.data.remote.book.BookDataSource
import com.example.bookstack.data.remote.book.GoogleBooksDataSource
import com.example.bookstack.data.remote.book.OpenBdDataSource
import com.example.bookstack.data.repository.BookRepository
import io.ktor.client.HttpClient
import org.koin.dsl.module

val appModule = module {
    // SupabaseConnectModule object のプロパティを Koin に登録
    single<HttpClient> { SupabaseConnectModule.ktorClient }

    // データソースの登録
    single { OpenBdDataSource(client = get()) }
    single { GoogleBooksDataSource(client = get()) }

    // 後方互換性のため、BookDataSourceインターフェースとしてOpenBdDataSourceを提供
    single<BookDataSource> { get<OpenBdDataSource>() }

    // Repositoryの登録
    single {
        BookRepository(
            openBdDataSource = get(),
            googleBooksDataSource = get()
        )
    }
}