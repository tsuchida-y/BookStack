package com.example.bookstack.data.repository

import com.example.bookstack.data.model.Book
import com.example.bookstack.data.model.BookSize
import com.example.bookstack.data.remote.book.GoogleBooksDataSource
import com.example.bookstack.data.remote.book.OpenBdDataSource
import com.example.bookstack.data.util.BookSizeConverter

/**
 * 書籍情報を取得するための唯一の信頼できる情報源 (Single Source of Truth)。
 * 複数のデータソース（OpenBD, Google Books）を統合し、判型サイズ判定を含む
 * 完全な書籍情報を提供する。
 *
 * @param openBdDataSource OpenBD APIのデータソース（優先）
 * @param googleBooksDataSource Google Books APIのデータソース（フォールバック）
 */
class BookRepository(
    private val openBdDataSource: OpenBdDataSource,
    private val googleBooksDataSource: GoogleBooksDataSource
) {
    /**
     * 書籍情報の取得優先順位:
     * 1. OpenBD APIでISBN検索
     * 2. Google Books APIでISBN検索（OpenBDで見つからない場合）
     * 3. Google Books APIでキーワード検索（ISBNで見つからず、keywordが指定されている場合）
     *
     * @param isbn 検索対象のISBNコード
     * @param keyword 検索キーワード（オプション、ISBNで見つからない場合に使用）
     * @return 書籍情報（Book）、見つからない場合はnull
     */
    suspend fun getBookDetails(isbn: String, keyword: String? = null): Book? {
        // 1. OpenBD APIでISBN検索を試行
        val bookFromOpenBd = openBdDataSource.getBookByIsbn(isbn)
        if (bookFromOpenBd != null) {
            return enhanceBookWithSize(bookFromOpenBd)
        }

        // 2. OpenBDで見つからない場合、Google Books APIでISBN検索
        val bookFromGoogleBooks = googleBooksDataSource.getBookByIsbn(isbn)
        if (bookFromGoogleBooks != null) {
            return enhanceBookWithSize(bookFromGoogleBooks)
        }

        // 3. ISBNでも見つからず、keywordが指定されている場合はキーワード検索
        if (keyword != null) {
            val booksFromKeyword = googleBooksDataSource.searchBooksByKeyword(keyword)
            val firstBook = booksFromKeyword.firstOrNull()
            if (firstBook != null) {
                return enhanceBookWithSize(firstBook)
            }
        }

        // 全てのデータソースで見つからなかった場合
        return null
    }

    /**
     * 書籍情報に判型サイズを付与する。
     * 既に判型サイズが設定されている場合は、それを優先する。
     * UNKNOWNまたはnullの場合は、タイトルと著者から再判定を試みる。
     *
     * @param book 元の書籍情報
     * @return 判型サイズが付与された書籍情報
     */
    private fun enhanceBookWithSize(book: Book): Book {
        // 既に有効なbookSizeが設定されている場合（UNKNOWNでない）はそのまま返す
        if (book.bookSize != null && book.bookSize != BookSize.UNKNOWN) {
            return book
        }

        // UNKNOWNまたはnullの場合、キーワードからの二次判定を試行
        // タイトルと著者情報を使用して判定
        val sizeFromKeywords = BookSizeConverter.convertKeywordsToBookSize(
            title = book.title,
            publisher = book.author // 出版社情報がない場合、著者で代用
        )

        // 二次判定でもUNKNOWNの場合は元のbookSizeを維持、それ以外は更新
        val finalSize = if (sizeFromKeywords != BookSize.UNKNOWN) {
            sizeFromKeywords
        } else {
            book.bookSize ?: BookSize.UNKNOWN
        }

        return book.copy(bookSize = finalSize)
    }
}
