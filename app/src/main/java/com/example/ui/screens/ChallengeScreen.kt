package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.EmojiBubble
import com.example.ui.components.GlassCard
import com.example.ui.components.IconBubble
import com.example.ui.components.SectionHeader
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandGreen
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.BrandPurple
import com.example.ui.theme.habitColors
import com.example.ui.viewmodel.HabitViewModel
import java.time.LocalDate

private data class Challenge(
    val emoji: String,
    val title: String,
    val description: String,
    val current: Int,
    val target: Int,
    val tint: Color
)

@Composable
fun ChallengeScreen(viewModel: HabitViewModel, onOpenCoach: () -> Unit) {
    val habits by viewModel.activeHabits.collectAsState()
    val index by viewModel.completionIndex.collectAsState()
    val metricLogs by viewModel.recentMetrics.collectAsState()
    val user by viewModel.currentUser.collectAsState()

    val today = LocalDate.now()
    val ids = habits.map { it.id }.toSet()
    val streak = index.streakWhere(ids, today)
    val best = index.bestStreak(ids)

    val last7 = (0..6).map { today.minusDays(it.toLong()) }
    val perfectDays = if (habits.isEmpty()) 0 else last7.count { index.countOn(it, ids) == habits.size }
    val waterTarget = user?.dailyWaterTarget ?: 12
    val stepTarget = user?.dailyStepsTarget ?: 2000
    val hydrationDays = last7.count { d ->
        (metricLogs.firstOrNull { it.dateStr == d.toString() }?.waterGlasses ?: 0) >= waterTarget
    }
    val stepDays = last7.count { d ->
        (metricLogs.firstOrNull { it.dateStr == d.toString() }?.steps ?: 0) >= stepTarget
    }
    val totalDone = index.totalInRange(ids, today.minusDays(29), today)

    val challenges = listOf(
        Challenge("🔥", "7-Day Streak", "Complete at least one habit every day for a week.", streak, 7, BrandOrange),
        Challenge("🌱", "21-Day Habit Builder", "Keep your daily streak alive for 21 days.", streak, 21, BrandGreen),
        Challenge("🏆", "30-Day Champion", "A full month without breaking the chain.", streak, 30, BrandPurple),
        Challenge("⭐", "Perfect Week", "Finish all habits on 7 of the last 7 days.", perfectDays, 7, BrandBlue),
        Challenge("💧", "Hydration Hero", "Hit your water goal on 7 of the last 7 days.", hydrationDays, 7, BrandBlue),
        Challenge("👟", "Step Master", "Reach your step goal on 7 of the last 7 days.", stepDays, 7, BrandOrange)
    )
    val completedCount = challenges.count { it.current >= it.target }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Challenges",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "$completedCount of ${challenges.size} completed",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                contentPadding = PaddingValues(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EmojiBubble(emoji = "🔥", tint = BrandOrange, size = 64.dp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "$streak day streak",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("challenge_streak")
                        )
                        Text(
                            text = "Best: $best days · Last 30 days: $totalDone check-ins",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_ai_coach"),
                shape = RoundedCornerShape(26.dp),
                onClick = onOpenCoach,
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBubble(icon = Icons.Default.AutoAwesome, tint = habitColors.accent, size = 48.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Habit Coach",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Get personalised tips based on your progress",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(text = "›", fontSize = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item { SectionHeader(title = "Active Challenges") }

        items(challenges.size) { i ->
            ChallengeItem(challenges[i])
        }
    }
}

@Composable
private fun ChallengeItem(c: Challenge) {
    val done = c.current >= c.target
    val shown = c.current.coerceAtMost(c.target)
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiBubble(emoji = c.emoji, tint = c.tint, size = 54.dp)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = c.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    if (done) {
                        Text(
                            text = "Completed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(BrandGreen)
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        )
                    } else {
                        Text(
                            text = "$shown/${c.target}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = c.tint
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = c.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(habitColors.track)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(shown / c.target.toFloat())
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (done) BrandGreen else c.tint)
                    )
                }
            }
        }
    }
}
