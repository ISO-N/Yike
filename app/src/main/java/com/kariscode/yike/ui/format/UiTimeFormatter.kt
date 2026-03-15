package com.kariscode.yike.ui.format

import java.time.Instant
import java.time.ZoneId

/**
 * 页面展示时间统一走同一 formatter，是为了让设置页与备份页对“本地时间”的表达保持一致，
 * 后续若要改展示格式时也只需要调整一个位置。
 */
fun formatLocalDateTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(zoneId)
        .toLocalDateTime()
        .toString()

