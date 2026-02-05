package com.example.bookstack.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabaseの`books`テーブルに保存するためのDTO (Data Transfer Object)。
 * テーブル構造に厳密に合わせた設計。
 */
@Serializable
data class BookDto(
    @SerialName("id")
    val id: String? = null, // UUID、INSERT時はnull（DB側で自動生成）

    @SerialName("user_id")
    val userId: String, // 所有ユーザーのID（auth.uid()）

    @SerialName("isbn")
    val isbn: String,

    @SerialName("title")
    val title: String,

    @SerialName("authors")
    val authors: List<String>? = null, // JSONB型の著者リスト

    @SerialName("cover_url")
    val coverUrl: String? = null,

    @SerialName("spine_color")
    val spineColor: String? = null, // 背表紙色（Hex）

    @SerialName("size_type")
    val sizeType: String? = null, // S/M/L/XL

    @SerialName("page_count")
    val pageCount: Int? = null,

    @SerialName("status")
    val status: String = "unread", // unread/reading/completed

    @SerialName("current_page")
    val currentPage: Int = 0,

    // embeddingは将来のAI機能用なので、現時点では含めない

    @SerialName("added_at")
    val addedAt: String? = null, // ISO8601形式、INSERT時はDB側でデフォルト値が設定される

    @SerialName("completed_at")
    val completedAt: String? = null
)

/**
 * BookドメインモデルをSupabase保存用のBookDtoに変換する。
 * @param userId 所有ユーザーのID
 */
fun Book.toBookDto(userId: String): BookDto {
    return BookDto(
        id = this.id,
        userId = userId,
        isbn = this.isbn,
        title = this.title,
        authors = listOf(this.author), // 単一著者を配列に変換
        coverUrl = this.coverImageUrl,
        sizeType = this.bookSize?.name, // EnumをString型に変換
        pageCount = this.pageCount,
        status = "unread" // 新規登録時はデフォルトで「未読」
    )
}

/**
 * Supabaseから取得したBookDtoをBookドメインモデルに変換する。
 */
fun BookDto.toBook(): Book {
    return Book(
        id = this.id,
        isbn = this.isbn,
        title = this.title,
        author = this.authors?.firstOrNull() ?: "不明", // 最初の著者を使用
        coverImageUrl = this.coverUrl,
        pageCount = this.pageCount,
        bookSize = this.sizeType?.let {
            try {
                BookSize.valueOf(it)
            } catch (_: IllegalArgumentException) {
                BookSize.UNKNOWN
            }
        }
    )
}
