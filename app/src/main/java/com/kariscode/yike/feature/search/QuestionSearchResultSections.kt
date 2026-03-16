package com.kariscode.yike.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kariscode.yike.domain.model.QuestionMasteryLevel
import com.kariscode.yike.ui.component.YikeBadge
import com.kariscode.yike.ui.component.YikePrimaryButton
import com.kariscode.yike.ui.component.YikeProgressBar
import com.kariscode.yike.ui.component.YikeSecondaryButton
import com.kariscode.yike.ui.component.YikeStateBanner
import com.kariscode.yike.ui.component.YikeSurfaceCard
import com.kariscode.yike.ui.format.UiDateTimeFormatters
import com.kariscode.yike.ui.theme.LocalYikeSpacing
import java.time.Instant
import java.time.ZoneId

/**
 * 结果区在空态时给出下一步建议，是为了避免用户面对 0 结果时不知道该放宽哪一类条件。
 */
@Composable
internal fun QuestionSearchResultSection(
    uiState: QuestionSearchUiState,
    onOpenEditor: (String) -> Unit,
    onOpenReview: (String) -> Unit
) {
    val spacing = LocalYikeSpacing.current
    if (uiState.results.isEmpty()) {
        YikeStateBanner(
            title = "没有找到符合条件的问题",
            description = "可以尝试清空熟练度或卡片筛选，先扩大范围，再进入专项处理。"
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(spacing.md)) {
        uiState.results.forEach { item ->
            YikeSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.context.question.prompt,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    YikeBadge(text = item.mastery.level.label)
                }
                Text(
                    text = buildAnswerSnippet(item),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    item.context.question.tags.forEach { tag ->
                        YikeBadge(text = tag)
                    }
                }
                Text(
                    text = "${item.context.deckName} / ${item.context.cardTitle}",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = buildMetaLine(item),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                YikeProgressBar(progress = item.mastery.progress)
                Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    YikeSecondaryButton(
                        text = "编辑问题",
                        onClick = { onOpenEditor(item.context.question.cardId) },
                        modifier = Modifier.weight(1f)
                    )
                    YikePrimaryButton(
                        text = if (item.isDue) "立即复习" else "查看卡片",
                        onClick = {
                            if (item.isDue) onOpenReview(item.context.question.cardId) else onOpenEditor(item.context.question.cardId)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * 答案片段统一截断，是为了让结果列表先帮助定位内容，而不是一屏被完整答案撑满。
 */
private fun buildAnswerSnippet(item: QuestionSearchResultUiModel): String {
    val answer = item.context.question.answer.ifBlank { "尚未填写答案。" }
    val snippet = answer.take(60)
    return if (answer.length > 60) "$snippet..." else snippet
}

/**
 * 元信息统一描述状态、复习次数和最近复习时间，是为了让用户在一个视线范围内判断是否值得现在处理。
 */
private fun buildMetaLine(item: QuestionSearchResultUiModel): String {
    val question = item.context.question
    val statusText = question.status.displayLabel
    val reviewedAtText = question.lastReviewedAt?.let(::formatSearchDateTime) ?: "尚未复习"
    val masteryHint = when (item.mastery.level) {
        QuestionMasteryLevel.NEW -> "新问题"
        QuestionMasteryLevel.LEARNING -> "仍在巩固"
        QuestionMasteryLevel.FAMILIAR -> "进入稳定区"
        QuestionMasteryLevel.MASTERED -> "掌握较稳"
    }
    return "$statusText · 复习 ${question.reviewCount} 次 · lapse ${question.lapseCount} 次 · 最近复习：$reviewedAtText · $masteryHint"
}

/**
 * 时间格式统一到月日时分，是为了让搜索结果、预览页和其他详情页保持一致的时间表达方式。
 */
private fun formatSearchDateTime(epochMillis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(zoneId)
        .format(UiDateTimeFormatters.PREVIEW_DATE)
