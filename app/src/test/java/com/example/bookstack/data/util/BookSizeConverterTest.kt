package com.example.bookstack.data.util

import com.example.bookstack.data.model.BookSize
import org.junit.Assert
import org.junit.Test

class BookSizeConverterTest {

    @Test
    fun `Cコードの3桁目が1ならサイズSを返す`() {
        val result = BookSizeConverter.convertCcodeToBookSize("C0197") // 文庫
        Assert.assertEquals(BookSize.S, result)
    }

    @Test
    fun `Cコードの3桁目が2ならサイズMを返す`() {
        val result = BookSizeConverter.convertCcodeToBookSize("C0297") // 新書
        Assert.assertEquals(BookSize.M, result)
    }

    @Test
    fun `Cコードの3桁目が4ならサイズXLを返す`() {
        val result = BookSizeConverter.convertCcodeToBookSize("C0497") // ムック
        Assert.assertEquals(BookSize.XL, result)
    }

    @Test
    fun `タイトルに文庫が含まれていればサイズSを返す`() {
        // 第一引数にタイトル、第二引数に出版社を渡すように修正
        val result = BookSizeConverter.convertKeywordsToBookSize(title = "羅生門 文庫版", publisher = "岩波書店")
        Assert.assertEquals(BookSize.S, result)
    }

    @Test
    fun `オライリーの書籍であればサイズXLを返す`() {
        val result = BookSizeConverter.convertKeywordsToBookSize(title = "Kotlinイン・アクション", publisher = "オライリー・ジャパン")
        Assert.assertEquals(BookSize.XL, result)
    }

    @Test
    fun `不正なCコードの場合はUNKNOWNを返す`() {
        Assert.assertEquals(BookSize.UNKNOWN, BookSizeConverter.convertCcodeToBookSize("ABC"))
        Assert.assertEquals(BookSize.UNKNOWN, BookSizeConverter.convertCcodeToBookSize(null))
    }

    @Test
    fun `convertKeywordsToBookSize - 岩波文庫を文庫本と判定`() {
        val result = BookSizeConverter.convertKeywordsToBookSize(
            title = "こころ",
            publisher = "岩波文庫"
        )
        Assert.assertEquals(BookSize.S, result)
    }

    @Test
    fun `convertKeywordsToBookSize - 講談社現代新書を新書と判定`() {
        val result = BookSizeConverter.convertKeywordsToBookSize(
            title = "サピエンス全史",
            publisher = "河出新書"
        )
        Assert.assertEquals(BookSize.M, result)
    }

    @Test
    fun `convertKeywordsToBookSize - ハードカバーを四六判と判定`() {
        val result = BookSizeConverter.convertKeywordsToBookSize(
            title = "プログラミング入門",
            publisher = "単行本ハードカバー"
        )
        Assert.assertEquals(BookSize.M, result)
    }

    @Test
    fun `convertKeywordsToBookSize - オライリーを大型本と判定`() {
        val result = BookSizeConverter.convertKeywordsToBookSize(
            title = "Effective Java",
            publisher = "オライリージャパン"
        )
        Assert.assertEquals(BookSize.XL, result)
    }

}