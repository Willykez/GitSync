package com.willykez.repomaster.data.github

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** One row in the Discover list — enough to display and clone, nothing more. */
data class GitHubRepoSummary(
    val fullName: String,
    val description: String?,
    val cloneUrl: String,
    val stars: Int,
    val defaultBranch: String,
    val private: Boolean,
)

sealed class GitHubResult<out T> {
    data class Success<T>(val data: T) : GitHubResult<T>()
    data class Error(val message: String) : GitHubResult<Nothing>()
}

/**
 * Minimal GitHub REST client — just enough to search public repos and list a signed-in
 * user's own repos, so cloning can be driven from inside the app instead of copying a URL
 * off github.com in a browser. Deliberately built on HttpURLConnection + org.json (both
 * already part of the Android platform) rather than adding Retrofit/OkHttp/Gson for what's
 * really just two GET endpoints.
 *
 * Search works with no token (GitHub's public search API is unauthenticated, just rate
 * limited — 10 requests/minute per IP). "My repos" needs a token, since it's inherently
 * about a specific account: pass any saved credential's PAT and it works the same way it
 * already does for git push/pull (needs at least the "repo" scope to list private repos).
 */
object GitHubApi {
    private const val BASE = "https://api.github.com"

    suspend fun searchRepos(query: String, token: String? = null): GitHubResult<List<GitHubRepoSummary>> {
        if (query.isBlank()) return GitHubResult.Success(emptyList())
        val url = "$BASE/search/repositories?q=${URLEncoder.encode(query, "UTF-8")}&per_page=30"
        return when (val r = http(url, "GET", token)) {
            is GitHubResult.Error -> r
            is GitHubResult.Success -> try {
                val json = JSONObject(r.data)
                GitHubResult.Success(parseRepoArray(json.getJSONArray("items")))
            } catch (e: Exception) {
                GitHubResult.Error("Couldn't read GitHub's response: ${e.message}")
            }
        }
    }

    suspend fun listMyRepos(token: String): GitHubResult<List<GitHubRepoSummary>> {
        val url = "$BASE/user/repos?per_page=50&sort=updated"
        return when (val r = http(url, "GET", token)) {
            is GitHubResult.Error -> r
            is GitHubResult.Success -> try {
                GitHubResult.Success(parseRepoArray(JSONArray(r.data)))
            } catch (e: Exception) {
                GitHubResult.Error("Couldn't read GitHub's response: ${e.message}")
            }
        }
    }

    /**
     * Creates a new repo under the token's own account. Needs a PAT with the "repo" scope
     * (classic) or "Administration: write" (fine-grained). GitHub's create-repo endpoint
     * always acts on the authenticated user's account — there's no separate "as org" call
     * here, matching what a person can do from github.com's own "New repository" button.
     */
    suspend fun createRepo(
        name: String, description: String = "", private: Boolean = true, token: String,
    ): GitHubResult<GitHubRepoSummary> {
        val body = JSONObject().apply {
            put("name", name)
            if (description.isNotBlank()) put("description", description)
            put("private", private)
            put("auto_init", true) // creates an initial commit so the repo is clonable right away
        }
        return when (val r = http("$BASE/user/repos", "POST", token, body.toString())) {
            is GitHubResult.Error -> r
            is GitHubResult.Success -> try {
                GitHubResult.Success(parseRepoObject(JSONObject(r.data)))
            } catch (e: Exception) {
                GitHubResult.Error("Couldn't read GitHub's response: ${e.message}")
            }
        }
    }

    /**
     * Permanently deletes a repo from GitHub. Needs a PAT with the "delete_repo" scope
     * (classic) — GitHub rejects this call with a 403 for tokens that only have "repo",
     * same as it would from the API docs' own example, so the error message from [http]
     * surfaces that directly rather than this function guessing at scopes up front.
     */
    suspend fun deleteRepo(fullName: String, token: String): GitHubResult<Unit> {
        return when (val r = http("$BASE/repos/$fullName", "DELETE", token)) {
            is GitHubResult.Error -> r
            is GitHubResult.Success -> GitHubResult.Success(Unit)
        }
    }

    private fun parseRepoArray(arr: JSONArray): List<GitHubRepoSummary> =
        (0 until arr.length()).map { i -> parseRepoObject(arr.getJSONObject(i)) }

    private fun parseRepoObject(o: JSONObject): GitHubRepoSummary = GitHubRepoSummary(
        fullName = o.getString("full_name"),
        description = if (o.isNull("description")) null else o.optString("description").ifBlank { null },
        cloneUrl = o.getString("clone_url"),
        stars = o.optInt("stargazers_count", 0),
        defaultBranch = o.optString("default_branch", "main"),
        private = o.optBoolean("private", false),
    )

    private suspend fun http(
        urlStr: String, method: String, token: String?, jsonBody: String? = null,
    ): GitHubResult<String> =
        withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 10_000
                    readTimeout = 10_000
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
                    if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
                    if (jsonBody != null) {
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        outputStream.use { it.write(jsonBody.toByteArray(Charsets.UTF_8)) }
                    }
                }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val body = stream?.bufferedReader()?.use { it.readText() } ?: ""

                if (code !in 200..299) {
                    val msg = try {
                        JSONObject(body).optString("message", "GitHub returned HTTP $code")
                    } catch (e: Exception) {
                        "GitHub returned HTTP $code"
                    }
                    GitHubResult.Error(msg)
                } else {
                    GitHubResult.Success(body)
                }
            } catch (e: Exception) {
                GitHubResult.Error(e.message ?: "Network error reaching GitHub")
            } finally {
                conn?.disconnect()
            }
        }
}
