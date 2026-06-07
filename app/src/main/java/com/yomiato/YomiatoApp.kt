package com.yomiato

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * アプリのエントリポイント。Hilt の依存グラフのルート。
 * WorkManager は Hilt 注入のため [Configuration.Provider] でカスタム初期化する
 * （AndroidManifest 側で既定の初期化プロバイダを無効化済み）。
 */
@HiltAndroidApp
class YomiatoApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
