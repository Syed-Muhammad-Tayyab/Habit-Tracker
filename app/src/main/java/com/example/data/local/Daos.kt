package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun observeUser(userId: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("UPDATE users SET passwordHash = :newHash WHERE id = :userId")
    suspend fun updatePassword(userId: String, newHash: String)

    @Query("UPDATE users SET currentMood = :mood WHERE id = :userId")
    suspend fun updateMood(userId: String, mood: Int)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: String)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE userId = :userId AND isArchived = 0 ORDER BY id ASC")
    fun getActiveHabits(userId: String): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE userId = :userId AND isArchived = 0 ORDER BY id ASC")
    suspend fun getActiveHabitsSync(userId: String): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :habitId AND userId = :userId LIMIT 1")
    suspend fun getHabitById(habitId: Long, userId: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabits(habits: List<HabitEntity>)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("UPDATE habits SET isArchived = 1 WHERE id = :habitId AND userId = :userId")
    suspend fun archiveHabit(habitId: Long, userId: String)

    @Query("DELETE FROM habits WHERE id = :habitId AND userId = :userId")
    suspend fun deleteHabit(habitId: Long, userId: String)

    @Query("SELECT * FROM habit_completions WHERE userId = :userId")
    fun observeAllCompletions(userId: String): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE userId = :userId")
    suspend fun getAllCompletionsSync(userId: String): List<HabitCompletionEntity>

    @Query("SELECT * FROM metric_logs WHERE userId = :userId ORDER BY dateStr DESC LIMIT 60")
    fun observeRecentMetricLogs(userId: String): Flow<List<MetricLogEntity>>

    @Query("DELETE FROM metric_logs WHERE userId = :userId")
    suspend fun deleteMetricLogs(userId: String)

    @Query("SELECT * FROM habit_completions WHERE userId = :userId AND yearMonth = :yearMonth")
    fun getCompletionsForMonth(userId: String, yearMonth: String): Flow<List<HabitCompletionEntity>>

    @Query("SELECT * FROM habit_completions WHERE userId = :userId AND yearMonth = :yearMonth")
    suspend fun getCompletionsForMonthSync(userId: String, yearMonth: String): List<HabitCompletionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setCompletion(completion: HabitCompletionEntity)

    @Query("DELETE FROM habit_completions WHERE habitId = :habitId AND dayIndex = :dayIndex AND yearMonth = :yearMonth AND userId = :userId")
    suspend fun removeCompletion(habitId: Long, dayIndex: Int, yearMonth: String, userId: String)

    @Query("SELECT * FROM metric_logs WHERE userId = :userId AND dateStr = :dateStr LIMIT 1")
    fun observeMetricLog(userId: String, dateStr: String): Flow<MetricLogEntity?>

    @Query("SELECT * FROM metric_logs WHERE userId = :userId AND dateStr = :dateStr LIMIT 1")
    suspend fun getMetricLog(userId: String, dateStr: String): MetricLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMetricLog(log: MetricLogEntity)

    @Query("SELECT * FROM metric_logs WHERE userId = :userId ORDER BY dateStr DESC LIMIT 30")
    suspend fun getRecentMetricLogs(userId: String): List<MetricLogEntity>
}
