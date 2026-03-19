package com.kariscode.yike.data.mapper

import com.kariscode.yike.data.local.db.dao.ArchivedCardSummaryRow
import com.kariscode.yike.data.local.db.dao.CardSummaryRow
import com.kariscode.yike.data.local.db.dao.DeckSummaryRow
import com.kariscode.yike.data.local.db.dao.QuestionContextRow
import com.kariscode.yike.domain.model.ArchivedCardSummary
import com.kariscode.yike.domain.model.CardSummary
import com.kariscode.yike.domain.model.DeckSummary
import com.kariscode.yike.domain.model.QuestionContext

/**
 * 聚合行在 mapper 层还原成领域摘要，可避免 Repository 重复理解 SQL 别名与层级字段。
 */
fun DeckSummaryRow.toDomain(): DeckSummary = DeckSummary(
    deck = deckDomainFrom(
        id = id,
        name = name,
        description = description,
        tagsJson = tagsJson,
        intervalStepCount = intervalStepCount,
        archived = archived,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt
    ),
    cardCount = cardCount,
    questionCount = questionCount,
    dueQuestionCount = dueQuestionCount
)

/**
 * 卡片摘要行与普通实体字段相近但不完全一致，把转换集中可避免列表查询继续散落手写构造。
 */
fun CardSummaryRow.toDomain(): CardSummary = CardSummary(
    card = cardDomainFrom(
        id = id,
        deckId = deckId,
        title = title,
        description = description,
        archived = archived,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt
    ),
    questionCount = questionCount,
    dueQuestionCount = dueQuestionCount
)

/**
 * 回收站行额外携带卡组名称，因此单独映射能避免页面层理解 SQL 别名字段。
 */
fun ArchivedCardSummaryRow.toDomain(): ArchivedCardSummary = ArchivedCardSummary(
    card = cardDomainFrom(
        id = id,
        deckId = deckId,
        title = title,
        description = description,
        archived = archived,
        sortOrder = sortOrder,
        createdAt = createdAt,
        updatedAt = updatedAt
    ),
    deckName = deckName,
    questionCount = questionCount,
    dueQuestionCount = dueQuestionCount
)

/**
 * 搜索与预览共用的上下文行统一映射成领域模型，是为了让熟练度筛选继续只依赖同一份 Question 语义。
 */
fun QuestionContextRow.toDomain(): QuestionContext = QuestionContext(
    question = questionDomainFrom(
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
    ),
    deckId = deckId,
    deckName = deckName,
    cardTitle = cardTitle
)

