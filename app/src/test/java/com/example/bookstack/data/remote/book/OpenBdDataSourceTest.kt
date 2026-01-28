package com.example.bookstack.data.remote.book

import com.example.bookstack.data.model.Book
import com.example.bookstack.data.model.BookSize
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class OpenBdDataSourceTest {

    private lateinit var mockEngine: MockEngine
    private lateinit var httpClient: HttpClient
    private lateinit var dataSource: OpenBdDataSource

    @Before
    fun setup() {
        mockEngine = MockEngine { request ->
            // Ktor 3系の安全なパス判定
            val isGetRequest = request.url.encodedPath.contains("/v1/get")

            if (isGetRequest) {
                val isbn = request.url.parameters["isbn"]
                // ヘッダーを事前に定義して型を確定させる
                val responseHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

                when (isbn) {
                    "9784041061907" -> respond(
                        content = """[{"summary":{"isbn":"9784041061907","title":"君の膵臓をたべたい","author":"住野よる","cover":"url"},"onix":{"DescriptiveDetail":{"Extent":[{"ExtentType":"00","ExtentValue":"334"}],"Subject":[{"SubjectSchemeIdentifier":"29","SubjectCode":"C0193"}]}}}]""",
                        status = HttpStatusCode.OK,
                        headers = responseHeaders
                    )
                    "9784101182018" -> respond(
                        content = """[{"summary":{"isbn":"9784101182018","title":"羅生門","author":"芥川龍之介"},"onix":{"DescriptiveDetail":{"Subject":[{"SubjectSchemeIdentifier":"29","SubjectCode":"C0111"}]}}}]""",
                        status = HttpStatusCode.OK,
                        headers = responseHeaders
                    )
                    "9999999999999" -> respond(
                        content = "[]",
                        status = HttpStatusCode.OK,
                        headers = responseHeaders
                    )
                    else -> respond(
                        content = "Not Found",
                        status = HttpStatusCode.NotFound
                    )
                }
            } else {
                respond(content = "Invalid Path", status = HttpStatusCode.NotFound)
            }
        }

        httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                    isLenient = true
                })
            }
        }
        dataSource = OpenBdDataSource(httpClient)
    }

    @After
    fun tearDown() {
        httpClient.close()
    }

    @Test
    fun getBookByIsbn_success_returnsBook() = runTest {
        val isbn = "9784041061907"
        val book = dataSource.getBookByIsbn(isbn)

        assertNotNull(book)
        assertEquals(isbn, book?.isbn)
        assertEquals("君の膵臓をたべたい", book?.title)
        assertEquals(334, book?.pageCount)
        assertEquals(BookSize.S, book?.bookSize)
    }

    @Test
    fun getBookByIsbn_noPageCount_returnsBookWithNullPageCount() = runTest {
        val isbn = "9784101182018"
        val book = dataSource.getBookByIsbn(isbn)

        assertNotNull(book)
        assertNull(book?.pageCount)
        assertEquals(BookSize.S, book?.bookSize)
    }

    @Test
    fun getBookByIsbn_notFound_returnsNull() = runTest {
        val isbn = "9999999999999"
        val book = dataSource.getBookByIsbn(isbn)

        assertNull(book)
    }

    @Test
    fun searchBooksByKeyword_alwaysReturnsEmptyList() = runTest {
        val books = dataSource.searchBooksByKeyword("テスト")
        assertEquals(0, books.size)
    }
}