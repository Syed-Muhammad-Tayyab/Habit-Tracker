package com.example.data.stats

import com.example.data.local.HabitCompletionEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Fast lookup of "which habits were completed on which date". Pure Kotlin, no Android deps. */
class CompletionIndex(completions: List<HabitCompletionEntity>) {
    private val byDate: Map<LocalDate, Set<Long>>

    init {
        val map = HashMap<LocalDate, MutableSet<Long>>()
        for (c in completions) {
            if (!c.completed) continue
            val date = dateOf(c.yearMonth, c.dayIndex) ?: continue
            map.getOrPut(date) { HashSet() }.add(c.habitId)
        }
        byDate = map
    }

    fun doneOn(date: LocalDate): Set<Long> = byDate[date] ?: emptySet()

    fun isDone(habitId: Long, date: LocalDate): Boolean = habitId in doneOn(date)

    /** Number of the given habits completed on [date]. */
    fun countOn(date: LocalDate, habitIds: Set<Long>): Int = doneOn(date).count { it in habitIds }

    /** Consecutive days a habit was done, ending at [upTo] (or the day before if [upTo] isn't done yet). */
    fun streakFor(habitId: Long, upTo: LocalDate): Int {
        var d = if (isDone(habitId, upTo)) upTo else upTo.minusDays(1)
        var n = 0
        while (isDone(habitId, d)) {
            n++
            d = d.minusDays(1)
        }
        return n
    }

    /** Consecutive days on which at least one of [habitIds] was done, ending at [upTo] (grace for today). */
    fun streakWhere(habitIds: Set<Long>, upTo: LocalDate): Int {
        if (habitIds.isEmpty()) return 0
        var d = if (countOn(upTo, habitIds) > 0) upTo else upTo.minusDays(1)
        var n = 0
        while (countOn(d, habitIds) > 0) {
            n++
            d = d.minusDays(1)
        }
        return n
    }

    /** Longest run of "active" days (>=1 habit done) anywhere in history. */
    fun bestStreak(habitIds: Set<Long>): Int {
        val days = byDate.filter { (_, ids) -> ids.any { it in habitIds } }.keys.sorted()
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in days) {
            run = if (prev != null && ChronoUnit.DAYS.between(prev, d) == 1L) run + 1 else 1
            if (run > best) best = run
            prev = d
        }
        return best
    }

    /** Days (within [from]..[to] inclusive) a given habit was done. */
    fun doneDaysInRange(habitId: Long, from: LocalDate, to: LocalDate): Int {
        if (to.isBefore(from)) return 0
        var n = 0
        var d = from
        while (!d.isAfter(to)) {
            if (isDone(habitId, d)) n++
            d = d.plusDays(1)
        }
        return n
    }

    /** Total completions (of [habitIds]) within [from]..[to] inclusive. */
    fun totalInRange(habitIds: Set<Long>, from: LocalDate, to: LocalDate): Int {
        if (to.isBefore(from)) return 0
        var n = 0
        var d = from
        while (!d.isAfter(to)) {
            n += countOn(d, habitIds)
            d = d.plusDays(1)
        }
        return n
    }

    /** Completion ratio 0..1 of [habitIds] across [from]..[to], only counting days up to [today]. */
    fun ratioInRange(habitIds: Set<Long>, from: LocalDate, to: LocalDate, today: LocalDate): Float {
        if (habitIds.isEmpty()) return 0f
        val end = if (to.isAfter(today)) today else to
        if (end.isBefore(from)) return 0f
        val days = ChronoUnit.DAYS.between(from, end).toInt() + 1
        val done = totalInRange(habitIds, from, end)
        return (done.toFloat() / (days * habitIds.size)).coerceIn(0f, 1f)
    }

    companion object {
        fun dateOf(yearMonth: String, dayIndex: Int): LocalDate? = try {
            val ym = YearMonth.parse(yearMonth)
            if (dayIndex in 0 until ym.lengthOfMonth()) ym.atDay(dayIndex + 1) else null
        } catch (e: Exception) {
            null
        }
    }
}

object DateUtil {
    /** Monday..Sunday week containing [date]. */
    fun weekStart(date: LocalDate): LocalDate = date.with(DayOfWeek.MONDAY)

    /** Sunday..Saturday strip used on the home screen. */
    fun sundayStart(date: LocalDate): LocalDate {
        val back = date.dayOfWeek.value % 7 // Sunday -> 0, Monday -> 1 ... Saturday -> 6
        return date.minusDays(back.toLong())
    }

    fun dayIndex(date: LocalDate): Int = date.dayOfMonth - 1

    fun yearMonth(date: LocalDate): String = YearMonth.from(date).toString()

    /** Short label such as "1k", "1.6k", "850". */
    fun compactCount(value: Int): String {
        if (value < 1000) return value.toString()
        val k = value / 1000f
        val txt = if (value % 1000 == 0) (value / 1000).toString() else String.format(java.util.Locale.US, "%.1f", k)
        return txt + "k"
    }

    /** Whole-number percent 0..100. */
    fun percent(done: Int, total: Int): Int =
        if (total <= 0) 0 else ((done.toLong() * 100) / total).toInt().coerceIn(0, 100)
}
