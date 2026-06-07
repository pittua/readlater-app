package com.yomiato.ui.components

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("M/d")
private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d HH:mm")

/** epoch millis を "M/d"（当日は "HH:mm"）で表示。 */
fun formatSavedDate(epochMillis: Long): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
    val today = Instant.now().atZone(zone).toLocalDate()
    return if (date == today) {
        Instant.ofEpochMilli(epochMillis).atZone(zone).format(DateTimeFormatter.ofPattern("HH:mm"))
    } else {
        date.format(dateFormatter)
    }
}

fun formatDateTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dateTimeFormatter)

/** 推定読了時間を "5分" 形式に。null なら null。 */
fun formatReadMinutes(minutes: Int?): String? = minutes?.let { "${it}分" }
