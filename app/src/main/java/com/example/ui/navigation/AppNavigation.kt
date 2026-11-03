package com.example.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.example.data.model.GitHubRepo
import com.example.data.repository.AuthState
import com.example.data.repository.GitHubRepository
import com.example.data.repository.ZipManager
import com.example.ui.account.AccountScreen
import com.example.ui.actions.ActionsScreen
import com.example.ui.auth.AuthScreen
import com.example.ui.copilot.CopilotScreen
import com.example.ui.home.HomeScreen
import com.example.ui.repos.CreateRepoScreen
import com.example.ui.repos.RepoDetailScreen
import com.example.ui.repos.ReposScreen
import com.example.ui.repos.ZipImportScreen

enum class MainTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    HOME("Início", Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
    REPOS("Repos", Icons.Filled.Folder, Icons.Outlined.Folder, "tab_repos"),
    COPILOT("Copilot", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "tab_copilot"),
    ACTIONS("Actions", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircleOutline, "tab_actions"),
    ACCOUNT("Conta", Icons.Filled.Person, Icons.Outlined.Person, "tab_account")
}

sealed class SubScreen {
    data class RepoDetail(val repo: GitHubRepo) : SubScreen()
    data object CreateRepo : SubScreen()
    data object ImportZip : SubScreen()
}

@Composable
fun AppNavigation(
    repository: GitHubRepository,
    zipManager: ZipManager,
    modifier: Modifier = Modifier
) {
    val authState by repository.authState.collectAsState()

    var currentTab by remember { mutableStateOf(MainTab.HOME) }
    var currentSubScreen by remember { mutableStateOf<SubScreen?>(null) }

    when (authState) {
        is AuthState.Authenticated -> {
            if (currentSubScreen != null) {
                // Render SubScreens with BackHandler
                when (val sub = currentSubScreen!!) {
                    is SubScreen.RepoDetail -> {
                        RepoDetailScreen(
                            repo = sub.repo,
                            repository = repository,
                            zipManager = zipManager,
                            onNavigateBack = { currentSubScreen = null }
                        )
                    }
                    is SubScreen.CreateRepo -> {
                        CreateRepoScreen(
                            repository = repository,
                            onNavigateBack = { currentSubScreen = null },
                            onRepoCreated = { newRepo ->
                                currentSubScreen = SubScreen.RepoDetail(newRepo)
                            }
                        )
                    }
                    is SubScreen.ImportZip -> {
                        ZipImportScreen(
                            zipManager = zipManager,
                            onNavigateBack = { currentSubScreen = null },
                            onRepoCreated = { newRepo ->
                                currentSubScreen = SubScreen.RepoDetail(newRepo)
                            }
                        )
                    }
                }
            } else {
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = NavigationBarDefaults.Elevation
                        ) {
                            MainTab.values().forEach { tab ->
                                val selected = currentTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { currentTab = tab },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.title
                                        )
                                    },
                                    label = { Text(tab.title) },
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    },
                    modifier = modifier.fillMaxSize()
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        when (currentTab) {
                            MainTab.HOME -> HomeScreen(
                                repository = repository,
                                onNavigateToRepos = { currentTab = MainTab.REPOS },
                                onNavigateToRepoDetail = { currentSubScreen = SubScreen.RepoDetail(it) },
                                onNavigateToActions = { currentTab = MainTab.ACTIONS },
                                onNavigateToCopilot = { currentTab = MainTab.COPILOT },
                                onNavigateToImportZip = { currentSubScreen = SubScreen.ImportZip },
                                onNavigateToCreateRepo = { currentSubScreen = SubScreen.CreateRepo }
                            )
                            MainTab.REPOS -> ReposScreen(
                                repository = repository,
                                onNavigateToRepoDetail = { currentSubScreen = SubScreen.RepoDetail(it) },
                                onNavigateToCreateRepo = { currentSubScreen = SubScreen.CreateRepo },
                                onNavigateToImportZip = { currentSubScreen = SubScreen.ImportZip }
                            )
                            MainTab.COPILOT -> CopilotScreen(
                                repository = repository,
                                onNavigateToRepo = { currentSubScreen = SubScreen.RepoDetail(it) }
                            )
                            MainTab.ACTIONS -> ActionsScreen(
                                repository = repository
                            )
                            MainTab.ACCOUNT -> AccountScreen(
                                repository = repository,
                                onLogout = {
                                    currentTab = MainTab.HOME
                                    currentSubScreen = null
                                }
                            )
                        }
                    }
                }
            }
        }
        else -> {
            // Unauthenticated or Loading state
            AuthScreen(
                repository = repository,
                onAuthenticated = {
                    currentTab = MainTab.HOME
                    currentSubScreen = null
                }
            )
        }
    }
}
