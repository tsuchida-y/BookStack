package com.example.bookstack.data.model

/**
 * アプリケーション内で使用する書籍情報のドメインモデル。
 * 外部APIの構造に依存せず、アプリのビジネスロジックで扱いやすい形式。
 */
data class Book(
    val id: String? = null, // SupabaseなどのバックエンドID、新規作成時はnull
    val isbn: String,
    val title: String,
    val author: String,
    val coverImageUrl: String?, // 書籍の表紙画像URL
    val pageCount: Int?,        // ページ数、情報がない場合はnull
    val bookSize: BookSize? = null // 判型サイズ (S/M/L/XLなど)、初期段階ではnull
)

/**
 * 書籍の判型サイズを表すEnumクラス。
 * Issue 2 で具体的な判定ロジックが実装される。
 */
enum class BookSize {
    S, // Small (文庫、新書など)
    M, // Medium (四六判、B6判など)
    L, // Large (A5判、B5判など)
    XL, // Extra Large (A4判以上など)
    UNKNOWN // 不明な場合
}
