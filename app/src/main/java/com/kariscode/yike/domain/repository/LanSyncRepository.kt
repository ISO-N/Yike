package com.kariscode.yike.domain.repository

import com.kariscode.yike.domain.model.LanSyncConflictResolution
import com.kariscode.yike.domain.model.LanSyncPeer
import com.kariscode.yike.domain.model.LanSyncPreview
import com.kariscode.yike.domain.model.LanSyncSessionState
import kotlinx.coroutines.flow.Flow

/**
 * 局域网同步仓储对外只暴露会话级动作和状态，
 * 是为了把发现、配对、网络传输和数据应用压缩到单一协调点，避免页面层跨多组件手工编排。
 */
interface LanSyncRepository {
    /**
     * 同步页持续订阅单一状态流即可获得设备列表、预览、进度和错误，
     * 这样页面不必理解底层存在多少个并行子流程。
     */
    fun observeSessionState(): Flow<LanSyncSessionState>

    /**
     * 会话启动后才允许发现与对外广播，是为了继续保持“只在用户主动使用时暴露局域网能力”的产品边界。
     */
    suspend fun startSession()

    /**
     * 结束会话时统一停掉广播、发现和心跳，可避免后台继续暴露设备信息或占用网络资源。
     */
    suspend fun stopSession()

    /**
     * 设备名通过仓储入口更新，是为了确保本地持久化、对外广播和页面展示使用同一份身份信息。
     */
    suspend fun updateLocalDisplayName(displayName: String)

    /**
     * 预览阶段先完成配对、远端摘要读取和冲突分析，
     * 这样用户能在真正传输数据前看到这次同步将产生的双向影响。
     */
    suspend fun prepareSync(peer: LanSyncPeer, pairingCode: String?): LanSyncPreview

    /**
     * 确认冲突决议后再真正执行同步，是为了让“用户选了什么”与“协议实际应用了什么”保持一一对应。
     */
    suspend fun runSync(preview: LanSyncPreview, resolutions: List<LanSyncConflictResolution>)

    /**
     * 取消动作集中在仓储层处理，是为了由同一处统一终止网络请求、心跳和会话级任务。
     */
    suspend fun cancelActiveSync()
}
