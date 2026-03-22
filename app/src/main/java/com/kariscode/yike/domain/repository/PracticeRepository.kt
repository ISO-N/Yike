package com.kariscode.yike.domain.repository

import com.kariscode.yike.domain.model.PracticeSessionArgs
import com.kariscode.yike.domain.model.QuestionContext

/**
 * PracticeRepository 把练习模式的只读查询口径单独收敛出来，
 * 是为了从接口层面阻断练习流误接正式复习事务写入链路的风险。
 */
interface PracticeRepository {
    /**
     * 练习模式允许按 deck/card/question 范围主动取数，
     * 因此仓储必须忽略 due 约束，只返回当前仍可见且有效的问题上下文。
     */
    suspend fun listPracticeQuestionContexts(args: PracticeSessionArgs): List<QuestionContext>
}
