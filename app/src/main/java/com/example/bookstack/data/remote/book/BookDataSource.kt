package com.example.bookstack.data.remote.book

import com.example.bookstack.data.model.Book

/**
 * 外部の書籍APIから書籍情報を取得するためのデータソースインターフェース。
 */
interface BookDataSource {
    /**
     * ISBNコードを元に書籍情報を取得し、Bookドメインモデルとして返す。
     * @param isbn 検索対象のISBNコード。
     * @return ISBNに対応するBookオブジェクト、見つからない場合はnull。
     */
    suspend fun getBookByIsbn(isbn: String): Book?

    /**
     * キーワードを元に書籍を検索し、Bookドメインモデルのリストとして返す。(将来的な実装)
     * @param keyword 検索キーワード。
     * @return 検索結果のBookオブジェクトのリスト。
     */
    suspend fun searchBooksByKeyword(keyword: String): List<Book>
}