package com.kariscode.yike.data.sync

/**
 * 局域网同步常量集中管理，是为了让 NSD、协议和心跳策略围绕同一套参数演进，而不是散落成多个魔法数字。
 */
object LanSyncConfig {
    const val PROTOCOL_VERSION: Int = 2
    const val SERVICE_TYPE: String = "_yike._tcp."
    const val PORT_RANGE_START: Int = 9420
    const val PORT_RANGE_END: Int = 9439
    const val HEARTBEAT_INTERVAL_MILLIS: Long = 10_000L
    const val HEARTBEAT_MAX_MISSES: Int = 3
    const val DEFAULT_PREVIEW_LIMIT: Int = 200
}
