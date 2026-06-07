package com.yomiato.data.ai

/** AI 要約・タグ処理の結果。UI でメッセージ表示と提案タグの提示に使う。 */
sealed interface AiOutcome {
    data class Success(val summary: String, val suggestedTags: List<String>) : AiOutcome
    data object NoKey : AiOutcome
    data class Error(val message: String) : AiOutcome
}
