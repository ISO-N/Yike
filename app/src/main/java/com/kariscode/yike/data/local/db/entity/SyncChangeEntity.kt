package com.kariscode.yike.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * SyncChangeEntity 以不可变流水保存每一次可同步改动，
 * 是为了让增量同步建立在“明确发生过哪些变化”之上，而不是依赖脆弱的时间戳猜测。
 */
@Entity(
    tableName = "sync_change",
    indices = [
        Index(value = ["entityType", "entityId", "seq"], name = "sync_change_entity_seq_idx"),
        Index(value = ["seq"], name = "sync_change_seq_idx")
    ]
)
data class SyncChangeEntity(
    @PrimaryKey(autoGenerate = true)
    val seq: Long = 0L,
    val entityType: String,
    val entityId: String,
    val operation: String,
    val summary: String,
    val payloadJson: String?,
    val payloadHash: String,
    val modifiedAt: Long
)
