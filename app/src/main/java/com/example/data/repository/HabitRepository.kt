package com.example.data.repository

import com.example.data.local.HabitCompletionEntity
import com.example.data.local.HabitDao
import com.example.data.local.HabitEntity
import com.example.data.local.MetricLogEntity
import com.example.data.local.UserDao
import com.example.data.local.UserEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class HabitRepository(
    private val userDao: UserDao,
    private val habitDao: HabitDao,
    private val sessionManager: SessionManager
) {
    val currentUserId: String
        get() = sessionManager.getCurrentUserId()

    fun getAppTheme(): String = sessionManager.getAppTheme()
    fun setAppTheme(theme: String) = sessionManager.setAppTheme(theme)

    fun observeCurrentUser(): Flow<UserEntity?> {
        return userDao.observeUser(currentUserId)
    }

    suspend fun getCurrentUser(): UserEntity {
        var user = userDao.getUserById(currentUserId)
        if (user == null) {
            // Initialize default guest user or current user
            val isGuest = currentUserId == "guest"
            user = UserEntity(
                id = currentUserId,
                name = if (isGuest) "Mithel Hourn" else "User $currentUserId",
                passwordHash = "",
                isGuest = isGuest
            )
            userDao.insertUser(user)
            seedDefaultHabits(currentUserId, withDemoHistory = isGuest)
            seedDefaultMetrics(currentUserId, withDemoHistory = isGuest)
        }
        return user
    }

    suspend fun registerUser(name: String, passwordRaw: String): Result<UserEntity> {
        if (name.isBlank()) return Result.failure(IllegalArgumentException("Name cannot be empty"))
        if (passwordRaw.length < 6) return Result.failure(IllegalArgumentException("Password must be at least 6 characters"))

        val newId = SecurityUtil.generateUniqueId()
        val existing = userDao.getUserById(newId)
        if (existing != null) {
            return registerUser(name, passwordRaw) // retry for collision
        }

        val passwordHash = SecurityUtil.hashPassword(passwordRaw)
        val user = UserEntity(
            id = newId,
            name = name,
            passwordHash = passwordHash,
            isGuest = false
        )
        userDao.insertUser(user)
        seedDefaultHabits(newId, withDemoHistory = false)
        seedDefaultMetrics(newId, withDemoHistory = false)
        sessionManager.setCurrentUserId(newId)
        return Result.success(user)
    }

    suspend fun loginUser(userId: String, passwordRaw: String): Result<UserEntity> {
        val cleanId = userId.trim().uppercase(Locale.ROOT)
        val user = userDao.getUserById(cleanId)
            ?: return Result.failure(IllegalArgumentException("No account found with ID $cleanId"))

        val hash = SecurityUtil.hashPassword(passwordRaw)
        if (user.passwordHash != hash) {
            return Result.failure(IllegalArgumentException("Incorrect password for account $cleanId"))
        }

        sessionManager.setCurrentUserId(cleanId)
        return Result.success(user)
    }

    suspend fun switchUser(userId: String) {
        sessionManager.setCurrentUserId(userId)
        getCurrentUser() // ensures seeded if missing
    }

    suspend fun continueAsGuest(): UserEntity {
        sessionManager.setCurrentUserId("guest")
        return getCurrentUser()
    }

    suspend fun changePassword(currentRaw: String, newRaw: String): Result<Unit> {
        val user = userDao.getUserById(currentUserId) ?: return Result.failure(IllegalStateException("User not found"))
        if (user.isGuest) {
            return Result.failure(IllegalArgumentException("Guest accounts do not use a password. Please create an account."))
        }
        val currentHash = SecurityUtil.hashPassword(currentRaw)
        if (user.passwordHash != currentHash) {
            return Result.failure(IllegalArgumentException("Current password does not match"))
        }
        if (newRaw.length < 6) {
            return Result.failure(IllegalArgumentException("New password must be at least 6 characters"))
        }
        val newHash = SecurityUtil.hashPassword(newRaw)
        userDao.updatePassword(currentUserId, newHash)
        return Result.success(Unit)
    }

    suspend fun updateMood(mood: Int) {
        userDao.updateMood(currentUserId, mood)
    }

    fun observeActiveHabits(): Flow<List<HabitEntity>> {
        return habitDao.getActiveHabits(currentUserId)
    }

    fun observeAllCompletions(): Flow<List<HabitCompletionEntity>> {
        return habitDao.observeAllCompletions(currentUserId)
    }

    fun observeRecentMetrics(): Flow<List<MetricLogEntity>> {
        return habitDao.observeRecentMetricLogs(currentUserId)
    }

    suspend fun toggleCompletion(habitId: Long, date: LocalDate, isCompleted: Boolean) {
        val dayIndex = date.dayOfMonth - 1
        val yearMonth = YearMonth.from(date).toString()
        if (isCompleted) {
            habitDao.setCompletion(
                HabitCompletionEntity(
                    habitId = habitId,
                    userId = currentUserId,
                    dayIndex = dayIndex,
                    yearMonth = yearMonth,
                    completed = true
                )
            )
        } else {
            habitDao.removeCompletion(habitId, dayIndex, yearMonth, currentUserId)
        }
    }

    suspend fun addHabit(name: String, emoji: String, category: String, durationMinutes: Int, goalDays: Int): Long {
        val habit = HabitEntity(
            userId = currentUserId,
            name = name.trim(),
            emoji = if (emoji.isBlank()) "🎯" else emoji,
            category = if (category.isBlank()) "Routine" else category,
            durationMinutes = if (durationMinutes <= 0) 20 else durationMinutes,
            goalDays = if (goalDays <= 0) 31 else goalDays
        )
        return habitDao.insertHabit(habit)
    }

    suspend fun updateHabit(habit: HabitEntity) {
        habitDao.updateHabit(habit)
    }

    suspend fun deleteHabit(habitId: Long) {
        habitDao.deleteHabit(habitId, currentUserId)
    }

    private suspend fun todayLog(): MetricLogEntity {
        val todayStr = getTodayDateStr()
        return habitDao.getMetricLog(currentUserId, todayStr)
            ?: MetricLogEntity(userId = currentUserId, dateStr = todayStr)
    }

    suspend fun updateWaterGlasses(delta: Int) {
        val current = todayLog()
        habitDao.insertOrUpdateMetricLog(current.copy(waterGlasses = (current.waterGlasses + delta).coerceIn(0, 30)))
    }

    suspend fun updateSteps(delta: Int) {
        val current = todayLog()
        habitDao.insertOrUpdateMetricLog(current.copy(steps = (current.steps + delta).coerceIn(0, 100_000)))
    }

    suspend fun updateSleepHours(delta: Float) {
        val current = todayLog()
        val v = (current.sleepHours + delta).coerceIn(0f, 24f)
        habitDao.insertOrUpdateMetricLog(current.copy(sleepHours = Math.round(v * 2f) / 2f))
    }

    suspend fun updateReadingMinutes(delta: Int) {
        val current = todayLog()
        habitDao.insertOrUpdateMetricLog(current.copy(readingMinutes = (current.readingMinutes + delta).coerceIn(0, 1440)))
    }

    suspend fun updateDailyTargets(stepsTarget: Int?, waterTarget: Int?) {
        val user = userDao.getUserById(currentUserId) ?: return
        userDao.updateUser(
            user.copy(
                dailyStepsTarget = stepsTarget ?: user.dailyStepsTarget,
                dailyWaterTarget = waterTarget ?: user.dailyWaterTarget
            )
        )
    }

    suspend fun exportUserDataJson(): String {
        val user = userDao.getUserById(currentUserId)
        val habits = habitDao.getActiveHabitsSync(currentUserId)
        val completions = habitDao.getAllCompletionsSync(currentUserId)
        val metrics = habitDao.getRecentMetricLogs(currentUserId)

        val root = JSONObject()
        root.put("userId", user?.id)
        root.put("userName", user?.name)
        root.put("exportedAt", System.currentTimeMillis())

        val habitsArray = JSONArray()
        habits.forEach { h ->
            val obj = JSONObject()
            obj.put("id", h.id)
            obj.put("name", h.name)
            obj.put("emoji", h.emoji)
            obj.put("category", h.category)
            obj.put("goalDays", h.goalDays)
            obj.put("durationMinutes", h.durationMinutes)
            habitsArray.put(obj)
        }
        root.put("habits", habitsArray)

        val compArray = JSONArray()
        completions.forEach { c ->
            val obj = JSONObject()
            obj.put("habitId", c.habitId)
            obj.put("dayIndex", c.dayIndex)
            obj.put("yearMonth", c.yearMonth)
            obj.put("completed", c.completed)
            compArray.put(obj)
        }
        root.put("completions", compArray)

        val metricsArray = JSONArray()
        metrics.forEach { m ->
            val obj = JSONObject()
            obj.put("date", m.dateStr)
            obj.put("steps", m.steps)
            obj.put("waterGlasses", m.waterGlasses)
            obj.put("sleepHours", m.sleepHours.toDouble())
            obj.put("readingMinutes", m.readingMinutes)
            metricsArray.put(obj)
        }
        root.put("metricLogs", metricsArray)

        return root.toString(2)
    }

    suspend fun deleteAccount(): Result<Unit> {
        val uid = currentUserId
        habitDao.deleteMetricLogs(uid)
        userDao.deleteUser(uid) // habits + completions are removed by ON DELETE CASCADE
        sessionManager.setCurrentUserId("guest")
        getCurrentUser() // make sure the (fresh) guest profile exists again
        return Result.success(Unit)
    }

    /**
     * Creates the starter habits. The built-in guest profile additionally gets a little demo
     * history (so charts are not empty on first launch); real accounts start with a clean slate.
     */
    private suspend fun seedDefaultHabits(userId: String, withDemoHistory: Boolean) {
        val existing = habitDao.getActiveHabitsSync(userId)
        if (existing.isNotEmpty()) return

        val defaults = listOf(
            HabitEntity(userId = userId, name = "Meditate and relax", emoji = "🧘", category = "Mindfulness", durationMinutes = 20),
            HabitEntity(userId = userId, name = "Drink a glass of water", emoji = "🥛", category = "Health", durationMinutes = 5),
            HabitEntity(userId = userId, name = "Go for a long walk", emoji = "🚶", category = "Fitness", durationMinutes = 30),
            HabitEntity(userId = userId, name = "Reading a Book", emoji = "📖", category = "Mind", durationMinutes = 30),
            HabitEntity(userId = userId, name = "Gym & Workout", emoji = "💪", category = "Fitness", durationMinutes = 45),
            HabitEntity(userId = userId, name = "Wake up at 05:00", emoji = "⏰", category = "Morning", durationMinutes = 10),
            HabitEntity(userId = userId, name = "Day Planning", emoji = "🗓️", category = "Productivity", durationMinutes = 15),
            HabitEntity(userId = userId, name = "Goal Journaling", emoji = "📓", category = "Mind", durationMinutes = 15)
        )
        habitDao.insertHabits(defaults)
        if (!withDemoHistory) return

        val habits = habitDao.getActiveHabitsSync(userId)
        val today = LocalDate.now()
        habits.forEachIndexed { index, habit ->
            val every = when (index % 4) {
                0 -> 1
                1 -> 2
                2 -> 3
                else -> 1
            }
            // last 45 days of history (never the future, and today is left for the user to tick)
            for (back in 1..45) {
                if ((back + index) % every != 0) continue
                if ((back * 7 + index * 3) % 11 == 0) continue // a few natural gaps
                val date = today.minusDays(back.toLong())
                habitDao.setCompletion(
                    HabitCompletionEntity(
                        habitId = habit.id,
                        userId = userId,
                        dayIndex = date.dayOfMonth - 1,
                        yearMonth = YearMonth.from(date).toString(),
                        completed = true
                    )
                )
            }
        }
        // demo metric history for the last week
        for (back in 1..7) {
            val date = today.minusDays(back.toLong())
            habitDao.insertOrUpdateMetricLog(
                MetricLogEntity(
                    userId = userId,
                    dateStr = date.toString(),
                    steps = 900 + ((back * 370) % 1500),
                    waterGlasses = 5 + (back * 3) % 7,
                    sleepHours = 6f + (back % 4) * 0.5f,
                    exerciseMinutes = 20 + (back * 5) % 40,
                    readingMinutes = 20 + (back * 10) % 60
                )
            )
        }
    }

    private suspend fun seedDefaultMetrics(userId: String, withDemoHistory: Boolean) {
        if (!withDemoHistory) return
        val todayStr = getTodayDateStr()
        if (habitDao.getMetricLog(userId, todayStr) == null) {
            habitDao.insertOrUpdateMetricLog(
                MetricLogEntity(
                    userId = userId,
                    dateStr = todayStr,
                    steps = 1600,
                    waterGlasses = 8,
                    sleepHours = 7f,
                    exerciseMinutes = 30,
                    readingMinutes = 240
                )
            )
        }
    }

    fun getTodayDateStr(): String = LocalDate.now().toString()

    fun getCurrentYearMonth(): String = YearMonth.now().toString()
}
