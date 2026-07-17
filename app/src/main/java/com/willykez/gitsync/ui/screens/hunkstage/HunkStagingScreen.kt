package com.willykez.gitsync.ui.screens.hunkstage

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.willykez.gitsync.App
import com.willykez.gitsync.git.GitEngine
import com.willykez.gitsync.git.GitResult
import com.willykez.gitsync.ui.theme.StatusAdded
import com.willykez.gitsync.ui.theme.StatusClean
import com.willykez.gitsync.ui.theme.StatusDeleted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** UI-side selection state for one hunk — mirrors GitEngine.HunkSelection but keyed by
 * mutable Compose state so checkboxes can toggle individual lines. Starts with every line
 * selected (i.e. "stage the whole hunk"), matching what tapping the file's own Stage action
 * would have done, so unchecking is the only action needed for the common "leave this one
 * line out" case. */
private class HunkUiState(hunk: GitEngine.DiffHunk) {
    val editIndex = hunk.editIndex
    val header = hunk.header
    val lines = hunk.lines
    val removedSelected = mutableStateMapOf<Int, Boolean>().apply {
        lines.filter { it.type == GitEngine.HunkLineType.REMOVED }.forEach { put(it.localIndex, true) }
    }
    val addedSelected = mutableStateMapOf<Int, Boolean>().apply {
        lines.filter { it.type == GitEngine.HunkLineType.ADDED }.forEach { put(it.localIndex, true) }
    }
    var hunkSelected by mutableStateOf(true) // convenience toggle for "select/deselect all lines in this hunk"
}

data class HunkStagingUiState(
    val path: String = "",
    val hunks: List<GitEngine.DiffHunk> = emptyList(),
    val isLoading: Boolean = true,
    val isWorking: Boolean = false,
    val message: String? = null,
    val done: Boolean = false,
)

class HunkStagingViewModel(app: Application) : AndroidViewModel(app) {
    private val repoRepo = (app as App).repoRepository
    private val _state = MutableStateFlow(HunkStagingUiState())
    val state: StateFlow<HunkStagingUiState> = _state.asStateFlow()

    private var repoId: Long = -1
    private var path: String = ""

    fun load(repoId: Long, path: String) {
        this.repoId = repoId
        this.path = path
        _state.value = _state.value.copy(path = path, isLoading = true)
        viewModelScope.launch {
            val repo = repoRepo.getById(repoId) ?: return@launch
            when (val gr = GitEngine.openRepo(repo.fullSavePath)) {
                is GitResult.Error -> _state.value = _state.value.copy(isLoading = false, message = gr.message)
                is GitResult.Success -> {
                    val g = gr.data
                    when (val hr = GitEngine.getWorkingVsIndexHunks(g, path)) {
                        is GitResult.Error -> _state.value = _state.value.copy(isLoading = false, message = hr.message)
                        is GitResult.Success -> _state.value = _state.value.copy(hunks = hr.data, isLoading = false)
                    }
                    g.close()
                }
            }
        }
    }

    fun stageSelected(selections: Map<Int, GitEngine.HunkSelection>) {
        viewModelScope.launch {
            val repo = repoRepo.getById(repoId) ?: return@launch
            _state.value = _state.value.copy(isWorking = true)
            when (val gr = GitEngine.openRepo(repo.fullSavePath)) {
                is GitResult.Error -> _state.value = _state.value.copy(isWorking = false, message = gr.message)
                is GitResult.Success -> {
                    val g = gr.data
                    when (val r = GitEngine.stageHunkSelections(g, path, selections)) {
                        is GitResult.Error -> _state.value = _state.value.copy(isWorking = false, message = r.message)
                        is GitResult.Success -> _state.value = _state.value.copy(isWorking = false, done = true, message = "Staged selected hunks")
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
fun HunkStagingScreen(
    repoId: Long, path: String,
    onBack: () -> Unit,
    vm: HunkStagingViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    val snack = remember { SnackbarHostState() }
    LaunchedEffect(repoId, path) { vm.load(repoId, path) }
    LaunchedEffect(state.message) { state.message?.let { snack.showSnackbar(it); vm.dismiss() } }
    LaunchedEffect(state.done) { if (state.done) onBack() }

    // Rebuilt whenever the loaded hunks change, so selection state resets cleanly per file.
    val hunkStates = remember(state.hunks) { state.hunks.map { HunkUiState(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.path.substringAfterLast('/'), fontWeight = FontWeight.SemiBold, maxLines = 1, fontSize = 15.sp) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface, titleContentColor = MaterialTheme.colorScheme.onSurface, navigationIconContentColor = MaterialTheme.colorScheme.onSurface),
            )
        },
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            if (hunkStates.isNotEmpty()) {
                Surface(tonalElevation = 4.dp) {
                    Button(
                        onClick = {
                            val selections = hunkStates.associate { hs ->
                                hs.editIndex to GitEngine.HunkSelection(
                                    removedLocalIndices = hs.removedSelected.filterValues { it }.keys,
                                    addedLocalIndices = hs.addedSelected.filterValues { it }.keys,
                                )
                            }
                            vm.stageSelected(selections)
                        },
                        enabled = !state.isWorking,
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                    ) {
                        if (state.isWorking) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else { Icon(Icons.Filled.Check, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text("Stage Selected Lines") }
                    }
                }
            }
        },
    ) { pad ->
        when {
            state.isLoading -> Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            hunkStates.isEmpty() -> Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("No unstaged changes in this file", color = StatusClean)
            }
            else -> LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(hunkStates) { hs -> HunkCard(hs) }
            }
        }
    }
}

@Composable
private fun HunkCard(hs: HunkUiState) {
    Card(shape = RoundedCornerShape(10.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(hs.header, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = {
                    val newValue = !hs.hunkSelected
                    hs.hunkSelected = newValue
                    hs.removedSelected.keys.forEach { hs.removedSelected[it] = newValue }
                    hs.addedSelected.keys.forEach { hs.addedSelected[it] = newValue }
                }) { Text(if (hs.hunkSelected) "Deselect all" else "Select all", fontSize = 11.sp) }
            }
            val hScroll = rememberScrollState()
            Column(Modifier.horizontalScroll(hScroll)) {
                hs.lines.forEach { line ->
                    HunkLineRow(line, hs)
                }
            }
        }
    }
}

@Composable
private fun HunkLineRow(line: GitEngine.DiffHunkLine, hs: HunkUiState) {
    val bg = when (line.type) {
        GitEngine.HunkLineType.ADDED -> Color(0xFF1A3A1A)
        GitEngine.HunkLineType.REMOVED -> Color(0xFF3A1A1A)
        GitEngine.HunkLineType.CONTEXT -> Color.Transparent
    }
    val fg = when (line.type) {
        GitEngine.HunkLineType.ADDED -> StatusAdded
        GitEngine.HunkLineType.REMOVED -> StatusDeleted
        GitEngine.HunkLineType.CONTEXT -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        Modifier.fillMaxWidth().background(bg).padding(horizontal = 4.dp, vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (line.type != GitEngine.HunkLineType.CONTEXT) {
            val checked = when (line.type) {
                GitEngine.HunkLineType.REMOVED -> hs.removedSelected[line.localIndex] ?: true
                GitEngine.HunkLineType.ADDED -> hs.addedSelected[line.localIndex] ?: true
                else -> true
            }
            Checkbox(
                checked = checked,
                onCheckedChange = { v ->
                    when (line.type) {
                        GitEngine.HunkLineType.REMOVED -> hs.removedSelected[line.localIndex] = v
                        GitEngine.HunkLineType.ADDED -> hs.addedSelected[line.localIndex] = v
                        else -> {}
                    }
                },
                modifier = Modifier.size(28.dp),
            )
        } else {
            Spacer(Modifier.width(28.dp))
        }
        Text(
            text = line.text,
            color = fg,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            softWrap = false,
        )
    }
}
