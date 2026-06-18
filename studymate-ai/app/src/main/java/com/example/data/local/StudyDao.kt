package com.example.data.local

import androidx.room.*
import com.example.data.models.StudyDocument
import com.example.data.models.Flashcard
import com.example.data.models.QuizQuestion
import com.example.data.models.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {

    // --- Study Documents ---
    @Query("SELECT * FROM study_documents ORDER BY dateAdded DESC")
    fun getAllDocuments(): Flow<List<StudyDocument>>

    @Query("SELECT * FROM study_documents WHERE id = :id")
    suspend fun getDocumentById(id: Int): StudyDocument?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: StudyDocument): Long

    @Update
    suspend fun updateDocument(document: StudyDocument)

    @Delete
    suspend fun deleteDocument(document: StudyDocument)

    @Query("UPDATE study_documents SET readingProgressPage = :page, readingProgressScrollOffset = :offset WHERE id = :id")
    suspend fun updateProgress(id: Int, page: Int, offset: Int)


    // --- Flashcards ---
    @Query("SELECT * FROM flashcards WHERE documentId = :documentId ORDER BY timestamp ASC")
    fun getFlashcardsForDocument(documentId: Int): Flow<List<Flashcard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcards(flashcards: List<Flashcard>)

    @Update
    suspend fun updateFlashcard(flashcard: Flashcard)

    @Query("UPDATE flashcards SET isMastered = :isMastered WHERE id = :id")
    suspend fun setFlashcardMastery(id: Int, isMastered: Boolean)

    @Query("DELETE FROM flashcards WHERE documentId = :documentId")
    suspend fun deleteFlashcardsForDocument(documentId: Int)


    // --- Quiz Questions ---
    @Query("SELECT * FROM quiz_questions WHERE documentId = :documentId")
    fun getQuizQuestionsForDocument(documentId: Int): Flow<List<QuizQuestion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizQuestions(questions: List<QuizQuestion>)

    @Update
    suspend fun updateQuizQuestion(question: QuizQuestion)

    @Query("UPDATE quiz_questions SET userAnswer = NULL, isCorrect = NULL WHERE documentId = :documentId")
    suspend fun resetQuizForDocument(documentId: Int)

    @Query("DELETE FROM quiz_questions WHERE documentId = :documentId")
    suspend fun deleteQuizzesForDocument(documentId: Int)


    // --- Chats ---
    @Query("SELECT * FROM chat_messages WHERE documentId = :documentId ORDER BY timestamp ASC")
    fun getChatHistory(documentId: Int): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage): Long

    @Query("DELETE FROM chat_messages WHERE documentId = :documentId")
    suspend fun clearChatHistory(documentId: Int)
}
