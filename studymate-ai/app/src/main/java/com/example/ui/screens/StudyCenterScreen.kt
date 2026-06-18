package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.Flashcard
import com.example.data.models.QuizQuestion
import com.example.ui.viewmodels.StudyViewModel

@Composable
fun StudyCenterScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDoc by viewModel.selectedDocument.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    val quizzes by viewModel.quizzes.collectAsState()

    var studySubTab by remember { mutableStateOf(StudySubTab.Flashcards) }

    if (selectedDoc == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Unlock Study Mode",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Select a course material from the Dashboard to generate questions.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Study Modes selector row
        TabRow(
            selectedTabIndex = studySubTab.ordinal,
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clip(RoundedCornerShape(12.dp))
        ) {
            StudySubTab.values().forEach { subTab ->
                Tab(
                    selected = studySubTab == subTab,
                    onClick = { studySubTab = subTab },
                    text = { Text(subTab.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        when (studySubTab) {
            StudySubTab.Flashcards -> {
                if (flashcards.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("No flashcards compiled for this document.", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    FlashcardSection(
                        flashcards = flashcards,
                        onMasteredClick = { cardId, mastered ->
                            viewModel.setFlashcardMastery(cardId, mastered)
                        }
                    )
                }
            }

            StudySubTab.PracticeQuizzes -> {
                if (quizzes.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("No quiz topics detected. Try pasting a larger segment of study content.", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    QuizSection(
                        quizzes = quizzes,
                        onAnswerSelected = { question, option ->
                            viewModel.answerQuiz(question, option)
                        },
                        onResetQuizClick = { viewModel.resetQuiz() }
                    )
                }
            }
        }
    }
}

@Composable
fun FlashcardSection(
    flashcards: List<Flashcard>,
    onMasteredClick: (Int, Boolean) -> Unit
) {
    var currentIndex by remember { mutableStateOf(0) }
    var rotated by remember { mutableStateOf(false) }

    // Animate flipping rotation
    val rotation by animateFloatAsState(
        targetValue = if (rotated) 180f else 0f,
        animationSpec = tween(durationMillis = 400)
    )

    if (currentIndex >= flashcards.size) {
        currentIndex = 0
    }

    val card = flashcards[currentIndex]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Tracker Index
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "⚡ TAP CARD TO FLIP",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Text(
                text = "Card ${currentIndex + 1} of ${flashcards.size}",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Core Swiping Card with 3D Rotate graphics layer
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (rotated) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 8 * density
                }
                .clickable { rotated = !rotated }
                .testTag("flashcard_flip_area")
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                // If rotated over 90 degrees, render the back of the card, mirrored
                if (rotation > 90f) {
                    Column(
                        modifier = Modifier.graphicsLayer { rotationY = 180f },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "OFFLINE AI TUTOR DEFINITION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = card.answer,
                            fontSize = 18.sp,
                            lineHeight = 26.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "CONCEPT QUESTIONS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = card.question,
                            fontSize = 20.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center
                        )
                        
                        if (card.isMastered) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Badge(containerColor = MaterialTheme.colorScheme.tertiary) {
                                Text("MASTERED ✅", color = MaterialTheme.colorScheme.onTertiary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Actions: review, mastered, next / prev
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = {
                    if (currentIndex > 0) {
                        currentIndex--
                        rotated = false
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = currentIndex > 0
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Prev Card")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Previous")
            }

            IconButton(
                onClick = {
                    onMasteredClick(card.id, !card.isMastered)
                },
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        if (card.isMastered) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Mark mastered",
                    tint = if (card.isMastered) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Button(
                onClick = {
                    if (currentIndex < flashcards.size - 1) {
                        currentIndex++
                        rotated = false
                    } else {
                        // Loop around
                        currentIndex = 0
                        rotated = false
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Next")
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = "Next Card")
            }
        }
    }
}

@Composable
fun QuizSection(
    quizzes: List<QuizQuestion>,
    onAnswerSelected: (QuizQuestion, String) -> Unit,
    onResetQuizClick: () -> Unit
) {
    val totalQuizzesCount = quizzes.size
    val answeredCount = quizzes.count { it.userAnswer != null }
    val correctAnswersCount = quizzes.count { it.isCorrect == true }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 64.dp)
    ) {
        // Quiz Score Banner at the head
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "📊 PRACTICE EXAM MODULE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Score: $correctAnswersCount / $totalQuizzesCount Correct",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    if (answeredCount > 0) {
                        Button(
                            onClick = onResetQuizClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Test")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reset", color = MaterialTheme.colorScheme.primaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // List quiz items in cards
        items(quizzes) { quiz ->
            QuizQuestionItem(
                question = quiz,
                onAnswerClick = { option -> onAnswerSelected(quiz, option) }
            )
        }
    }
}

@Composable
fun QuizQuestionItem(
    question: QuizQuestion,
    onAnswerClick: (String) -> Unit
) {
    // Parse double-colon separated array
    val options = remember(question.optionsString) {
        question.optionsString.split("::")
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    val isAnswered = question.userAnswer != null

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Type Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Badge(
                    containerColor = if (question.questionType == "MCQ") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = if (question.questionType == "MCQ") "Multiple Choice" else "True/False",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(4.dp)
                    )
                }
                if (isAnswered) {
                    val correct = question.isCorrect == true
                    Text(
                        text = if (correct) "CORRECT ✅" else "INCORRECT ❌",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = if (correct) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = question.questionText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Show options as clickable lists
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { option ->
                    val isOptionSelected = question.userAnswer == option
                    val isCorrectOption = option == question.correctAnswer

                    val cardColor = when {
                        !isAnswered -> MaterialTheme.colorScheme.surface
                        isCorrectOption -> Color(0xFFE8F5E9) // soft green for correct answer
                        isOptionSelected && !isCorrectOption -> Color(0xFFFFEBEE) // soft red for wrong chosen option
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                    }

                    val outlineColor = when {
                        !isAnswered && isOptionSelected -> MaterialTheme.colorScheme.primary
                        isAnswered && isCorrectOption -> Color(0xFF4CAF50)
                        isAnswered && isOptionSelected && !isCorrectOption -> MaterialTheme.colorScheme.error
                        else -> Color.Transparent
                    }

                    val textColor = when {
                        isAnswered && isCorrectOption -> Color(0xFF2E7D32)
                        isAnswered && isOptionSelected && !isCorrectOption -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    }

                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isAnswered) { onAnswerClick(option) }
                            .border(
                                width = if (isOptionSelected || (isAnswered && isCorrectOption)) 2.dp else 1.dp,
                                color = if (outlineColor == Color.Transparent) MaterialTheme.colorScheme.outlineVariant else outlineColor,
                                shape = RoundedCornerShape(10.dp)
                            )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when {
                                    isAnswered && isCorrectOption -> Icons.Default.Check
                                    isAnswered && isOptionSelected && !isCorrectOption -> Icons.Default.Close
                                    isOptionSelected -> Icons.Default.CheckCircle
                                    else -> Icons.Default.Circle
                                },
                                contentDescription = null,
                                tint = when {
                                    isAnswered && isCorrectOption -> Color(0xFF4CAF50)
                                    isAnswered && isOptionSelected && !isCorrectOption -> MaterialTheme.colorScheme.error
                                    isOptionSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outline
                                },
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = option,
                                fontSize = 14.sp,
                                color = textColor,
                                fontWeight = if (isOptionSelected || (isAnswered && isCorrectOption)) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

enum class StudySubTab(val displayName: String) {
    Flashcards("Flashcards"),
    PracticeQuizzes("Academic Quiz")
}
