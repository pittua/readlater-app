package com.yomiato.data.local

import androidx.room.TypeConverter

/** Room がそのまま扱えない型の変換。 */
class Converters {
    @TypeConverter
    fun fromExtractionStatus(value: ExtractionStatus): String = value.name

    @TypeConverter
    fun toExtractionStatus(value: String): ExtractionStatus =
        runCatching { ExtractionStatus.valueOf(value) }.getOrDefault(ExtractionStatus.PENDING)
}
