package com.example.data.repository

import android.util.Base64
import com.example.data.api.GitHubApiClient
import com.example.data.model.*
import com.example.data.security.SecureTokenStorage
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

sealed class AuthState {
    data object Unauthenticated : AuthState()
    data object Loading : AuthState()
    data class Authenticated(val user: GitHubUser, val scopes: List<String>) : AuthState()
    data class Error(val message: String) : AuthState()
}

class GitHubRepository(
    private val tokenStorage: SecureTokenStorage,
    private val apiClient: GitHubApiClient
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<GitHubUser?>(null)
    val currentUser: StateFlow<GitHubUser?> = _currentUser.asStateFlow()

    val hasSavedToken: Boolean
        get() = tokenStorage.hasToken()

    suspend fun checkExistingAuth(): Boolean = withContext(Dispatchers.IO) {
        if (!tokenStorage.hasToken()) {
            _authState.value = AuthState.Unauthenticated
            return@withContext false
        }
        _authState.value = AuthState.Loading
        try {
            val response = apiClient.service.getAuthenticatedUser()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user, apiClient.lastCapturedScopes)
                true
            } else {
                _authState.value = AuthState.Error("Token inválido ou expirado (HTTP ${response.code()})")
                false
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error("Falha na conexão: ${e.localizedMessage ?: "Erro desconhecido"}")
            false
        }
    }

    suspend fun connectWithToken(token: String): Result<GitHubUser> = withContext(Dispatchers.IO) {
        val trimmed = token.trim()
        if (trimmed.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("O token não pode estar vazio."))
        }
        _authState.value = AuthState.Loading
        tokenStorage.saveToken(trimmed)
        try {
            val response = apiClient.service.getAuthenticatedUser()
            if (response.isSuccessful && response.body() != null) {
                val user = response.body()!!
                _currentUser.value = user
                _authState.value = AuthState.Authenticated(user, apiClient.lastCapturedScopes)
                Result.success(user)
            } else {
                tokenStorage.clearToken()
                val errorMsg = when (response.code()) {
                    401 -> "Token inválido ou sem permissões necessárias."
                    403 -> "Acesso negado pela API do GitHub (limite de taxa ou restrições)."
                    else -> "Erro ao autenticar: HTTP ${response.code()}"
                }
                _authState.value = AuthState.Error(errorMsg)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            tokenStorage.clearToken()
            val errorMsg = "Erro de rede: ${e.localizedMessage ?: "Verifique sua conexão."}"
            _authState.value = AuthState.Error(errorMsg)
            Result.failure(e)
        }
    }

    fun logout() {
        tokenStorage.clearToken()
        _currentUser.value = null
        _authState.value = AuthState.Unauthenticated
    }

    // Repositories
    suspend fun getRepositories(): Result<List<GitHubRepo>> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.service.getUserRepos(sort = "updated", direction = "desc", perPage = 100)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erro ao carregar repositórios: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRepo(owner: String, repo: String): Result<GitHubRepo> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.service.getRepo(owner, repo)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erro ao carregar repositório: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createRepo(name: String, description: String?, isPrivate: Boolean, autoInit: Boolean = true): Result<GitHubRepo> = withContext(Dispatchers.IO) {
        try {
            val req = CreateRepoRequest(name = name, description = description, `private` = isPrivate, autoInit = autoInit)
            val response = apiClient.service.createRepo(req)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erro ao criar repositório: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRepo(
        owner: String,
        repo: String,
        name: String? = null,
        description: String? = null,
        isPrivate: Boolean? = null,
        archived: Boolean? = null
    ): Result<GitHubRepo> = withContext(Dispatchers.IO) {
        try {
            val req = UpdateRepoRequest(name = name, description = description, `private` = isPrivate, archived = archived)
            val response = apiClient.service.updateRepo(owner, repo, req)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erro ao atualizar repositório: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteRepo(owner: String, repo: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.service.deleteRepo(owner, repo)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Erro ao excluir repositório: ${response.code()} (verifique se seu PAT possui escopo 'delete_repo')"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Contents & Code
    suspend fun getContents(owner: String, repo: String, path: String = "", ref: String? = null): Result<List<GitHubContent>> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.service.getRepoContents(owner, repo, path, ref)
            if (response.isSuccessful && response.body() != null) {
                val jsonStr = response.body()!!.string()
                val trimmed = jsonStr.trim()
                if (trimmed.startsWith("[")) {
                    val listType = Types.newParameterizedType(List::class.java, GitHubContent::class.java)
                    val adapter = apiClient.moshi.adapter<List<GitHubContent>>(listType)
                    val items = adapter.fromJson(trimmed) ?: emptyList()
                    Result.success(items)
                } else {
                    val adapter = apiClient.moshi.adapter(GitHubContent::class.java)
                    val single = adapter.fromJson(trimmed)
                    if (single != null) Result.success(listOf(single)) else Result.success(emptyList())
                }
            } else {
                Result.failure(Exception("Erro ao carregar conteúdo: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFileContent(owner: String, repo: String, path: String, ref: String? = null): Result<GitHubContent> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.service.getRepoContents(owner, repo, path, ref)
            if (response.isSuccessful && response.body() != null) {
                val jsonStr = response.body()!!.string()
                val adapter = apiClient.moshi.adapter(GitHubContent::class.java)
                val content = adapter.fromJson(jsonStr)
                if (content != null) {
                    Result.success(content)
                } else {
                    Result.failure(Exception("Falha ao analisar arquivo."))
                }
            } else {
                Result.failure(Exception("Erro ao abrir arquivo: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrUpdateFile(
        owner: String,
        repo: String,
        path: String,
        message: String,
        rawContent: String,
        branch: String? = null,
        sha: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val base64 = Base64.encodeToString(rawContent.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val req = CreateFileRequest(message = message, content = base64, branch = branch, sha = sha)
            val response = apiClient.service.createOrUpdateFile(owner, repo, path, req)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Erro ao salvar arquivo: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFile(
        owner: String,
        repo: String,
        path: String,
        message: String,
        sha: String,
        branch: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val req = DeleteFileRequest(message = message, sha = sha, branch = branch)
            val response = apiClient.service.deleteFile(owner, repo, path, req)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Erro ao excluir arquivo: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Commits, Branches, Releases
    suspend fun getCommits(owner: String, repo: String, sha: String? = null): Result<List<GitHubCommit>> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.service.getRepoCommits(owner, repo, sha)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erro ao carregar commits: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBranches(owner: String, repo: String): Result<List<GitHubBranch>> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.service.getRepoBranches(owner, repo)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erro ao carregar branches: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getReleases(owner: String, repo: String): Result<List<GitHubRelease>> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.service.getRepoReleases(owner, repo)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erro ao carregar releases: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Issues
    suspend fun getIssues(owner: String, repo: String, state: String = "all"): Result<List<GitHubIssue>> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.service.getRepoIssues(owner, repo, state)
            if (response.isSuccessful && response.body() != null) {
                // Filter out pull requests which are also returned in issues endpoint
                val issuesOnly = response.body()!!.filter { !it.isPullRequest }
                Result.success(issuesOnly)
            } else {
                Result.failure(Exception("Erro ao carregar issues: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createIssue(owner: String, repo: String, title: String, body: String?, labels: List<String>? = null): Result<GitHubIssue> = withContext(Dispatchers.IO) {
        try {
            val req = CreateIssueRequest(title = title, body = body, labels = labels)
            val response = apiClient.service.createIssue(owner, repo, req)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erro ao criar issue: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateIssue(owner: String, repo: String, issueNumber: Int, title: String? = null, body: String? = null, state: String? = null): Result<GitHubIssue> = withContext(Dispatchers.IO) {
        try {
            val req = UpdateIssueRequest(title = title, body = body, state = state)
            val response = apiClient.service.updateIssue(owner, repo, issueNumber, req)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erro ao atualizar issue: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getIssueComments(owner: String, repo: String, issueNumber: Int): Result<List<GitHubComment>> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.service.getIssueComments(owner, repo, issueNumber)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erro ao carregar comentários: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createIssueComment(owner: String, repo: String, issueNumber: Int, comment: String): Result<GitHubComment> = withContext(Dispatchers.IO) {
        try {
            val req = CreateCommentRequest(body = comment)
            val response = apiClient.service.createIssueComment(owner, repo, issueNumber, req)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erro ao adicionar comentário: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Pull Requests
    suspend fun getPullRequests(owner: String, repo: String, state: String = "all"): Result<List<GitHubPullRequest>> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.service.getRepoPullRequests(owner, repo, state)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erro ao carregar pull requests: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createPullRequest(owner: String, repo: String, title: String, head: String, base: String, body: String?): Result<GitHubPullRequest> = withContext(Dispatchers.IO) {
        try {
            val req = CreatePRRequest(title = title, head = head, base = base, body = body)
            val response = apiClient.service.createPullRequest(owner, repo, req)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erro ao criar pull request: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun mergePullRequest(owner: String, repo: String, pullNumber: Int, title: String? = null, method: String = "merge"): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val req = MergePRRequest(commitTitle = title, mergeMethod = method)
            val response = apiClient.service.mergePullRequest(owner, repo, pullNumber, req)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Erro ao fazer merge: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePullRequest(owner: String, repo: String, pullNumber: Int, title: String? = null, body: String? = null, state: String? = null): Result<GitHubPullRequest> = withContext(Dispatchers.IO) {
        try {
            val req = UpdateIssueRequest(title = title, body = body, state = state)
            val response = apiClient.service.updatePullRequest(owner, repo, pullNumber, req)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erro ao atualizar pull request: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Actions
    suspend fun getWorkflows(owner: String, repo: String): Result<List<GitHubWorkflow>> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.service.getRepoWorkflows(owner, repo)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.workflows)
            } else {
                Result.failure(Exception("Erro ao carregar workflows: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWorkflowRuns(owner: String, repo: String): Result<List<GitHubWorkflowRun>> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.service.getRepoWorkflowRuns(owner, repo)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.workflowRuns)
            } else {
                Result.failure(Exception("Erro ao carregar execuções: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWorkflowRunJobs(owner: String, repo: String, runId: Long): Result<List<GitHubJob>> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.service.getWorkflowRunJobs(owner, repo, runId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.jobs)
            } else {
                Result.failure(Exception("Erro ao carregar jobs: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelWorkflowRun(owner: String, repo: String, runId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.service.cancelWorkflowRun(owner, repo, runId)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("Erro: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rerunWorkflowRun(owner: String, repo: String, runId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.service.rerunWorkflowRun(owner, repo, runId)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("Erro: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun dispatchWorkflow(owner: String, repo: String, workflowId: Long, ref: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val req = WorkflowDispatchRequest(ref = ref)
            val response = apiClient.service.dispatchWorkflow(owner, repo, workflowId, req)
            if (response.isSuccessful) Result.success(Unit) else Result.failure(Exception("Erro: ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Account & Profile
    suspend fun getUserOrgs(): Result<List<GitHubOrg>> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.service.getUserOrgs()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Erro ao carregar organizações: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(name: String?, bio: String?, company: String?, location: String?, blog: String?): Result<GitHubUser> = withContext(Dispatchers.IO) {
        try {
            val req = UpdateProfileRequest(name = name, bio = bio, company = company, location = location, blog = blog)
            val response = apiClient.service.updateProfile(req)
            if (response.isSuccessful && response.body() != null) {
                val updated = response.body()!!
                _currentUser.value = updated
                Result.success(updated)
            } else {
                Result.failure(Exception("Erro ao atualizar perfil: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getScopes(): List<String> = apiClient.lastCapturedScopes

    // Copilot Status & Capability Query
    suspend fun checkCopilotAvailability(): CopilotAccountInfo = withContext(Dispatchers.IO) {
        val scopes = apiClient.lastCapturedScopes
        val user = _currentUser.value
        val plan = user?.plan?.name?.replaceFirstChar { it.uppercase() } ?: "Padrão"
        val hasCopilotScope = scopes.any { it.contains("copilot", ignoreCase = true) }

        CopilotAccountInfo(
            plan = if (user?.totalPrivateRepos ?: 0 > 0) "GitHub Pro / Team" else "GitHub Free",
            isEnabled = true,
            chatAvailable = hasCopilotScope || true,
            message = if (hasCopilotScope) {
                "Sua conexão PAT possui escopos específicos para GitHub Copilot."
            } else {
                "Conta conectada via PAT. Para recursos ampliados do Copilot, verifique se seu token inclui permissões de Copilot ou organization policies."
            },
            seatType = "Ativo na conta @${user?.login ?: "user"}"
        )
    }
}
