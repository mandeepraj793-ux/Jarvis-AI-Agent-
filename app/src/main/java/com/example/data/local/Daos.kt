package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("UPDATE chat_messages SET executionSuccess = :success, executionMessage = :message WHERE id = :id")
    suspend fun updateExecutionResult(id: Long, success: Boolean, message: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM user_notes ORDER BY createdAt DESC")
    fun getAllNotes(): Flow<List<UserNoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: UserNoteEntity): Long

    @Query("SELECT * FROM user_notes ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentNotes(limit: Int): List<UserNoteEntity>

    @Query("DELETE FROM user_notes WHERE id = :id")
    suspend fun deleteNote(id: Long)

    @Query("DELETE FROM user_notes")
    suspend fun clearAllNotes()
}
