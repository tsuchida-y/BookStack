package com.example.bookstack.util

import org.junit.Assert.*
import org.junit.Test

/**
 * ISBN-13検証ロジックのテスト
 */
class IsbnValidatorTest {

    @Test
    fun `有効なISBN-13コードを正しく検証できる`() {
        // リーダブルコード（実在する書籍）
        assertTrue(isValidISBN13("9784873119038"))

        // 978プレフィックス
        assertTrue(isValidISBN13("9784048930598"))

        // 979プレフィックス
        assertTrue(isValidISBN13("9791234567896"))
    }

    @Test
    fun `桁数が13以外の場合はfalseを返す`() {
        // 12桁
        assertFalse(isValidISBN13("978487311903"))

        // 14桁
        assertFalse(isValidISBN13("97848731190384"))

        // 空文字
        assertFalse(isValidISBN13(""))
    }

    @Test
    fun `978または979で始まらない場合はfalseを返す`() {
        // 456で始まる（価格コードなど）
        assertFalse(isValidISBN13("4567890123456"))

        // 123で始まる
        assertFalse(isValidISBN13("1234567890123"))
    }

    @Test
    fun `チェックディジットが正しくない場合はfalseを返す`() {
        // 最後の桁を変更（チェックディジットが無効）
        assertFalse(isValidISBN13("9784873119039")) // 正: 9784873119038
        assertFalse(isValidISBN13("9784048930590")) // 正: 9784048930598
    }

    @Test
    fun `数字以外の文字が含まれる場合はfalseを返す`() {
        // ハイフン付き
        assertFalse(isValidISBN13("978-4-87311-903-8"))

        // アルファベット混入
        assertFalse(isValidISBN13("978487311903A"))

        // スペース混入
        assertFalse(isValidISBN13("978 4873119038"))
    }

    @Test
    fun `チェックディジット計算が正しく動作する`() {
        // 手動計算で確認した有効なISBN-13
        // 9784873119038の検証:
        // 9*1 + 7*3 + 8*1 + 4*3 + 8*1 + 7*3 + 3*1 + 1*3 + 1*1 + 9*3 + 0*1 + 3*3
        // = 9 + 21 + 8 + 12 + 8 + 21 + 3 + 3 + 1 + 27 + 0 + 9 = 122
        // 122 % 10 = 2
        // 10 - 2 = 8 (チェックディジット)
        assertTrue(isValidISBN13("9784873119038"))
    }

    @Test
    fun `実在する複数の書籍ISBNを検証できる`() {
        // よく使われる技術書のISBN
        val validISBNs = listOf(
            "9784873119038", // リーダブルコード
            "9784873119045", // リファクタリング
            "9784873118222", // 実践Terraform
            "9784295013341", // Androidアプリ開発の極意
            "9784048930598"  // ソードアート・オンライン
        )

        validISBNs.forEach { isbn ->
            assertTrue("ISBN $isbn should be valid", isValidISBN13(isbn))
        }
    }
}
