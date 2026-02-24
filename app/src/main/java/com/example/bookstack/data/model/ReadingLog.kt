package com.example.bookstack.data.model

import kotlinx.datetime.LocalDate

/**
 * 読書記録のドメインモデル。
 * ヒートマップ表示のために日々の読書進捗を記録する。
 */
data class ReadingLog(
    val id: String? = null,         // Supabase ID、新規作成時はnull
    val bookId: String,             // 関連書籍のID
    val readDate: LocalDate,        // 読んだ日付
    val pagesRead: Int,             // その日に読んだページ数
    val durationMins: Int? = null   // 読書時間（分）、オプション
)
