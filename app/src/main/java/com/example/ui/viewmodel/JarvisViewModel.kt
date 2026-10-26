package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.JarvisAiClient
import com.example.data.local.JarvisDatabase
import com.example.data.repository.JarvisRepository
import com.example.domain.action.ActionExecutor
import com.example.domain.model.ActionProposal
import com.example.domain.model.ChatMessage
import com.example.domain.model.ExecutionResult
import com.example.domain.model.JarvisSettings
import com.example.domain.model.MessageSender
import com.example.domain.voice.TextToSpeechManager
import com.example.domain.voice.VoiceInputManager
import com.example.domain.voice.VoiceRecognitionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JarvisUiState(
    val isThinking: Boolean = false,
    val pendingConfirmationProposal: ActionProposal? = null,
    val pendingMessageId: Long? = null,
    val statusBanner: String? = null,
    val settings: JarvisSettings = JarvisSettings(),
    val isMicAvailable: Boolean = true
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val database = JarvisDatabase.getInstance(application)
    private val repository = JarvisRepository(database.chatDao(), database.noteDao())
    private val actionExecutor = ActionExecutor(application, repository)
    private val aiClient = JarvisAiClient()

    val voiceInputManager = VoiceInputManager(application)
    val ttsManager = TextToSpeechManager(application)

    val speechState: StateFlow<VoiceRecognitionState> = voiceInputManager.state
    val rmsAmplitude: StateFlow<Float> = voiceInputManager.rmsAmplitude
    val isSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking

    val conversationHistory: StateFlow<List<ChatMessage>> = repository.messagesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(
        JarvisUiState(isMicAvailable = voiceInputManager.isAvailable())
    )
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

    init {
        // Initial welcome message if conversation is empty
        viewModelScope.launch {
            repository.messagesFlow.collect { list ->
                if (list.isEmpty()) {
                    repository.saveMessage(
                        sender = MessageSender.ASSISTANT,
                        text = "JARVIS operational. Ready for voice or text commands in English, Hindi, and Hinglish. How may I assist you today, sir?"
                    )
                }
            }
        }
    }

    fun startListening() {
        ttsManager.stop() // Silence TTS before listening
        val lang = _uiState.value.settings.languageCode
        voiceInputManager.startListening(languageCode = lang) { spokenText ->
            processUserQuery(spokenText)
        }
    }

    fun stopListening() {
        voiceInputManager.stopListening()
    }

    fun onUserTextSubmit(text: String) {
        if (text.isBlank()) return
        ttsManager.stop()
        voiceInputManager.stopListening()
        processUserQuery(text)
    }

    private fun processUserQuery(query: String) {
        viewModelScope.launch {
            // 1. Save user message to database
            repository.saveMessage(
                sender = MessageSender.USER,
                text = query
            )

            _uiState.update { it.copy(isThinking = true, statusBanner = "Analyzing query...") }

            // 2. Query AI Brain or local engine
            val recent = conversationHistory.value
            val isOffline = _uiState.value.settings.offlineModeOnly
            val aiResult = aiClient.query(query, recent, offlineOnly = isOffline)

            _uiState.update { it.copy(isThinking = false, statusBanner = null) }

            // 3. Play TTS if enabled
            if (_uiState.value.settings.voiceOutputEnabled && aiResult.spokenText.isNotBlank()) {
                ttsManager.speak(aiResult.spokenText)
            }

            // 4. Handle proposed action
            val proposal = aiResult.actionProposal
            if (proposal != null) {
                val shouldConfirm = proposal.requiresConfirmation ||
                        (_uiState.value.settings.requireConfirmationForSensitive && isActionSensitive(proposal))

                if (shouldConfirm) {
                    // Save assistant message awaiting confirmation
                    val msgId = repository.saveMessage(
                        sender = MessageSender.ASSISTANT,
                        text = aiResult.spokenText,
                        proposal = proposal
                    )
                    _uiState.update {
                        it.copy(
                            pendingConfirmationProposal = proposal,
                            pendingMessageId = msgId
                        )
                    }
                } else {
                    // Auto-execute safe action
                    val result = actionExecutor.execute(proposal)
                    repository.saveMessage(
                        sender = MessageSender.ASSISTANT,
                        text = aiResult.spokenText,
                        proposal = proposal,
                        result = result
                    )
                }
            } else {
                repository.saveMessage(
                    sender = MessageSender.ASSISTANT,
                    text = aiResult.spokenText
                )
            }
        }
    }

    private fun isActionSensitive(proposal: ActionProposal): Boolean {
        return when (proposal.type) {
            com.example.domain.model.JarvisActionType.SET_ALARM,
            com.example.domain.model.JarvisActionType.MAKE_CALL_PROMPT,
            com.example.domain.model.JarvisActionType.SEND_MESSAGE_PROMPT -> true
            else -> false
        }
    }

    fun confirmPendingAction() {
        val proposal = _uiState.value.pendingConfirmationProposal ?: return
        val msgId = _uiState.value.pendingMessageId

        viewModelScope.launch {
            val result = actionExecutor.execute(proposal)
            if (msgId != null) {
                repository.updateExecutionResult(msgId, result.success, result.message)
            }
            _uiState.update { it.copy(pendingConfirmationProposal = null, pendingMessageId = null) }
        }
    }

    fun cancelPendingAction() {
        val msgId = _uiState.value.pendingMessageId
        viewModelScope.launch {
            if (msgId != null) {
                repository.updateExecutionResult(msgId, false, "Action cancelled by user.")
            }
            _uiState.update { it.copy(pendingConfirmationProposal = null, pendingMessageId = null) }
        }
    }

    fun speakMessage(text: String) {
        ttsManager.speak(text)
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    fun updateSettings(newSettings: JarvisSettings) {
        _uiState.update { it.copy(settings = newSettings) }
        ttsManager.setLanguage(newSettings.languageCode)
        ttsManager.setPitch(newSettings.ttsPitch)
        ttsManager.setSpeechRate(newSettings.ttsSpeed)
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            ttsManager.stop()
            _uiState.update { it.copy(pendingConfirmationProposal = null, pendingMessageId = null) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceInputManager.stopListening()
        ttsManager.shutdown()
    }
}
