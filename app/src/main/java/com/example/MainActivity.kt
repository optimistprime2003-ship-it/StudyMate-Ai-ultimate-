package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DocumentChatScreen
import com.example.ui.screens.DocumentReaderScreen
import com.example.ui.screens.StudyCenterScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodels.AppTab
import com.example.ui.viewmodels.StudyViewModel

class MainActivity : ComponentActivity() {
    
    private val viewModel: StudyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppContainer(viewModel: StudyViewModel) {
    val currentTab by viewModel.currentTab.collectAsState()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("app_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = currentTab == AppTab.Dashboard,
                    onClick = { viewModel.selectTab(AppTab.Dashboard) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.Dashboard) Icons.Filled.LibraryBooks else Icons.Outlined.LibraryBooks,
                            contentDescription = "Dashboard"
                        )
                    },
                    label = { Text("Library") },
                    modifier = Modifier.testTag("nav_tab_dashboard")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.Reader,
                    onClick = { viewModel.selectTab(AppTab.Reader) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.Reader) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                            contentDescription = "Reader"
                        )
                    },
                    label = { Text("Read Aloud") },
                    modifier = Modifier.testTag("nav_tab_reader")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.StudyCenter,
                    onClick = { viewModel.selectTab(AppTab.StudyCenter) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.StudyCenter) Icons.Filled.Psychology else Icons.Outlined.Psychology,
                            contentDescription = "Study Center"
                        )
                    },
                    label = { Text("Revision") },
                    modifier = Modifier.testTag("nav_tab_study")
                )

                NavigationBarItem(
                    selected = currentTab == AppTab.Chat,
                    onClick = { viewModel.selectTab(AppTab.Chat) },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == AppTab.Chat) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline,
                            contentDescription = "Chat"
                        )
                    },
                    label = { Text("AI Tutor") },
                    modifier = Modifier.testTag("nav_tab_chat")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                AppTab.Dashboard -> DashboardScreen(viewModel = viewModel)
                AppTab.Reader -> DocumentReaderScreen(viewModel = viewModel)
                AppTab.StudyCenter -> StudyCenterScreen(viewModel = viewModel)
                AppTab.Chat -> DocumentChatScreen(viewModel = viewModel)
            }
        }
    }
}
