package com.willykez.repomaster.ui.screens.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.willykez.repomaster.App
import com.willykez.repomaster.data.db.entity.RepoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Files bigger than this are skipped rather than searched — same cap the editor uses for
 *  "too large to edit", so anything findable here is also a file you could actually open
 *  and jump into afterward. */
private const val MAX_SEARCHABLE_BYTES = 2 * 1024 * 1024 // 2 MB

/** Search stops (rather than hanging on a huge monorepo) once either cap is hit, whichever
 *  comes first — this is a "find where something lives," not an indexed project-wide search
 *  engine, so a bounded scan that returns fast beats an exhaustive one that doesn't. */
private const val MAX_RESULTS = 300
private const val MAX_FILES_SCANNED = 20_000

data class SearchMatch(
    val relativePath: String,
    val lineNumber: Int,
    val lineText: String,
    val matchStart: Int,
    val matchEnd: Int,
)

data class SearchUiState(
    val repo: RepoEntity? = null,
    val query: String = "",
    val caseSensitive: Boolean = false,
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val results: List<SearchMatch> = emptyList(),
    val truncated: Boolean = false,
    val message: String? = null,
)

/**
 * Full-text search across every text file in a repo's working copy — separate from the
 * editor's find-in-file (which only ever looked inside whatever single file was already
 * open). This is the "which file even has this string in it" question, answered by walking
 * the whole checkout on disk rather than one open buffer.
 */
class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val appRef = app as App
    private val repoRepo = appRef.repoRepository

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    fun load(repoId: Long) {
        viewModelScope.launch {
            val repo = repoRepo.getById(repoId) ?: return@launch
            _state.value = _state.value.copy(repo = repo)
        }
    }

    fun onQueryChanged(q: String) { _state.value = _state.value.copy(query = q) }
    fun onCaseSensitiveChanged(v: Boolean) { _state.value = _state.value.copy(caseSensitive = v) }

    fun search() {
        val repo = _state.value.repo ?: return
        val query = _state.value.query
        if (query.isBlank()) return

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true, hasSearched = true, results = emptyList(), truncated = false)
            val caseSensitive = _state.value.caseSensitive

            val (results, truncated) = withContext(Dispatchers.IO) {
                scan(File(repo.fullSavePath), query, caseSensitive)
            }

            _state.value = _state.value.copy(isSearching = false, results = results, truncated = truncated)
        }
    }

    fun cancelSearch() {
        searchJob?.cancel()
        _state.value = _state.value.copy(isSearching = false)
    }

    /** Walks the working copy depth-first, skipping `.git`, matching line-by-line in every
     *  text file it can safely read. Returns early — with [truncated] set — the moment either
     *  cap is hit, so a huge or deeply-nested repo can't turn this into a multi-minute scan. */
    private suspend fun scan(root: File, query: String, caseSensitive: Boolean): Pair<List<SearchMatch>, Boolean> {
        val results = mutableListOf<SearchMatch>()
        var filesScanned = 0
        var truncated = false

        root.walkTopDown()
            .onEnter { it.name != ".git" }
            .filter { it.isFile }
            .forEach fileLoop@{ file ->
                if (results.size >= MAX_RESULTS || filesScanned >= MAX_FILES_SCANNED) {
                    truncated = true
                    return@fileLoop
                }
                filesScanned++
                if (file.length() == 0L || file.length() > MAX_SEARCHABLE_BYTES) return@fileLoop

                val bytes = try { file.readBytes() } catch (e: Exception) { return@fileLoop }
                // Same crude-but-effective binary check as the editor: a NUL byte essentially
                // never shows up in real text, but is common in binary formats — skip those
                // rather than surfacing garbled matches from inside a compiled asset.
                if (bytes.contains(0)) return@fileLoop

                val text = String(bytes, Charsets.UTF_8)
                val relativePath = file.relativeTo(root).path

                text.lineSequence().forEachIndexed lineLoop@{ index, rawLine ->
                    if (results.size >= MAX_RESULTS) return@fileLoop
                    val matchIndex = rawLine.indexOf(query, ignoreCase = !caseSensitive)
                    if (matchIndex >= 0) {
                        // lineText is trimmed for display, so the match offsets need to shift
                        // left by however much leading whitespace got trimmed off.
                        val leadingTrim = rawLine.length - rawLine.trimStart().length
                        val start = (matchIndex - leadingTrim).coerceAtLeast(0)
                        results.add(
                            SearchMatch(
                                relativePath = relativePath,
                                lineNumber = index + 1,
                                lineText = rawLine.trim(),
                                matchStart = start,
                                matchEnd = start + query.length,
                            ),
                        )
                    }
                }
            }

        if (filesScanned >= MAX_FILES_SCANNED) truncated = true
        return results to truncated
    }

    fun dismissMessage() { _state.value = _state.value.copy(message = null) }
}
