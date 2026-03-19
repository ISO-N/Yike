package com.kariscode.yike.data.backup

import com.kariscode.yike.data.local.db.entity.CardEntity
import com.kariscode.yike.data.local.db.entity.DeckEntity
import com.kariscode.yike.data.local.db.entity.QuestionEntity
import com.kariscode.yike.data.local.db.entity.ReviewRecordEntity
import com.kariscode.yike.data.mapper.toEntity
import com.kariscode.yike.domain.model.Question
import com.kariscode.yike.domain.model.QuestionStatus
import com.kariscode.yike.domain.model.ReviewRecord
import com.kariscode.yike.domain.scheduler.ReviewSchedulerV1
import kotlinx.serialization.encodeToString

/**
 * 备份恢复映射从 BackupService 抽离，是为了让恢复流程只保留“校验成功后在事务中替换数据”这一条主线，
 * 同时把字符串解析、枚举解析和时间解析这些易错点集中到单文件便于审查与测试覆盖。
 */

/**
 * 备份模型恢复成 DeckEntity 的规则集中后，导入主流程就能更清楚地表达层级恢复顺序。
 */
internal fun BackupDeck.toEntity(): DeckEntity = DeckEntity(
    id = id,
    name = name,
    description = description,
    tagsJson = BackupJson.json.encodeToString(tags),
    intervalStepCount = ReviewSchedulerV1.normalizeIntervalStepCount(intervalStepCount),
    archived = archived,
    sortOrder = sortOrder,
    createdAt = BackupJson.parseEpochMillis(createdAt),
    updatedAt = BackupJson.parseEpochMillis(updatedAt)
)

/**
 * Card 备份恢复映射抽成扩展，是为了把时间解析与层级字段恢复放在一起维护。
 */
internal fun BackupCard.toEntity(): CardEntity = CardEntity(
    id = id,
    deckId = deckId,
    title = title,
    description = description,
    archived = archived,
    sortOrder = sortOrder,
    createdAt = BackupJson.parseEpochMillis(createdAt),
    updatedAt = BackupJson.parseEpochMillis(updatedAt)
)

/**
 * Question 恢复映射集中到单点后，状态字符串与领域模型之间的边界就不会散落在事务主流程里。
 */
internal fun BackupQuestion.toEntity(): QuestionEntity =
    Question(
        id = id,
        cardId = cardId,
        prompt = prompt,
        answer = answer,
        tags = tags,
        status = QuestionStatus.fromStorageValue(status),
        stageIndex = stageIndex,
        dueAt = BackupJson.parseEpochMillis(dueAt),
        lastReviewedAt = lastReviewedAt?.let(BackupJson::parseEpochMillis),
        reviewCount = reviewCount,
        lapseCount = lapseCount,
        createdAt = BackupJson.parseEpochMillis(createdAt),
        updatedAt = BackupJson.parseEpochMillis(updatedAt)
    ).toEntity()

/**
 * ReviewRecord 恢复映射独立出来，是为了让枚举解析与时间解析的风险点更容易被单独检查。
 */
internal fun BackupReviewRecord.toEntity(): ReviewRecordEntity =
    ReviewRecord(
        id = id,
        questionId = questionId,
        rating = enumValueOf(rating),
        oldStageIndex = oldStageIndex,
        newStageIndex = newStageIndex,
        oldDueAt = BackupJson.parseEpochMillis(oldDueAt),
        newDueAt = BackupJson.parseEpochMillis(newDueAt),
        reviewedAt = BackupJson.parseEpochMillis(reviewedAt),
        responseTimeMs = responseTimeMs,
        note = note
    ).toEntity()

