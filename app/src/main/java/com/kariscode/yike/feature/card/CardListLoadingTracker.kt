package com.kariscode.yike.feature.card

/**
 * 卡片页首屏需要同时等待卡组元信息和卡片列表首个结果，
 * 因此把完成信号收口成独立状态对象后，ViewModel 就不必再维护两个分散布尔值。
 */
internal data class CardListLoadingTracker(
    val deckLoaded: Boolean = false,
    val cardsLoaded: Boolean = false
) {
    /**
     * 只要任一初始化来源尚未完成，页面就应继续维持首屏加载态，
     * 这样后续实时刷新也不会反复触发 loading 闪烁。
     */
    val isLoading: Boolean
        get() = !(deckLoaded && cardsLoaded)

    /**
     * 卡组元信息完成后返回新快照，是为了让调用点显式表达“哪一路初始化已经结束”。
     */
    fun markDeckLoaded(): CardListLoadingTracker = copy(deckLoaded = true)

    /**
     * 卡片列表首个结果完成后返回新快照，是为了让列表订阅和标题加载共享同一 loading 规则。
     */
    fun markCardsLoaded(): CardListLoadingTracker = copy(cardsLoaded = true)
}
