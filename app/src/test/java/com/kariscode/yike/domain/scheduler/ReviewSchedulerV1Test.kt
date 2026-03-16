package com.kariscode.yike.domain.scheduler

import com.kariscode.yike.domain.model.ReviewRating
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 调度器测试的存在是为了把“阶段边界/跳级/重置”这些最易漂移的规则固定下来，
 * 避免后续 UI 或事务实现调整时悄悄改坏调度节奏。
 */
class ReviewSchedulerV1Test {
    /**
     * AGAIN 必须重置到 0 并标记 lapse，以便写库事务正确递增 lapseCount。
     */
    @Test
    fun again_resetsToStage0_andMarksLapse() {
        val scheduler = ReviewSchedulerV1()
        val reviewedAt = 0L

        val result = scheduler.scheduleNext(
            currentStageIndex = 4,
            rating = ReviewRating.AGAIN,
            reviewedAtEpochMillis = reviewedAt
        )

        assertEquals(0, result.nextStageIndex)
        assertTrue(result.isLapse)
        assertEquals(1, result.intervalDays)
        assertEquals(86_400_000L, result.nextDueAtEpochMillis)
    }

    /**
     * EASY 需要跳两级但不能越过最大阶段，以保证阶段边界稳定。
     */
    @Test
    fun easy_jumpsTwoStages_butCapsAtMaxStage() {
        val scheduler = ReviewSchedulerV1()
        val reviewedAt = 0L

        val result = scheduler.scheduleNext(
            currentStageIndex = 6,
            rating = ReviewRating.EASY,
            reviewedAtEpochMillis = reviewedAt
        )

        assertEquals(7, result.nextStageIndex)
        assertEquals(180, result.intervalDays)
        assertEquals(180L * 86_400_000L, result.nextDueAtEpochMillis)
    }

    /**
     * HARD 在 stage=0 时仍需保持为 0，避免出现负阶段导致数组越界或错误 dueAt。
     */
    @Test
    fun hard_atStage0_staysAt0() {
        val scheduler = ReviewSchedulerV1()
        val reviewedAt = 123L

        val result = scheduler.scheduleNext(
            currentStageIndex = 0,
            rating = ReviewRating.HARD,
            reviewedAtEpochMillis = reviewedAt
        )

        assertEquals(0, result.nextStageIndex)
        assertEquals(1, result.intervalDays)
        assertEquals(reviewedAt + 86_400_000L, result.nextDueAtEpochMillis)
    }

    // ── GOOD 评分 ────────────────────────────────────────────

    /**
     * GOOD 在 stage=0 时应升到 stage=1，间隔 2 天。
     */
    @Test
    fun good_atStage0_advancesToStage1() {
        val scheduler = ReviewSchedulerV1()
        val reviewedAt = 0L

        val result = scheduler.scheduleNext(
            currentStageIndex = 0,
            rating = ReviewRating.GOOD,
            reviewedAtEpochMillis = reviewedAt
        )

        assertEquals(1, result.nextStageIndex)
        assertEquals(false, result.isLapse)
        assertEquals(2, result.intervalDays)
        assertEquals(2L * 86_400_000L, result.nextDueAtEpochMillis)
    }

    /**
     * GOOD 在中间阶段正常 +1。
     */
    @Test
    fun good_atMidStage_advancesByOne() {
        val scheduler = ReviewSchedulerV1()
        val reviewedAt = 0L

        val result = scheduler.scheduleNext(
            currentStageIndex = 3,
            rating = ReviewRating.GOOD,
            reviewedAtEpochMillis = reviewedAt
        )

        assertEquals(4, result.nextStageIndex)
        assertEquals(15, result.intervalDays)
        assertEquals(15L * 86_400_000L, result.nextDueAtEpochMillis)
    }

    /**
     * GOOD 在 maxStage(7) 时不能越界，应停留在 7。
     */
    @Test
    fun good_atMaxStage_capsAtMaxStage() {
        val scheduler = ReviewSchedulerV1()
        val reviewedAt = 0L

        val result = scheduler.scheduleNext(
            currentStageIndex = 7,
            rating = ReviewRating.GOOD,
            reviewedAtEpochMillis = reviewedAt
        )

        assertEquals(7, result.nextStageIndex)
        assertEquals(180, result.intervalDays)
        assertEquals(180L * 86_400_000L, result.nextDueAtEpochMillis)
    }

    // ── HARD 评分降级 ────────────────────────────────────────

    /**
     * HARD 在 stage>0 时降一级。
     */
    @Test
    fun hard_atStage3_downgradesTo2() {
        val scheduler = ReviewSchedulerV1()
        val reviewedAt = 0L

        val result = scheduler.scheduleNext(
            currentStageIndex = 3,
            rating = ReviewRating.HARD,
            reviewedAtEpochMillis = reviewedAt
        )

        assertEquals(2, result.nextStageIndex)
        assertEquals(false, result.isLapse)
        assertEquals(4, result.intervalDays)
        assertEquals(4L * 86_400_000L, result.nextDueAtEpochMillis)
    }

    /**
     * HARD 在 stage=1 时降到 0，确认最低边界。
     */
    @Test
    fun hard_atStage1_downgradesTo0() {
        val scheduler = ReviewSchedulerV1()
        val reviewedAt = 0L

        val result = scheduler.scheduleNext(
            currentStageIndex = 1,
            rating = ReviewRating.HARD,
            reviewedAtEpochMillis = reviewedAt
        )

        assertEquals(0, result.nextStageIndex)
        assertEquals(1, result.intervalDays)
    }

    // ── AGAIN 边界 ───────────────────────────────────────────

    /**
     * AGAIN 在 stage=0 时保持为 0，不会产生负值。
     */
    @Test
    fun again_atStage0_staysAt0() {
        val scheduler = ReviewSchedulerV1()
        val reviewedAt = 0L

        val result = scheduler.scheduleNext(
            currentStageIndex = 0,
            rating = ReviewRating.AGAIN,
            reviewedAtEpochMillis = reviewedAt
        )

        assertEquals(0, result.nextStageIndex)
        assertTrue(result.isLapse)
        assertEquals(1, result.intervalDays)
    }

    /**
     * AGAIN 在 maxStage 时也必须重置到 0。
     */
    @Test
    fun again_atMaxStage_resetsTo0() {
        val scheduler = ReviewSchedulerV1()
        val reviewedAt = 0L

        val result = scheduler.scheduleNext(
            currentStageIndex = 7,
            rating = ReviewRating.AGAIN,
            reviewedAtEpochMillis = reviewedAt
        )

        assertEquals(0, result.nextStageIndex)
        assertTrue(result.isLapse)
        assertEquals(1, result.intervalDays)
    }

    // ── EASY 阶段 capping ────────────────────────────────────

    /**
     * EASY 在 stage=0 时跳两级到 stage=2。
     */
    @Test
    fun easy_atStage0_jumpsToStage2() {
        val scheduler = ReviewSchedulerV1()
        val reviewedAt = 0L

        val result = scheduler.scheduleNext(
            currentStageIndex = 0,
            rating = ReviewRating.EASY,
            reviewedAtEpochMillis = reviewedAt
        )

        assertEquals(2, result.nextStageIndex)
        assertEquals(4, result.intervalDays)
        assertEquals(4L * 86_400_000L, result.nextDueAtEpochMillis)
    }

    /**
     * EASY 在 maxStage-1(6) 时只能进到 maxStage(7)，不能越界到 8。
     * 注意：这与已有 easy_jumpsTwoStages_butCapsAtMaxStage 互为补充。
     */
    @Test
    fun easy_atMaxStage_capsAtMaxStage() {
        val scheduler = ReviewSchedulerV1()
        val reviewedAt = 0L

        val result = scheduler.scheduleNext(
            currentStageIndex = 7,
            rating = ReviewRating.EASY,
            reviewedAtEpochMillis = reviewedAt
        )

        assertEquals(7, result.nextStageIndex)
        assertEquals(180, result.intervalDays)
    }

    // ── 超出范围的 currentStageIndex 被 coerce 纠正 ─────────

    /**
     * 传入负数 stage 时，调度器应 coerce 到 0 后再计算，不能崩溃。
     */
    @Test
    fun negativeStageIndex_isCorrectedToZero() {
        val scheduler = ReviewSchedulerV1()
        val reviewedAt = 0L

        val result = scheduler.scheduleNext(
            currentStageIndex = -5,
            rating = ReviewRating.GOOD,
            reviewedAtEpochMillis = reviewedAt
        )

        assertEquals(1, result.nextStageIndex)
        assertEquals(2, result.intervalDays)
    }

    /**
     * 传入超过 maxStage 的 stage 时，调度器应 coerce 到 maxStage 后再计算。
     */
    @Test
    fun overflowStageIndex_isCorrectedToMaxStage() {
        val scheduler = ReviewSchedulerV1()
        val reviewedAt = 0L

        val result = scheduler.scheduleNext(
            currentStageIndex = 100,
            rating = ReviewRating.GOOD,
            reviewedAtEpochMillis = reviewedAt
        )

        assertEquals(7, result.nextStageIndex)
        assertEquals(180, result.intervalDays)
    }

    // ── 间隔天数与 dueAt 一致性 ──────────────────────────────

    /**
     * 验证所有阶段的 intervalDays 与 dueAt 之间的换算始终一致。
     */
    @Test
    fun dueAt_alwaysEqualsReviewedAtPlusIntervalDays() {
        val scheduler = ReviewSchedulerV1()
        val reviewedAt = 1_000_000_000_000L

        for (stage in 0..7) {
            for (rating in ReviewRating.entries) {
                val result = scheduler.scheduleNext(
                    currentStageIndex = stage,
                    rating = rating,
                    reviewedAtEpochMillis = reviewedAt
                )

                val expectedDueAt = reviewedAt + result.intervalDays.toLong() * 86_400_000L
                assertEquals(
                    "stage=$stage, rating=$rating",
                    expectedDueAt,
                    result.nextDueAtEpochMillis
                )
            }
        }
    }
}

