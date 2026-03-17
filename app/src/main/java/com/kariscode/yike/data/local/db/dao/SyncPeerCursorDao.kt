package com.kariscode.yike.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kariscode.yike.data.local.db.entity.SyncPeerCursorEntity

/**
 * Cursor 单独成表是为了把“我发到哪了”和“我收到了哪了”与设备主数据解耦，
 * 这样协议恢复逻辑只需更新游标，不必碰配对密钥等敏感字段。
 */
@Dao
interface SyncPeerCursorDao {
    /**
     * cursor 通过 replace 写入，是为了让成功同步后能原子覆盖为新的高水位。
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(cursor: SyncPeerCursorEntity): Long

    /**
     * 单设备 cursor 查询单独提供，是为了让预览与执行阶段都能基于同一进度事实做判断。
     */
    @Query("SELECT * FROM sync_peer_cursor WHERE deviceId = :deviceId LIMIT 1")
    suspend fun findById(deviceId: String): SyncPeerCursorEntity?
}
