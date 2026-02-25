package com.example.bookstack.util

/**
 * ISBN-13形式かどうかを検証する。
 *
 * 検証内容:
 * - 13桁であること
 * - 978または979で始まること（ISBNプレフィックス）
 * - チェックディジットが正しいこと（Modulus 10アルゴリズム）
 *
 * @param code スキャンされたバーコード文字列
 * @return true: 有効なISBN-13, false: 無効
 */
fun isValidISBN13(code: String): Boolean {
    // 1. 桁数チェック
    if (code.length != 13) return false

    // 2. 全て数字かチェック
    if (!code.all { it.isDigit() }) return false

    // 3. ISBNプレフィックスチェック（978または979）
    if (!code.startsWith("978") && !code.startsWith("979")) return false

    // 4. チェックディジット検証（Modulus 10アルゴリズム）
    val checksum = code.take(12).mapIndexed { index, char ->
        val digit = char.digitToInt()
        // 奇数位置(0,2,4...)はそのまま、偶数位置(1,3,5...)は3倍
        if (index % 2 == 0) digit else digit * 3
    }.sum()

    val checkDigit = (10 - (checksum % 10)) % 10
    return code[12].digitToInt() == checkDigit
}
