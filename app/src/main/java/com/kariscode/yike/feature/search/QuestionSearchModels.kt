package com.kariscode.yike.feature.search

import com.kariscode.yike.domain.model.QuestionContext
import com.kariscode.yike.domain.model.QuestionMasteryLevel
import com.kariscode.yike.domain.model.QuestionMasterySnapshot
import com.kariscode.yike.domain.model.QuestionStatus

/**
 * 卡组选项独立建模，是为了让筛选控件只依赖稳定的轻量结构，而不是直接持有领域对象。
 */
data class SearchDeckOption(
    val id: String,
    val name: String
)

/**
 * 卡片选项按当前卡组动态切换，是为了把“先定卡组再选卡片”的层级约束体现在状态里。
 */
data class SearchCardOption(
    val id: String,
    val title: String
)

/**
 * 搜索结果项预先携带熟练度和 due 状态，是为了让列表能直接渲染标签和行动按钮，不再重复推导。
 */
data class QuestionSearchResultUiModel(
    val context: QuestionContext,
    val mastery: QuestionMasterySnapshot,
    val isDue: Boolean
)

/**
 * 搜索页状态直接承载当前筛选值与结果集合，是为了让“筛选条件变化即刷新结果”保持清晰可追踪。
 */
data class QuestionSearchUiState(
    val isLoading: Boolean,
    val keyword: String,
    val selectedTag: String?,
    val selectedStatus: QuestionStatus?,
    val selectedDeckId: String?,
    val selectedCardId: String?,
    val selectedMasteryLevel: QuestionMasteryLevel?,
    val availableTags: List<String>,
    val deckOptions: List<SearchDeckOption>,
    val cardOptions: List<SearchCardOption>,
    val results: List<QuestionSearchResultUiModel>,
    val errorMessage: String?
)
