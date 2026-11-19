package com.ignatrozhko.governedworkflow.ui.common

private const val ZERO_WIDTH_SPACE = "​"
private val BREAK_AFTER = Regex("[/_-]")

/** Display form of a branch or path that wraps after `/`, `-` and `_` first; the text still wraps anywhere when a segment is too long. */
fun String.withBreakOpportunities(): String = replace(BREAK_AFTER) { it.value + ZERO_WIDTH_SPACE }
