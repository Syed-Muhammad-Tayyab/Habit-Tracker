package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    val id: String, // e.g. "HT-849201" or "guest"
    val name: String,
    val passwordHash: String, // SHA-256 hash or empty for guest
    val isGuest: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val currentMood: Int = 3, // 1 to 5
    val dailyStepsTarget: Int = 2000,
    val dailyWaterTarget: Int = 12
)

@Entity(
    tableName = "habits",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val name: String,
    val emoji: String = "🎯",
    val category: String = "Routine",
    val goalDays: Int = 31,
    val durationMinutes: Int = 20,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "habit_completions",
    primaryKeys = ["habitId", "dayIndex", "yearMonth"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"]), Index(value = ["habitId"])]
)
data class HabitCompletionEntity(
    val habitId: Long,
    val userId: String,
    val dayIndex: Int, // 0 to 30
    val yearMonth: String, // e.g. "2026-07"
    val completed: Boolean = true,
    val completedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "metric_logs",
    primaryKeys = ["userId", "dateStr"]
)
data class MetricLogEntity(
    val userId: String,
    val dateStr: String, // e.g. "2026-07-23"
    val steps: Int = 0,
    val waterGlasses: Int = 0,
    val sleepHours: Float = 0f,
    val exerciseMinutes: Int = 0,
    val readingMinutes: Int = 0
)
