package com.kariscode.yike.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kariscode.yike.data.local.db.entity.SyncPeerEntity
import kotlinx.coroutines.flow.Flow

/**
 * SyncPeerDao 收口已信任设备和心跳状态的持久化，是为了让发现层只关心网络事件，不承担信任存储职责。
 */
@Dao
interface SyncPeerDao {
    /**
     * 已信任设备以 upsert 方式写回，是为了让重复配对或名称刷新都走同一条持久化路径。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(peer: SyncPeerEntity): Long

    /**
     * 同步页需要持续感知可信设备变化，因此直接暴露 Flow 能让页面不必手动刷新信任状态。
     */
    @Query("SELECT * FROM sync_peer ORDER BY displayName ASC")
    fun observePeers(): Flow<List<SyncPeerEntity>>

    /**
     * 发现与心跳阶段只需要一次性快照时，直接查询列表可以避免为了单次合并状态额外建立订阅。
     */
    @Query("SELECT * FROM sync_peer ORDER BY displayName ASC")
    suspend fun listAll(): List<SyncPeerEntity>

    /**
     * 单设备查询集中在 DAO 中，是为了让协议层无需知道表结构即可完成鉴权与状态更新。
     */
    @Query("SELECT * FROM sync_peer WHERE deviceId = :deviceId LIMIT 1")
    suspend fun findById(deviceId: String): SyncPeerEntity?

    /**
     * 心跳结果只更新必要列，是为了避免每次网络探测都把整行实体重新写一遍。
     */
    @Query(
        "UPDATE sync_peer SET lastSeenAt = :lastSeenAt, missCount = :missCount, displayName = :displayName, protocolVersion = :protocolVersion WHERE deviceId = :deviceId"
    )
    suspend fun updateHeartbeat(
        deviceId: String,
        displayName: String,
        protocolVersion: Int,
        lastSeenAt: Long,
        missCount: Int
    ): Int
}
