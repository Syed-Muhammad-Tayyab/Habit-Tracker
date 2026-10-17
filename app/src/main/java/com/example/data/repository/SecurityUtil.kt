package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import kotlin.random.Random

object SecurityUtil {
    fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    fun generateUniqueId(): String {
        val rand = Random.nextInt(100000, 999999)
        return "HT-$rand"
    }
}

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("habit_session", Context.MODE_PRIVATE)

    fun getCurrentUserId(): String {
        return prefs.getString(KEY_USER_ID, "guest") ?: "guest"
    }

    fun setCurrentUserId(userId: String) {
        prefs.edit().putString(KEY_USER_ID, userId).apply()
    }

    fun getAppTheme(): String {
        return prefs.getString(KEY_THEME, "light") ?: "light" // "light", "black", "orange"
    }

    fun setAppTheme(theme: String) {
        prefs.edit().putString(KEY_THEME, theme).apply()
    }

    fun clearSession() {
        prefs.edit().putString(KEY_USER_ID, "guest").apply()
    }

    companion object {
        private const val KEY_USER_ID = "current_user_id"
        private const val KEY_THEME = "current_theme"
    }
}
