package com.example.bookstack.data.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * reading_logsテーブルのDTO（Data Transfer Object）。
 * Supabase Postgrestとのデータ送受信に使用。
 */
@Serializable
data class ReadingLogDto(
    @SerialName("id")
    val id: String? = null,

    @SerialName("user_id")
    val userId: String,

    @SerialName("book_id")
    val bookId: String,

    @SerialName("read_date")
    val readDate: LocalDate,

    @SerialName("pages_read")
    val pagesRead: Int,

    @SerialName("duration_mins")
    val durationMins: Int? = null
)

/**
 * ReadingLogドメインモデルからDTOへの変換。
 */
fun ReadingLog.toReadingLogDto(userId: String): ReadingLogDto {
    return ReadingLogDto(
        id = this.id,
        userId = userId,
        bookId = this.bookId,
        readDate = this.readDate,
        pagesRead = this.pagesRead,
        durationMins = this.durationMins
    )
}

/**
 * DTOからReadingLogドメインモデルへの変換。
 */
fun ReadingLogDto.toReadingLog(): ReadingLog {
    return ReadingLog(
        id = this.id,
        bookId = this.bookId,
        readDate = this.readDate,
        pagesRead = this.pagesRead,
        durationMins = this.durationMins
    )
}
