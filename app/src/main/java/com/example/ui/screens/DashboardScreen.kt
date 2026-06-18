package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.models.StudyDocument
import com.example.ui.viewmodels.AppTab
import com.example.ui.viewmodels.ImportUiState
import com.example.ui.viewmodels.StudyViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    viewModel: StudyViewModel,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.allDocuments.collectAsState()
    val selectedDoc by viewModel.selectedDocument.collectAsState()
    val importState by viewModel.importState.collectAsState()
    val flashcards by viewModel.flashcards.collectAsState()
    val quizzes by viewModel.quizzes.collectAsState()

    var showTextImportDialog by remember { mutableStateOf(false) }
    var showScanOcrDialog by remember { mutableStateOf(false) }
    var documentToDelete by remember { mutableStateOf<StudyDocument?>(null) }

    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                FloatingActionButton(
                    onClick = { showScanOcrDialog = true },
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.testTag("scan_ocr_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = "Scan Textbook OCR")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Scan Textbook", fontWeight = FontWeight.Bold)
                    }
                }

                FloatingActionButton(
                    onClick = { showTextImportDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("import_text_fab")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Import Textbook Chapter")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Notes", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(bottom = 88.dp)
            ) {
                // Header Banner
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    DashboardHeader()
                }

                // Stats Dashboard Bar
                item {
                    StatsCard(
                        documentCount = documents.size,
                        flashcardCount = flashcards.size,
                        masteredFlashcards = flashcards.count { it.isMastered },
                        quizQuestions = quizzes.size,
                        quizzesAttempted = quizzes.count { it.userAnswer != null },
                        quizzesCorrect = quizzes.count { it.isCorrect == true }
                    )
                }

                // High Density Active Document Card
                if (selectedDoc != null) {
                    item {
                        ActiveDocumentCard(
                            document = selectedDoc!!,
                            onNavigateToReader = { viewModel.selectTab(AppTab.Reader) }
                        )
                    }

                    item {
                        QuickToolsGrid(
                            flashcardCount = flashcards.size,
                            quizCount = quizzes.size,
                            onNavigateToQuiz = { viewModel.selectTab(AppTab.StudyCenter) },
                            onNavigateToReader = { viewModel.selectTab(AppTab.Reader) }
                        )
                    }

                    item {
                        AnalysisSnapshotSection(
                            flashcardCount = flashcards.size,
                            quizCount = quizzes.size,
                            document = selectedDoc!!,
                            onRescanClick = {
                                android.widget.Toast.makeText(
                                    context,
                                    "Active Study Material decrypted & successfully re-analyzed offline",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }

                // Library Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Course Library List",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (documents.isNotEmpty()) {
                            Text(
                                text = "${documents.size} Indexed Offline",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Material items list
                if (documents.isEmpty()) {
                    item {
                        EmptyStudyState(
                            onAddNotesClick = { showTextImportDialog = true },
                            onScanClick = { showScanOcrDialog = true }
                        )
                    }
                } else {
                    items(documents) { doc ->
                        DocumentListItem(
                            document = doc,
                            isSelected = doc.id == selectedDoc?.id,
                            onClick = { viewModel.selectDocument(doc) },
                            onDeleteClick = { documentToDelete = doc }
                        )
                    }
                }
            }


            // Global Import State Feedback Loader Toast-style banner
            when (val state = importState) {
                is ImportUiState.Processing -> {
                    Dialog(onDismissRequest = {}) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(state.message, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
                is ImportUiState.Success -> {
                    AlertDialog(
                        onDismissRequest = { viewModel.resetImportState() },
                        confirmButton = {
                            TextButton(onClick = { viewModel.resetImportState() }) {
                                Text("OK")
                            }
                        },
                        title = { Text("Offline AI Ready!") },
                        text = { Text(state.message) }
                    )
                }
                is ImportUiState.Error -> {
                    AlertDialog(
                        onDismissRequest = { viewModel.resetImportState() },
                        confirmButton = {
                            TextButton(onClick = { viewModel.resetImportState() }) {
                                Text("OK")
                            }
                        },
                        title = { Text("Import Error") },
                        text = { Text(state.message) }
                    )
                }
                ImportUiState.Idle -> { /* do nothing */ }
            }

            // Dialog for Text Input Notes
            if (showTextImportDialog) {
                AddNotesDialog(
                    onDismiss = { showTextImportDialog = false },
                    onConfirm = { title, content ->
                        viewModel.importTextDocument(title, "TXT", content)
                        showTextImportDialog = false
                    }
                )
            }

            // Dialog for Simulated Local OCR Scan
            if (showScanOcrDialog) {
                ScanOcrDialog(
                    onDismiss = { showScanOcrDialog = false },
                    onConfirm = { fileName ->
                        viewModel.runAdvancedImageOcr(null, fileName)
                        showScanOcrDialog = false
                    }
                )
            }

            // Confirm Deletion dialog
            documentToDelete?.let { doc ->
                AlertDialog(
                    onDismissRequest = { documentToDelete = null },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteCurrentDocument()
                                documentToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Delete")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { documentToDelete = null }) {
                            Text("Cancel")
                        }
                    },
                    title = { Text("Delete Course Material?") },
                    text = { Text("This will permanently discard \"${doc.title}\", including AI-generated quizzes, flashcards, study histories and offline memory registers.") }
                )
            }
        }
    }
}

@Composable
fun DashboardHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // High Density custom app brand mark
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column {
                Text(
                    text = "StudyMate AI",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "High Density Offline Indexer",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        }
        
        // Premium JD Avatar Profile Badge
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "JD",
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ActiveDocumentCard(
    document: StudyDocument,
    onNavigateToReader: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToReader() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Custom file type icon card block
                    val (badgeBg, badgeText, badgeLabel) = when (document.fileType) {
                        "PDF" -> Triple(Color(0xFFFFDADA), Color(0xFF410002), "PDF")
                        "TXT" -> Triple(Color(0xFFEADDFF), Color(0xFF21005D), "TXT")
                        else -> Triple(Color(0xFFD0BCFF), Color(0xFF381E72), document.fileType)
                    }
                    Box(
                        modifier = Modifier
                            .size(width = 44.dp, height = 52.dp)
                            .background(badgeBg, RoundedCornerShape(8.dp))
                            .border(1.dp, badgeText.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = badgeLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeText
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Box(modifier = Modifier.size(width = 20.dp, height = 2.dp).background(badgeText))
                            Spacer(modifier = Modifier.height(2.dp))
                            Box(modifier = Modifier.size(width = 12.dp, height = 1.5.dp).background(badgeText.copy(alpha = 0.5f)))
                        }
                    }

                    Column {
                        Text(
                            text = document.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Size: ${(document.fullText.length * 2) / 1024} KB • ${document.fullText.split("\\s+".toRegex()).size} words",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // AI Active tag
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE8DEF8), androidx.compose.foundation.shape.CircleShape)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "OFFLINE AI ACTIVE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1D192B),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Insight Sub-Box nested
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF3EDF7))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Current Insight",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF6750A4)
                        )
                        Text(
                            text = "Local LLM: StudyMate-1b",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF49454F),
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                    Text(
                        text = if (document.summaryShort.isNotBlank()) document.summaryShort else "Generating offline intelligent index and course outlines...",
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = Color(0xFF1D1B20)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickToolsGrid(
    flashcardCount: Int,
    quizCount: Int,
    onNavigateToQuiz: () -> Unit,
    onNavigateToReader: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Card 1: Generate Quiz (Primary Container color #D0BCFF, text #381E72)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFD0BCFF)),
            modifier = Modifier
                .weight(1f)
                .clickable { onNavigateToQuiz() }
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF381E72), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = "Quiz Icon",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "GENERATE QUIZ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF381E72),
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = if (quizCount > 0) "$quizCount questions ready" else "No exam active",
                    fontSize = 10.sp,
                    color = Color(0xFF381E72).copy(alpha = 0.8f)
                )
            }
        }

        // Card 2: Voice Reader (White surface, border #CAC4D0, primary icon)
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0)),
            modifier = Modifier
                .weight(1f)
                .clickable { onNavigateToReader() }
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFF6750A4), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Hearing,
                        contentDescription = "TTS Icon",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "VOICE READER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1D1B20),
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Offline TTS: 1.0x - 2.0x",
                    fontSize = 10.sp,
                    color = Color(0xFF49454F)
                )
            }
        }
    }
}

@Composable
fun AnalysisSnapshotSection(
    flashcardCount: Int,
    quizCount: Int,
    document: StudyDocument,
    onRescanClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ANALYSIS SNAPSHOT",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF49454F),
                letterSpacing = 1.sp
            )
            Text(
                text = "RESCAN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF6750A4),
                modifier = Modifier.clickable { onRescanClick() }
            )
        }

        // Snapshot Item 1: Key Concept
        SnapshotItem(
            title = "Key Concept Detected",
            description = "Main themes: Outline and academic notes compiled to secure local cache.",
            barColor = Color(0xFF6750A4)
        )

        // Snapshot Item 2: Flashcards
        SnapshotItem(
            title = "Flashcards Active",
            description = "$flashcardCount custom review cards loaded in local study database index.",
            barColor = Color(0xFF625B71).copy(alpha = 0.4f)
        )

        // Snapshot Item 3: Practice Exam
        SnapshotItem(
            title = "Practice Quizzes Extracted",
            description = "$quizCount multiple-choice study assessments prepared offline from raw text.",
            barColor = Color(0xFF625B71).copy(alpha = 0.4f)
        )
    }
}

@Composable
fun SnapshotItem(
    title: String,
    description: String,
    barColor: Color
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCAC4D0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left vertical line/bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .background(barColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1D1B20)
                )
                Text(
                    text = description,
                    fontSize = 10.sp,
                    color = Color(0xFF49454F),
                    lineHeight = 14.sp
                )
            }
        }
    }
}

@Composable
fun StatsCard(
    documentCount: Int,
    flashcardCount: Int,
    masteredFlashcards: Int,
    quizQuestions: Int,
    quizzesAttempted: Int,
    quizzesCorrect: Int
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Text(
                text = "📚 LOCAL REVISION INDEX",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                letterSpacing = 1.2.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    title = "Textbooks",
                    value = "$documentCount",
                    icon = Icons.Default.Book,
                    tint = MaterialTheme.colorScheme.primary
                )
                StatItem(
                    title = "Flashcards",
                    value = "$masteredFlashcards/$flashcardCount",
                    icon = Icons.Default.Style,
                    tint = MaterialTheme.colorScheme.secondary
                )
                StatItem(
                    title = "Quiz Score",
                    value = if (quizzesAttempted > 0) "$quizzesCorrect/$quizzesAttempted" else "0/0",
                    icon = Icons.Default.School,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun StatItem(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(tint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentListItem(
    document: StudyDocument,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDeleteClick
            )
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when (document.fileType) {
                            "IMAGE" -> MaterialTheme.colorScheme.tertiaryContainer
                            "TXT" -> MaterialTheme.colorScheme.secondaryContainer
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (document.fileType) {
                        "IMAGE" -> Icons.Default.PhotoLibrary
                        "TXT" -> Icons.Default.Description
                        "PDF" -> Icons.Default.PictureAsPdf
                        else -> Icons.Default.Task
                    },
                    contentDescription = document.fileType,
                    tint = when (document.fileType) {
                        "IMAGE" -> MaterialTheme.colorScheme.onTertiaryContainer
                        "TXT" -> MaterialTheme.colorScheme.onSecondaryContainer
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Badge(containerColor = MaterialTheme.colorScheme.outlineVariant) {
                        Text(document.fileType, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                    Text(
                        text = "Added ${(System.currentTimeMillis() - document.dateAdded) / 60000}m ago",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.testTag("delete_doc_${document.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete Document",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun EmptyStudyState(
    onAddNotesClick: () -> Unit,
    onScanClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.LibraryBooks,
                contentDescription = "Empty library",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Revision Library is Empty",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "StudyMate AI stores notes completely on-device. Add a textbook chapter or snap handwritten notes to begin local AI indexing.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onScanClick,
                    modifier = Modifier.weight(1.1f)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("OCR Scan", fontSize = 12.sp)
                }
                Button(
                    onClick = onAddNotesClick,
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Paste Textbook", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun AddNotesDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Paste Academic Study Materials") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Chapter Title / Subject") },
                    placeholder = { Text("e.g. Physics Ch. 3 Electromagnetism") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it; if (it.isNotBlank()) hasError = false },
                    label = { Text("Textbook Syllabus Content") },
                    placeholder = { Text("Paste questions, formula, paragraphs, or book texts here completely offline...") },
                    minLines = 6,
                    maxLines = 10,
                    modifier = Modifier.fillMaxWidth(),
                    isError = hasError
                )
                if (hasError) {
                    Text("This field cannot be empty.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (content.isNotBlank()) {
                        onConfirm(title, content)
                    } else {
                        hasError = true
                    }
                }
            ) {
                Text("Process Offline")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Discard")
            }
        }
    )
}

@Composable
fun ScanOcrDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var selectedFile by remember { mutableStateOf("handwritten_notes_physics.png") }
    val simulatedFiles = listOf(
        "handwritten_notes_physics.png",
        "scanned_thermodynamics_syllabus.jpg",
        "exam_cheatsheet_formulas.webp",
        "textbook_photo_sample.jpeg"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Advanced Local OCR Reader") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Select notes/handout from your device storage below to run neural network OCR offline:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                simulatedFiles.forEach { file ->
                    val isCurrent = file == selectedFile
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFile = file }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isCurrent) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(Icons.Default.Photo, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(file, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedFile) }) {
                Text("Perform OCR")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
