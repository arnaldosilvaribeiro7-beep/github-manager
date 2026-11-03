package com.example.ui.repos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.model.*
import com.example.data.repository.GitHubRepository
import com.example.data.repository.ZipManager
import com.example.ui.components.*
import kotlinx.coroutines.launch

enum class RepoTab(val label: String) {
    OVERVIEW("Visão Geral"),
    FILES("Arquivos"),
    COMMITS("Commits"),
    BRANCHES("Branches"),
    ISSUES("Issues"),
    PULL_REQUESTS("Pull Requests"),
    RELEASES("Releases"),
    SETTINGS("Configurações")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoDetailScreen(
    repo: GitHubRepo,
    repository: GitHubRepository,
    zipManager: ZipManager,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentRepo by remember { mutableStateOf(repo) }
    var selectedTab by remember { mutableStateOf(RepoTab.OVERVIEW) }
    val owner = currentRepo.owner?.login ?: ""
    val repoName = currentRepo.name

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = currentRepo.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            RepoVisibilityBadge(isPrivate = currentRepo.private)
                        }
                        Text(
                            text = owner,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    // Export ZIP Action
                    IconButton(onClick = {
                        coroutineScope.launch {
                            Toast.makeText(context, "Baixando ZIP do repositório...", Toast.LENGTH_SHORT).show()
                            val result = zipManager.exportRepoZip(owner, repoName, currentRepo.defaultBranch)
                            if (result.isSuccess) {
                                val file = result.getOrNull()
                                if (file != null) {
                                    val uri = try {
                                        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                                    } catch (e: Exception) {
                                        Uri.fromFile(file)
                                    }
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/zip"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartilhar ZIP"))
                                }
                            } else {
                                Toast.makeText(context, "Erro ao exportar: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }) {
                        Icon(Icons.Default.Download, contentDescription = "Exportar ZIP")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Scrollable Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                RepoTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.label, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    RepoTab.OVERVIEW -> RepoOverviewTab(
                        repo = currentRepo,
                        onNavigateToTab = { selectedTab = it }
                    )
                    RepoTab.FILES -> FileExplorerView(
                        repo = currentRepo,
                        repository = repository
                    )
                    RepoTab.COMMITS -> RepoCommitsTab(
                        owner = owner,
                        repo = repoName,
                        repository = repository
                    )
                    RepoTab.BRANCHES -> RepoBranchesTab(
                        owner = owner,
                        repo = repoName,
                        repository = repository
                    )
                    RepoTab.ISSUES -> RepoIssuesTab(
                        owner = owner,
                        repo = repoName,
                        repository = repository
                    )
                    RepoTab.PULL_REQUESTS -> RepoPullRequestsTab(
                        owner = owner,
                        repo = repoName,
                        repository = repository
                    )
                    RepoTab.RELEASES -> RepoReleasesTab(
                        owner = owner,
                        repo = repoName,
                        repository = repository
                    )
                    RepoTab.SETTINGS -> RepoSettingsTab(
                        repo = currentRepo,
                        repository = repository,
                        onRepoUpdated = { updated -> currentRepo = updated },
                        onRepoDeleted = { onNavigateBack() }
                    )
                }
            }
        }
    }
}

@Composable
fun RepoOverviewTab(
    repo: GitHubRepo,
    onNavigateToTab: (RepoTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Description Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Sobre o repositório",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = repo.description ?: "Nenhuma descrição fornecida para este repositório.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (repo.description != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (repo.language != null) {
                        LanguageIndicator(language = repo.language)
                    } else {
                        Text("Linguagem não detectada", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = "Branch padrão: ${repo.defaultBranch}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Stats Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                count = repo.stargazersCount.toString(),
                label = "Estrelas",
                icon = Icons.Default.StarBorder,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                count = repo.forksCount.toString(),
                label = "Forks",
                icon = Icons.Default.CallSplit,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                count = repo.openIssuesCount.toString(),
                label = "Issues",
                icon = Icons.Default.Adjust,
                modifier = Modifier.weight(1f),
                onClick = { onNavigateToTab(RepoTab.ISSUES) }
            )
        }

        // Quick Navigation Tiles
        Text(
            text = "Explorar seções",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )

        val navTiles = listOf(
            Triple("Arquivos de Código", "Navegar, editar, criar e baixar arquivos", RepoTab.FILES),
            Triple("Histórico de Commits", "Ver commits recentes e autores", RepoTab.COMMITS),
            Triple("Pull Requests", "Revisar e mesclar alterações", RepoTab.PULL_REQUESTS),
            Triple("Configurações do Repo", "Renomear, visibilidade e arquivamento", RepoTab.SETTINGS)
        )

        navTiles.forEach { (title, subtitle, targetTab) ->
            Surface(
                onClick = { onNavigateToTab(targetTab) },
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun RepoCommitsTab(owner: String, repo: String, repository: GitHubRepository) {
    var commits by remember { mutableStateOf<List<GitHubCommit>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(owner, repo) {
        isLoading = true
        coroutineScope.launch {
            val res = repository.getCommits(owner, repo)
            if (res.isSuccess) commits = res.getOrNull() ?: emptyList() else errorMessage = res.exceptionOrNull()?.message
            isLoading = false
        }
    }

    if (isLoading) {
        LoadingStateView(message = "Carregando histórico de commits...")
    } else if (errorMessage != null) {
        ErrorStateView(message = errorMessage!!)
    } else if (commits.isEmpty()) {
        EmptyStateView(icon = Icons.Default.History, title = "Nenhum commit encontrado")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(commits) { commit ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = commit.commit?.message?.trim() ?: "Commit sem mensagem",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (commit.author?.avatarUrl != null) {
                                AsyncImage(
                                    model = commit.author.avatarUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp).clip(CircleShape)
                                )
                            }
                            Text(
                                text = commit.author?.login ?: commit.commit?.author?.name ?: "Autor",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = commit.sha.take(7),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RepoBranchesTab(owner: String, repo: String, repository: GitHubRepository) {
    var branches by remember { mutableStateOf<List<GitHubBranch>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(owner, repo) {
        isLoading = true
        coroutineScope.launch {
            val res = repository.getBranches(owner, repo)
            if (res.isSuccess) branches = res.getOrNull() ?: emptyList() else errorMessage = res.exceptionOrNull()?.message
            isLoading = false
        }
    }

    if (isLoading) {
        LoadingStateView(message = "Carregando branches...")
    } else if (errorMessage != null) {
        ErrorStateView(message = errorMessage!!)
    } else if (branches.isEmpty()) {
        EmptyStateView(icon = Icons.Default.CallSplit, title = "Nenhuma branch encontrada")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(branches) { branch ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CallSplit, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(branch.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        if (branch.protected == true) {
                            StatusBadge(text = "Protegida", containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RepoIssuesTab(owner: String, repo: String, repository: GitHubRepository) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var issues by remember { mutableStateOf<List<GitHubIssue>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    fun loadIssues() {
        isLoading = true
        coroutineScope.launch {
            val res = repository.getIssues(owner, repo)
            if (res.isSuccess) issues = res.getOrNull() ?: emptyList() else errorMessage = res.exceptionOrNull()?.message
            isLoading = false
        }
    }

    LaunchedEffect(owner, repo) {
        loadIssues()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            LoadingStateView(message = "Carregando issues...")
        } else if (errorMessage != null) {
            ErrorStateView(message = errorMessage!!, onRetry = { loadIssues() })
        } else if (issues.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Adjust,
                title = "Nenhuma issue encontrada",
                subtitle = "Todas as tarefas foram resolvidas ou ainda não foram criadas.",
                actionButtonText = "Criar Issue",
                onActionClick = { showCreateDialog = true }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${issues.size} issues encontradas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FilledTonalButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nova Issue")
                        }
                    }
                }

                items(issues) { issue ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Adjust,
                                        contentDescription = null,
                                        tint = if (issue.state == "open") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(issue.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text("#${issue.number}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            if (!issue.body.isNullOrBlank()) {
                                Text(issue.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Por @${issue.user?.login ?: "autor"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                // Quick toggle state (open / closed)
                                TextButton(
                                    onClick = {
                                        val newState = if (issue.state == "open") "closed" else "open"
                                        coroutineScope.launch {
                                            val res = repository.updateIssue(owner, repo, issue.number, state = newState)
                                            if (res.isSuccess) {
                                                Toast.makeText(context, "Issue ${if (newState == "closed") "fechada" else "reaberta"}!", Toast.LENGTH_SHORT).show()
                                                loadIssues()
                                            }
                                        }
                                    }
                                ) {
                                    Text(if (issue.state == "open") "Fechar issue" else "Reabrir issue", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCreateDialog) {
            var newTitle by remember { mutableStateOf("") }
            var newBody by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = { Text("Criar Nova Issue") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Título da issue") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newBody,
                            onValueChange = { newBody = it },
                            label = { Text("Descrição / Comentário") },
                            minLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newTitle.isNotBlank()) {
                                coroutineScope.launch {
                                    val res = repository.createIssue(owner, repo, newTitle, newBody)
                                    showCreateDialog = false
                                    if (res.isSuccess) {
                                        Toast.makeText(context, "Issue criada com sucesso!", Toast.LENGTH_SHORT).show()
                                        loadIssues()
                                    } else {
                                        Toast.makeText(context, "Erro: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        enabled = newTitle.isNotBlank()
                    ) {
                        Text("Criar Issue")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
fun RepoPullRequestsTab(owner: String, repo: String, repository: GitHubRepository) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var prs by remember { mutableStateOf<List<GitHubPullRequest>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadPRs() {
        isLoading = true
        coroutineScope.launch {
            val res = repository.getPullRequests(owner, repo)
            if (res.isSuccess) prs = res.getOrNull() ?: emptyList() else errorMessage = res.exceptionOrNull()?.message
            isLoading = false
        }
    }

    LaunchedEffect(owner, repo) {
        loadPRs()
    }

    if (isLoading) {
        LoadingStateView(message = "Carregando pull requests...")
    } else if (errorMessage != null) {
        ErrorStateView(message = errorMessage!!, onRetry = { loadPRs() })
    } else if (prs.isEmpty()) {
        EmptyStateView(icon = Icons.Default.MergeType, title = "Nenhum pull request encontrado")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(prs) { pr ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MergeType,
                                    contentDescription = null,
                                    tint = if (pr.state == "open") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(pr.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text("#${pr.number}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Text(
                            text = "${pr.head?.ref ?: "branch"} → ${pr.base?.ref ?: "main"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (pr.state == "open") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val res = repository.mergePullRequest(owner, repo, pr.number)
                                            if (res.isSuccess) {
                                                Toast.makeText(context, "Pull Request mesclado com sucesso!", Toast.LENGTH_SHORT).show()
                                                loadPRs()
                                            } else {
                                                Toast.makeText(context, "Erro ao mesclar: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text("Fazer Merge")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RepoReleasesTab(owner: String, repo: String, repository: GitHubRepository) {
    var releases by remember { mutableStateOf<List<GitHubRelease>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(owner, repo) {
        isLoading = true
        coroutineScope.launch {
            val res = repository.getReleases(owner, repo)
            if (res.isSuccess) releases = res.getOrNull() ?: emptyList() else errorMessage = res.exceptionOrNull()?.message
            isLoading = false
        }
    }

    if (isLoading) {
        LoadingStateView(message = "Carregando releases...")
    } else if (errorMessage != null) {
        ErrorStateView(message = errorMessage!!)
    } else if (releases.isEmpty()) {
        EmptyStateView(icon = Icons.Default.Sell, title = "Nenhuma release publicada")
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(releases) { release ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(release.name ?: release.tagName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            StatusBadge(text = release.tagName, containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!release.body.isNullOrBlank()) {
                            Text(release.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RepoSettingsTab(
    repo: GitHubRepo,
    repository: GitHubRepository,
    onRepoUpdated: (GitHubRepo) -> Unit,
    onRepoDeleted: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val owner = repo.owner?.login ?: ""

    var repoName by remember { mutableStateOf(repo.name) }
    var description by remember { mutableStateOf(repo.description ?: "") }
    var isPrivate by remember { mutableStateOf(repo.private) }
    var isArchived by remember { mutableStateOf(repo.archived) }
    var isSaving by remember { mutableStateOf(false) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Configurações Gerais", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                OutlinedTextField(
                    value = repoName,
                    onValueChange = { repoName = it },
                    label = { Text("Nome do repositório") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Repositório Privado", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Apenas colaboradores autorizados podem acessar", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Arquivar Repositório", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text("Torna o repositório somente-leitura", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = isArchived, onCheckedChange = { isArchived = it })
                }

                Button(
                    onClick = {
                        isSaving = true
                        coroutineScope.launch {
                            val res = repository.updateRepo(
                                owner = owner,
                                repo = repo.name,
                                name = if (repoName != repo.name) repoName else null,
                                description = description,
                                isPrivate = isPrivate,
                                archived = isArchived
                            )
                            isSaving = false
                            if (res.isSuccess) {
                                val updated = res.getOrNull()!!
                                onRepoUpdated(updated)
                                Toast.makeText(context, "Repositório atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Erro: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Salvar Configurações")
                }
            }
        }

        // Danger Zone Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Zona de Perigo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Text(
                    text = "A exclusão do repositório é permanente e removerá todos os arquivos, branches, commits e issues.",
                    style = MaterialTheme.typography.bodySmall
                )
                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth().testTag("delete_repo_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Excluir este repositório")
                }
            }
        }
    }

    if (showDeleteConfirm) {
        ConfirmationDialog(
            title = "Excluir repositório",
            message = "Tem certeza absoluta de que deseja excluir o repositório '$owner/${repo.name}'? Esta ação não pode ser desfeita.",
            confirmText = "Excluir Repositório",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirm = false
                coroutineScope.launch {
                    val res = repository.deleteRepo(owner, repo.name)
                    if (res.isSuccess) {
                        Toast.makeText(context, "Repositório excluído com sucesso!", Toast.LENGTH_SHORT).show()
                        onRepoDeleted()
                    } else {
                        Toast.makeText(context, "Falha ao excluir: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}
