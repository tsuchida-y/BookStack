package com.example.bookstack.data.util

import com.example.bookstack.data.model.BookSize

/**
 * 書籍データAPI選定理由書(ReasonChoosingAPI.md)に基づいた判型サイズ判定
 */
object BookSizeConverter {

    // ✅ キーワードを定数化（保守性向上）
    private val 文庫_KEYWORDS = listOf(
        "文庫", "bunko", "pocket",
        "岩波文庫", "新潮文庫", "講談社文庫", "角川文庫", "集英社文庫",
        "ちくま文庫", "河出文庫", "光文社古典新訳文庫"
    )

    private val 新書_KEYWORDS = listOf(
        "新書", "shinsho", "選書",
        "岩波新書", "講談社現代新書", "中公新書", "ちくま新書", "集英社新書",
        "光文社新書", "PHPビジネス新書", "角川新書"
    )

    private val 四六判_KEYWORDS = listOf(
        "四六判", "単行本", "並製", "上製", "ハードカバー",
        "単行", "hardcover"
    )

    private val 大型本_KEYWORDS = listOf(
        // 技術書
        "技術", "リファレンス", "オライリー", "翔泳社", "インプレス",
        "technical", "reference", "programming",

        // 芸術・写真
        "画集", "写真集", "作品集", "美術", "art book",

        // 辞書・事典
        "辞典", "事典", "百科事典", "図鑑", "encyclopedia", "dictionary",

        // 絵本・児童書（大判）
        "大型絵本", "ビッグブック", "ビッグ", "big book",

        // 雑誌・ムック
        "ムック", "mook", "増刊"
    )

    private val コミック_KEYWORDS = listOf(
        "コミック", "漫画", "まんが", "マンガ", "comic", "manga",
        "ジャンプ", "マガジン", "サンデー", "ヤング", "ビッグ"
    )

    /**
     * Cコード(例: C0197)の形態コード(3文字目)からBookSizeを判定する
     */
    fun convertCcodeToBookSize(cCode: String?): BookSize {
        if (cCode == null || cCode.length < 3) return BookSize.UNKNOWN

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
     *
     * ✅ 大文字小文字を区別せず、部分一致で判定
     */
    fun convertKeywordsToBookSize(title: String, publisher: String): BookSize {
        val combinedText = "$title $publisher".lowercase()

        return when {
            // 🔹 文庫本の判定（最優先）
            文庫_KEYWORDS.any { it in combinedText } -> BookSize.S

            // 🔹 新書の判定
            新書_KEYWORDS.any { it in combinedText } -> BookSize.M

            // 🔹 コミックの判定
            コミック_KEYWORDS.any { it in combinedText } -> BookSize.M

            // 🔹 四六判の判定
            四六判_KEYWORDS.any { it in combinedText } -> BookSize.M

            // 🔹 大型本の判定
            大型本_KEYWORDS.any { it in combinedText } -> BookSize.XL

            // 🔹 どのキーワードにも該当しない
            else -> BookSize.UNKNOWN
        }
    }
}
