package com.kariscode.yike.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 对每个已信任设备分别维护双向 cursor，
 * 是为了在一端上传成功、另一端尚未确认时仍能保持后续会话可恢复、可重放。
 */
@Entity(tableName = "sync_peer_cursor")
data class SyncPeerCursorEntity(
    @PrimaryKey
    val deviceId: String,
    val lastLocalSeqAckedByPeer: Long,
    val lastRemoteSeqAppliedLocally: Long,
    val lastSessionId: String?
)
