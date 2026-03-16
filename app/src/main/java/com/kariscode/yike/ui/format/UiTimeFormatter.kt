package com.kariscode.yike.ui.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 页面展示时间统一走同一 formatter，是为了让设置页与备份页对"本地时间"的表达保持一致，
 * 后续若要改展示格式时也只需要调整一个位置。
 */
fun formatLocalDateTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(zoneId)
        .toLocalDateTime()
        .toString()

/**
 * 统一管理 UI 展示用的日期时间格式化器。
 */
object UiDateTimeFormatters {
    val PREVIEW_DATE = DateTimeFormatter.ofPattern("M 月 d 日 HH:mm")
}
