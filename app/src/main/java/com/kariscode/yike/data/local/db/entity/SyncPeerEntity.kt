package com.kariscode.yike.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 已配对设备集中保存在本地表中，是为了让发现结果、信任关系和最近一次可用状态围绕同一主键管理。
 */
@Entity(tableName = "sync_peer")
data class SyncPeerEntity(
    @PrimaryKey
    val deviceId: String,
    val displayName: String,
    val shortDeviceId: String,
    val encryptedSharedSecret: String,
    val protocolVersion: Int,
    val lastSeenAt: Long,
    val missCount: Int
)
