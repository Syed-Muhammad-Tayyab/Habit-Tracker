package com.example

import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.components.BottomBarItem
import com.example.ui.components.HabitBottomBar
import com.example.ui.components.ScreenBackground
import com.example.ui.screens.AiCoachScreen
import com.example.ui.screens.AuthDialogHost
import com.example.ui.screens.CalendarMatrixScreen
import com.example.ui.screens.ChallengeScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProgressScreen
import com.example.ui.theme.HabitTrackerTheme
import com.example.ui.viewmodel.HabitViewModel

const val TAB_HOME = 0
const val TAB_PROGRESS = 1
const val TAB_CALENDAR = 2
const val TAB_CHALLENGE = 3
const val TAB_PROFILE = 4

class MainActivity : ComponentActivity() {
    private val viewModel: HabitViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appTheme by viewModel.appTheme.collectAsState()
            val toastMessage by viewModel.toastMessage.collectAsState()

            // Keep status/navigation bar icon colours readable for the chosen theme.
            LaunchedEffect(appTheme) {
                val style = if (appTheme == "light") {
                    SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                } else {
                    SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                }
                this@MainActivity.enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            LaunchedEffect(toastMessage) {
                toastMessage?.let { msg ->
                    Toast.makeText(this@MainActivity, msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearToast()
                }
            }

            HabitTrackerTheme(themeKey = appTheme) {
                MainAppScaffold(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScaffold(viewModel: HabitViewModel) {
    var currentTab by rememberSaveable { mutableStateOf(TAB_HOME) }
    var showCoach by rememberSaveable { mutableStateOf(false) }

    val items = remember {
        listOf(
            BottomBarItem("Home", Icons.Outlined.Home, Icons.Filled.Home),
            BottomBarItem("Progress", Icons.Outlined.ShowChart, Icons.Filled.ShowChart),
            BottomBarItem("Calendar", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
            BottomBarItem("Challenge", Icons.Outlined.EmojiEvents, Icons.Filled.EmojiEvents),
            BottomBarItem("Profile", Icons.Outlined.Person, Icons.Filled.Person)
        )
    }

    // Back: close the coach, otherwise return to Home from any other tab.
    BackHandler(enabled = showCoach || currentTab != TAB_HOME) {
        if (showCoach) showCoach = false else currentTab = TAB_HOME
    }

    ScreenBackground {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (showCoach) {
                    AiCoachScreen(viewModel = viewModel, onBack = { showCoach = false })
                } else {
                    when (currentTab) {
                        TAB_HOME -> HomeScreen(viewModel = viewModel)
                        TAB_PROGRESS -> ProgressScreen(
                            viewModel = viewModel,
                            onBack = { currentTab = TAB_HOME },
                            onNavigate = { currentTab = it }
                        )
                        TAB_CALENDAR -> CalendarMatrixScreen(viewModel = viewModel)
                        TAB_CHALLENGE -> ChallengeScreen(
                            viewModel = viewModel,
                            onOpenCoach = { showCoach = true }
                        )
                        else -> ProfileScreen(
                            viewModel = viewModel,
                            onOpenCoach = { showCoach = true }
                        )
                    }
                }

                // Sign-in / register / password dialogs
                AuthDialogHost(viewModel = viewModel)
            }

            HabitBottomBar(
                items = items,
                selectedIndex = currentTab,
                onSelect = {
                    showCoach = false
                    currentTab = it
                },
                modifier = Modifier.testTag("main_bottom_nav_bar")
            )
        }
    }
}
