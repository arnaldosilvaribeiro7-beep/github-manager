package com.example.data.api

import com.example.data.model.*
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface GitHubApiService {

    // User & Account
    @GET("user")
    suspend fun getAuthenticatedUser(): Response<GitHubUser>

    @PATCH("user")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): Response<GitHubUser>

    @GET("user/orgs")
    suspend fun getUserOrgs(): Response<List<GitHubOrg>>

    // Repositories
    @GET("user/repos")
    suspend fun getUserRepos(
        @Query("sort") sort: String = "updated",
        @Query("direction") direction: String = "desc",
        @Query("per_page") perPage: Int = 100,
        @Query("type") type: String = "all"
    ): Response<List<GitHubRepo>>

    @GET("repos/{owner}/{repo}")
    suspend fun getRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GitHubRepo>

    @POST("user/repos")
    suspend fun createRepo(@Body body: CreateRepoRequest): Response<GitHubRepo>

    @PATCH("repos/{owner}/{repo}")
    suspend fun updateRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: UpdateRepoRequest
    ): Response<GitHubRepo>

    @DELETE("repos/{owner}/{repo}")
    suspend fun deleteRepo(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<Unit>

    // Contents & Code
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getRepoContents(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String = "",
        @Query("ref") ref: String? = null
    ): Response<ResponseBody>

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun createOrUpdateFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Body body: CreateFileRequest
    ): Response<ResponseBody>

    @HTTP(method = "DELETE", path = "repos/{owner}/{repo}/contents/{path}", hasBody = true)
    suspend fun deleteFile(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "path", encoded = true) path: String,
        @Body body: DeleteFileRequest
    ): Response<ResponseBody>

    @GET
    suspend fun getRawFile(@Url url: String): Response<ResponseBody>

    // Commits, Branches, Releases
    @GET("repos/{owner}/{repo}/commits")
    suspend fun getRepoCommits(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("sha") sha: String? = null,
        @Query("per_page") perPage: Int = 40
    ): Response<List<GitHubCommit>>

    @GET("repos/{owner}/{repo}/branches")
    suspend fun getRepoBranches(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<List<GitHubBranch>>

    @GET("repos/{owner}/{repo}/releases")
    suspend fun getRepoReleases(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<List<GitHubRelease>>

    // Issues
    @GET("repos/{owner}/{repo}/issues")
    suspend fun getRepoIssues(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "all",
        @Query("per_page") perPage: Int = 50
    ): Response<List<GitHubIssue>>

    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateIssueRequest
    ): Response<GitHubIssue>

    @PATCH("repos/{owner}/{repo}/issues/{issue_number}")
    suspend fun updateIssue(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Int,
        @Body body: UpdateIssueRequest
    ): Response<GitHubIssue>

    @GET("repos/{owner}/{repo}/issues/{issue_number}/comments")
    suspend fun getIssueComments(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Int
    ): Response<List<GitHubComment>>

    @POST("repos/{owner}/{repo}/issues/{issue_number}/comments")
    suspend fun createIssueComment(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("issue_number") issueNumber: Int,
        @Body body: CreateCommentRequest
    ): Response<GitHubComment>

    // Pull Requests
    @GET("repos/{owner}/{repo}/pulls")
    suspend fun getRepoPullRequests(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "all",
        @Query("per_page") perPage: Int = 50
    ): Response<List<GitHubPullRequest>>

    @POST("repos/{owner}/{repo}/pulls")
    suspend fun createPullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreatePRRequest
    ): Response<GitHubPullRequest>

    @PUT("repos/{owner}/{repo}/pulls/{pull_number}/merge")
    suspend fun mergePullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int,
        @Body body: MergePRRequest
    ): Response<Unit>

    @PATCH("repos/{owner}/{repo}/pulls/{pull_number}")
    suspend fun updatePullRequest(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("pull_number") pullNumber: Int,
        @Body body: UpdateIssueRequest
    ): Response<GitHubPullRequest>

    // Actions & Workflows
    @GET("repos/{owner}/{repo}/actions/workflows")
    suspend fun getRepoWorkflows(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Response<GitHubWorkflowsResponse>

    @GET("repos/{owner}/{repo}/actions/runs")
    suspend fun getRepoWorkflowRuns(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("per_page") perPage: Int = 30
    ): Response<GitHubWorkflowRunsResponse>

    @GET("repos/{owner}/{repo}/actions/runs/{run_id}/jobs")
    suspend fun getWorkflowRunJobs(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): Response<GitHubJobsResponse>

    @POST("repos/{owner}/{repo}/actions/runs/{run_id}/cancel")
    suspend fun cancelWorkflowRun(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): Response<Unit>

    @POST("repos/{owner}/{repo}/actions/runs/{run_id}/rerun")
    suspend fun rerunWorkflowRun(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("run_id") runId: Long
    ): Response<Unit>

    @POST("repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches")
    suspend fun dispatchWorkflow(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("workflow_id") workflowId: Long,
        @Body body: WorkflowDispatchRequest
    ): Response<Unit>

    // Git Data API (Blobs, Trees, Commits, Refs) for ZIP import
    @POST("repos/{owner}/{repo}/git/blobs")
    suspend fun createGitBlob(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: GitBlobRequest
    ): Response<GitBlobResponse>

    @POST("repos/{owner}/{repo}/git/trees")
    suspend fun createGitTree(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: GitTreeRequest
    ): Response<GitTreeResponse>

    @POST("repos/{owner}/{repo}/git/commits")
    suspend fun createGitCommit(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: GitCommitRequest
    ): Response<GitCommitResponse>

    @PATCH("repos/{owner}/{repo}/git/refs/{ref}")
    suspend fun updateGitRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "ref", encoded = true) ref: String,
        @Body body: GitRefRequest
    ): Response<GitRefResponse>

    @GET("repos/{owner}/{repo}/git/ref/{ref}")
    suspend fun getGitRef(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path(value = "ref", encoded = true) ref: String
    ): Response<GitRefResponse>

    // ZIP Export
    @Streaming
    @GET("repos/{owner}/{repo}/zipball/{ref}")
    suspend fun downloadRepoZip(
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("ref") ref: String
    ): Response<ResponseBody>
}
