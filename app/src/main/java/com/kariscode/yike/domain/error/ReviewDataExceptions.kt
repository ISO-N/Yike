package com.kariscode.yike.domain.error

/**
 * 题目已被删除或 ID 失效时，仓储需要抛出明确异常，
 * 这样上层才能区分“提交失败”和“目标数据已不存在”这两类完全不同的恢复路径。
 */
class QuestionNotFoundException(
    questionId: String
) : IllegalStateException("问题不存在，无法提交评分：$questionId")

/**
 * 复习入口依赖 cardId 重新加载标题与题目列表，
 * 因此当卡片缺失时需要抛出明确异常，让页面能给出可理解提示而不是统一归入未知加载失败。
 */
class CardNotFoundException(
    cardId: String
) : IllegalStateException("卡片不存在或已失效：$cardId")
