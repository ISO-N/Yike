package com.kariscode.yike.data.sync

import com.kariscode.yike.data.local.db.dao.SyncChangeDao
import com.kariscode.yike.data.local.db.entity.SyncChangeEntity
import com.kariscode.yike.domain.model.Card
import com.kariscode.yike.domain.model.Deck
import com.kariscode.yike.domain.model.Question
import com.kariscode.yike.domain.model.ReviewRecord
import com.kariscode.yike.domain.model.SyncChangeOperation
import com.kariscode.yike.domain.model.SyncEntityType
import com.kariscode.yike.domain.model.SyncedAppSettings

/**
 * 变更记录器把“如何序列化和摘要同步实体”集中起来，
 * 是为了让业务仓储只表达自己改了什么，而不是各自手写 payload、hash 和 summary 规则。
 */
class LanSyncChangeRecorder(
    private val syncChangeDao: SyncChangeDao,
    private val crypto: LanSyncCrypto
) {
    /**
     * 设置更新必须单独落成一条 journal，是为了让提醒和主题这类跨设备偏好也能纳入双向同步。
     */
    suspend fun recordSettingsUpsert(settings: SyncedAppSettings, modifiedAt: Long) {
        val payload = settings.toPayload()
        recordChange(
            entityType = SyncEntityType.SETTINGS,
            entityId = SETTINGS_ENTITY_ID,
            operation = SyncChangeOperation.UPSERT,
            summary = "应用设置",
            payloadJson = LanSyncJson.json.encodeToString(SyncSettingsPayload.serializer(), payload),
            modifiedAt = modifiedAt
        )
    }

    /**
     * Deck upsert 记录名称作为摘要，是为了让冲突列表和预览页无需再反查数据库也能展示可读标题。
     */
    suspend fun recordDeckUpsert(deck: Deck) {
        val payload = deck.toPayload()
        recordChange(
            entityType = SyncEntityType.DECK,
            entityId = deck.id,
            operation = SyncChangeOperation.UPSERT,
            summary = deck.name,
            payloadJson = LanSyncJson.json.encodeToString(SyncDeckPayload.serializer(), payload),
            modifiedAt = deck.updatedAt
        )
    }

    /**
     * Card upsert 记录标题摘要，是为了让多端冲突能直接提示用户是哪张卡片发生了双改。
     */
    suspend fun recordCardUpsert(card: Card) {
        val payload = card.toPayload()
        recordChange(
            entityType = SyncEntityType.CARD,
            entityId = card.id,
            operation = SyncChangeOperation.UPSERT,
            summary = card.title,
            payloadJson = LanSyncJson.json.encodeToString(SyncCardPayload.serializer(), payload),
            modifiedAt = card.updatedAt
        )
    }

    /**
     * Question 摘要只截取 prompt 前缀，是为了避免在同步列表里直接泄露过长答案内容或造成版面噪音。
     */
    suspend fun recordQuestionUpsert(question: Question) {
        val payload = question.toPayload()
        recordChange(
            entityType = SyncEntityType.QUESTION,
            entityId = question.id,
            operation = SyncChangeOperation.UPSERT,
            summary = question.prompt.take(MAX_SUMMARY_LENGTH),
            payloadJson = LanSyncJson.json.encodeToString(SyncQuestionPayload.serializer(), payload),
            modifiedAt = question.updatedAt
        )
    }

    /**
     * 复习记录作为追加型事件写入 journal，是为了让另一台设备能把这次评分视为完整历史事件而不是重新推导题目状态。
     */
    suspend fun recordReviewRecordInsert(record: ReviewRecord) {
        val payload = record.toPayload()
        recordChange(
            entityType = SyncEntityType.REVIEW_RECORD,
            entityId = record.id,
            operation = SyncChangeOperation.UPSERT,
            summary = "评分 ${record.rating.name}",
            payloadJson = LanSyncJson.json.encodeToString(SyncReviewRecordPayload.serializer(), payload),
            modifiedAt = record.reviewedAt
        )
    }

    /**
     * 删除必须保留 tombstone，而不是简单丢掉记录，是为了让离线设备在后续重连时仍能收到删除语义。
     */
    suspend fun recordDelete(
        entityType: SyncEntityType,
        entityId: String,
        summary: String,
        modifiedAt: Long
    ) {
        recordChange(
            entityType = entityType,
            entityId = entityId,
            operation = SyncChangeOperation.DELETE,
            summary = summary,
            payloadJson = null,
            modifiedAt = modifiedAt
        )
    }

    /**
     * 所有 journal 写入共用同一入口，是为了把 hash 算法和落库字段解释固定在单点维护。
     */
    private suspend fun recordChange(
        entityType: SyncEntityType,
        entityId: String,
        operation: SyncChangeOperation,
        summary: String,
        payloadJson: String?,
        modifiedAt: Long
    ) {
        val payloadHash = crypto.sha256(payloadJson.orEmpty())
        syncChangeDao.insert(
            SyncChangeEntity(
                entityType = entityType.storageValue(),
                entityId = entityId,
                operation = operation.storageValue(),
                summary = summary.take(MAX_SUMMARY_LENGTH),
                payloadJson = payloadJson,
                payloadHash = payloadHash,
                modifiedAt = modifiedAt
            )
        )
    }

    private companion object {
        private const val SETTINGS_ENTITY_ID: String = "app_settings"
        private const val MAX_SUMMARY_LENGTH: Int = 48
    }
}
