package com.yomiato.data.work

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** 記事取得 Worker のエンキューを担うヘルパ。 */
@Singleton
class ArticleFetchScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun enqueue(articleId: Long) {
        val request = OneTimeWorkRequestBuilder<FetchArticleWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setInputData(workDataOf(FetchArticleWorker.KEY_ARTICLE_ID to articleId))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "fetch_article_$articleId",
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
