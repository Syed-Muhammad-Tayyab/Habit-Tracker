package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.HabitEntity
import com.example.data.local.MetricLogEntity
import com.example.data.stats.DateUtil
import com.example.ui.components.EmojiBubble
import com.example.ui.components.GlassCard
import com.example.ui.components.CircleIconButton
import com.example.ui.components.IconBubble
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
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

private const val SLEEP_TARGET_HOURS = 8f
private const val READ_TARGET_HOURS = 6f
private const val EXERCISE_STREAK_TARGET = 12

@Composable
fun HomeScreen(viewModel: HabitViewModel) {
    val user by viewModel.currentUser.collectAsState()
    val habits by viewModel.activeHabits.collectAsState()
    val index by viewModel.completionIndex.collectAsState()
    val metricLogs by viewModel.recentMetrics.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var habitToEdit by remember { mutableStateOf<HabitEntity?>(null) }
    var showAllRoutine by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }
    var metricDialog by remember { mutableStateOf<String?>(null) }

    val today = LocalDate.now()
    val metrics: MetricLogEntity? = metricLogs.firstOrNull { it.dateStr == today.toString() }

    val habitIds = habits.map { it.id }.toSet()
    val doneToday = index.countOn(selectedDate, habitIds)
    val dailyPct = DateUtil.percent(doneToday, habits.size)

    val steps = metrics?.steps ?: 0
    val stepTarget = user?.dailyStepsTarget ?: 2000
    val stepPct = DateUtil.percent(steps, stepTarget)
    val water = metrics?.waterGlasses ?: 0
    val waterTarget = user?.dailyWaterTarget ?: 12
    val sleep = metrics?.sleepHours ?: 0f
    val readHours = (metrics?.readingMinutes ?: 0) / 60f

    val fitnessIds = habits.filter { it.category.equals("Fitness", ignoreCase = true) }.map { it.id }.toSet()
    val exerciseStreak = index.streakWhere(fitnessIds, today)

    val waterTrend = (6 downTo 0).map { back ->
        val d = today.minusDays(back.toLong()).toString()
        (metricLogs.firstOrNull { it.dateStr == d }?.waterGlasses ?: 0).toFloat()
    }

    val pendingToday = habits.filter { !index.isDone(it.id, today) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            HomeHeader(
                userName = user?.name ?: "Mithel Hourn",
                hasPending = pendingToday.isNotEmpty(),
                onBellClick = { showNotifications = true }
            )
        }

        item {
            WeekStrip(
                selectedDate = selectedDate,
                onSelect = { viewModel.selectDate(it) }
            )
        }

        item {
            GlassCard(contentPadding = PaddingValues(20.dp), modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Goals",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when {
                                habits.isEmpty() -> "Add a habit to get started"
                                dailyPct >= 80 -> "You are doing great today"
                                dailyPct >= 40 -> "Good progress, keep going"
                                else -> "Let's get started today"
                            },
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    RingProgress(
                        progress = dailyPct / 100f,
                        size = 66.dp,
                        strokeWidth = 7.dp,
                        color = habitColors.accent,
                        label = "$dailyPct%",
                        labelSize = 15
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    GoalBar(
                        title = "Sleep",
                        valueText = "${formatNumber(sleep)}/${formatNumber(SLEEP_TARGET_HOURS)} Hours",
                        progress = sleep / SLEEP_TARGET_HOURS,
                        color = BrandGreen,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("goal_sleep")
                            .clickable { metricDialog = "sleep" }
                    )
                    GoalBar(
                        title = "Exercise",
                        valueText = "${exerciseStreak.coerceAtMost(EXERCISE_STREAK_TARGET)}/$EXERCISE_STREAK_TARGET Streak",
                        progress = exerciseStreak / EXERCISE_STREAK_TARGET.toFloat(),
                        color = BrandPurple,
                        modifier = Modifier.weight(1f)
                    )
                    GoalBar(
                        title = "Read a Book",
                        valueText = "${formatNumber(readHours)}/${formatNumber(READ_TARGET_HOURS)} Hours",
                        progress = readHours / READ_TARGET_HOURS,
                        color = BrandOrange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("goal_read")
                            .clickable { metricDialog = "read" }
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.DirectionsWalk,
                    tint = BrandOrange,
                    title = "Daily Steps",
                    valueText = "${DateUtil.compactCount(steps)}/${DateUtil.compactCount(stepTarget)}",
                    unit = "Steps",
                    onMinus = { viewModel.updateSteps(-250) },
                    onPlus = { viewModel.updateSteps(250) },
                    testTag = "steps_card"
                ) {
                    RingProgress(
                        progress = stepPct / 100f,
                        size = 46.dp,
                        strokeWidth = 4.dp,
                        color = BrandOrange,
                        label = "$stepPct%",
                        labelSize = 11
                    )
                }
                MetricCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.WaterDrop,
                    tint = BrandBlue,
                    title = "Drink Water",
                    valueText = "$water/$waterTarget",
                    unit = "Glass",
                    onMinus = { viewModel.updateWater(-1) },
                    onPlus = { viewModel.updateWater(1) },
                    testTag = "water_card"
                ) {
                    Sparkline(values = waterTrend, color = BrandBlue)
                }
            }
        }

        item {
            SectionHeader(
                title = "Daily Routine",
                actionText = if (habits.size > 4) (if (showAllRoutine) "Show Less" else "See All") else null,
                onAction = { showAllRoutine = !showAllRoutine },
                trailing = {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(habitColors.accent)
                            .clickable { showAddDialog = true }
                            .testTag("add_routine_header_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Habit",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }

        if (habits.isEmpty()) {
            item { EmptyHabitsCard(onAddClick = { showAddDialog = true }) }
        } else {
            val visible = if (showAllRoutine) habits else habits.take(4)
            items(visible.size) { i ->
                val habit = visible[i]
                RoutineItem(
                    habit = habit,
                    isDone = index.isDone(habit.id, selectedDate),
                    streak = index.streakFor(habit.id, selectedDate),
                    onToggle = { viewModel.toggleHabitCompletion(habit.id, selectedDate) },
                    onEdit = { habitToEdit = habit }
                )
            }
            item {
                Text(
                    text = "Tap a routine to complete it · long-press to edit",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (showAddDialog) {
        AddEditHabitDialog(
            initialHabit = null,
            onDismiss = { showAddDialog = false },
            onSave = { name, emoji, category, duration, goal ->
                viewModel.addHabit(name, emoji, category, duration, goal)
            }
        )
    }

    habitToEdit?.let { habit ->
        AddEditHabitDialog(
            initialHabit = habit,
            onDismiss = { habitToEdit = null },
            onSave = { name, emoji, category, duration, goal ->
                viewModel.updateHabit(
                    habit.copy(name = name, emoji = emoji, category = category, durationMinutes = duration, goalDays = goal)
                )
            },
            onDelete = { habitId -> viewModel.deleteHabit(habitId) }
        )
    }

    if (showNotifications) {
        AlertDialog(
            onDismissRequest = { showNotifications = false },
            title = { Text("Today's reminders") },
            text = {
                if (pendingToday.isEmpty()) {
                    Text("All done for today. Great job! 🎉")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${pendingToday.size} habit(s) still to do:")
                        pendingToday.take(8).forEach { h ->
                            Text("${h.emoji}  ${h.name}  ·  ${h.durationMinutes} min", fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotifications = false }) { Text("Close") }
            }
        )
    }

    when (metricDialog) {
        "sleep" -> StepperDialog(
            title = "Sleep last night",
            valueText = "${formatNumber(sleep)} h",
            onMinus = { viewModel.updateSleep(-0.5f) },
            onPlus = { viewModel.updateSleep(0.5f) },
            onDismiss = { metricDialog = null }
        )
        "read" -> StepperDialog(
            title = "Reading time today",
            valueText = "${metrics?.readingMinutes ?: 0} min",
            onMinus = { viewModel.updateReading(-15) },
            onPlus = { viewModel.updateReading(15) },
            onDismiss = { metricDialog = null }
        )
    }
}

private fun formatNumber(v: Float): String =
    if (v % 1f == 0f) v.toInt().toString() else String.format(Locale.US, "%.1f", v)

@Composable
private fun HomeHeader(userName: String, hasPending: Boolean, onBellClick: () -> Unit) {
    val hour = LocalTime.now().hour
    val greeting = when {
        hour < 12 -> "Good Morning!"
        hour < 18 -> "Good Afternoon!"
        else -> "Good Evening!"
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.user_avatar_profile_1792138255311),
                contentDescription = "User Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = greeting,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = userName,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }
        CircleIconButton(
            icon = Icons.Default.Notifications,
            contentDescription = "Notifications",
            onClick = onBellClick,
            badge = hasPending,
            modifier = Modifier.testTag("home_bell")
        )
    }
}

@Composable
private fun WeekStrip(selectedDate: LocalDate, onSelect: (LocalDate) -> Unit) {
    val start = DateUtil.sundayStart(selectedDate)
    val colors = habitColors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        (0..6).forEach { offset ->
            val date = start.plusDays(offset.toLong())
            val isSelected = date == selectedDate
            val label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(30.dp))
                    .background(if (isSelected) colors.accent else colors.card)
                    .border(1.dp, if (isSelected) colors.accent else colors.cardStroke, RoundedCornerShape(30.dp))
                    .clickable { onSelect(date) }
                    .testTag("week_day_${date.dayOfMonth}")
                    .padding(top = 10.dp, bottom = 8.dp)
            ) {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${date.dayOfMonth}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun GoalBar(
    title: String,
    valueText: String,
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(habitColors.track)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0f, 1f))
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = valueText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    title: String,
    valueText: String,
    unit: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    testTag: String,
    trailing: @Composable () -> Unit
) {
    GlassCard(
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(26.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(icon = icon, tint = tint, size = 36.dp, contentDescription = title)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = valueText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            trailing()
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
        ) {
            StepChip(label = "−", onClick = onMinus)
            StepChip(label = "+", onClick = onPlus)
        }
    }
}

@Composable
private fun StepChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 38.dp, height = 28.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(habitColors.accentSoft)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = habitColors.accent
        )
    }
}

/** Small smooth line chart (no axes) used on the water card. */
@Composable
private fun Sparkline(values: List<Float>, color: Color) {
    Canvas(modifier = Modifier.size(width = 58.dp, height = 30.dp)) {
        if (values.size < 2) return@Canvas
        val maxV = values.max()
        val minV = values.min()
        val range = (maxV - minV).coerceAtLeast(1f)
        val stepX = size.width / (values.size - 1)
        val pts = values.mapIndexed { i, v ->
            val norm = (v - minV) / range
            Offset(i * stepX, size.height * 0.9f - norm * size.height * 0.8f)
        }
        val path = Path()
        path.moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size) {
            val prev = pts[i - 1]
            val cur = pts[i]
            val midX = (prev.x + cur.x) / 2f
            path.cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
        }
        drawPath(path = path, color = color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun RoutineItem(
    habit: HabitEntity,
    isDone: Boolean,
    streak: Int,
    onToggle: () -> Unit,
    onEdit: () -> Unit
) {
    val tint = habitTint(habit.category, habit.name)
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_toggle_${habit.id}"),
        shape = RoundedCornerShape(26.dp),
        onClick = onToggle,
        onLongClick = onEdit,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EmojiBubble(emoji = habit.emoji, tint = tint, size = 54.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (streak == 1) "Streak 1 day" else "Streak $streak days",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            if (isDone) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(BrandGreen),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(text = "Done", fontSize = 12.sp, color = BrandGreen, fontWeight = FontWeight.Medium)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Duration",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${habit.durationMinutes} Min",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHabitsCard(onAddClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onAddClick,
        contentPadding = PaddingValues(28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "✨", fontSize = 36.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No habits yet",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Tap here to add your first daily habit and start a streak!",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun StepperDialog(
    title: String,
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepChip(label = "−", onClick = onMinus)
                Text(text = valueText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                StepChip(label = "+", onClick = onPlus)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}
