package com.willykez.repomaster.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.willykez.repomaster.ui.components.EmptyTabState
import com.willykez.repomaster.ui.screens.branches.BranchesScreen
import com.willykez.repomaster.ui.screens.blame.BlameScreen
import com.willykez.repomaster.ui.screens.changes.ChangesScreen
import com.willykez.repomaster.ui.screens.conflicts.ConflictsScreen
import com.willykez.repomaster.ui.screens.credential.CredentialScreen
import com.willykez.repomaster.ui.screens.diff.DiffScreen
import com.willykez.repomaster.ui.screens.discover.DiscoverScreen
import com.willykez.repomaster.ui.screens.editor.FileEditorScreen
import com.willykez.repomaster.ui.screens.explorer.FileExplorerScreen
import com.willykez.repomaster.ui.screens.gitignore.GitignoreScreen
import com.willykez.repomaster.ui.screens.log.LogScreen
import com.willykez.repomaster.ui.screens.more.MoreScreen
import com.willykez.repomaster.ui.screens.remote.RemoteScreen
import com.willykez.repomaster.ui.screens.repolist.RepoListScreen
import com.willykez.repomaster.ui.screens.settings.SettingsScreen
import com.willykez.repomaster.ui.screens.stash.StashScreen
import com.willykez.repomaster.ui.screens.tags.TagsScreen

object Routes {
    // Bottom-tab roots — none of these carry a repoId in the route itself
    // anymore. Which repo they act on is shared UI state (see [selectedRepoId]
    // in [RepoMasterApp]), set by tapping a repo in the Repos tab.
    const val REPO_LIST  = "repos"
    const val CHANGES_TAB = "changes_tab"
    const val HISTORY_TAB = "history_tab"
    const val MORE        = "more"

    // Deep / stack screens — unchanged from before, still reached by pushing
    // on top of whichever tab is current, and still hide the bottom bar.
    const val CREDENTIALS = "credentials"
    const val DISCOVER   = "discover"
    const val SETTINGS   = "settings"
    const val BRANCHES   = "branches/{repoId}"
    const val DIFF       = "diff/{repoId}/{encodedPath}/{staged}"
    const val STASH      = "stash/{repoId}"
    const val REMOTE     = "remote/{repoId}"
    const val TAGS       = "tags/{repoId}"
    const val GITIGNORE  = "gitignore/{repoId}"
    const val EXPLORER   = "explorer/{repoId}/{encodedPath}"
    const val EDITOR     = "editor/{repoId}/{encodedPath}"
    const val CONFLICTS  = "conflicts/{repoId}"
    const val BLAME      = "blame/{repoId}/{encodedPath}"

    fun branches(id: Long) = "branches/$id"
    fun stash(id: Long)    = "stash/$id"
    fun remote(id: Long)   = "remote/$id"
    fun tags(id: Long)     = "tags/$id"
    fun gitignore(id: Long)= "gitignore/$id"
    fun diff(id: Long, path: String, staged: Boolean) =
        "diff/$id/${java.net.URLEncoder.encode(path, "UTF-8")}/$staged"
    fun explorer(id: Long, path: String = "") =
        "explorer/$id/${java.net.URLEncoder.encode(path.ifBlank { "." }, "UTF-8")}"
    fun editor(id: Long, path: String) =
        "editor/$id/${java.net.URLEncoder.encode(path, "UTF-8")}"
    fun conflicts(id: Long) = "conflicts/$id"
    fun blame(id: Long, path: String) = "blame/$id/${java.net.URLEncoder.encode(path, "UTF-8")}"
}

private data class TabItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val TAB_ITEMS = listOf(
    TabItem(Routes.REPO_LIST, "Repos", Icons.Filled.Folder),
    TabItem(Routes.CHANGES_TAB, "Changes", Icons.Filled.Dashboard),
    TabItem(Routes.HISTORY_TAB, "History", Icons.Filled.History),
    TabItem(Routes.MORE, "More", Icons.Filled.MoreHoriz),
)

private const val T = 240

/**
 * The whole app shell: a persistent bottom [NavigationBar] with the four
 * main tabs, wrapping the same stack-based [NavHost] the app used before for
 * every deep/detail screen (diff, branches, editor, etc.) — those still push
 * on top and hide the bottom bar, exactly like a typical tabs-plus-stack app.
 */
@Composable
fun RepoMasterApp() {
    val nav = rememberNavController()
    var selectedRepoId by rememberSaveable { mutableStateOf<Long?>(null) }

    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = TAB_ITEMS.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TAB_ITEMS.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                nav.navigate(tab.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, tab.label) },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.secondaryContainer,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            RepoMasterNavHost(
                nav = nav,
                selectedRepoId = selectedRepoId,
                onRepoSelected = { selectedRepoId = it },
            )
        }
    }
}

@Composable
private fun RepoMasterNavHost(
    nav: NavHostController,
    selectedRepoId: Long?,
    onRepoSelected: (Long) -> Unit,
) {
    NavHost(
        navController = nav,
        startDestination = Routes.REPO_LIST,
        enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(T)) },
        exitTransition  = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(T)) },
        popEnterTransition  = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(T)) },
        popExitTransition   = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(T)) },
    ) {
        composable(Routes.REPO_LIST) {
            RepoListScreen(
                onOpenRepo = { id -> onRepoSelected(id); nav.navigate(Routes.CHANGES_TAB) },
                onOpenCredentials = { nav.navigate(Routes.CREDENTIALS) },
                onOpenDiscover = { nav.navigate(Routes.DISCOVER) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.CHANGES_TAB) {
            val id = selectedRepoId
            if (id == null) {
                EmptyTabState("Pick a repo to see its changes", onGoToRepos = { nav.navigate(Routes.REPO_LIST) })
            } else {
                ChangesScreen(
                    repoId = id,
                    onBack = { nav.navigate(Routes.REPO_LIST) },
                    onOpenLog      = { nav.navigate(Routes.HISTORY_TAB) },
                    onOpenBranches = { nav.navigate(Routes.branches(id)) },
                    onOpenStash    = { nav.navigate(Routes.stash(id)) },
                    onOpenRemote   = { nav.navigate(Routes.remote(id)) },
                    onOpenTags     = { nav.navigate(Routes.tags(id)) },
                    onOpenGitignore= { nav.navigate(Routes.gitignore(id)) },
                    onOpenFiles    = { nav.navigate(Routes.explorer(id)) },
                    onOpenConflicts= { nav.navigate(Routes.conflicts(id)) },
                    onOpenDiff     = { path, staged -> nav.navigate(Routes.diff(id, path, staged)) },
                )
            }
        }

        composable(Routes.HISTORY_TAB) {
            val id = selectedRepoId
            if (id == null) {
                EmptyTabState("Pick a repo to see its history", onGoToRepos = { nav.navigate(Routes.REPO_LIST) })
            } else {
                LogScreen(
                    repoId = id,
                    onBack = { nav.navigate(Routes.REPO_LIST) },
                    onOpenDiff = { repoId, sha -> nav.navigate("diff/$repoId/${java.net.URLEncoder.encode(sha, "UTF-8")}/commit") },
                )
            }
        }

        composable(Routes.MORE) {
            MoreScreen(
                repoId = selectedRepoId,
                onOpenBranches = { selectedRepoId?.let { nav.navigate(Routes.branches(it)) } },
                onOpenStash = { selectedRepoId?.let { nav.navigate(Routes.stash(it)) } },
                onOpenRemote = { selectedRepoId?.let { nav.navigate(Routes.remote(it)) } },
                onOpenTags = { selectedRepoId?.let { nav.navigate(Routes.tags(it)) } },
                onOpenGitignore = { selectedRepoId?.let { nav.navigate(Routes.gitignore(it)) } },
                onOpenFiles = { selectedRepoId?.let { nav.navigate(Routes.explorer(it)) } },
                onOpenConflicts = { selectedRepoId?.let { nav.navigate(Routes.conflicts(it)) } },
                onOpenDiscover = { nav.navigate(Routes.DISCOVER) },
                onOpenCredentials = { nav.navigate(Routes.CREDENTIALS) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.SETTINGS) { SettingsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.DISCOVER) { DiscoverScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.CREDENTIALS) { CredentialScreen(onBack = { nav.popBackStack() }) }

        composable(Routes.BRANCHES, arguments = listOf(navArgument("repoId") { type = NavType.LongType })) { bs ->
            BranchesScreen(repoId = bs.arguments!!.getLong("repoId"), onBack = { nav.popBackStack() })
        }
        composable(Routes.DIFF, arguments = listOf(
            navArgument("repoId") { type = NavType.LongType },
            navArgument("encodedPath") { type = NavType.StringType },
            navArgument("staged") { type = NavType.StringType },
        )) { bs ->
            DiffScreen(
                repoId = bs.arguments!!.getLong("repoId"),
                encodedPath = bs.arguments!!.getString("encodedPath") ?: "",
                stagedOrCommit = bs.arguments!!.getString("staged") ?: "false",
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.STASH, arguments = listOf(navArgument("repoId") { type = NavType.LongType })) { bs ->
            StashScreen(repoId = bs.arguments!!.getLong("repoId"), onBack = { nav.popBackStack() })
        }
        composable(Routes.REMOTE, arguments = listOf(navArgument("repoId") { type = NavType.LongType })) { bs ->
            RemoteScreen(repoId = bs.arguments!!.getLong("repoId"), onBack = { nav.popBackStack() })
        }
        composable(Routes.TAGS, arguments = listOf(navArgument("repoId") { type = NavType.LongType })) { bs ->
            TagsScreen(repoId = bs.arguments!!.getLong("repoId"), onBack = { nav.popBackStack() })
        }
        composable(Routes.GITIGNORE, arguments = listOf(navArgument("repoId") { type = NavType.LongType })) { bs ->
            GitignoreScreen(repoId = bs.arguments!!.getLong("repoId"), onBack = { nav.popBackStack() })
        }
        composable(Routes.EXPLORER, arguments = listOf(
            navArgument("repoId") { type = NavType.LongType },
            navArgument("encodedPath") { type = NavType.StringType },
        )) { bs ->
            val id = bs.arguments!!.getLong("repoId")
            val decoded = java.net.URLDecoder.decode(bs.arguments!!.getString("encodedPath") ?: "", "UTF-8")
            val path = if (decoded == ".") "" else decoded
            FileExplorerScreen(
                repoId = id,
                relativePath = path,
                onBack = { nav.popBackStack() },
                onOpenFolder = { childPath -> nav.navigate(Routes.explorer(id, childPath)) },
                onOpenFile = { filePath -> nav.navigate(Routes.editor(id, filePath)) },
                onOpenBlame = { filePath -> nav.navigate(Routes.blame(id, filePath)) },
            )
        }
        composable(Routes.EDITOR, arguments = listOf(
            navArgument("repoId") { type = NavType.LongType },
            navArgument("encodedPath") { type = NavType.StringType },
        )) { bs ->
            val path = java.net.URLDecoder.decode(bs.arguments!!.getString("encodedPath") ?: "", "UTF-8")
            FileEditorScreen(
                repoId = bs.arguments!!.getLong("repoId"),
                relativePath = path,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.CONFLICTS, arguments = listOf(navArgument("repoId") { type = NavType.LongType })) { bs ->
            val id = bs.arguments!!.getLong("repoId")
            ConflictsScreen(
                repoId = id,
                onBack = { nav.popBackStack() },
                onEditFile = { path -> nav.navigate(Routes.editor(id, path)) },
            )
        }
        composable(Routes.BLAME, arguments = listOf(
            navArgument("repoId") { type = NavType.LongType },
            navArgument("encodedPath") { type = NavType.StringType },
        )) { bs ->
            val id = bs.arguments!!.getLong("repoId")
            val path = java.net.URLDecoder.decode(bs.arguments!!.getString("encodedPath") ?: "", "UTF-8")
            BlameScreen(
                repoId = id, path = path,
                onBack = { nav.popBackStack() },
                onOpenCommit = { sha -> nav.navigate("diff/$id/${java.net.URLEncoder.encode(sha, "UTF-8")}/commit") },
            )
        }
    }
}
