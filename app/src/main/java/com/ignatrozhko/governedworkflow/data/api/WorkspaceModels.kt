package com.ignatrozhko.governedworkflow.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonTransformingSerializer

@Serializable
data class PhaseTransition(val from: String = "", val to: String = "", val time: String = "")

@Serializable
data class Plan(
    val description: String = "",
    val execution: List<PlanSubphase> = emptyList(),
    @SerialName("systemDiagram")
    @Serializable(with = PlanDiagramsSerializer::class)
    val diagrams: List<PlanDiagram> = emptyList(),
)

@Serializable
data class PlanSubphase(
    val id: String = "",
    val name: String = "",
    val scope: PhaseScope = PhaseScope(),
    val tasks: List<PlanTask> = emptyList(),
)

@Serializable
data class PlanTask(
    val title: String = "",
    val description: String = "",
    val agent: String = "",
    val status: String = "",
    val files: List<String> = emptyList(),
)

@Serializable
data class PlanDiagram(val title: String = "", val diagram: String = "")

/**
 * `systemDiagram` is a list of titled diagrams, but older plans and fresh workspaces store one bare mermaid
 * string (empty before planning). Like the web panel, a string becomes a single untitled diagram, a blank one none.
 */
internal object PlanDiagramsSerializer : JsonTransformingSerializer<List<PlanDiagram>>(ListSerializer(PlanDiagram.serializer())) {
    override fun transformDeserialize(element: JsonElement): JsonElement {
        if (element !is JsonPrimitive || !element.isString) return element
        if (element.content.isBlank()) return JsonArray(emptyList())
        return JsonArray(listOf(JsonObject(mapOf("diagram" to element))))
    }
}

@Serializable
data class PhaseScope(val must: List<String> = emptyList(), val may: List<String> = emptyList())

@Serializable
data class ResearchEntry(
    val id: Long,
    val topic: String = "",
    val summary: String = "",
    val findings: List<ResearchFinding> = emptyList(),
    val proven: Int = 0,
    @SerialName("proven_notes") val provenNotes: String? = null,
    @SerialName("created_at") val createdAt: String = "",
) {
    /** The backend stores 1 for proven, -1 for rejected and 0 while awaiting review. */
    val proofStatus: ProofStatus
        get() = when {
            proven > 0 -> ProofStatus.Proven
            proven < 0 -> ProofStatus.Rejected
            else -> ProofStatus.Pending
        }
}

enum class ProofStatus { Pending, Proven, Rejected }

@Serializable
data class ResearchFinding(
    val summary: String = "",
    val details: String = "",
    val proof: ResearchProof? = null,
)

@Serializable
data class ResearchProof(
    val type: String = "",
    val file: String? = null,
    @SerialName("line_start") val lineStart: Int? = null,
    @SerialName("line_end") val lineEnd: Int? = null,
    val snippet: String? = null,
    val url: String? = null,
    val title: String? = null,
)

@Serializable
data class ProgressEntry(
    val summary: String = "",
    val details: JsonObject? = null,
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
data class Discussion(
    val id: Long,
    val text: String = "",
    val author: String = "",
    val type: String = "",
    val status: String = "",
    val hidden: Int = 0,
    val replies: List<CommentReply> = emptyList(),
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class Comment(
    val id: Long,
    val scope: String = "",
    val target: String = "",
    val text: String = "",
    val author: String = "",
    @SerialName("file_path") val filePath: String? = null,
    @SerialName("line_start") val lineStart: Int? = null,
    @SerialName("line_end") val lineEnd: Int? = null,
    val resolved: Boolean = false,
    val resolution: String? = null,
    val replies: List<CommentReply> = emptyList(),
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class CommentReply(
    val id: Long,
    val text: String = "",
    val author: String = "",
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
internal data class CommentsResponse(val comments: List<Comment> = emptyList())

@Serializable
data class Criterion(
    val id: Long,
    val description: String = "",
    val type: String = "",
    val status: String = "",
    val source: String = "",
    val validated: Int = 0,
    @SerialName("validation_message") val validationMessage: String? = null,
    val details: JsonObject? = null,
)

@Serializable
internal data class CriteriaResponse(val criteria: List<Criterion> = emptyList())

@Serializable
data class Proposal(
    val id: Long,
    val title: String = "",
    val body: String = "",
    val reason: String = "",
    val type: String = "",
    val status: String = "",
    val origin: String = "",
    @SerialName("created_at") val createdAt: String = "",
) {
    val isOpen: Boolean get() = status == OPEN_STATUS

    private companion object {
        const val OPEN_STATUS = "proposed"
    }
}

@Serializable
internal data class ProposalsResponse(val proposals: List<Proposal> = emptyList())

@Serializable
data class ScratchpadFile(
    val name: String,
    val title: String = "",
    val size: Long = 0,
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
internal data class ScratchpadsResponse(val files: List<ScratchpadFile> = emptyList())

@Serializable
internal data class ScratchpadContent(val content: String = "")

@Serializable
data class PhaseInfo(
    val id: String,
    val name: String = "",
    @SerialName("is_user_gate") val isUserGate: Boolean = false,
)

@Serializable
internal data class PhasesResponse(val phases: List<PhaseInfo> = emptyList())

@Serializable
data class ProjectSettings(
    @SerialName("yolo_mode_default") val yoloModeDefault: Boolean = false,
    @SerialName("fast_mode_default") val fastModeDefault: Boolean = false,
    @SerialName("review_mode_default") val reviewModeDefault: String = "",
)

@Serializable
internal data class BranchesResponse(val local: List<String> = emptyList(), val remote: List<String> = emptyList())

@Serializable
data class CreatedWorkspace(val branch: String, val workspace: String = "")

@Serializable
data class DiffFile(
    val path: String,
    @SerialName("old_path") val oldPath: String? = null,
    val additions: Int = 0,
    val deletions: Int = 0,
    val diff: String = "",
)

/** A git repository the Changes tab can diff; [path] "." is the workspace itself in a single-repo project. */
@Serializable
data class CodeRepo(val name: String, val path: String) {
    /** The panel resolves a missing `repo` to the workspace root, which is the only form older panels understand. */
    val queryValue: String? get() = path.takeUnless { it == WORKSPACE_ROOT }

    private companion object {
        const val WORKSPACE_ROOT = "."
    }
}

@Serializable
internal data class ReposResponse(val repos: List<CodeRepo> = emptyList())

@Serializable
internal data class DiffResponse(val files: List<DiffFile> = emptyList())

@Serializable
data class CommitInfo(
    val sha: String,
    @SerialName("full_sha") val fullSha: String = sha,
    val subject: String = "",
    @SerialName("author_name") val authorName: String = "",
    @SerialName("author_date") val authorDate: String = "",
)

@Serializable
internal data class HistoryResponse(val commits: List<CommitInfo> = emptyList())

@Serializable
data class Annotation(
    val id: Long,
    @SerialName("file_path") val filePath: String = "",
    @SerialName("line_start") val lineStart: Int = 0,
    @SerialName("line_end") val lineEnd: Int = 0,
    val text: String = "",
    val author: String = "",
    val stale: Boolean = false,
)

@Serializable
internal data class AnnotationsResponse(val annotations: List<Annotation> = emptyList())

@Serializable
internal data class LaunchConfig(@SerialName("claude_command") val claudeCommand: String = "")

@Serializable
data class ModuleInfo(val id: String, val name: String = "", val description: String = "")

@Serializable
internal data class ModulesResponse(val modules: List<ModuleInfo> = emptyList())

@Serializable
internal data class EnabledModulesResponse(val modules: List<String> = emptyList())
