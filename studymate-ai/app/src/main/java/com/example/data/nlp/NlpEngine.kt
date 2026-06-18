package com.example.data.nlp

import com.example.data.models.Flashcard
import com.example.data.models.QuizQuestion
import java.util.Locale
import kotlin.math.ln
import kotlin.math.sqrt

object NlpEngine {

    /**
     * Extracts key points/concepts based on term frequency and sentence analysis.
     */
    fun analyzeDocument(title: String, text: String): AnalysisResult {
        val paragraphs = text.split("\n")
            .map { it.trim() }
            .filter { it.length > 20 }

        if (paragraphs.isEmpty()) {
            return generateEmptyResult(title)
        }

        // 1. Term frequency of important words
        val stopWords = setOf(
            "the", "a", "an", "and", "or", "but", "if", "then", "else", "when", 
            "at", "by", "for", "with", "about", "against", "between", "into", 
            "through", "during", "before", "after", "above", "below", "to", "from", 
            "up", "down", "in", "out", "on", "off", "over", "under", "again", 
            "further", "then", "once", "here", "there", "all", "any", "both", 
            "each", "few", "more", "most", "other", "some", "such", "no", "nor", 
            "not", "only", "own", "same", "so", "than", "too", "very", "s", "t", 
            "can", "will", "just", "don", "should", "now", "of", "is", "this", "that", 
            "it", "its", "their", "them", "are", "was", "were", "be", "been", "have", "has", "had"
        )

        val words = text.lowercase(Locale.ROOT)
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 3 && it !in stopWords }

        val wordCounts = words.groupingBy { it }.eachCount()
        val sortedWords = wordCounts.entries.sortedByDescending { it.value }
        
        // Extract top keywords/concepts
        val keyConcepts = sortedWords.take(8).map { it.key.replaceFirstChar { char -> char.uppercase() } }

        // 2. Extract equations & formulas
        val formulaRegex = Regex("([A-Za-z0-9_-]+[\\s]*=[\\s]*[A-Za-z0-9_+\\-*/\\s()]+|[a-zA-Z]+²|[E|M|C|v|F|a|m|p|t|V|A|B|R]\\s*=\\s*[a-zA-Z0-9\\s+\\-*/]+)")
        val foundFormulas = mutableSetOf<String>()
        text.split("\n", ".").forEach { line ->
            if (formulaRegex.containsMatchIn(line)) {
                val match = formulaRegex.find(line)?.value?.trim()
                if (match != null && match.length in 5..35 && match.contains("=")) {
                    foundFormulas.add(match)
                }
            }
        }
        val importantFormulas = foundFormulas.take(6).toList().ifEmpty {
            listOf("Focus Formula: Main Idea = (Keywords * Detail) + Structured Summary")
        }

        // 3. Define summaries of different levels
        val selectedKeySentences = extractKeySentences(paragraphs, wordCounts, 5)
        
        val summaryShort = "This document revolves around ${keyConcepts.take(3).joinToString(", ")}. " +
                "It covers fundamental structures and discussions relating to ${keyConcepts.drop(3).take(3).joinToString(", ")}. " +
                "Key takeaway: ${selectedKeySentences.firstOrNull() ?: "Study of academic materials and key concepts."}"

        val summaryMedium = "### Executive Overview\n\n" +
                "Analysis of \"$title\" reveals a central emphasis on **${keyConcepts.firstOrNull() ?: "Main Subjects"}**.\n\n" +
                "### Primary Themes\n" +
                selectedKeySentences.mapIndexed { index, s -> " - **Theme ${index + 1}**: $s" }.joinToString("\n") +
                "\n\n### Core Terminologies\n" +
                keyConcepts.take(5).map { "- **$it**: An essential analytical factor discussed thoroughly throughout this resource." }.joinToString("\n")

        val summaryDetailed = "### Comprehensive Academic Breakdown: $title\n\n" +
                "This resource is structured into layered thematic structures exploring **${keyConcepts.joinToString(", ")}**.\n\n" +
                "#### In-Depth Analytical Readings\n" +
                paragraphs.take(4).mapIndexed { idx, p -> "**Section ${idx + 1}**: $p" }.joinToString("\n\n") +
                "\n\n#### Verified Scientific Conclusions\n" +
                "- Heavily discussed properties and dynamics of ${keyConcepts.firstOrNull()}.\n" +
                "- Methodologies to review, practice and analyze ${keyConcepts.getOrNull(1)} in multiple contexts.\n" +
                "- Scientific correlations between ${keyConcepts.getOrNull(2)} and related metrics."

        val summaryExamRevision = "### ⚡ EXAM FAST-REVISION CHEATSHEET ⚡\n\n" +
                "#### 🎯 High-Yield Exam Topics\n" +
                keyConcepts.take(4).mapIndexed { idx, item -> "**Q${idx+1} Focus**: Detailed mechanics, processes, and applications of **$item**." }.joinToString("\n") +
                "\n\n#### 📌 Formulas & Key Postulates to Memorize\n" +
                importantFormulas.map { "- `$it`" }.joinToString("\n") +
                "\n\n#### ⏰ Quick Recall Facts\n" +
                selectedKeySentences.take(3).map { "• **Fact**: $it" }.joinToString("\n")

        val summaryBulletPoints = "### Key Takeaway Highlights\n\n" +
                selectedKeySentences.map { "• $it" }.joinToString("\n")

        // 4. Generate local Flashcards
        val flashcards = mutableListOf<Flashcard>()
        keyConcepts.forEachIndexed { index, concept ->
            val contextSentence = paragraphs.firstOrNull { it.contains(concept, ignoreCase = true) }
            val answer = if (contextSentence != null) {
                if (contextSentence.length > 120) contextSentence.take(120) + "..." else contextSentence
            } else {
                "The academic study, definitions, and application principles of $concept discussed in the materials."
            }
            flashcards.add(
                Flashcard(
                    documentId = 0,
                    question = "Explain the fundamental significance of '$concept' as presented in this chapter.",
                    answer = answer
                )
            )
            flashcards.add(
                Flashcard(
                    documentId = 0,
                    question = "What are the primary characteristics and applications associated with '$concept'?",
                    answer = "It is identified as a crucial pillar in this chapter, involving: ${keyConcepts.filter { it != concept }.take(3).joinToString(", ")}."
                )
            )
        }

        // 5. Generate practice quizzes (MCQs and True/False)
        val quizzes = mutableListOf<QuizQuestion>()
        keyConcepts.take(4).forEachIndexed { idx, concept ->
            // MCQ
            val correctAns = "A primary subject with high frequency, critical to explaining the chapter's thesis."
            val optB = "A minor introductory placeholder with negligible impact on the overall lesson."
            val optC = "An archaic theory that has been thoroughly disproved by modern methodologies."
            val optD = "A purely decorative formatting style with zero educational or conceptual meaning."
            val options = listOf(correctAns, optB, optC, optD).shuffled()
            val correctIdx = options.indexOf(correctAns)
            val letters = listOf("A", "B", "C", "D")
            val formattedOptions = options.mapIndexed { oIdx, text -> "${letters[oIdx]}. $text" }.joinToString("::")
            
            quizzes.add(
                QuizQuestion(
                    documentId = 0,
                    questionText = "In the context of this study material, what is the core role of '$concept'?",
                    optionsString = formattedOptions,
                    correctAnswer = "${letters[correctIdx]}. $correctAns",
                    questionType = "MCQ"
                )
            )

            // True / False
            val tfQuestion = "True or False: According to the document, '$concept' represents an essential and frequently discussed analytical pillar in these notes?"
            quizzes.add(
                QuizQuestion(
                    documentId = 0,
                    questionText = tfQuestion,
                    optionsString = "A. True::B. False",
                    correctAnswer = "A. True",
                    questionType = "TF"
                )
            )
        }

        return AnalysisResult(
            keyConcepts = keyConcepts,
            summaryShort = summaryShort,
            summaryMedium = summaryMedium,
            summaryDetailed = summaryDetailed,
            summaryExamRevision = summaryExamRevision,
            summaryBulletPoints = summaryBulletPoints,
            flashcards = flashcards,
            quizzes = quizzes
        )
    }

    private fun extractKeySentences(
        paragraphs: List<String>,
        wordCounts: Map<String, Int>,
        count: Int
    ): List<String> {
        val sentences = paragraphs.flatMap { p -> p.split(Regex("(?<=[.!?])\\s+")) }
            .map { it.trim() }
            .filter { it.split(" ").size in 10..22 }

        val scoredSentences = sentences.map { sentence ->
            val sWords = sentence.lowercase()
                .replace(Regex("[^a-z0-9\\s]"), "")
                .split(" ")
            val score = sWords.sumOf { wordCounts[it] ?: 0 }
            sentence to score
        }

        return scoredSentences.sortedByDescending { it.second }
            .map { it.first }
            .distinct()
            .take(count)
    }

    private fun generateEmptyResult(title: String): AnalysisResult {
        return AnalysisResult(
            keyConcepts = listOf("General Studies"),
            summaryShort = "This document is relatively brief or contains scanned text to examine.",
            summaryMedium = "No large paragraphs detected. Try using the Advanced OCR engine to scan handwritten textbook screenshots or photos of notes offline!",
            summaryDetailed = "This empty document has no readable text. Ensure to import searchable textbooks or run Tesseract OCR on documents containing images.",
            summaryExamRevision = "Exam Note: Double check the scanning input files to generate high-yield flashcards and questions.",
            summaryBulletPoints = "• Document empty or waiting scanned OCR text extraction.",
            flashcards = listOf(
                Flashcard(0, 0, "What should you do if text isn't detected?", "You should import a clear high contrast screenshot or use the scan to OCR feature immediately!")
            ),
            quizzes = listOf(
                QuizQuestion(0, 0, "Is internet required for StudyMate AI?", "A. Yes, constantly::B. No, completely offline", "B. No, completely offline", "MCQ")
            )
        )
    }

    /**
     * Completely local search vector similarity / keyword scanner for Chat with Document!
     */
    fun answerQuestionOffline(documentText: String, query: String): ChatAnswer {
        val queryWords = query.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s]"), "")
            .split(Regex("\\s+"))
            .filter { it.length > 2 }

        if (queryWords.isEmpty() || documentText.isBlank()) {
            return ChatAnswer(
                "I am ready to help you analyze these notes! Please ask a specific question like: 'Explain this topic' or 'Summarize page 5'.",
                0.0,
                ""
            )
        }

        val sentences = documentText.split(Regex("(?<=[.!?\\n])\\s+"))
            .map { it.trim() }
            .filter { it.length > 15 }

        // Find best matching sentence or paragraph segment
        var bestMatches = sentences.map { sentence ->
            val sLower = sentence.lowercase(Locale.ROOT)
            var hits = 0
            queryWords.forEach { qWord ->
                if (sLower.contains(qWord)) {
                    hits++
                }
            }
            val score = hits.toDouble() / (queryWords.size + Math.sqrt(sentence.split(" ").size.toDouble()))
            sentence to score
        }.sortedByDescending { it.second }

        val bestMatch = bestMatches.firstOrNull()
        val score = bestMatch?.second ?: 0.0

        val reply = if (score > 0.05) {
            val matchingSectors = bestMatches.take(3).map { it.first }.joinToString("... ")
            
            val isExplainStyle = query.contains("explain", ignoreCase = true) || query.contains("why", ignoreCase = true)
            val isAgeTen = query.contains("10", ignoreCase = true) || query.contains("child", ignoreCase = true)

            when {
                isAgeTen -> {
                    "**[StudyMate On-Device AI • Explained Like You're 10]**\n\n" +
                    "Let's think of this in simple terms! Think of this concept like a puzzle piece. Here is what we found in your notes:\n\n" +
                    "\"$matchingSectors\"\n\n" +
                    "Basically, this means everything works together step-by-step, making it easy to learn and remember without any tricky jargon!"
                }
                isExplainStyle -> {
                    "**[StudyMate On-Device AI • Concept Explanation]**\n\n" +
                    "Based on your document context, here is an in-depth breakdown of your query:\n\n" +
                    "\"$matchingSectors\"\n\n" +
                    "**Key Takeaways to Understand**:\n" +
                    "- **Context Alignment**: Directly referenced across multiple paragraphs.\n" +
                    "- **Academic Context**: The study emphasizes this as a foundational rule."
                }
                else -> {
                    "**[StudyMate On-Device AI • Direct Reference Answer]**\n\n" +
                    "From your study material, I've located the following key references:\n\n" +
                    "\"$matchingSectors\"\n\n" +
                    "Let me know if you would like me to explain this in simpler terms or generate a custom quiz question based on this fact!"
                }
            }
        } else {
            // General QA Fallback using query tokens
            val keywords = queryWords.joinToString(", ") { "'$it'" }
            "**[StudyMate On-Device AI • General Reference]**\n\n" +
            "I searched the document for terms related to $keywords, but didn't find precise matching parameters.\n\n" +
            "**However, here is a general study suggestion:**\n" +
            "- Make sure you select or extract the document text in the reader first.\n" +
            "- Try asking a question using distinct keywords appearing directly on screen!\n" +
            "- For image documents, confirm you have run the **Advanced OCR** scan first to build the offline database index."
        }

        return ChatAnswer(
            text = reply,
            confidence = score,
            bestSegment = bestMatch?.first ?: ""
        )
    }
}

data class AnalysisResult(
    val keyConcepts: List<String>,
    val summaryShort: String,
    val summaryMedium: String,
    val summaryDetailed: String,
    val summaryExamRevision: String,
    val summaryBulletPoints: String,
    val flashcards: List<Flashcard>,
    val quizzes: List<QuizQuestion>
)

data class ChatAnswer(
    val text: String,
    val confidence: Double,
    val bestSegment: String
)
