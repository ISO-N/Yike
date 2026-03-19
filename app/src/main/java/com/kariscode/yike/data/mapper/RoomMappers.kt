package com.kariscode.yike.data.mapper

import com.kariscode.yike.data.local.db.entity.CardEntity
import com.kariscode.yike.data.local.db.entity.DeckEntity
import com.kariscode.yike.data.local.db.entity.QuestionEntity
import com.kariscode.yike.data.local.db.entity.ReviewRecordEntity
import com.kariscode.yike.domain.model.Card
import com.kariscode.yike.domain.model.Deck
import com.kariscode.yike.domain.model.Question
import com.kariscode.yike.domain.model.ReviewRecord

/**
 * 映射层的存在是为了隔离 Room 字段与 domain 语义，
 * 这样后续调整索引字段或序列化细节时，不会把变化直接传播到业务与 UI。
 */

/**
 * 让数据层调用点直接 `entity.toDomain()`，是为了消除 `RoomMappers.run { ... }` 的样板噪音，
 * 让仓储代码更聚焦于“查什么/写什么”而不是“如何把 mapper 引入作用域”。
 */
fun DeckEntity.toDomain(): Deck = deckDomainFrom(
    id = id,
    name = name,
    description = description,
    tagsJson = tagsJson,
    intervalStepCount = intervalStepCount,
    archived = archived,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * 映射回 Entity 时统一走同一套 tags 编码策略，是为了避免不同写入口产生不兼容 JSON 格式。
 */
fun Deck.toEntity(): DeckEntity = DeckEntity(
    id = id,
    name = name,
    description = description,
    tagsJson = encodeTags(tags),
    intervalStepCount = intervalStepCount,
    archived = archived,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Card 的实体到领域映射保持无副作用，是为了让缓存/同步等后续替换数据源时仍能复用同一语义。
 */
fun CardEntity.toDomain(): Card = cardDomainFrom(
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
 * Card 写回 Entity 的映射集中后，上层就不必理解 Room 字段约束（例如列名、默认值等）。
 */
fun Card.toEntity(): CardEntity = CardEntity(
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
 * Question 的映射统一放在数据边界，是为了保证 status/tags 的容错语义不会散落在多个仓储中。
 */
fun QuestionEntity.toDomain(): Question = questionDomainFrom(
    id = id,
    cardId = cardId,
    prompt = prompt,
    answer = answer,
    tagsJson = tagsJson,
    status = status,
    stageIndex = stageIndex,
    dueAt = dueAt,
    lastReviewedAt = lastReviewedAt,
    reviewCount = reviewCount,
    lapseCount = lapseCount,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * Question 持久化边界只暴露领域模型，是为了让调度/校验逻辑在 domain 层闭环而不泄露 Entity 细节。
 */
fun Question.toEntity(): QuestionEntity = QuestionEntity(
    id = id,
    cardId = cardId,
    prompt = prompt,
    answer = answer,
    tagsJson = encodeTags(tags),
    status = encodeStatus(status),
    stageIndex = stageIndex,
    dueAt = dueAt,
    lastReviewedAt = lastReviewedAt,
    reviewCount = reviewCount,
    lapseCount = lapseCount,
    createdAt = createdAt,
    updatedAt = updatedAt
)

/**
 * ReviewRecord 的评分字段在历史数据中可能不完整，容错解码集中在此处能避免读库全量失败。
 */
fun ReviewRecordEntity.toDomain(): ReviewRecord = ReviewRecord(
    id = id,
    questionId = questionId,
    rating = decodeRating(rating),
    oldStageIndex = oldStageIndex,
    newStageIndex = newStageIndex,
    oldDueAt = oldDueAt,
    newDueAt = newDueAt,
    reviewedAt = reviewedAt,
    responseTimeMs = responseTimeMs,
    note = note
)

/**
 * ReviewRecord 写入仍使用枚举 name，是为了让备份/同步/统计在同一套评分字符串上对齐。
 */
fun ReviewRecord.toEntity(): ReviewRecordEntity = ReviewRecordEntity(
    id = id,
    questionId = questionId,
    rating = rating.name,
    oldStageIndex = oldStageIndex,
    newStageIndex = newStageIndex,
    oldDueAt = oldDueAt,
    newDueAt = newDueAt,
    reviewedAt = reviewedAt,
    responseTimeMs = responseTimeMs,
    note = note
)
