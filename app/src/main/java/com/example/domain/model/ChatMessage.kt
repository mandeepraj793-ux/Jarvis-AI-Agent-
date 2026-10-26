package com.example.domain.model

enum class MessageSender {
    USER,
    ASSISTANT,
    SYSTEM
}

data class ChatMessage(
    val id: Long = 0,
    val sender: MessageSender,
    val text: String,
    val actionProposal: ActionProposal? = null,
    val executionResult: ExecutionResult? = null,
    val timestamp: Long = System.currentTimeMillis()
)

data class JarvisSettings(
    val languageCode: String = "en-IN", // "en-IN", "hi-IN", "en-US"
    val ttsSpeed: Float = 1.0f,
    val ttsPitch: Float = 1.0f,
    val voiceOutputEnabled: Boolean = true,
    val requireConfirmationForSensitive: Boolean = true,
    val offlineModeOnly: Boolean = false
)
