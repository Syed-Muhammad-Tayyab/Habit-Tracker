package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.stats.CompletionIndex
import com.example.data.stats.DateUtil
import com.example.ui.components.CircleIconButton
import com.example.ui.components.EmojiBubble
import com.example.ui.components.GlassCard
import com.example.ui.components.IconBubble
import com.example.ui.components.RingProgress
import com.example.ui.components.SectionHeader
import com.example.ui.components.SegmentedPills
import com.example.ui.components.habitTint
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.habitColors
import com.example.ui.viewmodel.HabitViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private data class Bar(val label: String, val ratio: Float, val isCurrent: Boolean)

private data class Period(val from: LocalDate, val to: LocalDate, val title: String)

private data class HistoryRow(
    val emoji: String?,
    val iconTint: Color,
    val title: String,
    val subtitle: String,
    val pct: Int,
    val isMetric: Boolean
)

@Composable
fun ProgressScreen(
    viewModel: HabitViewModel,
    onBack: () -> Unit,
    onNavigate: (Int) -> Unit
) {
    val habits by viewModel.activeHabits.collectAsState()
    val index by viewModel.completionIndex.collectAsState()
    val user by viewModel.currentUser.collectAsState()
    val metricLogs by viewModel.recentMetrics.collectAsState()

    var tab by remember { mutableStateOf("Weekly") }
    var selectedBar by remember { mutableStateOf<Int?>(null) }
    var showAll by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }

    val today = LocalDate.now()
    val ids = habits.map { it.id }.toSet()

    val bars: List<Bar> = when (tab) {
        "Monthly" -> monthlyBars(index, ids, today)
        "Yearly" -> yearlyBars(index, ids, today)
        else -> weeklyBars(index, ids, today)
    }
    val period: Period = when (tab) {
        "Monthly" -> Period(today.withDayOfMonth(1), today.withDayOfMonth(today.lengthOfMonth()), "This month")
        "Yearly" -> Period(today.withDayOfYear(1), today.withDayOfYear(today.lengthOfYear()), "This year")
        else -> {
            val start = DateUtil.weekStart(today)
            Period(start, start.plusDays(6), "This week")
        }
    }
    val activeBar = selectedBar?.takeIf { it in bars.indices } ?: bars.indexOfFirst { it.isCurrent }.coerceAtLeast(0)
    val average = if (bars.isEmpty()) 0 else (bars.map { it.ratio }.average() * 100).toInt()

    // ---- history rows ------------------------------------------------------------------------
    val rows = mutableListOf<HistoryRow>()
    val todayLog = metricLogs.firstOrNull { it.dateStr == today.toString() }
    val stepsNow = todayLog?.steps ?: 0
    val stepTarget = user?.dailyStepsTarget ?: 2000
    val waterNow = todayLog?.waterGlasses ?: 0
    val waterTarget = user?.dailyWaterTarget ?: 12
    rows += HistoryRow(
        emoji = null, iconTint = BrandBlue, title = "Daily Steps",
        subtitle = "${formatThousands(stepsNow)} of ${formatThousands(stepTarget)} Steps",
        pct = DateUtil.percent(stepsNow, stepTarget), isMetric = true
    )
    rows += HistoryRow(
        emoji = "💧", iconTint = BrandBlue, title = "Drink Water",
        subtitle = "$waterNow of $waterTarget Glasses",
        pct = DateUtil.percent(waterNow, waterTarget), isMetric = true
    )
    val elapsedEnd = if (period.to.isAfter(today)) today else period.to
    val elapsedDays = (ChronoUnit.DAYS.between(period.from, elapsedEnd).toInt() + 1).coerceAtLeast(1)
    habits.forEach { h ->
        val done = index.doneDaysInRange(h.id, period.from, elapsedEnd)
        rows += HistoryRow(
            emoji = h.emoji,
            iconTint = habitTint(h.category, h.name),
            title = h.name,
            subtitle = "$done of $elapsedDays Days Completed",
            pct = DateUtil.percent(done, elapsedDays),
            isMetric = false
        )
    }
    val visibleRows = if (showAll) rows else rows.take(4)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircleIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onBack
                )
                Text(
                    text = "Progress",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Box {
                    CircleIconButton(
                        icon = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        onClick = { menuOpen = true }
                    )
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Open calendar") },
                            onClick = { menuOpen = false; onNavigate(2) }
                        )
                        DropdownMenuItem(
                            text = { Text("View challenges") },
                            onClick = { menuOpen = false; onNavigate(3) }
                        )
                    }
                }
            }
        }

        item {
            SegmentedPills(
                options = listOf("Weekly", "Monthly", "Yearly"),
                selected = tab,
                onSelect = { tab = it; selectedBar = null }
            )
        }

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
                    Column {
                        Text(
                            text = "Progress Habits",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${period.title} · avg $average%",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                BarChart(
                    bars = bars,
                    activeIndex = activeBar,
                    onSelect = { selectedBar = it }
                )
            }
        }

        item {
            SectionHeader(
                title = "Progress History",
                actionText = if (rows.size > 4) (if (showAll) "Show Less" else "See All") else null,
                onAction = { showAll = !showAll }
            )
        }

        items(visibleRows.size) { i ->
            HistoryItem(visibleRows[i])
        }
    }
}

// ---- chart data ---------------------------------------------------------------------------------

private fun weeklyBars(index: CompletionIndex, ids: Set<Long>, today: LocalDate): List<Bar> {
    val start = DateUtil.weekStart(today)
    return (0..6).map { i ->
        val d = start.plusDays(i.toLong())
        Bar(
            label = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
            ratio = index.ratioInRange(ids, d, d, today),
            isCurrent = d == today
        )
    }
}

private fun monthlyBars(index: CompletionIndex, ids: Set<Long>, today: LocalDate): List<Bar> {
    val ym = YearMonth.from(today)
    val weeks = (ym.lengthOfMonth() + 6) / 7
    return (0 until weeks).map { w ->
        val from = ym.atDay(1 + w * 7)
        val to = ym.atDay(minOf(7 + w * 7, ym.lengthOfMonth()))
        Bar(
            label = "W${w + 1}",
            ratio = index.ratioInRange(ids, from, to, today),
            isCurrent = !today.isBefore(from) && !today.isAfter(to)
        )
    }
}

private fun yearlyBars(index: CompletionIndex, ids: Set<Long>, today: LocalDate): List<Bar> {
    return (1..12).map { m ->
        val ym = YearMonth.of(today.year, m)
        Bar(
            label = ym.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(1),
            ratio = index.ratioInRange(ids, ym.atDay(1), ym.atEndOfMonth(), today),
            isCurrent = m == today.monthValue
        )
    }
}

private fun formatThousands(n: Int): String = String.format(Locale.US, "%,d", n)

// ---- UI pieces ----------------------------------------------------------------------------------

@Composable
private fun BarChart(bars: List<Bar>, activeIndex: Int, onSelect: (Int) -> Unit) {
    val trackHeight = 190.dp
    val headroom = 44.dp
    val barWidth: Dp = if (bars.size > 7) 16.dp else if (bars.size > 5) 34.dp else 40.dp

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEachIndexed { i, bar ->
            val selected = i == activeIndex
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(i) }
                    .testTag("progress_bar_$i")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(trackHeight + headroom),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    HatchedBar(
                        ratio = bar.ratio,
                        width = barWidth,
                        height = trackHeight,
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                    if (selected) {
                        val fillTop = trackHeight * (1f - bar.ratio.coerceIn(0f, 1f))
                        // value pill
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = headroom + fillTop - 38.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(habitColors.accent)
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${(bar.ratio * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        // ring marker on the bar top
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .offset(y = headroom + fillTop - 7.dp)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(3.dp, habitColors.accent, CircleShape)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = bar.label,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** A tall pill with diagonal hatching as its empty track and a solid accent fill from the bottom. */
@Composable
private fun HatchedBar(ratio: Float, width: Dp, height: Dp, modifier: Modifier = Modifier) {
    val accent = habitColors.accent
    val hatch = habitColors.track
    val base = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    Canvas(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(width / 2))
            .background(base)
    ) {
        val gap = 7.dp.toPx()
        val sw = 1.6.dp.toPx()
        var x = -size.height
        while (x < size.width) {
            drawLine(
                color = hatch,
                start = Offset(x, size.height),
                end = Offset(x + size.height, 0f),
                strokeWidth = sw
            )
            x += gap
        }
        val fillHeight = size.height * ratio.coerceIn(0f, 1f)
        if (fillHeight > 0f) {
            val radius = minOf(size.width / 2f, fillHeight / 2f)
            drawRoundRect(
                color = accent,
                topLeft = Offset(0f, size.height - fillHeight),
                size = Size(size.width, fillHeight),
                cornerRadius = CornerRadius(radius, radius)
            )
        }
    }
}

@Composable
private fun HistoryItem(row: HistoryRow) {
    val ringColor = if (row.pct >= 60) BrandBlue else BrandOrange
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (row.emoji != null) {
                EmojiBubble(emoji = row.emoji, tint = row.iconTint, size = 54.dp)
            } else {
                IconBubble(
                    icon = Icons.Default.DirectionsWalk,
                    tint = row.iconTint,
                    size = 54.dp,
                    contentDescription = row.title
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = row.subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RingProgress(
                progress = row.pct / 100f,
                size = 50.dp,
                strokeWidth = 4.dp,
                color = ringColor,
                label = "${row.pct}%",
                labelSize = 12
            )
        }
    }
}
