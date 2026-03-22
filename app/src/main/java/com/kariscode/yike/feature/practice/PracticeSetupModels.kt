package com.kariscode.yike.feature.practice

import com.kariscode.yike.domain.model.PracticeOrderMode

/**
 * 卡组选项独立建模，是为了让设置页能直接展示每个卡组的练习规模，而不必在 Composable 内再临时聚合。
 */
data class PracticeDeckOptionUiModel(
    val deckId: String,
    val deckName: String,
    val cardCount: Int,
    val questionCount: Int,
    val isSelected: Boolean
)

/**
 * 卡片选项显式带出所属卡组，是为了让多卡组联合练习时仍能保持范围来源清晰可见。
 */
data class PracticeCardOptionUiModel(
    val cardId: String,
    val deckId: String,
    val deckName: String,
    val cardTitle: String,
    val questionCount: Int,
    val isSelected: Boolean
)

/**
 * 题目选项提前补齐卡组和卡片上下文，是为了让第二版题目级手选不必跳到其他页面确认来源。
 */
data class PracticeQuestionOptionUiModel(
    val questionId: String,
    val cardId: String,
    val deckName: String,
    val cardTitle: String,
    val prompt: String,
    val answerPreview: String,
    val isSelected: Boolean
)

/**
 * 练习设置页状态同时持有全部候选和当前选择，是为了让范围变化时能一次性重算卡片、题目与题量结果。
 */
data class PracticeSetupUiState(
    val isLoading: Boolean,
    val deckOptions: List<PracticeDeckOptionUiModel>,
    val cardOptions: List<PracticeCardOptionUiModel>,
    val questionOptions: List<PracticeQuestionOptionUiModel>,
    val selectedDeckIds: Set<String>,
    val selectedCardIds: Set<String>,
    val selectedQuestionIds: Set<String>?,
    val orderMode: PracticeOrderMode,
    val effectiveQuestionCount: Int,
    val errorMessage: String?
)
