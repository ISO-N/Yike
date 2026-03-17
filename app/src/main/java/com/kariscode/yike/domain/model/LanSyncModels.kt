package com.kariscode.yike.domain.model

/**
 * 同步实体类型显式枚举化，是为了让冲突检测、传输编排和落库顺序围绕同一份语义收敛，
 * 避免字符串常量散落后逐步出现“网络层一种叫法、数据层另一种叫法”的漂移。
 */
enum class SyncEntityType {
    SETTINGS,
    DECK,
    CARD,
    QUESTION,
    REVIEW_RECORD
}

/**
 * 变更操作只有 upsert 和 delete 两种，是为了把“同步要做的最小动作”固定下来，
 * 这样 journal、远端应用和冲突判定都不必再理解 UI 侧的细粒度编辑过程。
 */
enum class SyncChangeOperation {
    UPSERT,
    DELETE
}

/**
 * 可信状态单独建模后，页面可以明确区分“发现到了设备”和“已经允许互传数据”这两个阶段，
 * 从而把配对风险从同步主流程里拆开。
 */
enum class LanSyncTrustState {
    TRUSTED,
    UNTRUSTED
}

/**
 * 连接健康状态显式暴露，是为了让心跳结果能直接影响 UI 提示，而不必把“最后出现时间”硬解释成可用性。
 */
enum class LanSyncPeerHealth {
    AVAILABLE,
    STALE,
    OFFLINE
}

/**
 * 已发现设备保留显示信息、网络地址和配对状态，是为了让同步页在真正开始拉取数据前就能完成设备选择。
 */
data class LanSyncPeer(
    val deviceId: String,
    val displayName: String,
    val shortDeviceId: String,
    val hostAddress: String,
    val port: Int,
    val protocolVersion: Int,
    val trustState: LanSyncTrustState,
    val health: LanSyncPeerHealth,
    val lastSeenAt: Long
)

/**
 * 本机配置单独抽成领域模型，是为了把“同步身份”和“业务设置”分开，避免换设备名误伤提醒或主题等设置。
 */
data class LanSyncLocalProfile(
    val deviceId: String,
    val displayName: String,
    val shortDeviceId: String,
    val pairingCode: String
)

/**
 * 同步预览提前汇总双向变更规模，是为了让用户在真正传输前先知道会发生多少上传、下载和人工决策。
 */
data class LanSyncPreview(
    val peer: LanSyncPeer,
    val localChangeCount: Int,
    val remoteChangeCount: Int,
    val settingsChangeCount: Int,
    val conflicts: List<LanSyncConflictItem>,
    val isFirstPairing: Boolean
)

/**
 * 冲突项显式带上双方摘要和原因，是为了让确认页不必在展示时再次反查 journal 才能拼出可读文案。
 */
data class LanSyncConflictItem(
    val entityType: SyncEntityType,
    val entityId: String,
    val summary: String,
    val localSummary: String?,
    val remoteSummary: String?,
    val reason: String
)

/**
 * 冲突决议只保留三种选择，是为了让协议执行结果稳定可复现，避免页面层临时发明未定义行为。
 */
enum class LanSyncConflictChoice {
    KEEP_LOCAL,
    KEEP_REMOTE,
    SKIP
}

/**
 * 冲突决议单独建模，是为了让“用户做了什么选择”可以和“最终执行了什么变更”一一对应，便于测试与排查。
 */
data class LanSyncConflictResolution(
    val entityType: SyncEntityType,
    val entityId: String,
    val choice: LanSyncConflictChoice
)

/**
 * 进度阶段单独建模后，页面可以用稳定语义展示当前步骤，而不是用分散布尔值拼凑“现在大概在做什么”。
 */
enum class LanSyncStage {
    IDLE,
    DISCOVERING,
    PAIRING,
    PREVIEWING,
    TRANSFERRING,
    APPLYING,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * 统一进度模型同时保留字节级和条目级统计，是为了兼顾大文件传输感知和小批量实体应用的可读性。
 */
data class LanSyncProgress(
    val stage: LanSyncStage,
    val message: String,
    val bytesTransferred: Long,
    val totalBytes: Long?,
    val itemsProcessed: Int,
    val totalItems: Int?
)

/**
 * 失败原因显式枚举化，是为了让 UI 文案保持稳定友好，同时允许日志继续保留底层异常细节。
 */
enum class LanSyncFailureReason {
    DISCOVERY_FAILED,
    PAIRING_FAILED,
    PROTOCOL_MISMATCH,
    AUTH_FAILED,
    NETWORK_TIMEOUT,
    HASH_MISMATCH,
    APPLY_FAILED,
    CANCELLED,
    UNKNOWN
}

/**
 * 会话状态把本机信息、设备列表、预览、进度和错误收口到同一模型中，
 * 是为了让 ViewModel 能按状态机推进，而不是同时维护多组彼此独立的临时字段。
 */
data class LanSyncSessionState(
    val localProfile: LanSyncLocalProfile,
    val peers: List<LanSyncPeer>,
    val isSessionActive: Boolean,
    val preview: LanSyncPreview?,
    val progress: LanSyncProgress,
    val activeFailure: LanSyncFailureReason?,
    val message: String?
)
