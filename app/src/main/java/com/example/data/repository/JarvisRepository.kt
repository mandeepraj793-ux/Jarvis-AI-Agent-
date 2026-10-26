package com.example.data.repository

import com.example.data.local.ChatDao
import com.example.data.local.ChatMessageEntity
import com.example.data.local.NoteDao
import com.example.data.local.UserNoteEntity
import com.example.domain.model.ActionProposal
import com.example.domain.model.ChatMessage
import com.example.domain.model.ExecutionResult
import com.example.domain.model.JarvisActionType
import com.example.domain.model.MessageSender
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

class JarvisRepository(
    private val chatDao: ChatDao,
    private val noteDao: NoteDao
) {
    val messagesFlow: Flow<List<ChatMessage>> = chatDao.getAllMessages().map { entities ->
        entities.map { it.toDomain() }
    }

    val notesFlow: Flow<List<UserNoteEntity>> = noteDao.getAllNotes()

    suspend fun saveMessage(
        sender: MessageSender,
        text: String,
        proposal: ActionProposal? = null,
        result: ExecutionResult? = null
    ): Long {
        val argsJson = proposal?.arguments?.let { map ->
            JSONObject(map as Map<*, *>).toString()
        }
        val entity = ChatMessageEntity(
            sender = sender.name,
            text = text,
            actionType = proposal?.type?.name,
            actionTarget = proposal?.target,
            actionArgsJson = argsJson,
            actionExplanation = proposal?.explanation,
            executionSuccess = result?.success,
            executionMessage = result?.message,
            timestamp = System.currentTimeMillis()
        )
        return chatDao.insertMessage(entity)
    }

    suspend fun updateExecutionResult(messageId: Long, success: Boolean, message: String) {
        chatDao.updateExecutionResult(messageId, success, message)
    }

    suspend fun clearHistory() {
        chatDao.clearHistory()
    }

    suspend fun saveNote(title: String, content: String): Long {
        return noteDao.insertNote(UserNoteEntity(title = title, content = content))
    }

    suspend fun getRecentNotes(limit: Int = 5): List<UserNoteEntity> {
        return noteDao.getRecentNotes(limit)
    }

    suspend fun deleteNote(id: Long) {
        noteDao.deleteNote(id)
    }

    suspend fun clearAllNotes() {
        noteDao.clearAllNotes()
    }

    private fun ChatMessageEntity.toDomain(): ChatMessage {
        val proposal = if (!actionType.isNullOrBlank()) {
            val argsMap = mutableMapOf<String, String>()
            if (!actionArgsJson.isNullOrBlank()) {
                try {
                    val json = JSONObject(actionArgsJson)
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        argsMap[key] = json.optString(key, "")
                    }
                } catch (_: Exception) {}
            }
            ActionProposal(
                type = JarvisActionType.fromString(actionType),
                target = actionTarget ?: "",
                arguments = argsMap,
                explanation = actionExplanation ?: "",
                requiresConfirmation = false
            )
        } else null

        val result = if (executionSuccess != null) {
            ExecutionResult(
                success = executionSuccess,
                message = executionMessage ?: ""
            )
        } else null

        return ChatMessage(
            id = id,
            sender = try { MessageSender.valueOf(sender) } catch (_: Exception) { MessageSender.SYSTEM },
            text = text,
            actionProposal = proposal,
            executionResult = result,
            timestamp = timestamp
        )
    }
}
