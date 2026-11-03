package com.example.ui.account

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.GitHubOrg
import com.example.data.model.GitHubUser
import com.example.data.repository.GitHubRepository
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.StatCard
import com.example.ui.components.StatusBadge
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    repository: GitHubRepository,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val user by repository.currentUser.collectAsState()
    var orgs by remember { mutableStateOf<List<GitHubOrg>>(emptyList()) }
    var scopes by remember { mutableStateOf<List<String>>(emptyList()) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        scopes = repository.getScopes()
        coroutineScope.launch {
            val orgsRes = repository.getUserOrgs()
            if (orgsRes.isSuccess) {
                orgs = orgsRes.getOrNull() ?: emptyList()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Conta e Perfil", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }, modifier = Modifier.testTag("logout_icon_button")) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Desconectar", tint = MaterialTheme.colorScheme.error)
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Header Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (user?.avatarUrl != null) {
                            AsyncImage(
                                model = user!!.avatarUrl,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(36.dp))
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user?.name ?: user?.login ?: "Usuário",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "@${user?.login ?: ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (!user?.bio.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = user!!.bio!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Edit Profile Button
                    OutlinedButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Editar Perfil")
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Public Information entries
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!user?.company.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Business, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(user!!.company!!, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (!user?.location.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(user!!.location!!, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (!user?.blog.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(user!!.blog!!, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (!user?.email.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(user!!.email!!, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Stats Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    count = ((user?.publicRepos ?: 0) + (user?.totalPrivateRepos ?: 0)).toString(),
                    label = "Repositórios",
                    icon = Icons.Default.Folder,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    count = (user?.followers ?: 0).toString(),
                    label = "Seguidores",
                    icon = Icons.Default.People,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    count = (user?.following ?: 0).toString(),
                    label = "Seguindo",
                    icon = Icons.Default.PersonAdd,
                    modifier = Modifier.weight(1f)
                )
            }

            // Accessible Organizations
            if (orgs.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Organizações Acessíveis (${orgs.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(orgs) { org ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (org.avatarUrl != null) {
                                            AsyncImage(model = org.avatarUrl, contentDescription = null, modifier = Modifier.size(18.dp).clip(CircleShape))
                                        }
                                        Text(org.login, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Token Permissions & Scopes Card
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Text("Permissões do Token (Escopos)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }

                    Text(
                        text = "Armazenamento protegido com Android Keystore (AES-256 GCM).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    if (scopes.isEmpty()) {
                        Text(
                            text = "Escopos padrão concedidos pelo seu Personal Access Token.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            scopes.forEach { scope ->
                                Text("• $scope", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // Disconnect Button
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Gerenciamento de Conexão", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Button(
                        onClick = { showLogoutDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth().testTag("disconnect_account_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Desconectar Conta")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Edit Profile Dialog
        if (showEditProfileDialog) {
            var newName by remember { mutableStateOf(user?.name ?: "") }
            var newBio by remember { mutableStateOf(user?.bio ?: "") }
            var newCompany by remember { mutableStateOf(user?.company ?: "") }
            var newLocation by remember { mutableStateOf(user?.location ?: "") }
            var newBlog by remember { mutableStateOf(user?.blog ?: "") }

            AlertDialog(
                onDismissRequest = { showEditProfileDialog = false },
                title = { Text("Editar Perfil") },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = newBio, onValueChange = { newBio = it }, label = { Text("Bio") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = newCompany, onValueChange = { newCompany = it }, label = { Text("Empresa") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = newLocation, onValueChange = { newLocation = it }, label = { Text("Localização") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = newBlog, onValueChange = { newBlog = it }, label = { Text("Website / Blog") }, modifier = Modifier.fillMaxWidth())
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val res = repository.updateProfile(
                                    name = newName.ifBlank { null },
                                    bio = newBio.ifBlank { null },
                                    company = newCompany.ifBlank { null },
                                    location = newLocation.ifBlank { null },
                                    blog = newBlog.ifBlank { null }
                                )
                                showEditProfileDialog = false
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Perfil atualizado no GitHub!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Erro: ${res.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    ) {
                        Text("Salvar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditProfileDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        // Logout Confirmation Dialog
        if (showLogoutDialog) {
            ConfirmationDialog(
                title = "Desconectar conta",
                message = "Tem certeza de que deseja desconectar? O Personal Access Token será removido de forma segura do dispositivo.",
                confirmText = "Desconectar",
                isDestructive = true,
                onConfirm = {
                    showLogoutDialog = false
                    repository.logout()
                    onLogout()
                },
                onDismiss = { showLogoutDialog = false }
            )
        }
    }
}
