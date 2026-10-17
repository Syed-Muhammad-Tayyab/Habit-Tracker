package com.example.data.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiHabitCoach {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeHabitsAndCoach(
        habitSummary: String,
        userQuery: String = "",
        useHighThinking: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "AI Coach needs a Gemini API key. Create a file named .env in the project root containing GEMINI_API_KEY=your_key, then rebuild the app."
        }

        // Determine model: complex reasoning with thinking mode
        val model = if (useHighThinking) "gemini-2.5-pro" else "gemini-2.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val prompt = if (userQuery.isNotBlank()) {
            """
            You are an expert behavioural psychologist and elite habit coach.
            User's current habits and statistics:
            $habitSummary
            
            User's question or request:
            $userQuery
            
            Provide actionable, encouraging, and science-grounded habit coaching (e.g. habit stacking, cue-routine-reward loops, friction reduction). Keep the tone energetic, clear, and structured with bullet points.
            """.trimIndent()
        } else {
            """
            You are an expert behavioural psychologist and elite habit coach.
            Analyze the following user habit tracking progress and data:
            $habitSummary
            
            Provide:
            1. 🌟 Top 2 Strengths & Wins to celebrate
            2. ⚡ 2 High-leverage areas for friction reduction & habit stacking
            3. 🎯 A customized 3-step action plan for tomorrow
            4. 🔥 One memorable motivational insight
            Keep it inspiring, practical, concise, and structured.
            """.trimIndent()
        }

        try {
            val root = JSONObject()
            val contents = JSONArray()
            val contentObj = JSONObject()
            val parts = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            parts.put(partObj)
            contentObj.put("parts", parts)
            contents.put(contentObj)
            root.put("contents", contents)

            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            root.put("generationConfig", genConfig)

            val reqBody = root.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(reqBody)
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    return@withContext "AI Coach is currently unavailable (Code: ${response.code}). Please verify your GEMINI_API_KEY in the .env file."
                }
                val respJson = JSONObject(bodyStr)
                val candidates = respJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val candContent = firstCandidate.optJSONObject("content")
                    val candParts = candContent?.optJSONArray("parts")
                    if (candParts != null && candParts.length() > 0) {
                        val text = candParts.getJSONObject(0).optString("text")
                        if (text.isNotBlank()) return@withContext text
                    }
                }
                "No coaching response generated. Try refining your request."
            }
        } catch (e: Exception) {
            "Unable to connect to Gemini AI Coach: ${e.localizedMessage ?: "Network error"}"
        }
    }
}
