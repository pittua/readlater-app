package com.yomiato.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sell
import androidx.compose.ui.graphics.vector.ImageVector
import com.yomiato.R

/**
 * ボトムナビに並ぶトップレベル画面の定義。
 * 検索は受信トレイのトップバーから push 遷移するためボトムナビには含めない。
 */
enum class TopDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    Inbox("inbox", R.string.nav_inbox, Icons.AutoMirrored.Filled.Article),
    Folders("folders", R.string.nav_folders, Icons.Filled.Folder),
    Tags("tags", R.string.nav_tags, Icons.Filled.Sell),
    Settings("settings", R.string.nav_settings, Icons.Filled.Settings),
}

/** push 遷移する詳細ルート。 */
object Routes {
    const val SEARCH = "search"
    const val READER = "reader/{articleId}"
    const val FOLDER = "folder/{folderId}"
    const val TAG = "tag/{tagId}"

    fun reader(articleId: Long) = "reader/$articleId"
    fun folder(folderId: Long) = "folder/$folderId"
    fun tag(tagId: Long) = "tag/$tagId"
}
