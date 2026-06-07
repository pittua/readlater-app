package com.yomiato.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yomiato.data.repository.ArticleRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * 保存済み記事の本文・メタを非同期取得する Worker。
 * オフライン時は WorkManager の NetworkType.CONNECTED 制約で復帰時に自動実行される。
 */
@HiltWorker
class FetchArticleWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: ArticleRepository,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val articleId = inputData.getLong(KEY_ARTICLE_ID, -1L)
        if (articleId <= 0) return Result.failure()

        val success = repository.fetchAndUpdate(articleId)
        // 失敗時はネットワーク要因の可能性があるためリトライさせる（FAILED は DB に記録済み）。
        return if (success) Result.success() else Result.retry()
    }

    companion object {
        const val KEY_ARTICLE_ID = "article_id"
    }
}
