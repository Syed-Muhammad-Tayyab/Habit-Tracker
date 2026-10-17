package com.example

import com.example.data.local.HabitCompletionEntity
import com.example.data.stats.CompletionIndex
import com.example.data.stats.DateUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HabitStatsTest {
    private fun c(habit: Long, ym: String, idx: Int) =
        HabitCompletionEntity(habitId = habit, userId = "u", dayIndex = idx, yearMonth = ym)

    private val index = CompletionIndex(
        // 27-30 Sep + 1-4 Oct for habit 1, plus an invalid day index (31 Sep) that must be ignored
        (26..29).map { c(1, "2026-09", it) } + (0..3).map { c(1, "2026-10", it) } + c(2, "2026-09", 30)
    )

    @Test
    fun streakCrossesMonthBoundary() {
        assertEquals(8, index.streakFor(1, LocalDate.of(2026, 10, 4)))
    }

    @Test
    fun streakHasGraceForToday() {
        assertEquals(8, index.streakFor(1, LocalDate.of(2026, 10, 5)))
        assertEquals(0, index.streakFor(1, LocalDate.of(2026, 10, 6)))
    }

    @Test
    fun invalidDayIndexIsIgnored() {
        assertTrue(index.doneOn(LocalDate.of(2026, 10, 1)).none { it == 2L })
    }

    @Test
    fun bestStreak() {
        assertEquals(8, index.bestStreak(setOf(1L)))
    }

    @Test
    fun weekStartsAreCorrect() {
        assertEquals(LocalDate.of(2026, 10, 11), DateUtil.sundayStart(LocalDate.of(2026, 10, 17)))
        assertEquals(LocalDate.of(2026, 10, 12), DateUtil.weekStart(LocalDate.of(2026, 10, 18)))
    }

    @Test
    fun compactCounts() {
        assertEquals("1k", DateUtil.compactCount(1000))
        assertEquals("1.6k", DateUtil.compactCount(1600))
        assertEquals("850", DateUtil.compactCount(850))
    }

    @Test
    fun percentIsClamped() {
        assertEquals(0, DateUtil.percent(5, 0))
        assertEquals(100, DateUtil.percent(12, 10))
        assertEquals(70, DateUtil.percent(7, 10))
    }
}
