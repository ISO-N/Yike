package com.kariscode.yike.data.mapper

import com.kariscode.yike.domain.model.Card
import com.kariscode.yike.domain.model.Deck
import com.kariscode.yike.domain.model.Question
import com.kariscode.yike.domain.model.QuestionStatus
import com.kariscode.yike.domain.model.ReviewRating
import java.lang.IllegalArgumentException

/**
 * 复用同一套字段组装入口，是为了让 Deck 映射在实体行、聚合行与后续扩展字段时保持口径一致。
 */
internal fun deckDomainFrom(
    id: String,
    name: String,
    description: String,
    tagsJson: String,
    intervalStepCount: Int,
    archived: Boolean,
    sortOrder: Int,
    createdAt: Long,
    updatedAt: Long
): Deck = Deck(
    id = id,
    name = name,
    description = description,
    tags = decodeTags(tagsJson),
    intervalStepCount = intervalStepCount,
    archived = archived,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Card 字段在多处聚合查询会复用，集中构造器能避免一旦字段新增就要在多处同步补齐。
 */
internal fun cardDomainFrom(
    id: String,
    deckId: String,
    title: String,
    description: String,
    archived: Boolean,
    sortOrder: Int,
    createdAt: Long,
    updatedAt: Long
): Card = Card(
    id = id,
    deckId = deckId,
    title = title,
    description = description,
    archived = archived,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Question 组装在搜索/预览/复习多个入口共享，统一入口能降低不同 SQL Row 对字段默认值的漂移风险。
 */
internal fun questionDomainFrom(
    id: String,
    cardId: String,
    prompt: String,
    answer: String,
    tagsJson: String,
    status: String,
    stageIndex: Int,
    dueAt: Long,
    lastReviewedAt: Long?,
    reviewCount: Int,
    lapseCount: Int,
    createdAt: Long,
    updatedAt: Long
): Question = Question(
    id = id,
    cardId = cardId,
    prompt = prompt,
    answer = answer,
    tags = decodeTags(tagsJson),
    status = decodeStatus(status),
    stageIndex = stageIndex,
    dueAt = dueAt,
    lastReviewedAt = lastReviewedAt,
    reviewCount = reviewCount,
    lapseCount = lapseCount,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Room 状态字段统一走领域枚举自带的存储值，是为了让持久化边界只维护一套状态编码规则。
 */
internal fun encodeStatus(status: QuestionStatus): String = status.storageValue

/**
 * 反向解析收口到领域模型后，数据层就不需要为同一套默认值语义重复维护多个 `when` 分支。
 */
internal fun decodeStatus(status: String): QuestionStatus = QuestionStatus.fromStorageValue(status)

/**
 * 评分字符串保持宽松兜底，是为了让历史备份或异常脏数据不会因为单条记录失效而中断整个读取流程。
 */
internal fun decodeRating(rating: String): ReviewRating = try {
    ReviewRating.valueOf(rating)
} catch (_: IllegalArgumentException) {
    ReviewRating.AGAIN
}

