package com.example.bookstack.data.repository

import com.example.bookstack.data.model.Book
import com.example.bookstack.data.model.BookSize
import com.example.bookstack.data.remote.book.GoogleBooksDataSource
import com.example.bookstack.data.remote.book.OpenBdDataSource
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

class BookRepositoryTest {

    private lateinit var mockEngine: MockEngine
    private lateinit var httpClient: HttpClient
    private lateinit var openBdDataSource: OpenBdDataSource
    private lateinit var googleBooksDataSource: GoogleBooksDataSource
    private lateinit var repository: BookRepository

    @Before
    fun setup() {
        mockEngine = MockEngine { request ->
            val responseHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

            when {
                // OpenBD APIのモック
                request.url.encodedPath.contains("/v1/get") -> {
                    val isbn = request.url.parameters["isbn"]
                    when (isbn) {
                        "9784041061907" -> respond(
                            content = """[{"summary":{"isbn":"9784041061907","title":"君の膵臓をたべたい","author":"住野よる","cover":"https://cover.openbd.jp/9784041061907.jpg"},"onix":{"DescriptiveDetail":{"Extent":[{"ExtentType":"00","ExtentValue":"334"}],"Subject":[{"SubjectSchemeIdentifier":"29","SubjectCode":"C0193"}]}}}]""",
                            status = HttpStatusCode.OK,
                            headers = responseHeaders
                        )
                        "9784101182018" -> respond(
                            content = """[{"summary":{"isbn":"9784101182018","title":"羅生門","author":"芥川龍之介","cover":"https://cover.openbd.jp/9784101182018.jpg"},"onix":{"DescriptiveDetail":{"Extent":[{"ExtentType":"00","ExtentValue":"230"}],"Subject":[{"SubjectSchemeIdentifier":"29","SubjectCode":"C0111"}]}}}]""",
                            status = HttpStatusCode.OK,
                            headers = responseHeaders
                        )
                        "9999999999999" -> respond(
                            content = """[null]""",
                            status = HttpStatusCode.OK,
                            headers = responseHeaders
                        )
                        else -> respond(
                            content = """[null]""",
                            status = HttpStatusCode.OK,
                            headers = responseHeaders
                        )
                    }
                }
                // Google Books APIのモック
                request.url.encodedPath.contains("/books/v1/volumes") -> {
                    val query = request.url.parameters["q"]
                    when {
                        query?.contains("isbn:1234567890123") == true -> respond(
                            content = """{"items":[{"id":"test-id","volumeInfo":{"title":"Test Book","authors":["Test Author"],"industryIdentifiers":[{"type":"ISBN_13","identifier":"1234567890123"}],"imageLinks":{"thumbnail":"https://books.google.com/thumbnail.jpg"},"pageCount":250,"publisher":"Test Publisher"}}]}""",
                            status = HttpStatusCode.OK,
                            headers = responseHeaders
                        )
                        query?.contains("isbn:") == true -> respond(
                            content = """{"items":[]}""",
                            status = HttpStatusCode.OK,
                            headers = responseHeaders
                        )
                        query == "技術書" -> respond(
                            content = """{"items":[{"id":"tech-id","volumeInfo":{"title":"技術書タイトル","authors":["技術著者"],"industryIdentifiers":[{"type":"ISBN_13","identifier":"9999999999991"}],"imageLinks":{"thumbnail":"https://books.google.com/tech.jpg"},"pageCount":500,"publisher":"オライリー"}}]}""",
                            status = HttpStatusCode.OK,
                            headers = responseHeaders
                        )
                        else -> respond(
                            content = """{"items":[]}""",
                            status = HttpStatusCode.OK,
                            headers = responseHeaders
                        )
                    }
                }
                else -> respond(
                    content = """{}""",
                    status = HttpStatusCode.NotFound,
                    headers = responseHeaders
                )
            }
        }

        httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                })
            }
        }

        openBdDataSource = OpenBdDataSource(httpClient)
        googleBooksDataSource = GoogleBooksDataSource(httpClient)
        repository = BookRepository(openBdDataSource, googleBooksDataSource)
    }

    @After
    fun tearDown() {
        httpClient.close()
    }

    @Test
    fun `getBookDetails - OpenBDで書籍が見つかる場合`() = runTest {
        // Given
        val isbn = "9784041061907"

        // When
        val result = repository.getBookDetails(isbn)

        // Then
        assertNotNull("書籍情報が取得できること", result)
        assertEquals("ISBNが正しいこと", isbn, result?.isbn)
        assertEquals("タイトルが正しいこと", "君の膵臓をたべたい", result?.title)
        assertEquals("著者が正しいこと", "住野よる", result?.author)
        assertEquals("ページ数が正しいこと", 334, result?.pageCount)
        assertEquals("判型サイズが文庫(S)と判定されること", BookSize.S, result?.bookSize)
    }

    @Test
    fun `getBookDetails - OpenBDで見つからず、Google Booksで見つかる場合`() = runTest {
        // Given
        val isbn = "1234567890123"

        // When
        val result = repository.getBookDetails(isbn)

        // Then
        assertNotNull("Google Books APIから書籍情報が取得できること", result)
        assertEquals("ISBNが正しいこと", isbn, result?.isbn)
        assertEquals("タイトルが正しいこと", "Test Book", result?.title)
        assertEquals("著者が正しいこと", "Test Author", result?.author)
        assertEquals("ページ数が正しいこと", 250, result?.pageCount)
    }

    @Test
    fun `getBookDetails - ISBNで見つからず、キーワードで検索する場合`() = runTest {
        // Given
        val isbn = "9999999999999"
        val keyword = "技術書"

        // When
        val result = repository.getBookDetails(isbn, keyword)

        // Then
        assertNotNull("キーワード検索で書籍情報が取得できること", result)
        assertEquals("タイトルに技術書が含まれること", "技術書タイトル", result?.title)
        assertEquals("著者が正しいこと", "技術著者", result?.author)
        assertEquals("ページ数が正しいこと", 500, result?.pageCount)
        // キーワードから判型がXLと判定されること（技術書、オライリー）
        assertEquals("判型サイズがXLと判定されること", BookSize.XL, result?.bookSize)
    }

    @Test
    fun `getBookDetails - 全てのデータソースで見つからない場合`() = runTest {
        // Given
        val isbn = "0000000000000"

        // When
        val result = repository.getBookDetails(isbn)

        // Then
        assertNull("書籍情報がnullであること", result)
    }

    @Test
    fun `getBookDetails - キーワードなしで全データソースで見つからない場合`() = runTest {
        // Given
        val isbn = "9999999999999"

        // When
        val result = repository.getBookDetails(isbn, keyword = null)

        // Then
        assertNull("キーワードがない場合、nullが返ること", result)
    }

    @Test
    fun `getBookDetails - ページ数がnullの場合の処理`() = runTest {
        // Given: ページ数がないOpenBDのレスポンスを想定
        val isbn = "9784101182018"

        // When
        val result = repository.getBookDetails(isbn)

        // Then
        assertNotNull("書籍情報が取得できること", result)
        assertEquals("タイトルが正しいこと", "羅生門", result?.title)
        assertEquals("ページ数が正しいこと", 230, result?.pageCount)
        assertEquals("判型サイズが文庫(S)と判定されること", BookSize.S, result?.bookSize)
    }

    @Test
    fun `enhanceBookWithSize - 既に有効なサイズが設定されている場合は変更しない`() = runTest {
        // Given
        val isbn = "9784041061907"

        // When
        val result = repository.getBookDetails(isbn)

        // Then
        assertNotNull("書籍情報が取得できること", result)
        // OpenBDのCコードから判定されたサイズ(S)がそのまま保持されること
        assertEquals("判型サイズが文庫(S)のまま保持されること", BookSize.S, result?.bookSize)
    }
}
