package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubUser(
    val id: Long = 0,
    val login: String = "",
    val name: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val bio: String? = null,
    val company: String? = null,
    val blog: String? = null,
    val location: String? = null,
    val email: String? = null,
    @Json(name = "public_repos") val publicRepos: Int = 0,
    @Json(name = "total_private_repos") val totalPrivateRepos: Int = 0,
    val followers: Int = 0,
    val following: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
    val plan: GitHubPlan? = null
)

@JsonClass(generateAdapter = true)
data class GitHubPlan(
    val name: String? = null,
    val space: Long = 0,
    val collaborators: Int = 0,
    @Json(name = "private_repos") val privateRepos: Int = 0
)

@JsonClass(generateAdapter = true)
data class GitHubRepo(
    val id: Long = 0,
    val name: String = "",
    @Json(name = "full_name") val fullName: String = "",
    val private: Boolean = false,
    val description: String? = null,
    val fork: Boolean = false,
    val url: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "default_branch") val defaultBranch: String = "main",
    @Json(name = "stargazers_count") val stargazersCount: Int = 0,
    @Json(name = "forks_count") val forksCount: Int = 0,
    @Json(name = "open_issues_count") val openIssuesCount: Int = 0,
    val language: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    val archived: Boolean = false,
    val visibility: String? = null,
    val owner: GitHubUser? = null
)

@JsonClass(generateAdapter = true)
data class GitHubCommit(
    val sha: String = "",
    val commit: CommitDetail? = null,
    val author: CommitUser? = null,
    @Json(name = "html_url") val htmlUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class CommitDetail(
    val message: String = "",
    val author: CommitAuthor? = null
)

@JsonClass(generateAdapter = true)
data class CommitAuthor(
    val name: String? = null,
    val email: String? = null,
    val date: String? = null
)

@JsonClass(generateAdapter = true)
data class CommitUser(
    val login: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubBranch(
    val name: String = "",
    val commit: BranchCommit? = null,
    val `protected`: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class BranchCommit(
    val sha: String = "",
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubIssue(
    val id: Long = 0,
    val number: Int = 0,
    val title: String = "",
    val body: String? = null,
    val state: String = "open",
    val user: GitHubUser? = null,
    val comments: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    val labels: List<GitHubLabel> = emptyList(),
    @Json(name = "pull_request") val pullRequest: Any? = null,
    @Json(name = "html_url") val htmlUrl: String? = null
) {
    val isPullRequest: Boolean get() = pullRequest != null
}

@JsonClass(generateAdapter = true)
data class GitHubLabel(
    val id: Long? = null,
    val name: String = "",
    val color: String? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubPullRequest(
    val id: Long = 0,
    val number: Int = 0,
    val title: String = "",
    val body: String? = null,
    val state: String = "open",
    val user: GitHubUser? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "diff_url") val diffUrl: String? = null,
    val head: PRBranch? = null,
    val base: PRBranch? = null,
    val mergeable: Boolean? = null,
    val merged: Boolean? = false
)

@JsonClass(generateAdapter = true)
data class PRBranch(
    val label: String? = null,
    val ref: String? = null,
    val sha: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflowsResponse(
    @Json(name = "total_count") val totalCount: Int = 0,
    val workflows: List<GitHubWorkflow> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflow(
    val id: Long = 0,
    val name: String = "",
    val path: String = "",
    val state: String = "active"
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflowRunsResponse(
    @Json(name = "total_count") val totalCount: Int = 0,
    @Json(name = "workflow_runs") val workflowRuns: List<GitHubWorkflowRun> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflowRun(
    val id: Long = 0,
    val name: String? = null,
    @Json(name = "head_branch") val headBranch: String? = null,
    @Json(name = "head_sha") val headSha: String? = null,
    val status: String? = null, // "queued", "in_progress", "completed"
    val conclusion: String? = null, // "success", "failure", "cancelled", etc.
    val event: String? = null,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
    @Json(name = "run_number") val runNumber: Int? = null,
    @Json(name = "workflow_id") val workflowId: Long? = null
)

@JsonClass(generateAdapter = true)
data class GitHubJobsResponse(
    @Json(name = "total_count") val totalCount: Int = 0,
    val jobs: List<GitHubJob> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubJob(
    val id: Long = 0,
    @Json(name = "run_id") val runId: Long? = null,
    val name: String = "",
    val status: String? = null,
    val conclusion: String? = null,
    @Json(name = "started_at") val startedAt: String? = null,
    @Json(name = "completed_at") val completedAt: String? = null,
    val steps: List<GitHubJobStep> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitHubJobStep(
    val name: String = "",
    val status: String? = null,
    val conclusion: String? = null,
    val number: Int = 0
)

@JsonClass(generateAdapter = true)
data class GitHubContent(
    val type: String = "file", // "file" or "dir"
    val name: String = "",
    val path: String = "",
    val sha: String? = null,
    val size: Long? = null,
    val content: String? = null,
    val encoding: String? = null,
    @Json(name = "download_url") val downloadUrl: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubRelease(
    val id: Long = 0,
    @Json(name = "tag_name") val tagName: String = "",
    val name: String? = null,
    val body: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "published_at") val publishedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubOrg(
    val id: Long = 0,
    val login: String = "",
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class GitHubComment(
    val id: Long = 0,
    val body: String = "",
    val user: GitHubUser? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

// Request payloads
@JsonClass(generateAdapter = true)
data class CreateRepoRequest(
    val name: String,
    val description: String? = null,
    val `private`: Boolean = false,
    @Json(name = "auto_init") val autoInit: Boolean = true
)

@JsonClass(generateAdapter = true)
data class UpdateRepoRequest(
    val name: String? = null,
    val description: String? = null,
    val `private`: Boolean? = null,
    val archived: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class CreateFileRequest(
    val message: String,
    val content: String, // Base64 encoded
    val branch: String? = null,
    val sha: String? = null
)

@JsonClass(generateAdapter = true)
data class DeleteFileRequest(
    val message: String,
    val sha: String,
    val branch: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateIssueRequest(
    val title: String,
    val body: String? = null,
    val labels: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class UpdateIssueRequest(
    val title: String? = null,
    val body: String? = null,
    val state: String? = null
)

@JsonClass(generateAdapter = true)
data class CreatePRRequest(
    val title: String,
    val head: String,
    val base: String,
    val body: String? = null
)

@JsonClass(generateAdapter = true)
data class MergePRRequest(
    @Json(name = "commit_title") val commitTitle: String? = null,
    @Json(name = "merge_method") val mergeMethod: String = "merge"
)

@JsonClass(generateAdapter = true)
data class CreateCommentRequest(
    val body: String
)

@JsonClass(generateAdapter = true)
data class UpdateProfileRequest(
    val name: String? = null,
    val bio: String? = null,
    val company: String? = null,
    val location: String? = null,
    val blog: String? = null
)

@JsonClass(generateAdapter = true)
data class WorkflowDispatchRequest(
    val ref: String,
    val inputs: Map<String, String>? = null
)

// Git Data API (Blobs, Trees, Commits, Refs) for ZIP import
@JsonClass(generateAdapter = true)
data class GitBlobRequest(
    val content: String,
    val encoding: String = "base64"
)

@JsonClass(generateAdapter = true)
data class GitBlobResponse(
    val sha: String = "",
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class GitTreeRequest(
    @Json(name = "base_tree") val baseTree: String? = null,
    val tree: List<GitTreeItem>
)

@JsonClass(generateAdapter = true)
data class GitTreeItem(
    val path: String,
    val mode: String = "100644", // 100644 for file, 100755 for executable
    val type: String = "blob",
    val sha: String
)

@JsonClass(generateAdapter = true)
data class GitTreeResponse(
    val sha: String = "",
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class GitCommitRequest(
    val message: String,
    val tree: String,
    val parents: List<String> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GitCommitResponse(
    val sha: String = "",
    val url: String? = null
)

@JsonClass(generateAdapter = true)
data class GitRefRequest(
    val sha: String,
    val force: Boolean = true
)

@JsonClass(generateAdapter = true)
data class GitRefResponse(
    val ref: String = "",
    @Json(name = "object") val obj: GitRefObject? = null
)

@JsonClass(generateAdapter = true)
data class GitRefObject(
    val sha: String = "",
    val type: String = ""
)

data class CopilotAccountInfo(
    val plan: String = "Indisponível",
    val isEnabled: Boolean = false,
    val chatAvailable: Boolean = false,
    val message: String = "Acesso verificado com sua conta GitHub.",
    val seatType: String? = null
)
