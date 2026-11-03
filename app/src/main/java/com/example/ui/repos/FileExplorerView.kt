package com.example.ui.repos

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GitHubContent
import com.example.data.model.GitHubRepo
import com.example.data.repository.GitHubRepository
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ErrorStateView
import com.example.ui.components.LoadingStateView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileExplorerView(
    repo: GitHubRepo,
    repository: GitHubRepository,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val owner = repo.owner?.login ?: ""
    val repoName = repo.name

    var currentPath by remember { mutableStateOf("") }
    var currentRef by remember { mutableStateOf(repo.defaultBranch) }
    var items by remember { mutableStateOf<List<GitHubContent>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Active File Viewer state
    var selectedFile by remember { mutableStateOf<GitHubContent?>(null) }
    var fileContentText by remember { mutableStateOf<String?>(null) }
    var isFileLoading by remember { mutableStateOf(false) }

    // Dialogs
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showEditFileDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<GitHubContent?>(null) }

    fun loadContents(path: String) {
        isLoading = true
        errorMessage = null
        selectedFile = null
        fileContentText = null
        coroutineScope.launch {
            val result = repository.getContents(owner, repoName, path, currentRef)
            if (result.isSuccess) {
                items = result.getOrNull()?.sortedWith(
                    compareBy<GitHubContent> { it.type != "dir" }.thenBy { it.name.lowercase() }
                ) ?: emptyList()
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Erro ao carregar conteúdo da pasta"
            }
            isLoading = false
        }
    }

    LaunchedEffect(currentPath, currentRef) {
        loadContents(currentPath)
    }

    // Single File upload launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "uploaded_file"
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes() ?: byteArrayOf()
                    val textContent = String(bytes, Charsets.UTF_8)
                    val targetPath = if (currentPath.isEmpty()) fileName else "$currentPath/$fileName"

                    val result = repository.createOrUpdateFile(
                        owner = owner,
                        repo = repoName,
                        path = targetPath,
                        message = "Upload de arquivo: $fileName",
                        rawContent = textContent,
                        branch = currentRef
                    )
                    if (result.isSuccess) {
                        Toast.makeText(context, "Arquivo enviado com sucesso!", Toast.LENGTH_SHORT).show()
                        loadContents(currentPath)
                    } else {
                        Toast.makeText(context, "Falha ao enviar arquivo: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Erro: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Path Breadcrumbs & Actions Row
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Breadcrumbs
                val segments = currentPath.split("/").filter { it.isNotEmpty() }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            currentPath = ""
                            selectedFile = null
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Raiz", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(repoName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    var builtPath = ""
                    for (seg in segments) {
                        builtPath += if (builtPath.isEmpty()) seg else "/$seg"
                        val segPath = builtPath
                        Text("/", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 2.dp))
                        TextButton(
                            onClick = {
                                currentPath = segPath
                                selectedFile = null
                            },
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(seg, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                // Action buttons: New File / Upload
                Row {
                    IconButton(onClick = { showCreateFileDialog = true }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.NoteAdd, contentDescription = "Novo arquivo", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { filePickerLauncher.launch("*/*") }, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.UploadFile, contentDescription = "Upload de arquivo", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // File Viewer or Directory List
        if (selectedFile != null) {
            FileDetailViewer(
                file = selectedFile!!,
                content = fileContentText,
                isLoading = isFileLoading,
                owner = owner,
                repo = repoName,
                branch = currentRef,
                onClose = {
                    selectedFile = null
                    fileContentText = null
                },
                onEdit = { showEditFileDialog = true },
                onDelete = {
                    fileToDelete = selectedFile
                    showDeleteConfirmDialog = true
                }
            )
        } else {
            if (isLoading) {
                LoadingStateView(message = "Carregando arquivos...")
            } else if (errorMessage != null) {
                ErrorStateView(message = errorMessage!!, onRetry = { loadContents(currentPath) })
            } else if (items.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.FolderOpen,
                    title = "Pasta vazia",
                    subtitle = "Nenhum arquivo nesta pasta.",
                    actionButtonText = "Criar arquivo",
                    onActionClick = { showCreateFileDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(items) { item ->
                        val isDir = item.type == "dir"
                        Surface(
                            onClick = {
                                if (isDir) {
                                    currentPath = item.path
                                } else {
                                    selectedFile = item
                                    isFileLoading = true
                                    coroutineScope.launch {
                                        val res = repository.getFileContent(owner, repoName, item.path, currentRef)
                                        if (res.isSuccess) {
                                            val c = res.getOrNull()
                                            val decoded = if (c?.encoding == "base64" && c.content != null) {
                                                try {
                                                    val clean = c.content.replace("\n", "").replace("\r", "")
                                                    String(Base64.decode(clean, Base64.DEFAULT), Charsets.UTF_8)
                                                } catch (e: Exception) {
                                                    "[Conteúdo binário ou não decodificável]"
                                                }
                                            } else {
                                                c?.content ?: ""
                                            }
                                            fileContentText = decoded
                                        } else {
                                            fileContentText = "Erro ao carregar conteúdo do arquivo: ${res.exceptionOrNull()?.message}"
                                        }
                                        isFileLoading = false
                                    }
                                }
                            },
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isDir) Icons.Default.Folder else Icons.Default.Description,
                                        contentDescription = null,
                                        tint = if (isDir) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                if (!isDir && item.size != null) {
                                    Text(
                                        text = formatFileSize(item.size),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (isDir) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    }
                }
            }
        }
    }

    // Create File / Folder Dialog
    if (showCreateFileDialog) {
        CreateFileDialog(
            currentFolder = currentPath,
            onDismiss = { showCreateFileDialog = false },
            onConfirm = { pathName, content, message ->
                coroutineScope.launch {
                    val fullPath = if (currentPath.isEmpty()) pathName else "$currentPath/$pathName"
                    val res = repository.createOrUpdateFile(
                        owner = owner,
                        repo = repoName,
                        path = fullPath,
                        message = message.ifBlank { "Criar $pathName" },
                        rawContent = content,
                        branch = currentRef
                    )
                    showCreateFileDialog = false
                    if (res.isSuccess) {
                        Toast.makeText(context, "Arquivo criado com sucesso!", Toast.LENGTH_SHORT).show()
                        loadContents(currentPath)
                    } else {
                        Toast.makeText(context, "Erro: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Edit File Dialog
    if (showEditFileDialog && selectedFile != null) {
        EditFileDialog(
            fileName = selectedFile!!.name,
            initialContent = fileContentText ?: "",
            onDismiss = { showEditFileDialog = false },
            onConfirm = { newContent, commitMsg ->
                coroutineScope.launch {
                    val res = repository.createOrUpdateFile(
                        owner = owner,
                        repo = repoName,
                        path = selectedFile!!.path,
                        message = commitMsg.ifBlank { "Atualizar ${selectedFile!!.name}" },
                        rawContent = newContent,
                        branch = currentRef,
                        sha = selectedFile!!.sha
                    )
                    showEditFileDialog = false
                    if (res.isSuccess) {
                        Toast.makeText(context, "Arquivo atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                        fileContentText = newContent
                    } else {
                        Toast.makeText(context, "Erro: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }

    // Delete File Confirmation
    if (showDeleteConfirmDialog && fileToDelete != null) {
        ConfirmationDialog(
            title = "Excluir arquivo",
            message = "Tem certeza que deseja excluir '${fileToDelete!!.name}' do repositório? Esta ação criará um commit de remoção.",
            confirmText = "Excluir",
            isDestructive = true,
            onConfirm = {
                showDeleteConfirmDialog = false
                val target = fileToDelete!!
                fileToDelete = null
                selectedFile = null
                coroutineScope.launch {
                    val res = repository.deleteFile(
                        owner = owner,
                        repo = repoName,
                        path = target.path,
                        message = "Remover ${target.name}",
                        sha = target.sha ?: "",
                        branch = currentRef
                    )
                    if (res.isSuccess) {
                        Toast.makeText(context, "Arquivo excluído com sucesso!", Toast.LENGTH_SHORT).show()
                        loadContents(currentPath)
                    } else {
                        Toast.makeText(context, "Erro: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            onDismiss = {
                showDeleteConfirmDialog = false
                fileToDelete = null
            }
        )
    }
}

@Composable
fun FileDetailViewer(
    file: GitHubContent,
    content: String?,
    isLoading: Boolean,
    owner: String,
    repo: String,
    branch: String,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val rawUrl = "https://raw.githubusercontent.com/$owner/$repo/$branch/${file.path}"

    Column(modifier = Modifier.fillMaxSize()) {
        // File Viewer Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                    Column {
                        Text(
                            text = file.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = file.path,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // File Actions (Copy Content, Copy RAW, Share, Edit, Delete)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Código", content ?: "")
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Conteúdo copiado!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar conteúdo", modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("RAW URL", rawUrl)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "URL RAW copiada!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = "Copiar URL RAW", modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(rawUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.OpenInBrowser, contentDescription = "Abrir RAW", modifier = Modifier.size(18.dp))
                    }

                    IconButton(
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, content ?: rawUrl)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Compartilhar ${file.name}"))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Compartilhar", modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Code Content Viewer
        if (isLoading) {
            LoadingStateView(message = "Carregando código...")
        } else {
            val lines = content?.lines() ?: emptyList()
            SelectionContainer {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState())
                        .padding(8.dp)
                ) {
                    // Line Numbers column
                    Column(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .widthIn(min = 28.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        lines.indices.forEach { index ->
                            Text(
                                text = "${index + 1}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }

                    // Vertical subtle separator
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(IntrinsicSize.Max)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Code Text Column
                    Column {
                        lines.forEach { line ->
                            Text(
                                text = line,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CreateFileDialog(
    currentFolder: String,
    onDismiss: () -> Unit,
    onConfirm: (path: String, content: String, commitMsg: String) -> Unit
) {
    var fileName by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var commitMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo Arquivo ou Pasta") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Dica: Você pode criar pastas digitando caminhos como 'src/utils/Helper.kt'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Nome do arquivo (ex: index.js)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Conteúdo inicial") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = commitMsg,
                    onValueChange = { commitMsg = it },
                    label = { Text("Mensagem do commit (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fileName.isNotBlank()) {
                        onConfirm(fileName.trim(), content, commitMsg)
                    }
                },
                enabled = fileName.isNotBlank()
            ) {
                Text("Criar e Commitar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun EditFileDialog(
    fileName: String,
    initialContent: String,
    onDismiss: () -> Unit,
    onConfirm: (newContent: String, commitMsg: String) -> Unit
) {
    var content by remember { mutableStateOf(initialContent) }
    var commitMsg by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar $fileName") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Conteúdo do arquivo") },
                    minLines = 8,
                    maxLines = 14,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = commitMsg,
                    onValueChange = { commitMsg = it },
                    label = { Text("Mensagem do commit") },
                    placeholder = { Text("Atualizar $fileName") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(content, commitMsg) }
            ) {
                Text("Salvar alterações")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
