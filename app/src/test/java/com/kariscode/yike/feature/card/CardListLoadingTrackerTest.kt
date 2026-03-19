package com.kariscode.yike.feature.card

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * CardListLoadingTrackerTest 锁定首屏 loading 的组合规则，
 * 避免后续继续演进卡片页时又把初始化状态拆回多个分散布尔值。
 */
class CardListLoadingTrackerTest {
    /**
     * 只有卡组和卡片都完成首个结果后才结束首屏加载，
     * 这样标题和列表不会出现一边准备好、一边提前闪空态的问题。
     */
    @Test
    fun markDeckLoaded_andMarkCardsLoaded_finishInitialLoadingOnlyAfterBothComplete() {
        val deckLoadedOnly = CardListLoadingTracker().markDeckLoaded()
        val fullyLoaded = deckLoadedOnly.markCardsLoaded()

        assertTrue(deckLoadedOnly.isLoading)
        assertFalse(fullyLoaded.isLoading)
    }
}
