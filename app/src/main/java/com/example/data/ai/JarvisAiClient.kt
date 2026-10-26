package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import com.example.domain.model.ActionProposal
import com.example.domain.model.ChatMessage
import com.example.domain.model.JarvisActionType
import com.example.domain.model.MessageSender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class JarvisAiResult(
    val spokenText: String,
    val actionProposal: ActionProposal? = null,
    val isOfflineResponse: Boolean = false
)

class JarvisAiClient(
    private val apiService: GeminiApiService = GeminiClientProvider.service
) {
    companion object {
        private const val TAG = "JarvisAiClient"

        private val SYSTEM_INSTRUCTION = """
            You are JARVIS AI, an ultra-fast, intelligent, polite, and secure Android assistant.
            You understand English, Hindi, and Hinglish fluently.
            Keep spoken text concise, direct, and conversational.
            
            When answering, you MUST output a JSON object with this exact structure:
            {
              "speech_response": "Short vocal response in the user's language (English/Hindi/Hinglish)",
              "action": {
                "type": "OPEN_APP | SEARCH_WEB | SET_ALARM | CREATE_NOTE | LIST_NOTES | OPEN_SETTINGS | MAKE_CALL_PROMPT | SEND_MESSAGE_PROMPT | SPEAK_ONLY",
                "target": "target app, URL query, setting name, or number",
                "arguments": {
                  "query": "search query or app name",
                  "hour": "hour in 24h format (e.g. 7)",
                  "minutes": "minutes (e.g. 0)",
                  "label": "alarm label",
                  "note_title": "title",
                  "note_content": "body of the note",
                  "phone_number": "number",
                  "message_text": "text content"
                },
                "requires_confirmation": boolean,
                "explanation": "Brief explanation of what device action will happen"
              }
            }
            
            Action Rules:
            1. If the user asks to open an app (e.g. 'Open YouTube', 'WhatsApp chalao', 'Chrome kholo', 'Open camera', 'Calculator kholo', 'Open Spotify', 'Maps kholo'), set type to 'OPEN_APP' with target 'youtube'/'whatsapp'/'chrome'/'camera'/'calculator'/'spotify'/'maps' and requires_confirmation=false.
            2. If the user asks to search something online, set type to 'SEARCH_WEB' with query in arguments.
            3. If user asks to set an alarm (e.g. 'Set alarm for 7 AM', 'Kal subah 6 baje ka alarm lagao'), set type to 'SET_ALARM', fill hour/minutes, requires_confirmation=true.
            4. If user asks to save or write a note (e.g. 'Note banao', 'Remember to buy groceries'), set type to 'CREATE_NOTE', requires_confirmation=false.
            5. If user asks for notes, set type to 'LIST_NOTES'.
            6. If user asks for settings (WiFi, Bluetooth, Apps), set type to 'OPEN_SETTINGS' with target 'wifi'/'bluetooth'/'main'.
            7. If user asks to call or message someone, set type to 'MAKE_CALL_PROMPT' or 'SEND_MESSAGE_PROMPT' with requires_confirmation=true.
            8. For general knowledge, coding, or chit-chat, set type to 'SPEAK_ONLY' and provide a helpful answer in speech_response.
        """.trimIndent()
    }

    suspend fun query(
        userInput: String,
        recentHistory: List<ChatMessage>,
        offlineOnly: Boolean = false
    ): JarvisAiResult = withContext(Dispatchers.IO) {
        // If offline mode is explicitly requested, evaluate via local intent engine
        if (offlineOnly) {
            return@withContext parseLocalRuleIntent(userInput, isOffline = true)
        }

        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API key is not configured; using on-device local intent engine")
            return@withContext parseLocalRuleIntent(userInput, isOffline = false)
        }

        // Prepare bounded history (last 4 items to save tokens)
        val contents = mutableListOf<ContentItem>()
        recentHistory.takeLast(4).forEach { msg ->
            val role = if (msg.sender == MessageSender.USER) "user" else "model"
            contents.add(
                ContentItem(
                    role = role,
                    parts = listOf(PartItem(text = msg.text))
                )
            )
        }
        contents.add(
            ContentItem(
                role = "user",
                parts = listOf(PartItem(text = userInput))
            )
        )

        val request = GeminiRequest(
            contents = contents,
            generationConfig = GenerationConfig(
                temperature = 0.3f,
                topP = 0.9f,
                responseMimeType = "application/json"
            ),
            systemInstruction = ContentItem(
                parts = listOf(PartItem(text = SYSTEM_INSTRUCTION))
            )
        )

        try {
            val response = apiService.generateContent(apiKey, request)
            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!rawJson.isNullOrBlank()) {
                parseStructuredGeminiOutput(rawJson)
            } else {
                parseLocalRuleIntent(userInput, isOffline = false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini network call failed: ${e.message}, falling back to local intent parser")
            parseLocalRuleIntent(userInput, isOffline = true)
        }
    }

    private fun parseStructuredGeminiOutput(rawText: String): JarvisAiResult {
        try {
            // Find JSON content inside possibly markdown backticks
            val cleaned = rawText
                .substringAfter("```json", rawText)
                .substringAfter("```", rawText)
                .substringBeforeLast("```", rawText)
                .trim()

            val json = JSONObject(cleaned)
            val speech = json.optString("speech_response", "Online system operational, sir.")

            val actionObj = json.optJSONObject("action")
            val proposal = if (actionObj != null) {
                val actionTypeStr = actionObj.optString("type", "SPEAK_ONLY")
                val target = actionObj.optString("target", "")
                val requiresConf = actionObj.optBoolean("requires_confirmation", false)
                val explanation = actionObj.optString("explanation", "")

                val argsMap = mutableMapOf<String, String>()
                val argsObj = actionObj.optJSONObject("arguments")
                if (argsObj != null) {
                    val keys = argsObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        argsMap[key] = argsObj.optString(key, "")
                    }
                }

                ActionProposal(
                    type = JarvisActionType.fromString(actionTypeStr),
                    target = target,
                    arguments = argsMap,
                    explanation = explanation,
                    requiresConfirmation = requiresConf
                )
            } else null

            return JarvisAiResult(
                spokenText = speech,
                actionProposal = proposal,
                isOfflineResponse = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse structured JSON: ${e.message}. Raw: $rawText")
            return JarvisAiResult(
                spokenText = rawText.take(150),
                actionProposal = null,
                isOfflineResponse = false
            )
        }
    }

    /**
     * Local deterministic intent parser supporting English, Hindi, and Hinglish offline or without network.
     */
    fun parseLocalRuleIntent(input: String, isOffline: Boolean): JarvisAiResult {
        val lower = input.lowercase().trim()

        // 1. App opening intents (Hindi/Hinglish/English)
        val appMatches = listOf(
            Pair(listOf("youtube", "yt"), "youtube"),
            Pair(listOf("whatsapp", "whats app"), "whatsapp"),
            Pair(listOf("chrome", "browser", "internet"), "chrome"),
            Pair(listOf("map", "maps", "navigation", "rasta"), "maps"),
            Pair(listOf("calculator", "calci", "hisaab"), "calculator"),
            Pair(listOf("camera", "photo", "khicho"), "camera"),
            Pair(listOf("spotify", "music", "gana", "geet"), "spotify")
        )

        for ((keywords, appTarget) in appMatches) {
            if (keywords.any { lower.contains(it) } &&
                (lower.contains("open") || lower.contains("kholo") || lower.contains("chalao") || lower.contains("launch") || lower.contains("start"))) {
                val appLabel = appTarget.replaceFirstChar { it.uppercase() }
                val speech = if (lower.contains("kholo") || lower.contains("chalao")) {
                    "$appLabel khol raha hoon, sir."
                } else {
                    "Opening $appLabel, sir."
                }
                return JarvisAiResult(
                    spokenText = speech,
                    actionProposal = ActionProposal(
                        type = JarvisActionType.OPEN_APP,
                        target = appTarget,
                        arguments = mapOf("app" to appTarget),
                        explanation = "Launch $appLabel application",
                        requiresConfirmation = false
                    ),
                    isOfflineResponse = isOffline
                )
            }
        }

        // 2. Settings intents
        if (lower.contains("wifi") || lower.contains("wi-fi")) {
            return JarvisAiResult(
                spokenText = "Opening Wi-Fi settings, sir.",
                actionProposal = ActionProposal(
                    type = JarvisActionType.OPEN_SETTINGS,
                    target = "wifi",
                    explanation = "Open device Wi-Fi configuration",
                    requiresConfirmation = false
                ),
                isOfflineResponse = isOffline
            )
        }
        if (lower.contains("bluetooth")) {
            return JarvisAiResult(
                spokenText = "Opening Bluetooth settings, sir.",
                actionProposal = ActionProposal(
                    type = JarvisActionType.OPEN_SETTINGS,
                    target = "bluetooth",
                    explanation = "Open device Bluetooth configuration",
                    requiresConfirmation = false
                ),
                isOfflineResponse = isOffline
            )
        }
        if (lower.contains("setting") || lower.contains("settings")) {
            return JarvisAiResult(
                spokenText = "Opening system settings.",
                actionProposal = ActionProposal(
                    type = JarvisActionType.OPEN_SETTINGS,
                    target = "main",
                    explanation = "Open Android Settings screen",
                    requiresConfirmation = false
                ),
                isOfflineResponse = isOffline
            )
        }

        // 3. Alarm intents
        if (lower.contains("alarm") || lower.contains("uthana") || lower.contains("wake me up")) {
            // Simple hour extraction regex
            val hourRegex = Regex("""(\d{1,2})\s*(am|pm|baje|o'clock)?""")
            val match = hourRegex.find(lower)
            var hour = "7"
            var minute = "0"
            if (match != null) {
                val num = match.groupValues[1].toIntOrNull() ?: 7
                val modifier = match.groupValues.getOrNull(2) ?: ""
                hour = if (modifier == "pm" && num < 12) (num + 12).toString() else num.toString()
            }
            val speech = if (lower.contains("baje") || lower.contains("uthana")) {
                "$hour baje ka alarm set karne ke liye confirmation chahiye, sir."
            } else {
                "Preparing alarm for $hour:00. Please confirm execution."
            }
            return JarvisAiResult(
                spokenText = speech,
                actionProposal = ActionProposal(
                    type = JarvisActionType.SET_ALARM,
                    target = "alarm",
                    arguments = mapOf("hour" to hour, "minutes" to minute, "label" to "JARVIS Alarm"),
                    explanation = "Set system alarm for $hour:${minute.padStart(2, '0')}",
                    requiresConfirmation = true
                ),
                isOfflineResponse = isOffline
            )
        }

        // 4. Notes intents
        if (lower.contains("note") || lower.contains("yaad rakhna") || lower.contains("remember")) {
            if (lower.contains("show") || lower.contains("read") || lower.contains("list") || lower.contains("dikhao")) {
                return JarvisAiResult(
                    spokenText = "Fetching your saved notes, sir.",
                    actionProposal = ActionProposal(
                        type = JarvisActionType.LIST_NOTES,
                        explanation = "Display recent saved notes",
                        requiresConfirmation = false
                    ),
                    isOfflineResponse = isOffline
                )
            } else {
                val cleanedNote = input
                    .replace(Regex("(?i)take a note of|create a note|note that|note:|yaad rakhna ki|yaad rakhna|note"), "")
                    .trim()
                val noteText = if (cleanedNote.isNotBlank()) cleanedNote else "Quick reminder from voice input"
                return JarvisAiResult(
                    spokenText = "Note recorded, sir.",
                    actionProposal = ActionProposal(
                        type = JarvisActionType.CREATE_NOTE,
                        target = "note",
                        arguments = mapOf("note_title" to "Voice Note", "note_content" to noteText),
                        explanation = "Save note: $noteText",
                        requiresConfirmation = false
                    ),
                    isOfflineResponse = isOffline
                )
            }
        }

        // 5. Search Web
        if (lower.startsWith("search") || lower.startsWith("google") || lower.contains("dhoondo")) {
            val query = input
                .replace(Regex("(?i)^search for|^search|^google|dhoondo"), "")
                .trim()
            return JarvisAiResult(
                spokenText = "Searching the web for $query.",
                actionProposal = ActionProposal(
                    type = JarvisActionType.SEARCH_WEB,
                    target = query,
                    arguments = mapOf("query" to query),
                    explanation = "Open web search for '$query'",
                    requiresConfirmation = false
                ),
                isOfflineResponse = isOffline
            )
        }

        // 6. Generic response
        val fallbackMsg = if (isOffline) {
            "Offline mode active, sir. Predefined commands like opening apps, setting alarms, or saving notes are available."
        } else {
            "I have processed your command, sir. All core Android protocols are standing by."
        }
        return JarvisAiResult(
            spokenText = fallbackMsg,
            actionProposal = null,
            isOfflineResponse = isOffline
        )
    }
}
