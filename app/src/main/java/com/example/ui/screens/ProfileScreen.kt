package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.GlassCard
import com.example.ui.components.IconBubble
import com.example.ui.components.SectionHeader
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandOrange
import com.example.ui.theme.habitColors
import com.example.ui.viewmodel.AuthDialogType
import com.example.ui.viewmodel.HabitViewModel
import java.time.LocalDate

@Composable
fun ProfileScreen(viewModel: HabitViewModel, onOpenCoach: () -> Unit) {
    val user by viewModel.currentUser.collectAsState()
    val habits by viewModel.activeHabits.collectAsState()
    val index by viewModel.completionIndex.collectAsState()
    val themeKey by viewModel.appTheme.collectAsState()
    val context = LocalContext.current

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var exportedJson by remember { mutableStateOf<String?>(null) }
    var targetDialog by remember { mutableStateOf<String?>(null) }

    val ids = habits.map { it.id }.toSet()
    val today = LocalDate.now()
    val bestStreak = index.bestStreak(ids)
    val monthCheckIns = index.totalInRange(ids, today.withDayOfMonth(1), today)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(
                text = "Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // ---- identity card ---------------------------------------------------------------------
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.user_avatar_profile_1792138255311),
                        contentDescription = "User Avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .border(3.dp, habitColors.accent, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = user?.name ?: "Mithel Hourn",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(habitColors.accentSoft)
                            .clickable {
                                val id = user?.id ?: "guest"
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Habit ID", id))
                                viewModel.showToast("Account ID copied")
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = if (user?.isGuest == true) "Guest Session" else (user?.id ?: "HT-000000"),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = habitColors.accent
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy ID",
                            tint = habitColors.accent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatColumn("${habits.size}", "Habits")
                        StatColumn("$bestStreak", "Best streak")
                        StatColumn("$monthCheckIns", "This month")
                    }
                }
            }
        }

        // ---- shortcuts & goals ------------------------------------------------------------------
        item { SectionHeader(title = "Goals") }
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                ActionRow(
                    icon = Icons.Default.DirectionsWalk,
                    title = "Daily steps goal",
                    subtitle = "${user?.dailyStepsTarget ?: 2000} steps",
                    tint = BrandOrange,
                    onClick = { targetDialog = "steps" },
                    testTag = "profile_steps_goal"
                )
                ActionRow(
                    icon = Icons.Default.WaterDrop,
                    title = "Daily water goal",
                    subtitle = "${user?.dailyWaterTarget ?: 12} glasses",
                    tint = BrandBlue,
                    onClick = { targetDialog = "water" },
                    testTag = "profile_water_goal"
                )
                ActionRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "AI Habit Coach",
                    subtitle = "Personalised tips from your data",
                    tint = habitColors.accent,
                    onClick = onOpenCoach,
                    testTag = "profile_ai_coach"
                )
            }
        }

        // ---- theme ------------------------------------------------------------------------------
        item { SectionHeader(title = "Appearance") }
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ColorLens, contentDescription = "Theme", tint = habitColors.accent)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Colour theme",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ThemeOptionChip("Sky", Color(0xFFD3EBF8), Color(0xFF34A9DE), themeKey == "light") {
                        viewModel.switchTheme("light")
                    }
                    ThemeOptionChip("Night", Color(0xFF0B1118), Color(0xFF4CB8E8), themeKey == "black") {
                        viewModel.switchTheme("black")
                    }
                    ThemeOptionChip("Sunset", Color(0xFF1A110D), Color(0xFFFF8A3D), themeKey == "orange") {
                        viewModel.switchTheme("orange")
                    }
                }
            }
        }

        // ---- account ----------------------------------------------------------------------------
        item { SectionHeader(title = "Account") }
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                if (user?.isGuest != false) {
                    ActionRow(
                        icon = Icons.Default.PersonAdd,
                        title = "Create Account",
                        subtitle = "Get a unique ID to keep your habits separate",
                        onClick = { viewModel.openAuthDialog(AuthDialogType.Register) },
                        testTag = "profile_create_account_btn"
                    )
                    ActionRow(
                        icon = Icons.Default.Login,
                        title = "Sign In with ID",
                        subtitle = "Access an existing account (HT-XXXXXX)",
                        onClick = { viewModel.openAuthDialog(AuthDialogType.Login) },
                        testTag = "profile_signin_btn"
                    )
                } else {
                    ActionRow(
                        icon = Icons.Default.Lock,
                        title = "Change Password",
                        subtitle = "Update your account password",
                        onClick = { viewModel.openAuthDialog(AuthDialogType.ChangePassword) },
                        testTag = "profile_change_password_btn"
                    )
                    ActionRow(
                        icon = Icons.Default.Login,
                        title = "Switch Account",
                        subtitle = "Sign in to another account ID",
                        onClick = { viewModel.openAuthDialog(AuthDialogType.Login) },
                        testTag = "profile_switch_account_btn"
                    )
                    ActionRow(
                        icon = Icons.Default.Logout,
                        title = "Log Out",
                        subtitle = "Return to the guest profile",
                        onClick = { viewModel.continueAsGuest() },
                        testTag = "profile_logout_btn"
                    )
                }
            }
        }

        // ---- data -------------------------------------------------------------------------------
        item { SectionHeader(title = "Data") }
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                ActionRow(
                    icon = Icons.Default.Download,
                    title = "Export my data (JSON)",
                    subtitle = "Copy all habits, history and metrics",
                    onClick = { viewModel.exportData { json -> exportedJson = json } },
                    testTag = "profile_export_data_btn"
                )
                ActionRow(
                    icon = Icons.Default.Shield,
                    title = "Stored on this device",
                    subtitle = "Everything is saved locally and scoped to your ID",
                    onClick = { viewModel.showToast("Your data lives only on this device") }
                )
                ActionRow(
                    icon = Icons.Default.DeleteForever,
                    title = "Delete account data",
                    subtitle = "Permanently remove records from this device",
                    onClick = { showDeleteConfirm = true },
                    isDanger = true,
                    testTag = "profile_delete_account_btn"
                )
            }
        }
    }

    // ---- dialogs ----------------------------------------------------------------------------------
    when (targetDialog) {
        "steps" -> StepperDialog(
            title = "Daily steps goal",
            valueText = "${user?.dailyStepsTarget ?: 2000}",
            onMinus = { viewModel.adjustStepsTarget(-500) },
            onPlus = { viewModel.adjustStepsTarget(500) },
            onDismiss = { targetDialog = null }
        )
        "water" -> StepperDialog(
            title = "Daily water goal (glasses)",
            valueText = "${user?.dailyWaterTarget ?: 12}",
            onMinus = { viewModel.adjustWaterTarget(-1) },
            onPlus = { viewModel.adjustWaterTarget(1) },
            onDismiss = { targetDialog = null }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete account data?") },
            text = {
                Text("This permanently erases all habits, history and settings for this account on this device. It cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteAccount()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    exportedJson?.let { json ->
        AlertDialog(
            onDismissRequest = { exportedJson = null },
            title = { Text("Exported data") },
            text = {
                Column {
                    Text(
                        text = "Preview (copy to get the full JSON):",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = json.take(600) + if (json.length > 600) "\n… [truncated]" else "",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Habit Export JSON", json))
                        viewModel.showToast("Export copied to clipboard")
                        exportedJson = null
                    }
                ) { Text("Copy") }
            },
            dismissButton = {
                TextButton(onClick = { exportedJson = null }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun StatColumn(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ThemeOptionChip(
    label: String,
    bgColor: Color,
    accentColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) habitColors.accent else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(bgColor)
                .border(3.dp, accentColor, CircleShape)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tint: Color = Color.Unspecified,
    isDanger: Boolean = false,
    testTag: String = ""
) {
    val iconTint = when {
        isDanger -> MaterialTheme.colorScheme.error
        tint != Color.Unspecified -> tint
        else -> habitColors.accent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(icon = icon, tint = iconTint, size = 42.dp, contentDescription = title)
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDanger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
