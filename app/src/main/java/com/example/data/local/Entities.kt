package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sender: String, // "USER", "ASSISTANT", "SYSTEM"
    val text: String,
    val actionType: String? = null,
    val actionTarget: String? = null,
    val actionArgsJson: String? = null,
    val actionExplanation: String? = null,
    val executionSuccess: Boolean? = null,
    val executionMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_notes")
data class UserNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis()
)
