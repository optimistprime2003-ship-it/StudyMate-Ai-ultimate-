package com.example.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_documents")
data class StudyDocument(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val fileType: String, // PDF, DOCX, PPTX, TXT, IMAGE
    val fullText: String,
    val summaryShort: String = "",
    val summaryMedium: String = "",
    val summaryDetailed: String = "",
    val summaryExamRevision: String = "",
    val summaryBulletPoints: String = "",
    val filePath: String? = null,
    val readingProgressPage: Int = 0,
    val readingProgressScrollOffset: Int = 0,
    val dateAdded: Long = System.currentTimeMillis()
)

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val documentId: Int,
    val question: String,
    val answer: String,
    val isMastered: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "quiz_questions")
data class QuizQuestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val documentId: Int,
    val questionText: String,
    val optionsString: String, // Double-colon separated "Option A::Option B::Option C::Option D"
    val correctAnswer: String,
    val userAnswer: String? = null,
    val isCorrect: Boolean? = null,
    val questionType: String = "MCQ" // MCQ, TF
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val documentId: Int,
    val role: String, // user, assistant
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
