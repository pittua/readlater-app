package com.yomiato.data.local

/** 記事の取得・本文抽出の状態。 */
enum class ExtractionStatus {
    /** 保存直後。バックグラウンドで取得待ち。 */
    PENDING,

    /** メタデータ・本文の取得に成功。 */
    SUCCESS,

    /** 取得・抽出に失敗（元 URL フォールバックを提示）。 */
    FAILED,
}
