package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.util.Base64
import com.example.data.api.GitHubApiClient
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

data class ZipEntryInfo(
    val path: String,
    val size: Long,
    val isDirectory: Boolean
)

data class ZipInspectionResult(
    val totalFiles: Int,
    val totalBytes: Long,
    val entries: List<ZipEntryInfo>
)

sealed class ZipUploadProgress {
    data class Status(val message: String) : ZipUploadProgress()
    data class FileProgress(val current: Int, val total: Int, val fileName: String) : ZipUploadProgress()
    data class Success(val repo: GitHubRepo) : ZipUploadProgress()
    data class Error(val error: String) : ZipUploadProgress()
}

class ZipManager(
    private val context: Context,
    private val apiClient: GitHubApiClient
) {
    suspend fun inspectZip(uri: Uri): Result<ZipInspectionResult> = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Não foi possível abrir o arquivo ZIP selecionado."))

            val entries = mutableListOf<ZipEntryInfo>()
            var totalBytes = 0L
            var totalFiles = 0

            ZipInputStream(inputStream).use { zis ->
                var entry: ZipEntry? = zis.nextEntry
                while (entry != null) {
                    val name = entry.name
                    // Ignore internal macOS / git files
                    if (!name.startsWith("__MACOSX") && !name.contains(".DS_Store") && !name.startsWith(".git/")) {
                        val isDir = entry.isDirectory
                        val size = if (entry.size >= 0) entry.size else 0L
                        entries.add(ZipEntryInfo(path = name, size = size, isDirectory = isDir))
                        if (!isDir) {
                            totalFiles++
                            totalBytes += size
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }

            if (totalFiles == 0) {
                return@withContext Result.failure(Exception("O arquivo ZIP está vazio ou não contém arquivos válidos."))
            }

            Result.success(ZipInspectionResult(totalFiles, totalBytes, entries))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun uploadZipToNewRepo(
        uri: Uri,
        repoName: String,
        repoDescription: String?,
        isPrivate: Boolean
    ): Flow<ZipUploadProgress> = flow {
        emit(ZipUploadProgress.Status("Criando novo repositório @$repoName no GitHub..."))

        // 1. Create repository
        val createRepoReq = CreateRepoRequest(
            name = repoName,
            description = repoDescription,
            `private` = isPrivate,
            autoInit = true
        )
        val createResp = apiClient.service.createRepo(createRepoReq)
        if (!createResp.isSuccessful || createResp.body() == null) {
            emit(ZipUploadProgress.Error("Falha ao criar repositório: HTTP ${createResp.code()}"))
            return@flow
        }
        val repo = createResp.body()!!
        val owner = repo.owner?.login ?: ""
        val branch = repo.defaultBranch.ifBlank { "main" }

        emit(ZipUploadProgress.Status("Obtendo referência inicial da branch $branch..."))

        // 2. Fetch head ref sha (poll slightly if newly initialized)
        var headSha: String? = null
        for (attempt in 1..4) {
            try {
                kotlinx.coroutines.delay(1000)
                val refResp = apiClient.service.getGitRef(owner, repoName, "heads/$branch")
                if (refResp.isSuccessful && refResp.body()?.obj?.sha != null) {
                    headSha = refResp.body()!!.obj!!.sha
                    break
                }
            } catch (_: Exception) {}
        }

        // 3. Extract and upload Blobs via Git Data API
        emit(ZipUploadProgress.Status("Preparando arquivos para envio..."))
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream == null) {
            emit(ZipUploadProgress.Error("Falha ao reabrir arquivo ZIP."))
            return@flow
        }

        val treeItems = mutableListOf<GitTreeItem>()
        var processedCount = 0

        // Count non-directory entries
        val validEntries = mutableListOf<Pair<String, ByteArray>>()
        ZipInputStream(inputStream).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                if (!entry.isDirectory && !name.startsWith("__MACOSX") && !name.contains(".DS_Store") && !name.startsWith(".git/")) {
                    val buffer = ByteArray(8192)
                    val baos = ByteArrayOutputStream()
                    var read: Int
                    while (zis.read(buffer).also { read = it } != -1) {
                        baos.write(buffer, 0, read)
                    }
                    validEntries.add(name to baos.toByteArray())
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val total = validEntries.size
        for ((path, bytes) in validEntries) {
            processedCount++
            emit(ZipUploadProgress.FileProgress(processedCount, total, path))

            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val blobResp = apiClient.service.createGitBlob(owner, repoName, GitBlobRequest(content = base64))
            if (blobResp.isSuccessful && blobResp.body() != null) {
                val sha = blobResp.body()!!.sha
                treeItems.add(
                    GitTreeItem(
                        path = path.trimStart('/'),
                        mode = "100644",
                        type = "blob",
                        sha = sha
                    )
                )
            } else {
                emit(ZipUploadProgress.Status("Aviso: Falha ao enviar blob para $path"))
            }
        }

        if (treeItems.isEmpty()) {
            emit(ZipUploadProgress.Error("Nenhum arquivo pôde ser enviado."))
            return@flow
        }

        emit(ZipUploadProgress.Status("Construindo árvore de arquivos (Git Tree)..."))
        val treeReq = GitTreeRequest(baseTree = headSha, tree = treeItems)
        val treeResp = apiClient.service.createGitTree(owner, repoName, treeReq)
        if (!treeResp.isSuccessful || treeResp.body() == null) {
            emit(ZipUploadProgress.Error("Falha ao criar Git Tree: HTTP ${treeResp.code()}"))
            return@flow
        }
        val newTreeSha = treeResp.body()!!.sha

        emit(ZipUploadProgress.Status("Criando commit no GitHub..."))
        val parents = if (headSha != null) listOf(headSha) else emptyList()
        val commitReq = GitCommitRequest(
            message = "Importação de projeto via ZIP (GitHub Manager)",
            tree = newTreeSha,
            parents = parents
        )
        val commitResp = apiClient.service.createGitCommit(owner, repoName, commitReq)
        if (!commitResp.isSuccessful || commitResp.body() == null) {
            emit(ZipUploadProgress.Error("Falha ao criar commit: HTTP ${commitResp.code()}"))
            return@flow
        }
        val newCommitSha = commitResp.body()!!.sha

        emit(ZipUploadProgress.Status("Atualizando referência da branch $branch..."))
        val refReq = GitRefRequest(sha = newCommitSha, force = true)
        val updateRefResp = apiClient.service.updateGitRef(owner, repoName, "heads/$branch", refReq)
        if (!updateRefResp.isSuccessful) {
            emit(ZipUploadProgress.Error("Falha ao atualizar branch: HTTP ${updateRefResp.code()}"))
            return@flow
        }

        emit(ZipUploadProgress.Success(repo))
    }

    suspend fun exportRepoZip(
        owner: String,
        repo: String,
        ref: String
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val response = apiClient.service.downloadRepoZip(owner, repo, ref)
            if (!response.isSuccessful || response.body() == null) {
                return@withContext Result.failure(Exception("Falha ao baixar ZIP: HTTP ${response.code()}"))
            }

            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val outputFile = File(exportDir, "${repo}-${ref}.zip")

            val body = response.body()!!
            body.byteStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    input.copyTo(output)
                }
            }

            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
