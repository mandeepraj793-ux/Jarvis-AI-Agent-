package com.example.domain.model

enum class JarvisActionType {
    OPEN_APP,
    SEARCH_WEB,
    SET_ALARM,
    CREATE_NOTE,
    LIST_NOTES,
    OPEN_SETTINGS,
    MAKE_CALL_PROMPT,
    SEND_MESSAGE_PROMPT,
    SPEAK_ONLY,
    UNKNOWN;

    companion object {
        fun fromString(value: String?): JarvisActionType {
            return entries.firstOrNull { it.name.equals(value?.trim(), ignoreCase = true) } ?: UNKNOWN
        }
    }
}

data class ActionProposal(
    val type: JarvisActionType,
    val target: String = "",
    val arguments: Map<String, String> = emptyMap(),
    val explanation: String = "",
    val requiresConfirmation: Boolean = false
)

data class ExecutionResult(
    val success: Boolean,
    val message: String,
    val requiresUserAction: Boolean = false
)
