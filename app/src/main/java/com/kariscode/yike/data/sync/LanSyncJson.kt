package com.kariscode.yike.data.sync

import kotlinx.serialization.json.Json

/**
 * 同步协议与 journal 需要稳定 JSON 口径，因此单独维护编解码配置能避免被备份格式的 prettyPrint 等展示需求牵连。
 */
object LanSyncJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
}
