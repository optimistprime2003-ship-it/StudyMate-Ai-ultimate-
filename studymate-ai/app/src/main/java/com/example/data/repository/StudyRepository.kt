package com.example.data.repository

import com.example.data.local.StudyDao
import com.example.data.models.StudyDocument
import com.example.data.models.Flashcard
import com.example.data.models.QuizQuestion
import com.example.data.models.ChatMessage
import com.example.data.nlp.NlpEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StudyRepository(private val studyDao: StudyDao) {

    val allDocuments: Flow<List<StudyDocument>> = studyDao.getAllDocuments()

    fun getFlashcards(documentId: Int): Flow<List<Flashcard>> =
        studyDao.getFlashcardsForDocument(documentId)

    fun getQuizQuestions(documentId: Int): Flow<List<QuizQuestion>> =
        studyDao.getQuizQuestionsForDocument(documentId)

    fun getChatHistory(documentId: Int): Flow<List<ChatMessage>> =
        studyDao.getChatHistory(documentId)

    suspend fun getDocumentById(id: Int): StudyDocument? = withContext(Dispatchers.IO) {
        studyDao.getDocumentById(id)
    }

    /**
     * Imports a document, automatically performs fully local NLP engine scans to derive key facts,
     * summaries, definitions, exam prep cheatsheets, ready-to-test flashcards, and quizzes!
     */
    suspend fun importDocument(title: String, fileType: String, text: String): Int = withContext(Dispatchers.IO) {
        val analysis = NlpEngine.analyzeDocument(title, text)

        val newDoc = StudyDocument(
            title = title,
            fileType = fileType,
            fullText = text,
            summaryShort = analysis.summaryShort,
            summaryMedium = analysis.summaryMedium,
            summaryDetailed = analysis.summaryDetailed,
            summaryExamRevision = analysis.summaryExamRevision,
            summaryBulletPoints = analysis.summaryBulletPoints
        )

        val docId = studyDao.insertDocument(newDoc).toInt()

        // Prepare flashcards with foreign documentId
        val flashcardsToSave = analysis.flashcards.map { it.copy(documentId = docId) }
        studyDao.insertFlashcards(flashcardsToSave)

        // Prepare quizzes with foreign documentId
        val quizzesToSave = analysis.quizzes.map { it.copy(documentId = docId) }
        studyDao.insertQuizQuestions(quizzesToSave)

        // Insert initial system tutor welcome message for Chat mode
        val welcomeMsg = ChatMessage(
            documentId = docId,
            role = "assistant",
            text = "Welcome to your offline AI study assistant! 🎓 I've indexed **\"$title\"** completely offline.\n\n" +
                    "Ask me questions like:\n" +
                    "• *\"What are the key points?\"*\n" +
                    "• *\"Give me exam questions.\"*\n" +
                    "• *\"Explain this chapter like I am 10.\"*",
            timestamp = System.currentTimeMillis()
        )
        studyDao.insertChatMessage(welcomeMsg)

        docId
    }

    suspend fun addChatMessage(documentId: Int, role: String, text: String) = withContext(Dispatchers.IO) {
        val userMsg = ChatMessage(documentId = documentId, role = role, text = text)
        studyDao.insertChatMessage(userMsg)
    }

    suspend fun updateReadingProgress(documentId: Int, page: Int, offset: Int) = withContext(Dispatchers.IO) {
        studyDao.updateProgress(documentId, page, offset)
    }

    suspend fun setFlashcardMastery(cardId: Int, isMastered: Boolean) = withContext(Dispatchers.IO) {
        studyDao.setFlashcardMastery(cardId, isMastered)
    }

    suspend fun answerQuizQuestion(question: QuizQuestion) = withContext(Dispatchers.IO) {
        studyDao.updateQuizQuestion(question)
    }

    suspend fun resetQuiz(documentId: Int) = withContext(Dispatchers.IO) {
        studyDao.resetQuizForDocument(documentId)
    }

    suspend fun deleteDocument(document: StudyDocument) = withContext(Dispatchers.IO) {
        studyDao.deleteDocument(document)
        studyDao.deleteFlashcardsForDocument(document.id)
        studyDao.deleteQuizzesForDocument(document.id)
        studyDao.clearChatHistory(document.id)
    }

    suspend fun clearChatHistory(documentId: Int) = withContext(Dispatchers.IO) {
        studyDao.clearChatHistory(documentId)
        val doc = studyDao.getDocumentById(documentId)
        if (doc != null) {
            val welcomeMsg = ChatMessage(
                documentId = documentId,
                role = "assistant",
                text = "Chat history has been reset completely. Ask me any question about **\"${doc.title}\"** offline!",
                timestamp = System.currentTimeMillis()
            )
            studyDao.insertChatMessage(welcomeMsg)
        }
    }
}
