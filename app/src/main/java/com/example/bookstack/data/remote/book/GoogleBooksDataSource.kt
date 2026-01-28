package com.example.bookstack.data.remote.book

import com.example.bookstack.data.model.Book
import com.example.bookstack.data.model.GoogleBookDto
import com.example.bookstack.data.model.toBook
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

/**
 * Google Books APIから書籍情報を取得するデータソース実装。
 * OpenBDで見つからない場合のフォールバックとして使用される。
 */
class GoogleBooksDataSource(private val client: HttpClient) : BookDataSource {

    companion object {
        private const val BASE_URL = "https://www.googleapis.com/books/v1"
    }

    override suspend fun getBookByIsbn(isbn: String): Book? {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/volumes") {
                url {
                    parameters.append("q", "isbn:$isbn")
                }
            }

            if (response.status.isSuccess()) {
                val googleBookDto: GoogleBookDto = response.body()

                // itemsの最初の要素をBookに変換
                googleBookDto.items?.firstOrNull()?.toBook()
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error fetching book from Google Books API: ${e.message}")
            null
        }
    }

    override suspend fun searchBooksByKeyword(keyword: String): List<Book> {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/volumes") {
                url {
                    parameters.append("q", keyword)
                    parameters.append("maxResults", "10")
                }
            }

            if (response.status.isSuccess()) {
                val googleBookDto: GoogleBookDto = response.body()

                // 全てのitemsをBookに変換し、nullでないものだけ返す
                googleBookDto.items?.mapNotNull { it.toBook() } ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            println("Error searching books from Google Books API: ${e.message}")
            emptyList()
        }
    }
}
