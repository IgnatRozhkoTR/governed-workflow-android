package com.ignatrozhko.governedworkflow.data.api

/**
 * Comment scope/target keys exactly as the web panel builds them, so comments made on the phone show up
 * on the same plan item, finding or file in the browser and in Claude's `workspace_get_comments`.
 */
object CommentTargets {
    const val PLAN_SCOPE = "plan"
    const val RESEARCH_SCOPE = "research"
    const val REVIEW_SCOPE = "review"
    const val WHOLE_PLAN = "plan"
    private const val FINDING_PREFIX_LENGTH = 60

    fun subphase(id: String): String = "subphase:$id"

    fun task(title: String): String = "task:$title"

    fun finding(summary: String): String = "Finding: ${summary.take(FINDING_PREFIX_LENGTH)}"
}

/** `state.comments` is keyed `"<scope>:<target>"`; review comments for a file are keyed by its path. */
fun WorkspaceState.commentsFor(scope: String, target: String): List<Comment> = comments["$scope:$target"].orEmpty()

fun WorkspaceState.reviewComments(): List<Comment> =
    comments.filterKeys { it.startsWith("${CommentTargets.REVIEW_SCOPE}:") }.values.flatten()
