package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.GeminiHabitCoach
import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitEntity
import com.example.data.local.HabitTrackerDatabase
import com.example.data.local.MetricLogEntity
import com.example.data.local.UserEntity
import com.example.data.repository.HabitRepository
import com.example.data.repository.SessionManager
import com.example.data.stats.CompletionIndex
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

sealed class AuthDialogType {
    object None : AuthDialogType()
    object Login : AuthDialogType()
    object Register : AuthDialogType()
    data class IdReveal(val generatedId: String, val userName: String) : AuthDialogType()
    object ChangePassword : AuthDialogType()
}

@OptIn(ExperimentalCoroutinesApi::class)
class HabitViewModel(application: Application) : AndroidViewModel(application) {
    private val db = HabitTrackerDatabase.getInstance(application)
    private val sessionManager = SessionManager(application)
    val repository = HabitRepository(db.userDao(), db.habitDao(), sessionManager)

    private val _currentUserId = MutableStateFlow(sessionManager.getCurrentUserId())
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _appTheme = MutableStateFlow(sessionManager.getAppTheme())
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    /** Day selected on the Home screen / Calendar screen. */
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    /** Month shown on the Calendar screen. */
    private val _calendarMonth = MutableStateFlow(YearMonth.now())
    val calendarMonth: StateFlow<YearMonth> = _calendarMonth.asStateFlow()

    private val _authDialog = MutableStateFlow<AuthDialogType>(AuthDialogType.None)
    val authDialog: StateFlow<AuthDialogType> = _authDialog.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _aiCoachingResult = MutableStateFlow<String?>(null)
    val aiCoachingResult: StateFlow<String?> = _aiCoachingResult.asStateFlow()

    val currentUser: StateFlow<UserEntity?> = _currentUserId.flatMapLatest {
        repository.observeCurrentUser()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeHabits: StateFlow<List<HabitEntity>> = _currentUserId.flatMapLatest {
        repository.observeActiveHabits()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Every completion ever stored for the current user (small data set, filtered in memory). */
    val allCompletions: StateFlow<List<HabitCompletionEntity>> = _currentUserId.flatMapLatest {
        repository.observeAllCompletions()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Fast lookup structure derived from [allCompletions]. */
    val completionIndex: StateFlow<CompletionIndex> = allCompletions
        .map { CompletionIndex(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CompletionIndex(emptyList()))

    /** Most recent daily metric logs (newest first). */
    val recentMetrics: StateFlow<List<MetricLogEntity>> = _currentUserId.flatMapLatest {
        repository.observeRecentMetrics()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.getCurrentUser() // make sure the profile (and first-run data) exists
        }
    }

    // ---- navigation / selection ------------------------------------------------------------

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun setCalendarMonth(month: YearMonth) {
        _calendarMonth.value = month
    }

    fun shiftCalendarMonth(delta: Int) {
        _calendarMonth.value = _calendarMonth.value.plusMonths(delta.toLong())
    }

    // ---- habits ---------------------------------------------------------------------------

    fun toggleHabitCompletion(habitId: Long, date: LocalDate = _selectedDate.value) {
        viewModelScope.launch {
            val isDone = completionIndex.value.isDone(habitId, date)
            repository.toggleCompletion(habitId, date, !isDone)
        }
    }

    fun addHabit(name: String, emoji: String, category: String, durationMinutes: Int, goalDays: Int) {
        viewModelScope.launch {
            repository.addHabit(name, emoji, category, durationMinutes, goalDays)
            showToast("Habit added")
        }
    }

    fun updateHabit(habit: HabitEntity) {
        viewModelScope.launch {
            repository.updateHabit(habit)
            showToast("Habit updated")
        }
    }

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
            showToast("Habit deleted")
        }
    }

    // ---- daily metrics --------------------------------------------------------------------

    fun updateWater(delta: Int) {
        viewModelScope.launch { repository.updateWaterGlasses(delta) }
    }

    fun updateSteps(delta: Int) {
        viewModelScope.launch { repository.updateSteps(delta) }
    }

    fun updateSleep(deltaHours: Float) {
        viewModelScope.launch { repository.updateSleepHours(deltaHours) }
    }

    fun updateReading(deltaMinutes: Int) {
        viewModelScope.launch { repository.updateReadingMinutes(deltaMinutes) }
    }

    fun adjustStepsTarget(delta: Int) {
        val current = currentUser.value?.dailyStepsTarget ?: 2000
        viewModelScope.launch {
            repository.updateDailyTargets((current + delta).coerceIn(500, 50_000), null)
        }
    }

    fun adjustWaterTarget(delta: Int) {
        val current = currentUser.value?.dailyWaterTarget ?: 12
        viewModelScope.launch {
            repository.updateDailyTargets(null, (current + delta).coerceIn(1, 30))
        }
    }

    fun updateMood(mood: Int) {
        viewModelScope.launch { repository.updateMood(mood) }
    }

    // ---- theme / auth ---------------------------------------------------------------------

    fun switchTheme(theme: String) {
        _appTheme.value = theme
        repository.setAppTheme(theme)
    }

    fun openAuthDialog(dialog: AuthDialogType) {
        _authDialog.value = dialog
    }

    fun dismissAuthDialog() {
        _authDialog.value = AuthDialogType.None
    }

    fun register(name: String, passwordRaw: String) {
        viewModelScope.launch {
            repository.registerUser(name, passwordRaw)
                .onSuccess { user ->
                    _currentUserId.value = user.id
                    _authDialog.value = AuthDialogType.IdReveal(user.id, user.name)
                }
                .onFailure { err -> showToast(err.message ?: "Registration failed") }
        }
    }

    fun login(id: String, passwordRaw: String) {
        viewModelScope.launch {
            repository.loginUser(id, passwordRaw)
                .onSuccess { user ->
                    _currentUserId.value = user.id
                    _authDialog.value = AuthDialogType.None
                    showToast("Welcome back, ${user.name}!")
                }
                .onFailure { err -> showToast(err.message ?: "Login failed") }
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            val guest = repository.continueAsGuest()
            _currentUserId.value = guest.id
            _authDialog.value = AuthDialogType.None
            showToast("Continuing as Guest")
        }
    }

    fun changePassword(currentRaw: String, newRaw: String) {
        viewModelScope.launch {
            repository.changePassword(currentRaw, newRaw)
                .onSuccess {
                    _authDialog.value = AuthDialogType.None
                    showToast("Password updated successfully")
                }
                .onFailure { err -> showToast(err.message ?: "Failed to update password") }
        }
    }

    fun exportData(onDataReady: (String) -> Unit) {
        viewModelScope.launch {
            onDataReady(repository.exportUserDataJson())
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            repository.deleteAccount()
            _currentUserId.value = "guest"
            showToast("Account data deleted")
        }
    }

    // ---- AI coach -------------------------------------------------------------------------

    fun requestAiCoaching(userQuery: String = "") {
        viewModelScope.launch {
            _isAiLoading.value = true
            val habits = activeHabits.value
            val index = completionIndex.value
            val today = LocalDate.now()
            val monthStart = today.withDayOfMonth(1)

            val sb = StringBuilder()
            sb.append("Date: ").append(today).append("\n")
            sb.append("Total Active Habits: ").append(habits.size).append("\n")
            habits.forEach { h ->
                val doneThisMonth = index.doneDaysInRange(h.id, monthStart, today)
                val streak = index.streakFor(h.id, today)
                sb.append("- ${h.emoji} ${h.name} (${h.category}): $doneThisMonth days this month, current streak $streak days\n")
            }
            recentMetrics.value.firstOrNull { it.dateStr == today.toString() }?.let { m ->
                sb.append("Today's water: ").append(m.waterGlasses).append(" glasses\n")
                sb.append("Today's steps: ").append(m.steps).append("\n")
                sb.append("Last night's sleep: ").append(m.sleepHours).append(" hours\n")
            }
            currentUser.value?.let { sb.append("Mood rating (1-5): ").append(it.currentMood).append("\n") }

            val result = try {
                GeminiHabitCoach.analyzeHabitsAndCoach(
                    habitSummary = sb.toString(),
                    userQuery = userQuery,
                    useHighThinking = false
                )
            } catch (e: Exception) {
                "Unable to reach the AI Coach right now: ${e.localizedMessage ?: "unknown error"}"
            }
            _aiCoachingResult.value = result
            _isAiLoading.value = false
        }
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
