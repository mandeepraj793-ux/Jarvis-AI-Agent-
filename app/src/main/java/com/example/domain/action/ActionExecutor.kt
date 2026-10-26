package com.example.domain.action

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import com.example.data.repository.JarvisRepository
import com.example.domain.model.ActionProposal
import com.example.domain.model.ExecutionResult
import com.example.domain.model.JarvisActionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ActionExecutor(
    private val context: Context,
    private val repository: JarvisRepository
) {
    companion object {
        private const val TAG = "ActionExecutor"

        // Allowlisted packages
        private val ALLOWLISTED_PACKAGES = mapOf(
            "youtube" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "chrome" to "com.android.chrome",
            "browser" to "com.android.chrome",
            "maps" to "com.google.android.apps.maps",
            "calculator" to "com.google.android.calculator",
            "spotify" to "com.spotify.music"
        )
    }

    suspend fun execute(proposal: ActionProposal): ExecutionResult = withContext(Dispatchers.Main) {
        try {
            when (proposal.type) {
                JarvisActionType.OPEN_APP -> executeOpenApp(proposal)
                JarvisActionType.SEARCH_WEB -> executeSearchWeb(proposal)
                JarvisActionType.SET_ALARM -> executeSetAlarm(proposal)
                JarvisActionType.CREATE_NOTE -> executeCreateNote(proposal)
                JarvisActionType.LIST_NOTES -> executeListNotes()
                JarvisActionType.OPEN_SETTINGS -> executeOpenSettings(proposal)
                JarvisActionType.MAKE_CALL_PROMPT -> executeMakeCallPrompt(proposal)
                JarvisActionType.SEND_MESSAGE_PROMPT -> executeSendMessagePrompt(proposal)
                JarvisActionType.SPEAK_ONLY -> ExecutionResult(
                    success = true,
                    message = "Voice response generated."
                )
                JarvisActionType.UNKNOWN -> ExecutionResult(
                    success = false,
                    message = "Action type is unrecognized or unsupported."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Action execution failed: ${e.message}", e)
            ExecutionResult(
                success = false,
                message = "Execution failed: ${e.localizedMessage ?: "System intent rejected"}"
            )
        }
    }

    private fun executeOpenApp(proposal: ActionProposal): ExecutionResult {
        val target = proposal.target.lowercase().trim()

        // Special handling for camera
        if (target == "camera") {
            val cameraIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            return if (cameraIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(cameraIntent)
                ExecutionResult(true, "Camera opened successfully.")
            } else {
                ExecutionResult(false, "No compatible camera app detected.")
            }
        }

        // Check allowlist alias or direct package name
        val packageName = ALLOWLISTED_PACKAGES[target] ?: target
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)

        return if (launchIntent != null) {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
            ExecutionResult(true, "Launched ${proposal.target.replaceFirstChar { it.uppercase() }}.")
        } else {
            // Attempt to search Play Store or browser for the app
            val searchIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=$target")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (searchIntent.resolveActivity(pm) != null) {
                context.startActivity(searchIntent)
                ExecutionResult(true, "App not installed. Navigated to store search.", requiresUserAction = true)
            } else {
                ExecutionResult(false, "App '${proposal.target}' is not installed on this device.")
            }
        }
    }

    private fun executeSearchWeb(proposal: ActionProposal): ExecutionResult {
        val query = proposal.arguments["query"] ?: proposal.target
        if (query.isBlank()) {
            return ExecutionResult(false, "Search query was empty.")
        }

        // Use standard Web Search intent
        val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra(SearchManager.QUERY, query)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return if (searchIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(searchIntent)
            ExecutionResult(true, "Web search dispatched for '$query'.")
        } else {
            // Fallback to browser URL
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(browserIntent)
            ExecutionResult(true, "Web search opened in browser.")
        }
    }

    private fun executeSetAlarm(proposal: ActionProposal): ExecutionResult {
        val hour = proposal.arguments["hour"]?.toIntOrNull() ?: 7
        val minutes = proposal.arguments["minutes"]?.toIntOrNull() ?: 0
        val label = proposal.arguments["label"] ?: "JARVIS Alarm"

        val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour.coerceIn(0, 23))
            putExtra(AlarmClock.EXTRA_MINUTES, minutes.coerceIn(0, 59))
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false) // Open clock app so user sees the confirmation
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return if (alarmIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(alarmIntent)
            ExecutionResult(true, "Alarm set for ${hour.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')} ($label).")
        } else {
            ExecutionResult(false, "No alarm application found on device.")
        }
    }

    private suspend fun executeCreateNote(proposal: ActionProposal): ExecutionResult {
        val title = proposal.arguments["note_title"] ?: "Voice Note"
        val content = proposal.arguments["note_content"] ?: proposal.target
        if (content.isBlank()) {
            return ExecutionResult(false, "Note content cannot be empty.")
        }

        val noteId = repository.saveNote(title = title, content = content)
        return ExecutionResult(
            success = noteId > 0,
            message = "Note saved locally (ID: $noteId)."
        )
    }

    private suspend fun executeListNotes(): ExecutionResult {
        val notes = repository.getRecentNotes(3)
        return if (notes.isEmpty()) {
            ExecutionResult(true, "No notes found in local storage.")
        } else {
            val summary = notes.joinToString("; ") { "${it.title}: ${it.content}" }
            ExecutionResult(true, "Found ${notes.size} recent notes: $summary")
        }
    }

    private fun executeOpenSettings(proposal: ActionProposal): ExecutionResult {
        val target = proposal.target.lowercase().trim()
        val intent = when (target) {
            "wifi", "wi-fi" -> Intent(Settings.ACTION_WIFI_SETTINGS)
            "bluetooth" -> Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            "apps", "applications" -> Intent(Settings.ACTION_APPLICATION_SETTINGS)
            else -> Intent(Settings.ACTION_SETTINGS)
        }.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        context.startActivity(intent)
        return ExecutionResult(true, "Opened ${target.ifBlank { "system" }} settings.")
    }

    private fun executeMakeCallPrompt(proposal: ActionProposal): ExecutionResult {
        val number = proposal.arguments["phone_number"] ?: proposal.target
        val sanitized = number.filter { it.isDigit() || it == '+' }
        if (sanitized.isBlank()) {
            return ExecutionResult(false, "Invalid phone number provided.")
        }

        // Safe: ACTION_DIAL opens dialer pad for user to review and press call, never silent calling
        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitized")).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(dialIntent)
        return ExecutionResult(true, "Dialer opened for $sanitized.")
    }

    private fun executeSendMessagePrompt(proposal: ActionProposal): ExecutionResult {
        val text = proposal.arguments["message_text"] ?: proposal.target
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val chooser = Intent.createChooser(shareIntent, "Send message via").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(chooser)
        return ExecutionResult(true, "Share chooser opened.")
    }
}
