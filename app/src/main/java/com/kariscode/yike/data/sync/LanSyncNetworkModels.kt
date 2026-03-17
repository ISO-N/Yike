package com.kariscode.yike.data.sync

import kotlinx.serialization.Serializable

/**
 * hello 响应只暴露发现阶段必须知道的最小资料，是为了在未配对前不把摘要和数据规模直接暴露给局域网内其他设备。
 */
@Serializable
data class LanSyncHelloResponse(
    val deviceId: String,
    val displayName: String,
    val shortDeviceId: String,
    val protocolVersion: Int,
    val pairingNonce: String
)

/**
 * 加密信封单独建模后，请求和响应都能复用同一结构，不必为每个端点重复声明 iv/cipherText 字段。
 */
@Serializable
data class LanSyncEncryptedEnvelope(
    val iv: String,
    val cipherText: String
)

/**
 * 首次配对请求显式带上发起方身份，是为了让被动设备在建立信任时同时拿到对端的基础资料。
 */
@Serializable
data class LanSyncPairInitRequest(
    val initiatorDeviceId: String,
    val initiatorDisplayName: String,
    val payload: LanSyncEncryptedEnvelope
)

/**
 * 配对响应继续使用加密信封，是为了让“配对是否成功”这一结论也不会在局域网明文暴露。
 */
@Serializable
data class LanSyncPairInitResponse(
    val payload: LanSyncEncryptedEnvelope
)

/**
 * 配对载荷只在临时配对密钥保护下传输共享密钥，是为了把首次信任建立的敏感内容尽量缩到最小范围。
 */
@Serializable
data class LanSyncPairInitPayload(
    val sharedSecret: String
)

/**
 * 配对响应载荷只表达最小确认信息，是为了让请求方知道对端已经持久化共享密钥而无需额外拉更多数据。
 */
@Serializable
data class LanSyncPairInitResponsePayload(
    val accepted: Boolean
)

/**
 * 受保护请求统一带设备 id 和加密体，是为了让服务端先定位可信设备，再用对应共享密钥解密业务载荷。
 */
@Serializable
data class LanSyncProtectedRequest(
    val requesterDeviceId: String,
    val payload: LanSyncEncryptedEnvelope
)

/**
 * 受保护响应保持同一信封结构，是为了让客户端围绕统一解密路径处理 push/pull/ack/ping 等不同响应。
 */
@Serializable
data class LanSyncProtectedResponse(
    val payload: LanSyncEncryptedEnvelope
)

/**
 * ping 载荷只保留请求时间，是为了让对端能够回传健康信息同时减少无意义字段。
 */
@Serializable
data class LanSyncPingPayload(
    val requestedAt: Long
)

/**
 * ping 响应返回最新显示名与协议版本，是为了让发现页能顺带刷新设备卡片，而不是把心跳只当作“活着没”。
 */
@Serializable
data class LanSyncPingResponsePayload(
    val deviceId: String,
    val displayName: String,
    val shortDeviceId: String,
    val protocolVersion: Int,
    val respondedAt: Long
)

/**
 * pull 请求允许只拉 header，是为了让冲突预览能先基于轻量摘要做决定，再按需传完整载荷。
 */
@Serializable
data class LanSyncPullChangesPayload(
    val afterSeq: Long,
    val headersOnly: Boolean
)

/**
 * pull 响应同时回传最新 seq，是为了让发起方在 preview 阶段就知道远端当前高水位。
 */
@Serializable
data class LanSyncPullChangesResponsePayload(
    val changes: List<SyncChangePayload>,
    val latestSeq: Long
)

/**
 * push 请求显式带 sessionId，是为了在网络失败重试时让服务端识别同一会话并避免重复应用。
 */
@Serializable
data class LanSyncPushChangesPayload(
    val sessionId: String,
    val changes: List<SyncChangePayload>
)

/**
 * push 响应只返回远端实际应用到的本地最大 seq，是为了让发起方只推进确定已被对端接收的 cursor。
 */
@Serializable
data class LanSyncPushChangesResponsePayload(
    val appliedLocalSeqMax: Long
)

/**
 * ack 请求用于确认“我已经把你的哪些变更落到本地了”，从而把双向 cursor 的责任显式拆开。
 */
@Serializable
data class LanSyncAckPayload(
    val sessionId: String,
    val remoteSeqApplied: Long
)

/**
 * ack 响应只需返回是否接受即可，因为对端高水位已经包含在请求体中，无需重复回显。
 */
@Serializable
data class LanSyncAckResponsePayload(
    val accepted: Boolean
)
