package com.kariscode.yike.domain.model

/**
 * Question 的状态需要显式建模以支撑“归档后不进入默认列表/待复习”的规则，
 * 否则状态语义容易在查询条件里被硬编码并在多个层次出现偏差。
 */
enum class QuestionStatus(
    val storageValue: String,
    val displayLabel: String
) {
    ACTIVE(
        storageValue = "active",
        displayLabel = "进行中"
    ),
    ARCHIVED(
        storageValue = "archived",
        displayLabel = "已归档"
    );

    companion object {
        /**
         * 存储层的非法值统一回退到 ACTIVE，
         * 是为了让旧数据或异常输入不会把题目默默排除出默认工作流。
         */
        fun fromStorageValue(value: String): QuestionStatus = entries.firstOrNull { status ->
            status.storageValue == value
        } ?: ACTIVE
    }
}

