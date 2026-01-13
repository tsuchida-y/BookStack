@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.example.bookstack.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Google Books APIから取得する書籍情報のDTO (Data Transfer Object)。
 * Google Books APIのJSONレスポンス構造に厳密に合わせる。
 */
@Serializable
data class GoogleBookDto(
    @SerialName("items")
    val items: List<GoogleBookItemDto>? = null
)

@Serializable
data class GoogleBookItemDto(
    @SerialName("id")
    val id: String? = null,
    @SerialName("volumeInfo")
    val volumeInfo: GoogleBookVolumeInfoDto? = null
)

@Serializable
data class GoogleBookVolumeInfoDto(
    @SerialName("title")
    val title: String? = null,
    @SerialName("authors")
    val authors: List<String>? = null,
    @SerialName("industryIdentifiers")
    val industryIdentifiers: List<GoogleBookIndustryIdentifierDto>? = null,
    @SerialName("imageLinks")
    val imageLinks: GoogleBookImageLinksDto? = null,
    @SerialName("pageCount")
    val pageCount: Int? = null,
    @SerialName("publisher")
    val publisher: String? = null,
    @SerialName("publishedDate")
    val publishedDate: String? = null,
    @SerialName("description")
    val description: String? = null
)

@Serializable
data class GoogleBookIndustryIdentifierDto(
    @SerialName("type")
    val type: String? = null, // 例: ISBN_13, ISBN_10
    @SerialName("identifier")
    val identifier: String? = null
)

@Serializable
data class GoogleBookImageLinksDto(
    @SerialName("smallThumbnail")
    val smallThumbnail: String? = null,
    @SerialName("thumbnail")
    val thumbnail: String? = null,
    @SerialName("large")
    val large: String? = null,
    @SerialName("medium")
    val medium: String? = null
)

/**
 * GoogleBookDto（内部のGoogleBookItemDto）からBookドメインモデルへの変換拡張関数。
 * Google Books APIのJSON構造から必要な情報を抽出し、Bookオブジェクトを生成する。
 * BookSizeはIssue 2で決定されるため、ここではnullとする。
 *
 * Google Books APIは検索結果として複数のアイテムを返す可能性があるため、
 * ここでは最初のアイテムを対象として変換する。
 */
fun GoogleBookItemDto.toBook(): Book? {
    val volumeInfo = this.volumeInfo ?: return null

    // ISBN_13を優先的に取得
    val isbn = volumeInfo.industryIdentifiers?.firstOrNull { it.type == "ISBN_13" }?.identifier
        ?: volumeInfo.industryIdentifiers?.firstOrNull { it.type == "ISBN_10" }?.identifier
        ?: return null // ISBNが見つからない場合は変換できない

    val author = volumeInfo.authors?.joinToString(", ") ?: "著者不明"
    val coverUrl = volumeInfo.imageLinks?.thumbnail ?: volumeInfo.imageLinks?.smallThumbnail

    return Book(
        isbn = isbn,
        title = volumeInfo.title ?: "タイトル不明",
        author = author,
        coverImageUrl = coverUrl,
        pageCount = volumeInfo.pageCount,
        bookSize = null // 判型サイズは後のIssueで設定
    )
}
