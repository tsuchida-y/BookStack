@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.example.bookstack.data.model

import com.example.bookstack.data.util.BookSizeConverter
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenBD APIから取得する書籍情報のDTO (Data Transfer Object)。
 * OpenBD APIのJSONレスポンス構造に厳密に合わせる。
 *
 * OpenBD APIのレスポンスは配列の中にオブジェクトがあり、その中にsummaryなどの情報が含まれるため、
 * トップレベルのDTOはそれを反映した構造とする。
 */

@Serializable
data class OpenBdBookDto(
    @SerialName("summary")
    val summary: OpenBdSummaryDto? = null,
    @SerialName("onix")
    val onix: OpenBdOnixDto? = null
)

@Serializable
data class OpenBdSummaryDto(
    @SerialName("isbn")
    val isbn: String? = null,
    @SerialName("title")
    val title: String? = null,
    @SerialName("author")
    val author: String? = null,
    @SerialName("cover") // 画像URL
    val cover: String? = null
)

@Serializable
data class OpenBdOnixDto(
    @SerialName("DescriptiveDetail")
    val descriptiveDetail: OpenBdDescriptiveDetailDto? = null
)

@Serializable
data class OpenBdDescriptiveDetailDto(
    @SerialName("Extent")
    val extent: List<OpenBdExtentDto>? = null,
    @SerialName("Subject")
    val subject: List<OpenBdSubjectDto>? = null
)
@Serializable
data class OpenBdSubjectDto( // ★ 新しく定義
    @SerialName("SubjectSchemeIdentifier")
    val subjectSchemeIdentifier: String? = null, // "29" がCコード
    @SerialName("SubjectCode")
    val subjectCode: String? = null
)

@Serializable
data class OpenBdExtentDto(
    @SerialName("ExtentType")
    val extentType: String? = null, // 00:ページ数
    @SerialName("ExtentValue")
    val extentValue: String? = null // ページ数の値
)

/**
 * OpenBdBookDtoからBookドメインモデルへの変換拡張関数。
 * OpenBDの複雑なJSON構造から必要な情報を抽出し、Bookオブジェクトを生成する。
 * BookSizeはIssue 2で決定されるため、ここではnullとする。
 */
fun OpenBdBookDto.toBook(): Book? {
    val summary = this.summary ?: return null // summaryがない場合は変換できない
    val isbn = summary.isbn ?: return null // ISBNがない場合は変換できない

    // cover URLが空文字列の場合にnullに変換
    val coverUrl = summary.cover?.takeIf { it.isNotBlank() }

    // Cコードの取得 (OpenBDのJSON構造から抽出)
    val cCode = this.onix?.descriptiveDetail?.subject
        ?.firstOrNull { it.subjectSchemeIdentifier == "29" } // 29はCコード
        ?.subjectCode

    // ONIXデータからページ数を抽出
    val pageCount = this.onix
        ?.descriptiveDetail
        ?.extent
        ?.firstOrNull { it.extentType == "00" } // ページ数を表すExtentType "00"
        ?.extentValue
        ?.toIntOrNull() // 文字列をIntに変換

    return Book(
        isbn = isbn,
        title = summary.title ?: "タイトル不明",
        author = summary.author ?: "著者不明",
        coverImageUrl = summary.cover,
        pageCount = pageCount,
        bookSize = BookSizeConverter.convertCcodeToBookSize(cCode) // 判型サイズは後のIssueで設定
    )
}
