package com.willykez.gitsync.ui.screens.search

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.willykez.gitsync.App
import com.willykez.gitsync.git.CommitInfo
import com.willykez.gitsync.git.GitEngine
import com.willykez.gitsync.git.GitResult
import com.willykez.gitsync.git.SearchMatch
import com.willykez.gitsync.ui.theme.StatusClean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SearchMode { FILES, COMMITS }

data class SearchUiState(
    val query: String = "",
    val mode: SearchMode = SearchMode.FILES,
    val fileResults: List<SearchMatch> = emptyList(),
    val commitResults: List<CommitInfo> = emptyList(),
    val isSearching: Boolean = false,
    val message: String? = null,
    val hasSearched: Boolean = false,
)

class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val repoRepo = (app as App).repoRepository
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var repoId: Long = -1
    private var searchJob: kotlinx.coroutines.Job? = null

    fun init(repoId: Long) { this.repoId = repoId }

    fun onQueryChanged(q: String) { _state.value = _state.value.copy(query = q) }

    fun onModeChanged(m: SearchMode) {
        _state.value = _state.value.copy(mode = m)
        if (_state.value.query.isNotBlank()) runSearch()
    }

    fun runSearch() {
        val query = _state.value.query
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _state.value = _state.value.copy(isSearching = true, hasSearched = true)
            val repo = repoRepo.getById(repoId) ?: return@launch
            when (val gr = GitEngine.openRepo(repo.fullSavePath)) {
                is GitResult.Error -> _state.value = _state.value.copy(isSearching = false, message = gr.message)
                is GitResult.Success -> {
                    val g = gr.data
                    when (_state.value.mode) {
                        SearchMode.FILES -> when (val r = GitEngine.searchWorkingTree(g, query)) {
                            is GitResult.Error -> _state.value = _state.value.copy(isSearching = false, message = r.message)
                            is GitResult.Success -> _state.value = _state.value.copy(isSearching = false, fileResults = r.data)
                        }
                        SearchMode.COMMITS -> when (val r = GitEngine.searchCommitMessages(g, query)) {
                            is GitResult.Error -> _state.value = _state.value.copy(isSearching = false, message = r.message)
                            is GitResult.Success -> _state.value = _state.value.copy(isSearching = false, commitResults = r.data)
                        }
                    }
                    g.close()
                }
            }
        }
    }

    fun dismiss() { _state.value = _state.value.copy(message = null) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    repoId: Long,
    onBack: () -> Unit,
    onOpenFile: (path: String) -> Unit,
    onOpenCommitDiff: (sha: String) -> Unit,
    vm: SearchViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    val snack = remember { SnackbarHostState() }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(repoId) { vm.init(repoId) }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(state.message) { state.message?.let { snack.showSnackbar(it); vm.dismiss() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = vm::onQueryChanged,
                        placeholder = { Text(if (state.mode == SearchMode.FILES) "Search files…" else "Search commit messages…") },
                        singleLine = true,
                        trailingIcon = {
                            if (state.query.isNotEmpty()) {
                                IconButton(onClick = { vm.onQueryChanged("") }) { Icon(Icons.Filled.Close, "Clear") }
                            }
                        },
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { vm.runSearch() }),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                actions = { IconButton(onClick = vm::runSearch) { Icon(Icons.Filled.Search, "Search") } },
            )
        },
        snackbarHost = { SnackbarHost(snack) },
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(pad)) {
            TabRow(selectedTabIndex = if (state.mode == SearchMode.FILES) 0 else 1) {
                Tab(selected = state.mode == SearchMode.FILES, onClick = { vm.onModeChanged(SearchMode.FILES) },
                    text = { Text("Files") })
                Tab(selected = state.mode == SearchMode.COMMITS, onClick = { vm.onModeChanged(SearchMode.COMMITS) },
                    text = { Text("Commit messages") })
            }
            when {
                state.isSearching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                !state.hasSearched -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Search across every file in the working tree, or commit history", color = StatusClean, fontSize = 13.sp)
                }
                state.mode == SearchMode.FILES && state.fileResults.isEmpty() -> EmptyResults()
                state.mode == SearchMode.COMMITS && state.commitResults.isEmpty() -> EmptyResults()
                state.mode == SearchMode.FILES -> LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.fileResults) { m -> FileMatchRow(m) { onOpenFile(m.path) } }
                }
                else -> LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(state.commitResults) { c -> CommitMatchRow(c) { onOpenCommitDiff(c.sha) } }
                }
            }
        }
    }
}

@Composable
private fun EmptyResults() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No matches", color = StatusClean) }
}

@Composable
private fun FileMatchRow(m: SearchMatch, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(m.path, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
            Text("Line ${m.lineNumber}", fontSize = 10.sp, color = StatusClean)
            Text(m.lineText, fontFamily = FontFamily.Monospace, fontSize = 11.sp, maxLines = 1)
        }
    }
}

@Composable
private fun CommitMatchRow(c: CommitInfo, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(c.message, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1)
            Text("${c.shortSha} · ${c.authorName}", fontSize = 10.sp, color = StatusClean)
        }
    }
}
