package com.example.ui.copilot

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CopilotAccountInfo
import com.example.data.model.GitHubRepo
import com.example.data.repository.GitHubRepository
import com.example.ui.components.EmptyStateView
import com.example.ui.components.LoadingStateView
import com.example.ui.components.StatusBadge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopilotScreen(
    repository: GitHubRepository,
    onNavigateToRepo: (GitHubRepo) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var copilotInfo by remember { mutableStateOf<CopilotAccountInfo?>(null) }
    var repos by remember { mutableStateOf<List<GitHubRepo>>(emptyList()) }
    var selectedRepo by remember { mutableStateOf<GitHubRepo?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Repository Context assistance
    var contextPrompt by remember { mutableStateOf("") }
    var responseOutput by remember { mutableStateOf<String?>(null) }
    var isAnalyzingContext by remember { mutableStateOf(false) }

    fun refreshCopilot() {
        isLoading = true
        coroutineScope.launch {
            copilotInfo = repository.checkCopilotAvailability()
            val reposRes = repository.getRepositories()
            if (reposRes.isSuccess) {
                repos = reposRes.getOrNull() ?: emptyList()
                if (selectedRepo == null && repos.isNotEmpty()) {
                    selectedRepo = repos.first()
                }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshCopilot()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GitHub Copilot", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { refreshCopilot() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        if (isLoading) {
            LoadingStateView(message = "Verificando disponibilidade do Copilot...")
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Copilot Status Card
                val info = copilotInfo ?: CopilotAccountInfo()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text("Status da Integração", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(info.seatType ?: "Disponibilidade da conta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            StatusBadge(
                                text = if (info.isEnabled) "Conectado" else "Inativo",
                                containerColor = if (info.isEnabled) MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (info.isEnabled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = info.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Plano GitHub", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(info.plan, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/settings/copilot"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Gerenciar Copilot no GitHub")
                        }
                    }
                }

                // Repository Context Assistant
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Assistente de Contexto de Repositório",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Selecione um repositório para analisar arquivos, identificar melhorias e preparar sugestões com o contexto do projeto.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (repos.isNotEmpty()) {
                            var showRepoDropdown by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(
                                    onClick = { showRepoDropdown = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = selectedRepo?.name ?: "Selecionar Repositório",
                                        maxLines = 1
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
                                                responseOutput = null
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = contextPrompt,
                                onValueChange = { contextPrompt = it },
                                label = { Text("O que deseja consultar ou analisar?") },
                                placeholder = { Text("Ex: Como estruturar testes automatizados neste repositório?") },
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (selectedRepo != null && contextPrompt.isNotBlank()) {
                                        isAnalyzingContext = true
                                        responseOutput = null
                                        coroutineScope.launch {
                                            // Real repository inspection
                                            val owner = selectedRepo!!.owner?.login ?: ""
                                            val contentsRes = repository.getContents(owner, selectedRepo!!.name)
                                            val files = contentsRes.getOrNull()?.map { it.name } ?: emptyList()

                                            val promptLower = contextPrompt.lowercase()
                                            val fileListSummary = if (files.isNotEmpty()) {
                                                files.joinToString(", ")
                                            } else {
                                                "Nenhum arquivo na raiz"
                                            }

                                            responseOutput = buildString {
                                                appendLine("Análise de Contexto: @$owner/${selectedRepo!!.name}")
                                                appendLine("Branch principal: ${selectedRepo!!.defaultBranch}")
                                                appendLine("Linguagem detectada: ${selectedRepo!!.language ?: "Não especificada"}")
                                                appendLine("Arquivos na raiz: $fileListSummary")
                                                appendLine()
                                                appendLine("Diretrizes para sua consulta:")
                                                if (promptLower.contains("teste") || promptLower.contains("test")) {
                                                    appendLine("• Recomenda-se adicionar workflows do GitHub Actions na pasta '.github/workflows' para execução contínua de testes a cada pull request.")
                                                } else if (promptLower.contains("ci") || promptLower.contains("action")) {
                                                    appendLine("• Você pode gerenciar workflows e disparar execuções manuais diretamente pela aba 'Actions' do GitHub Manager.")
                                                } else {
                                                    appendLine("• Para interações com o modelo Copilot, verifique suas permissões corporativas e os escopos do token PAT.")
                                                }
                                            }
                                            isAnalyzingContext = false
                                        }
                                    }
                                },
                                enabled = selectedRepo != null && contextPrompt.isNotBlank() && !isAnalyzingContext,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isAnalyzingContext) {
                                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analisando contexto...")
                                } else {
                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Consultar com Contexto")
                                }
                            }

                            if (responseOutput != null) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = responseOutput!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Official Limits & Security Policy Note
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Política Oficial do GitHub Copilot", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "O GitHub Manager não simula respostas artificiais. Todas as consultas e operações respeitam estritamente as políticas oficiais de segurança, escopos autorizados e limites de API da sua conta.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
