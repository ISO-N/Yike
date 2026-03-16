package com.kariscode.yike.feature.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kariscode.yike.core.constant.EntityIdPrefixes
import com.kariscode.yike.core.message.ErrorMessages
import com.kariscode.yike.core.message.SuccessMessages
import com.kariscode.yike.core.time.TimeProvider
import com.kariscode.yike.core.viewmodel.typedViewModelFactory
import com.kariscode.yike.domain.model.Card
import com.kariscode.yike.domain.model.CardSummary
import com.kariscode.yike.domain.model.QuestionMasteryCalculator
import com.kariscode.yike.domain.model.QuestionMasteryLevel
import com.kariscode.yike.domain.model.QuestionQueryFilters
import com.kariscode.yike.domain.model.QuestionStatus
import com.kariscode.yike.domain.repository.CardRepository
import com.kariscode.yike.domain.repository.DeckRepository
import com.kariscode.yike.domain.repository.StudyInsightsRepository
import java.util.UUID
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 卡片编辑草稿状态集中在 ViewModel，是为了让“标题必填”等校验规则有稳定落点，
 * 避免在 UI 中散落多个临时状态变量导致保存行为不可预测。
 */
data class CardEditorDraft(
    val cardId: String?,
    val title: String,
    val description: String,
    val validationMessage: String? = null
)

/**
 * 熟练度摘要集中在卡片页状态里，是为了让卡组层先暴露“整体薄弱分布”，再让用户进入具体卡片。
 */
data class DeckMasterySummary(
    val totalQuestions: Int,
    val newCount: Int,
    val learningCount: Int,
    val familiarCount: Int,
    val masteredCount: Int
)

/**
 * 卡片列表状态同时包含 deck 信息与列表聚合结果，原因是页面标题与返回行为都依赖 deckId，
 * 且进程重建时需要只靠参数就能重新加载对应数据。
 */
data class CardListUiState(
    val deckId: String,
    val deckName: String?,
    val isLoading: Boolean,
    val items: List<CardSummary>,
    val masterySummary: DeckMasterySummary?,
    val editor: CardEditorDraft?,
    val pendingDelete: CardSummary?,
    val message: String?,
    val errorMessage: String?
)

/**
 * 卡片列表 ViewModel 负责协调 deck 信息加载与列表聚合流订阅，
 * 以避免页面同时处理多个异步来源导致状态分叉。
 */
class CardListViewModel(
    private val deckId: String,
    private val deckRepository: DeckRepository,
    private val cardRepository: CardRepository,
    private val studyInsightsRepository: StudyInsightsRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        CardListUiState(
            deckId = deckId,
            deckName = null,
            isLoading = true,
            items = emptyList(),
            masterySummary = null,
            editor = null,
            pendingDelete = null,
            message = null,
            errorMessage = null
        )
    )
    val uiState: StateFlow<CardListUiState> = _uiState.asStateFlow()

    init {
        /**
         * 初始数据加载使用 coroutineScope 并行执行 deckName 和卡片列表查询，
         * 避免顺序执行导致的等待时间叠加。
         */
        viewModelScope.launch {
            coroutineScope {
                val deckDeferred = async { deckRepository.findById(deckId) }
                val now = timeProvider.nowEpochMillis()
                val cardsDeferred = async {
                    cardRepository.observeActiveCardSummaries(deckId, now).catch {}.first()
                }
                val deck = deckDeferred.await()
                val cards = cardsDeferred.await()
                _uiState.update {
                    it.copy(
                        deckName = deck?.name,
                        items = cards ?: emptyList(),
                        isLoading = false
                    )
                }
            }
            // 初始加载完成后，启动 Flow 订阅以保持列表实时更新
            launch {
                val now = timeProvider.nowEpochMillis()
                cardRepository.observeActiveCardSummaries(deckId, now)
                    .catch { throwable ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                message = null,
                                errorMessage = throwable.message ?: ErrorMessages.LOAD_FAILED
                            )
                        }
                    }
                    .collect { items ->
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                items = items,
                                errorMessage = null
                            )
                        }
                        refreshMasterySummary()
                    }
            }
        }
    }

    /**
     * 新建卡片先打开空草稿，便于复用同一套保存校验逻辑。
     */
    fun onCreateCardClick() {
        openEditor(CardEditorDraft(cardId = null, title = "", description = ""))
    }

    /**
     * 编辑卡片时把现有字段写入草稿，避免 UI 自己维护副本导致更新错位。
     */
    fun onEditCardClick(item: CardSummary) {
        openEditor(
            CardEditorDraft(
                cardId = item.card.id,
                title = item.card.title,
                description = item.card.description
            )
        )
    }

    /**
     * 标题属于必填字段，因此每次输入变更都需要清理上次校验提示以避免误导。
     */
    fun onDraftTitleChange(value: String) {
        updateEditor { it.copy(title = value, validationMessage = null) }
    }

    /**
     * 描述不参与必填校验，但仍需进入草稿以确保保存读取到一致状态。
     */
    fun onDraftDescriptionChange(value: String) {
        updateEditor { it.copy(description = value, validationMessage = null) }
    }

    /**
     * 关闭编辑器直接丢弃草稿，明确“未保存不落库”的交互语义。
     */
    fun onDismissEditor() {
        _uiState.update { it.copy(editor = null, errorMessage = null) }
    }

    /**
     * 保存时统一注入 ID 与时间戳，避免上层在多个入口创建卡片时产生不同的时间语义。
     */
    fun onConfirmSave() {
        val editor = _uiState.value.editor ?: return
        val trimmedTitle = editor.title.trim()
        if (trimmedTitle.isBlank()) {
            _uiState.update { it.copy(editor = editor.copy(validationMessage = ErrorMessages.TITLE_REQUIRED)) }
            return
        }

        viewModelScope.launch {
            runCatching {
                val now = timeProvider.nowEpochMillis()
                // Room 的 upsert 会自动处理"不存在则插入，存在则更新"的逻辑
                // 直接构建对象并 upsert，无需先查询
                val card = Card(
                    id = editor.cardId ?: "${EntityIdPrefixes.CARD}${UUID.randomUUID()}",
                    deckId = deckId,
                    title = trimmedTitle,
                    description = editor.description,
                    archived = false,
                    sortOrder = 0,
                    createdAt = now,
                    updatedAt = now
                )
                cardRepository.upsert(card)
            }.onSuccess {
                _uiState.update { it.copy(editor = null, message = SuccessMessages.SAVED, errorMessage = null) }
            }.onFailure {
                _uiState.update { it.copy(message = null, errorMessage = ErrorMessages.SAVE_FAILED) }
            }
        }
    }

    /**
     * 归档用于默认列表过滤，以降低误删风险并保持内容可恢复。
     */
    fun onArchiveCardClick(item: CardSummary) {
        executeMutation(errorMessage = ErrorMessages.UPDATE_FAILED) {
            val now = timeProvider.nowEpochMillis()
            cardRepository.setArchived(cardId = item.card.id, archived = !item.card.archived, updatedAt = now)
        }
    }

    /**
     * 删除需要先进入确认态，避免在列表交互中误触造成不可逆的数据丢失。
     */
    fun onDeleteCardClick(item: CardSummary) {
        _uiState.update { it.copy(pendingDelete = item, errorMessage = null) }
    }

    /**
     * 退出确认态可以避免误触删除，符合高风险操作需要二次确认的原则。
     */
    fun onDismissDelete() {
        _uiState.update { it.copy(pendingDelete = null, errorMessage = null) }
    }

    /**
     * 删除是高风险操作，确认后通过级联约束清理问题与复习记录，避免数据残留。
     */
    fun onConfirmDelete() {
        val pending = _uiState.value.pendingDelete ?: return
        executeMutation(errorMessage = ErrorMessages.DELETE_FAILED) {
            cardRepository.delete(pending.card.id)
            _uiState.update { it.copy(pendingDelete = null, message = SuccessMessages.DELETED, errorMessage = null) }
        }
    }

    /**
     * 打开编辑器时统一清空旧反馈，是为了让创建和编辑都从同一个干净状态开始。
     */
    private fun openEditor(editor: CardEditorDraft) {
        _uiState.update {
            it.copy(
                editor = editor,
                message = null,
                errorMessage = null
            )
        }
    }

    /**
     * 草稿更新收口后，标题与描述的输入路径就不需要各自重复 editor 判空模板。
     */
    private fun updateEditor(transform: (CardEditorDraft) -> CardEditorDraft) {
        _uiState.update { state ->
            val editor = state.editor ?: return@update state
            state.copy(editor = transform(editor))
        }
    }

    /**
     * 列表页写操作统一经由同一失败反馈出口，是为了避免归档、删除等分支再次遗漏异常收口。
     */
    private fun executeMutation(
        errorMessage: String,
        action: suspend () -> Unit
    ) {
        viewModelScope.launch {
            runCatching { action() }
                .onFailure {
                    _uiState.update { it.copy(message = null, errorMessage = errorMessage) }
                }
        }
    }

    /**
     * 熟练度摘要基于真实问题集合即时计算，是为了遵守”不写回数据库，只按字段推导”的 P0 约束。
     * 使用 groupBy 避免对每个问题重复计算熟练度。
     */
    private fun refreshMasterySummary() {
        viewModelScope.launch {
            runCatching {
                val questions = studyInsightsRepository.searchQuestionContexts(
                    filters = QuestionQueryFilters(
                        deckId = deckId,
                        status = QuestionStatus.ACTIVE
                    )
                )
                // 先计算一次熟练度，再用 groupBy 分类，避免重复计算
                val masteryCounts = questions.groupBy { context ->
                    QuestionMasteryCalculator.snapshot(context.question).level
                }
                DeckMasterySummary(
                    totalQuestions = questions.size,
                    newCount = masteryCounts[QuestionMasteryLevel.NEW]?.size ?: 0,
                    learningCount = masteryCounts[QuestionMasteryLevel.LEARNING]?.size ?: 0,
                    familiarCount = masteryCounts[QuestionMasteryLevel.FAMILIAR]?.size ?: 0,
                    masteredCount = masteryCounts[QuestionMasteryLevel.MASTERED]?.size ?: 0
                )
            }.onSuccess { summary ->
                _uiState.update { it.copy(masterySummary = summary) }
            }.onFailure {
                _uiState.update { it.copy(masterySummary = null) }
            }
        }
    }

    companion object {
        /**
         * 工厂将 deckId 与容器依赖注入 ViewModel，避免 ViewModel 直接依赖全局单例。
         */
        fun factory(
            deckId: String,
            deckRepository: DeckRepository,
            cardRepository: CardRepository,
            studyInsightsRepository: StudyInsightsRepository,
            timeProvider: TimeProvider
        ): ViewModelProvider.Factory = typedViewModelFactory {
            CardListViewModel(
                deckId = deckId,
                deckRepository = deckRepository,
                cardRepository = cardRepository,
                studyInsightsRepository = studyInsightsRepository,
                timeProvider = timeProvider
            )
        }
    }
}
