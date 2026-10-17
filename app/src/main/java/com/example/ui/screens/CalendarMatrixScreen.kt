package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.stats.DateUtil
import com.example.ui.components.CircleIconButton
import com.example.ui.components.EmojiBubble
import com.example.ui.components.GlassCard
import com.example.ui.components.RingProgress
import com.example.ui.components.SectionHeader
import com.example.ui.components.habitTint
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.habitColors
import com.example.ui.viewmodel.HabitViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun CalendarMatrixScreen(viewModel: HabitViewModel) {
    val habits by viewModel.activeHabits.collectAsState()
    val index by viewModel.completionIndex.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val month by viewModel.calendarMonth.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    val today = LocalDate.now()
    val ids = habits.map { it.id }.toSet()
    val monthStart = month.atDay(1)
    val monthEnd = month.atEndOfMonth()
    val elapsedEnd = if (monthEnd.isAfter(today)) today else monthEnd
    val elapsedDays = if (elapsedEnd.isBefore(monthStart)) 0 else ChronoUnit.DAYS.between(monthStart, elapsedEnd).toInt() + 1

    val completedTotal = if (elapsedDays == 0) 0 else index.totalInRange(ids, monthStart, elapsedEnd)
    val possibleTotal = habits.size * elapsedDays
    val overallPct = DateUtil.percent(completedTotal, possibleTotal)
    var perfectDays = 0
    if (habits.isNotEmpty() && elapsedDays > 0) {
        var d = monthStart
        while (!d.isAfter(elapsedEnd)) {
            if (index.countOn(d, ids) == habits.size) perfectDays++
            d = d.plusDays(1)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ---- month switcher ----------------------------------------------------------------------
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleIconButton(
                    icon = Icons.Default.ChevronLeft,
                    contentDescription = "Previous month",
                    onClick = { viewModel.shiftCalendarMonth(-1) },
                    modifier = Modifier.testTag("calendar_prev_month")
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = month.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${month.year}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CircleIconButton(
                    icon = Icons.Default.ChevronRight,
                    contentDescription = "Next month",
                    onClick = { viewModel.shiftCalendarMonth(1) },
                    modifier = Modifier.testTag("calendar_next_month")
                )
            }
        }

        // ---- month grid ---------------------------------------------------------------------------
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach { l ->
                        Text(
                            text = l,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                val leading = monthStart.dayOfWeek.value % 7 // Sunday-first
                val cells = leading + month.lengthOfMonth()
                val rowCount = (cells + 6) / 7
                for (r in 0 until rowCount) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (c in 0 until 7) {
                            val dayNumber = r * 7 + c - leading + 1
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayNumber in 1..month.lengthOfMonth()) {
                                    val date = month.atDay(dayNumber)
                                    DayCell(
                                        date = date,
                                        ratio = if (date.isAfter(today) || habits.isEmpty()) 0f
                                        else index.countOn(date, ids).toFloat() / habits.size,
                                        isSelected = date == selectedDate,
                                        isToday = date == today,
                                        onClick = { viewModel.selectDate(date) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---- habits for the selected day ----------------------------------------------------------
        item {
            SectionHeader(
                title = if (selectedDate == today) "Today's Habits"
                else "${selectedDate.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)} ${selectedDate.dayOfMonth} Habits"
            )
        }

        if (habits.isEmpty()) {
            item {
                Text(
                    text = "No habits yet. Add some from the Home tab.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(habits.size) { i ->
            val habit = habits[i]
            val done = index.isDone(habit.id, selectedDate)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calendar_toggle_${habit.id}"),
                shape = RoundedCornerShape(24.dp),
                onClick = { viewModel.toggleHabitCompletion(habit.id, selectedDate) },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    EmojiBubble(emoji = habit.emoji, tint = habitTint(habit.category, habit.name), size = 46.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = habit.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = "${habit.category} · ${habit.durationMinutes} min",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(if (done) habitColors.accent else Color.Transparent)
                            .border(
                                2.dp,
                                if (done) habitColors.accent else MaterialTheme.colorScheme.outline,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (done) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Completed",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // ---- month summary ------------------------------------------------------------------------
        item { SectionHeader(title = "Month Summary") }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                contentPadding = PaddingValues(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RingProgress(
                        progress = overallPct / 100f,
                        size = 96.dp,
                        strokeWidth = 9.dp,
                        color = habitColors.accent,
                        label = "$overallPct%",
                        labelSize = 20
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SummaryChip("Completed", "$completedTotal", BrandBlue)
                        SummaryChip("Possible", "$possibleTotal", BrandPurple)
                        SummaryChip("Perfect days", "$perfectDays", BrandGreen)
                    }
                }
            }
        }

        if (habits.isNotEmpty()) {
            item { SectionHeader(title = "Habit Ranking") }
            item {
                val ranked = habits.map { h ->
                    val done = if (elapsedDays == 0) 0 else index.doneDaysInRange(h.id, monthStart, elapsedEnd)
                    Triple(h, done, DateUtil.percent(done, elapsedDays))
                }.sortedByDescending { it.third }

                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(30.dp),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    ranked.forEachIndexed { rank, (h, done, pct) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (rank) {
                                            0 -> BrandBlue
                                            1 -> BrandOrange
                                            2 -> BrandGreen
                                            else -> habitColors.track
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${rank + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (rank < 3) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${h.emoji} ${h.name}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(habitColors.track)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(pct / 100f)
                                            .height(5.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(habitTint(h.category, h.name))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "$pct%",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "$done/$elapsedDays d",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (rank != ranked.lastIndex) Spacer(modifier = Modifier.height(14.dp))
                    }
                }
            }
        }

        // ---- mood ----------------------------------------------------------------------------------
        item { SectionHeader(title = "How are you feeling?") }
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                val moods = listOf(1 to "😞", 2 to "😐", 3 to "🙂", 4 to "😄", 5 to "🤩")
                val currentMood = user?.currentMood ?: 3
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    moods.forEach { (rating, emoji) ->
                        val isSel = rating == currentMood
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSel) habitColors.accent else habitColors.track.copy(alpha = 0.6f))
                                .clickable { viewModel.updateMood(rating) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                                .testTag("mood_$rating"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = emoji, fontSize = 22.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    ratio: Float,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit
) {
    val accent = habitColors.accent
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isSelected) accent else Color.Transparent)
            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, accent, CircleShape) else Modifier)
            .clickable(onClick = onClick)
            .testTag("calendar_day_${date.dayOfMonth}"),
        contentAlignment = Alignment.Center
    ) {
        RingProgress(
            progress = if (isSelected) 0f else ratio,
            size = 40.dp,
            strokeWidth = 3.dp,
            color = if (ratio >= 0.999f) BrandGreen else accent,
            label = "${date.dayOfMonth}",
            labelSize = 12,
            labelColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun SummaryChip(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
