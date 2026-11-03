package com.example.ui.actions

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.GitHubRepository
import com.example.ui.components.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionsScreen(
    repository: GitHubRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var repos by remember { mutableStateOf<List<GitHubRepo>>(emptyList()) }
    var selectedRepo by remember { mutableStateOf<GitHubRepo?>(null) }
    var showRepoDropdown by remember { mutableStateOf(false) }

    var workflows by remember { mutableStateOf<List<GitHubWorkflow>>(emptyList()) }
    var runs by remember { mutableStateOf<List<GitHubWorkflowRun>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Run details dialog
    var selectedRun by remember { mutableStateOf<GitHubWorkflowRun?>(null) }
    var runJobs by remember { mutableStateOf<List<GitHubJob>>(emptyList()) }
    var isJobsLoading by remember { mutableStateOf(false) }

    // Dispatch dialog
    var workflowToDispatch by remember { mutableStateOf<GitHubWorkflow?>(null) }

    fun loadDataForRepo(repo: GitHubRepo) {
        isLoading = true
        errorMessage = null
        val owner = repo.owner?.login ?: ""
        coroutineScope.launch {
            val workflowsRes = repository.getWorkflows(owner, repo.name)
            val runsRes = repository.getWorkflowRuns(owner, repo.name)

            if (runsRes.isSuccess) {
                runs = runsRes.getOrNull() ?: emptyList()
            }
            if (workflowsRes.isSuccess) {
                workflows = workflowsRes.getOrNull() ?: emptyList()
            }
            if (runsRes.isFailure && workflowsRes.isFailure) {
                errorMessage = runsRes.exceptionOrNull()?.message ?: "Erro ao carregar dados do Actions"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        coroutineScope.launch {
            val reposRes = repository.getRepositories()
            if (reposRes.isSuccess) {
                repos = reposRes.getOrNull() ?: emptyList()
                if (repos.isNotEmpty()) {
                    selectedRepo = repos.first()
                    loadDataForRepo(repos.first())
                } else {
                    isLoading = false
                }
            } else {
                errorMessage = reposRes.exceptionOrNull()?.message
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GitHub Actions", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { selectedRepo?.let { loadDataForRepo(it) } },
                        enabled = selectedRepo != null
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Repo Selector Dropdown
            if (repos.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Box {
                        OutlinedButton(
                            onClick = { showRepoDropdown = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Repositório: ${selectedRepo?.name ?: "Selecionar"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }

                        DropdownMenu(
                            expanded = showRepoDropdown,
                            onDismissRequest = { showRepoDropdown = false }
                        ) {
                            repos.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r.name) },
                                    onClick = {
                                        selectedRepo = r
                                        showRepoDropdown = false
                                        loadDataForRepo(r)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Workflows and Runs List
            if (isLoading) {
                LoadingStateView(message = "Carregando workflows e execuções...")
            } else if (errorMessage != null && runs.isEmpty() && workflows.isEmpty()) {
                ErrorStateView(
                    message = errorMessage!!,
                    onRetry = { selectedRepo?.let { loadDataForRepo(it) } }
                )
            } else if (selectedRepo == null) {
                EmptyStateView(
                    icon = Icons.Default.PlayCircleOutline,
                    title = "Nenhum repositório disponível",
                    subtitle = "Crie ou conecte um repositório para utilizar o GitHub Actions."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Workflows List section
                    if (workflows.isNotEmpty()) {
                        item {
                            Text(
                                text = "Workflows Cadastrados (${workflows.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        items(workflows) { wf ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(wf.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                        Text(wf.path, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(
                                        onClick = { workflowToDispatch = wf },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Executar workflow", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }

                    // Workflow Runs Section
                    item {
                        Text(
                            text = "Histórico de Execuções (${runs.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    if (runs.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Nenhuma execução registrada para este repositório.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(runs) { run ->
                            val isSuccess = run.conclusion == "success"
                            val isFailure = run.conclusion == "failure"
                            val isInProgress = run.status == "in_progress" || run.status == "queued"

                            val statusIcon = when {
                                isSuccess -> Icons.Default.CheckCircle
                                isFailure -> Icons.Default.Cancel
                                isInProgress -> Icons.Default.Pending
                                else -> Icons.Default.HelpOutline
                            }
                            val iconColor = when {
                                isSuccess -> MaterialTheme.colorScheme.secondary
                                isFailure -> MaterialTheme.colorScheme.error
                                isInProgress -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Surface(
                                onClick = {
                                    selectedRun = run
                                    isJobsLoading = true
                                    val owner = selectedRepo?.owner?.login ?: ""
                                    coroutineScope.launch {
                                        val jobsRes = repository.getWorkflowRunJobs(owner, selectedRepo!!.name, run.id)
                                        if (jobsRes.isSuccess) {
                                            runJobs = jobsRes.getOrNull() ?: emptyList()
                                        }
                                        isJobsLoading = false
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = statusIcon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = run.name ?: "Execução #${run.runNumber ?: run.id}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${run.headBranch ?: "main"} • Disparado por ${run.event ?: "push"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = run.conclusion?.replaceFirstChar { it.uppercase() } ?: run.status ?: "",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Medium,
                                            color = iconColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // Run Details Dialog (Jobs and Steps)
        if (selectedRun != null) {
            val r = selectedRun!!
            val owner = selectedRepo?.owner?.login ?: ""
            val repoName = selectedRepo?.name ?: ""

            AlertDialog(
                onDismissRequest = { selectedRun = null },
                title = {
                    Text(
                        text = r.name ?: "Detalhes da Execução #${r.runNumber}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Branch: ${r.headBranch ?: "main"} | Evento: ${r.event ?: "push"}", style = MaterialTheme.typography.bodySmall)
                        Text("Status: ${r.status} | Conclusão: ${r.conclusion ?: "em andamento"}", style = MaterialTheme.typography.bodySmall)

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        Text("Jobs e Etapas:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)

                        if (isJobsLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp).align(Alignment.CenterHorizontally))
                        } else if (runJobs.isEmpty()) {
                            Text("Nenhum job registrado para esta execução.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            runJobs.forEach { job ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(job.name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        job.steps.forEach { step ->
                                            val stepSuccess = step.conclusion == "success"
                                            val stepIcon = if (stepSuccess) "✓" else "•"
                                            Text(
                                                text = "$stepIcon ${step.name} (${step.conclusion ?: step.status ?: ""})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (stepSuccess) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Actions: Rerun / Cancel / Open on Web
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val res = repository.rerunWorkflowRun(owner, repoName, r.id)
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "Execução reiniciada!", Toast.LENGTH_SHORT).show()
                                            selectedRepo?.let { loadDataForRepo(it) }
                                            selectedRun = null
                                        } else {
                                            Toast.makeText(context, "Erro: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Reexecutar")
                            }

                            if (r.status == "in_progress" || r.status == "queued") {
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            val res = repository.cancelWorkflowRun(owner, repoName, r.id)
                                            if (res.isSuccess) {
                                                Toast.makeText(context, "Execução cancelada!", Toast.LENGTH_SHORT).show()
                                                selectedRepo?.let { loadDataForRepo(it) }
                                                selectedRun = null
                                            } else {
                                                Toast.makeText(context, "Erro: ${res.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancelar")
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedRun = null }) {
                        Text("Fechar")
                    }
                }
            )
        }

        // Manual Dispatch Dialog
        if (workflowToDispatch != null) {
            val wf = workflowToDispatch!!
            val owner = selectedRepo?.owner?.login ?: ""
            val repoName = selectedRepo?.name ?: ""
            var branchRef by remember { mutableStateOf(selectedRepo?.defaultBranch ?: "main") }

            AlertDialog(
                onDismissRequest = { workflowToDispatch = null },
                title = { Text("Executar ${wf.name}") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Disparar execução manual (workflow_dispatch):", style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(
                            value = branchRef,
                            onValueChange = { branchRef = it },
                            label = { Text("Branch de execução") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val res = repository.dispatchWorkflow(owner, repoName, wf.id, branchRef.trim())
                                workflowToDispatch = null
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Workflow disparado!", Toast.LENGTH_SHORT).show()
                                    selectedRepo?.let { loadDataForRepo(it) }
                                } else {
                                    Toast.makeText(context, "Erro: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    ) {
                        Text("Disparar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { workflowToDispatch = null }) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}
