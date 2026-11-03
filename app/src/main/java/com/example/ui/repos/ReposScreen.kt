package com.example.ui.repos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GitHubRepo
import com.example.data.repository.GitHubRepository
import com.example.ui.components.*
import kotlinx.coroutines.launch

enum class RepoFilter(val label: String) {
    ALL("Todos"),
    PUBLIC("Públicos"),
    PRIVATE("Privados"),
    ARCHIVED("Arquivados")
}

enum class RepoSort(val label: String) {
    UPDATED("Atualização"),
    NAME("Nome"),
    STARS("Estrelas")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReposScreen(
    repository: GitHubRepository,
    onNavigateToRepoDetail: (GitHubRepo) -> Unit,
    onNavigateToCreateRepo: () -> Unit,
    onNavigateToImportZip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var repos by remember { mutableStateOf<List<GitHubRepo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(RepoFilter.ALL) }
    var selectedSort by remember { mutableStateOf(RepoSort.UPDATED) }
    var showSortMenu by remember { mutableStateOf(false) }

    fun loadRepos() {
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val result = repository.getRepositories()
            if (result.isSuccess) {
                repos = result.getOrNull() ?: emptyList()
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Erro ao carregar repositórios"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadRepos()
    }

    val filteredRepos = remember(repos, searchQuery, selectedFilter, selectedSort) {
        var list = repos

        // Filter
        list = when (selectedFilter) {
            RepoFilter.ALL -> list
            RepoFilter.PUBLIC -> list.filter { !it.private }
            RepoFilter.PRIVATE -> list.filter { it.private }
            RepoFilter.ARCHIVED -> list.filter { it.archived }
        }

        // Search
        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        (it.description?.contains(searchQuery, ignoreCase = true) == true) ||
                        (it.language?.contains(searchQuery, ignoreCase = true) == true)
            }
        }

        // Sort
        when (selectedSort) {
            RepoSort.UPDATED -> list.sortedByDescending { it.updatedAt ?: "" }
            RepoSort.NAME -> list.sortedBy { it.name.lowercase() }
            RepoSort.STARS -> list.sortedByDescending { it.stargazersCount }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Repositórios",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToImportZip) {
                        Icon(Icons.Default.Archive, contentDescription = "Importar ZIP")
                    }
                    IconButton(onClick = { loadRepos() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Recarregar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreateRepo,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("create_repo_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Criar Repositório")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar repositório por nome, linguagem...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar busca")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("repo_search_field")
            )

            // Filters and Sort Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Filter chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(RepoFilter.values()) { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Sort Dropdown button
                Box {
                    IconButton(
                        onClick = { showSortMenu = true }
                    ) {
                        Icon(Icons.Default.Sort, contentDescription = "Ordenar")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        RepoSort.values().forEach { sort ->
                            DropdownMenuItem(
                                text = { Text(sort.label) },
                                onClick = {
                                    selectedSort = sort
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (selectedSort == sort) {
                                        Icon(Icons.Default.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Repositories List
            if (isLoading && repos.isEmpty()) {
                LoadingStateView(message = "Carregando repositórios...")
            } else if (errorMessage != null && repos.isEmpty()) {
                ErrorStateView(
                    message = errorMessage!!,
                    onRetry = { loadRepos() }
                )
            } else if (filteredRepos.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.FolderOpen,
                    title = if (searchQuery.isNotEmpty()) "Nenhum repositório correspondente" else "Nenhum repositório encontrado",
                    subtitle = if (searchQuery.isNotEmpty()) "Tente buscar com outros termos." else "Crie seu primeiro repositório no botão '+' ou importe um ZIP.",
                    actionButtonText = if (searchQuery.isEmpty()) "Criar repositório" else null,
                    onActionClick = onNavigateToCreateRepo
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "${filteredRepos.size} repositórios",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    items(filteredRepos) { repo ->
                        Surface(
                            onClick = { onNavigateToRepoDetail(repo) },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
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
                                            imageVector = if (repo.private) Icons.Default.Lock else Icons.Default.Public,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = repo.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (repo.archived) {
                                            StatusBadge(
                                                text = "Arquivado",
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        RepoVisibilityBadge(isPrivate = repo.private)
                                    }
                                }

                                if (!repo.description.isNullOrBlank()) {
                                    Text(
                                        text = repo.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (repo.language != null) {
                                        LanguageIndicator(language = repo.language)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.StarBorder, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = repo.stargazersCount.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(text = repo.defaultBranch, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    if (repo.openIssuesCount > 0) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(Icons.Default.Adjust, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(text = repo.openIssuesCount.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }
}
