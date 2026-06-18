package com.example.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.models.StudyDocument
import com.example.ui.viewmodels.StudyViewModel

@Composable
fun DocumentReaderScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val selectedDoc by viewModel.selectedDocument.collectAsState()
    
    val isPlaying by viewModel.ttsManager.isPlaying.collectAsState()
    val activeSentenceIdx by viewModel.ttsManager.currentSentenceIndex.collectAsState()

    var activeSummaryMode by remember { mutableStateOf(SummaryMode.FullText) }
    var readingSpeed by remember { mutableStateOf(1.0f) }

    val scrollState = rememberScrollState()

    if (selectedDoc == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Document Selected",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Select a textbook or notes file from the Dashboard to start reading.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
        return
    }

    val doc = selectedDoc!!

    // Parse whole text into local sentences for highlighting during read aloud
    val sentences = remember(doc.fullText) {
        doc.fullText.split(Regex("(?<=[.!?\\n])\\s+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // TTS Audio Controller Panel
            TtsControlPanel(
                isPlaying = isPlaying,
                isInitialized = viewModel.ttsManager.isInitialized.collectAsState().value,
                readingSpeed = readingSpeed,
                onSpeedChange = {
                    readingSpeed = it
                    viewModel.ttsManager.setSpeed(it)
                },
                onPlayPauseClick = {
                    if (isPlaying) {
                        viewModel.ttsManager.pause()
                    } else {
                        val currentTextToRead = when (activeSummaryMode) {
                            SummaryMode.FullText -> doc.fullText
                            SummaryMode.Short -> doc.summaryShort
                            SummaryMode.Medium -> doc.summaryMedium
                            SummaryMode.Detailed -> doc.summaryDetailed
                            SummaryMode.ExamRevision -> doc.summaryExamRevision
                            SummaryMode.BulletPoints -> doc.summaryBulletPoints
                        }
                        viewModel.ttsManager.playText(currentTextToRead)
                    }
                },
                onStopClick = { viewModel.ttsManager.stop() }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Document Info Bar
            Card(
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(14.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = doc.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = "Format: ${doc.fileType} • Offline AI Summarized",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Summary Modes Chip Row (AI Customizer)
            ScrollableTabRow(
                selectedTabIndex = activeSummaryMode.ordinal,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth(),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                SummaryMode.values().forEach { mode ->
                    Tab(
                        selected = activeSummaryMode == mode,
                        onClick = {
                            activeSummaryMode = mode
                            viewModel.ttsManager.stop() // reset if screen switching
                        },
                        text = {
                            Text(
                                text = mode.displayName,
                                fontWeight = if (activeSummaryMode == mode) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    )
                }
            }

            // Document View Area (Renders text or processed markdown content depending on active chip)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(20.dp)
            ) {
                when (activeSummaryMode) {
                    SummaryMode.FullText -> {
                        // Highlights current sentence which is read aloud
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            sentences.forEachIndexed { sIdx, sentence ->
                                val isHighlighted = isPlaying && sIdx == activeSentenceIdx
                                val bgMod = if (isHighlighted) {
                                    Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
                                        .padding(4.dp)
                                } else {
                                    Modifier.padding(4.dp)
                                }

                                Text(
                                    text = sentence,
                                    fontSize = 16.sp,
                                    lineHeight = 24.sp,
                                    fontFamily = FontFamily.Serif,
                                    color = if (isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    modifier = bgMod
                                )
                            }
                        }
                    }

                    SummaryMode.Short -> {
                        SummaryBlock(
                            title = "Short Summary",
                            icon = Icons.Default.Summarize,
                            text = doc.summaryShort
                        )
                    }

                    SummaryMode.Medium -> {
                        SummaryBlock(
                            title = "Executive Overview",
                            icon = Icons.Default.Feed,
                            text = doc.summaryMedium
                        )
                    }

                    SummaryMode.Detailed -> {
                        SummaryBlock(
                            title = "In-Depth Academic Breakdown",
                            icon = Icons.Default.MenuBook,
                            text = doc.summaryDetailed
                        )
                    }

                    SummaryMode.ExamRevision -> {
                        SummaryBlock(
                            title = "High-Yield Fast Revision",
                            icon = Icons.Default.OfflineBolt,
                            text = doc.summaryExamRevision
                        )
                    }

                    SummaryMode.BulletPoints -> {
                        SummaryBlock(
                            title = "Quick Bullet Highlights",
                            icon = Icons.Default.List,
                            text = doc.summaryBulletPoints
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryBlock(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = text.replace("### ", "").replace("## ", "").replace("**", ""), // Clean formatting
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

@Composable
fun TtsControlPanel(
    isPlaying: Boolean,
    isInitialized: Boolean,
    readingSpeed: Float,
    onSpeedChange: (Float) -> Unit,
    onPlayPauseClick: () -> Unit,
    onStopClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Audio speed slider label
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = "Read Aloud Control",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Read Aloud: Speed ${"%.1f".format(readingSpeed)}x",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(
                        onClick = onStopClick,
                        enabled = isInitialized && isPlaying,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (isPlaying) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = if (isPlaying) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onPlayPauseClick,
                        enabled = isInitialized,
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                            .testTag("tts_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Slider(
                value = readingSpeed,
                onValueChange = onSpeedChange,
                valueRange = 0.5f..2.0f,
                steps = 5,
                colors = SliderDefaults.colors(
                    activeTickColor = Color.Transparent,
                    inactiveTickColor = Color.Transparent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
            )
        }
    }
}

enum class SummaryMode(val displayName: String) {
    FullText("Full Chapter"),
    Short("Short AI Summary"),
    Medium("Medium Summary"),
    Detailed("Detailed Reading"),
    ExamRevision("Exam Revision"),
    BulletPoints("Main Bullets")
}
