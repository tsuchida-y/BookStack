package com.example.bookstack.data.remote.book

import com.example.bookstack.data.model.Book
import com.example.bookstack.data.model.OpenBdBookDto
import com.example.bookstack.data.model.toBook
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json

/**
 * OpenBD APIから書籍情報を取得するデータソース実装。
 */
class OpenBdDataSource(private val client: HttpClient) : BookDataSource {

    companion object {
        private const val BASE_URL = "https://api.openbd.jp/v1"
    }

    override suspend fun getBookByIsbn(isbn: String): Book? {
        return try {
            val response: HttpResponse = client.get("$BASE_URL/get") {
                url {
                    parameters.append("isbn", isbn)
                }
            }

            // response ではなく response.status に対して isSuccess() を呼ぶ
            if (response.status.isSuccess()) {
                // Ktorのbody<T>()機能を使うと、ListSerializerなどを手動で書かずにデシリアライズ可能です
                val bookDtoList: List<OpenBdBookDto?> = response.body()

                // OpenBDは該当なしの場合 [null] や [] を返すため、安全に処理
                bookDtoList.firstOrNull()?.toBook()
            } else {
                null
            }
        } catch (e: Exception) {
            println("Error fetching book from OpenBD: ${e.message}")
            null
        }
    }

    override suspend fun searchBooksByKeyword(keyword: String): List<Book> {
        return emptyList()
    }
}