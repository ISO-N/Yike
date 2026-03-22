package com.kariscode.yike.domain.model

/**
 * 练习顺序作为独立枚举暴露，是为了让设置页、会话页与导航参数都共享同一套语义，
 * 避免后续在不同层分别使用字符串常量造成扩展时漂移。
 */
enum class PracticeOrderMode(
    val storageValue: String,
    val label: String
) {
    SEQUENTIAL(storageValue = "sequential", label = "顺序"),
    RANDOM(storageValue = "random", label = "随机");

    companion object {
        /**
         * 路由与持久状态恢复都只能拿到字符串，因此统一从单点恢复枚举可以避免各层各自兜底。
         */
        fun fromStorageValue(value: String?): PracticeOrderMode = entries.firstOrNull { mode ->
            mode.storageValue == value
        } ?: SEQUENTIAL
    }
}

/**
 * 练习会话参数统一承载范围与顺序，是为了让首页、卡组页、搜索页和练习页共享同一套输入协议，
 * 同时也为第二版题目级手选保留稳定扩展位。
 */
data class PracticeSessionArgs(
    val deckIds: List<String> = emptyList(),
    val cardIds: List<String> = emptyList(),
    val questionIds: List<String> = emptyList(),
    val orderMode: PracticeOrderMode = PracticeOrderMode.SEQUENTIAL
) {
    /**
     * 构造阶段先完成去重与去空，是为了让导航、查询和测试都只面对规范化后的选择结果。
     */
    fun normalized(): PracticeSessionArgs = copy(
        deckIds = deckIds.normalizeIds(),
        cardIds = cardIds.normalizeIds(),
        questionIds = questionIds.normalizeIds()
    )

    /**
     * 练习入口需要快速判断是否带着局部上下文进入，以便设置页决定展示何种默认提示。
     */
    fun hasScopedSelection(): Boolean = deckIds.isNotEmpty() || cardIds.isNotEmpty() || questionIds.isNotEmpty()
}

/**
 * 选择范围只允许保留真实 ID 值，是为了避免空字符串在 SQL `IN (...)` 里变成难定位的脏条件。
 */
private fun List<String>.normalizeIds(): List<String> = map(String::trim)
    .filter(String::isNotBlank)
    .distinct()
