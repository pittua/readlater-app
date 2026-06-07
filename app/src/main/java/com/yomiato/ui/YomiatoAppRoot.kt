package com.yomiato.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.yomiato.ui.list.InboxScreen
import com.yomiato.ui.navigation.Routes
import com.yomiato.ui.navigation.TopDestination
import com.yomiato.ui.organize.FolderArticlesScreen
import com.yomiato.ui.organize.FoldersScreen
import com.yomiato.ui.organize.TagArticlesScreen
import com.yomiato.ui.organize.TagsScreen
import com.yomiato.ui.reader.ReaderScreen
import com.yomiato.ui.search.SearchScreen
import com.yomiato.ui.settings.SettingsScreen

/**
 * アプリのナビゲーションルート。トップレベルはボトムナビ、詳細は push 遷移。
 * ボトムナビはトップレベル画面でのみ表示する。
 */
@Composable
fun YomiatoAppRoot() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val topRoutes = TopDestination.entries.map { it.route }.toSet()
    val showBottomBar = currentDestination?.route in topRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    TopDestination.entries.forEach { dest ->
                        val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(stringResource(dest.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopDestination.Inbox.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            // ---- トップレベル ----
            composable(TopDestination.Inbox.route) {
                InboxScreen(
                    onOpenArticle = { navController.navigate(Routes.reader(it)) },
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                )
            }
            composable(TopDestination.Folders.route) {
                FoldersScreen(
                    onOpenFolder = { id, _ -> navController.navigate(Routes.folder(id)) },
                )
            }
            composable(TopDestination.Tags.route) {
                TagsScreen(
                    onOpenTag = { id, _ -> navController.navigate(Routes.tag(id)) },
                )
            }
            composable(TopDestination.Settings.route) {
                SettingsScreen()
            }

            // ---- 詳細 ----
            composable(Routes.SEARCH) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onOpenArticle = { navController.navigate(Routes.reader(it)) },
                )
            }
            composable(
                route = Routes.READER,
                arguments = listOf(navArgument("articleId") { type = NavType.LongType }),
            ) {
                ReaderScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = Routes.FOLDER,
                arguments = listOf(navArgument("folderId") { type = NavType.LongType }),
            ) {
                FolderArticlesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenArticle = { navController.navigate(Routes.reader(it)) },
                )
            }
            composable(
                route = Routes.TAG,
                arguments = listOf(navArgument("tagId") { type = NavType.LongType }),
            ) {
                TagArticlesScreen(
                    onBack = { navController.popBackStack() },
                    onOpenArticle = { navController.navigate(Routes.reader(it)) },
                )
            }
        }
    }
}
