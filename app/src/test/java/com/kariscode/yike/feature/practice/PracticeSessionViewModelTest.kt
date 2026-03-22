package com.kariscode.yike.feature.practice

import androidx.lifecycle.SavedStateHandle
import com.kariscode.yike.domain.model.PracticeOrderMode
import com.kariscode.yike.domain.model.PracticeSessionArgs
import com.kariscode.yike.domain.model.Question
import com.kariscode.yike.domain.model.QuestionContext
import com.kariscode.yike.domain.model.QuestionStatus
import com.kariscode.yike.testsupport.FakePracticeRepository
import com.kariscode.yike.testsupport.FixedTimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PracticeSessionViewModel 测试锁定题目顺序、空答案和恢复行为，
 * 是为了守住练习模式“固定顺序、可恢复、不落库”的会话体验。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PracticeSessionViewModelTest {

    /**
     * 空答案必须在会话页展示为“无答案”，
     * 这样用户才会理解为内容尚未填写，而不是页面漏渲染。
     */
    @Test
    fun init_blankAnswerFallsBackToNoAnswer() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = FakePracticeRepository().apply {
                questionContexts = listOf(questionContext(questionId = "q_1", answer = ""))
            }
            val viewModel = PracticeSessionViewModel(
                args = PracticeSessionArgs(),
                practiceRepository = repository,
                timeProvider = FixedTimeProvider(nowEpochMillis = 1_000L),
                savedStateHandle = SavedStateHandle()
            )
            advanceUntilIdle()

            assertEquals("无答案", viewModel.uiState.value.currentQuestion?.answerText)
        } finally {
            Dispatchers.resetMain()
        }
    }

    /**
     * 随机模式恢复时必须继续沿用同一个 seed 和索引，
     * 否则用户在切回应用后会看到题序被重新洗牌。
     */
    @Test
    fun randomMode_reusesSavedSeedAndIndexAfterRecreation() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = FakePracticeRepository().apply {
                questionContexts = listOf(
                    questionContext(questionId = "q_1"),
                    questionContext(questionId = "q_2"),
                    questionContext(questionId = "q_3")
                )
            }
            val savedStateHandle = SavedStateHandle()
            val args = PracticeSessionArgs(orderMode = PracticeOrderMode.RANDOM)

            val firstViewModel = PracticeSessionViewModel(
                args = args,
                practiceRepository = repository,
                timeProvider = FixedTimeProvider(nowEpochMillis = 9_999L),
                savedStateHandle = savedStateHandle
            )
            advanceUntilIdle()
            val firstOrder = buildList {
                add(firstViewModel.uiState.value.currentQuestion!!.questionId)
                firstViewModel.onNextQuestionClick()
                add(firstViewModel.uiState.value.currentQuestion!!.questionId)
                firstViewModel.onNextQuestionClick()
                add(firstViewModel.uiState.value.currentQuestion!!.questionId)
            }
            val expectedCurrentQuestion = firstViewModel.uiState.value.currentQuestion!!.questionId
            val expectedSeed = firstViewModel.uiState.value.sessionSeed

            val recreatedViewModel = PracticeSessionViewModel(
                args = args,
                practiceRepository = repository,
                timeProvider = FixedTimeProvider(nowEpochMillis = 9_999L),
                savedStateHandle = savedStateHandle
            )
            advanceUntilIdle()
            val restoredSeed = recreatedViewModel.uiState.value.sessionSeed
            val restoredCurrentQuestion = recreatedViewModel.uiState.value.currentQuestion!!.questionId

            recreatedViewModel.onPreviousQuestionClick()
            val restoredOrder = buildList {
                add(recreatedViewModel.uiState.value.currentQuestion!!.questionId)
                recreatedViewModel.onNextQuestionClick()
                add(recreatedViewModel.uiState.value.currentQuestion!!.questionId)
            }

            assertEquals(expectedSeed, restoredSeed)
            assertEquals(expectedCurrentQuestion, restoredCurrentQuestion)
            assertEquals(firstOrder[1], restoredOrder[0])
            assertEquals(firstOrder[2], restoredOrder[1])
        } finally {
            Dispatchers.resetMain()
        }
    }

    /**
     * 当查询结果为空时，会话页必须进入可理解空状态，
     * 这样设置页清空题目选择后不会把用户带进一张空白卡片。
     */
    @Test
    fun init_withoutQuestions_setsEmptyState() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val viewModel = PracticeSessionViewModel(
                args = PracticeSessionArgs(),
                practiceRepository = FakePracticeRepository(),
                timeProvider = FixedTimeProvider(nowEpochMillis = 1_000L),
                savedStateHandle = SavedStateHandle()
            )
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isEmpty)
            assertEquals(0, viewModel.uiState.value.totalCount)
        } finally {
            Dispatchers.resetMain()
        }
    }

    /**
     * 题目辅助构造函数固定最小上下文字段，是为了让测试更聚焦会话行为而不是内容装配。
     */
    private fun questionContext(
        questionId: String,
        answer: String = "答案"
    ): QuestionContext = QuestionContext(
        question = Question(
            id = questionId,
            cardId = "card_1",
            prompt = "问题 $questionId",
            answer = answer,
            tags = emptyList(),
            status = QuestionStatus.ACTIVE,
            stageIndex = 0,
            dueAt = 10_000L,
            lastReviewedAt = null,
            reviewCount = 0,
            lapseCount = 0,
            createdAt = 1L,
            updatedAt = 1L
        ),
        deckId = "deck_1",
        deckName = "deck_1",
        cardTitle = "card_1"
    )
}
