package com.example.bookstack.data.util

import com.example.bookstack.data.model.BookSize

/**
 * 書籍データAPI選定理由書(ReasonChoosingAPI.md)に基づいた判型サイズ判定
 */
object BookSizeConverter {

    /**
     * Cコード(例: C0197)の形態コード(3文字目)からBookSizeを判定する
     */
    fun convertCcodeToBookSize(cCode: String?): BookSize {
        if (cCode == null || cCode.length < 3) return BookSize.UNKNOWN

        // Cxxxx の 3文字目(形態)を取得
        val formCode = cCode.getOrNull(2)

        return when (formCode) {
            '1' -> BookSize.S   // 文庫
            '2', '9' -> BookSize.M // 新書、コミック
            '0', '3' -> BookSize.L // 単行本、全集・双書
            '4', '5', '6', '7' -> BookSize.XL // ムック、事典、図鑑、絵本
            else -> BookSize.UNKNOWN
        }
    }

    /**
     * キーワードからの二次判定
     * Cコードが '0'(単行本) の場合や、取得不能な場合に使用する
     */
    fun convertKeywordsToBookSize(title: String, publisher: String): BookSize {
        val target = "$title $publisher"

        return when {
            // 技術書・大型本の判定
            target.contains("技術") ||
                    target.contains("リファレンス") ||
                    target.contains("オライリー") -> BookSize.XL

            // その他のキーワード補助
            target.contains("文庫") -> BookSize.S
            target.contains("新書") -> BookSize.M

            else -> BookSize.UNKNOWN
        }
    }
}