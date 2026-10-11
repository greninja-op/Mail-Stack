package com.greninjaop.mailorganizer.data.tasks

/**
 * Google Tasks domain models (Phase 16).
 *
 * Google API shapes are parsed into these app-owned models at the
 * [GoogleTasksHttp] boundary — no Google-specific classes leak past it
 * (phase §13). All models are plain Kotlin, unit-testable on the JVM.
 */

/** A Google Tasks task list (subset of the API's tasklist resource). */
data class GoogleTaskList(
    val id: String,
    val title: String,
    /** Last update, epoch millis, when the API provides it. */
    val updatedEpochMs: Long? = null,
) {
    /** Display title; never blank in the UI (phase §12). */
    val displayTitle: String get() = title.ifBlank { "Untitled list" }

    /** True for the API's `@default` alias — the user's default list. */
    val isDefaultAlias: Boolean get() = id == DEFAULT_TASK_LIST_ID
}

/** A Google Task (subset of the API's task resource). */
data class GoogleTask(
    val id: String,
    val title: String,
    val notes: String?,
    val status: TaskStatus,
    /** Due instant, epoch millis, when the API provides it. */
    val dueEpochMs: Long?,
    val updatedEpochMs: Long? = null,
    /** The task list this task lives in (our request context). */
    val taskListId: String? = null,
)

/** Mirrors the API's `status` field. */
enum class TaskStatus {
    NEEDS_ACTION,
    COMPLETED,
    ;

    companion object {
        fun fromApi(value: String?): TaskStatus =
            if (value == "completed") COMPLETED else NEEDS_ACTION

        fun toApi(status: TaskStatus): String = when (status) {
            NEEDS_ACTION -> "needsAction"
            COMPLETED -> "completed"
        }
    }
}

/** One page of a task-listing response. */
data class TaskPage(
    val tasks: List<GoogleTask>,
    val nextPageToken: String?,
)

/**
 * What the user wants created — built from an [ActionCandidate] via
 * [buildTaskProposal], never from invented data (phase §16–§17).
 */
data class TaskDraft(
    val title: String,
    val notes: String?,
    /** Due instant, epoch millis. Null = no due date (phase §20). */
    val dueEpochMs: Long?,
    /**
     * Deterministic dedupe key ([TaskDedupe.idempotencyKey]). Embedded in
     * the created task's notes as a marker so a lost response can be
     * recovered without creating a duplicate (phase §26–§27).
     */
    val idempotencyKey: String,
)

/** Field updates for an existing task (phase §31 preparation). */
data class TaskPatch(
    val title: String? = null,
    val notes: String? = null,
    val dueEpochMs: Long? = null,
    /** True to clear a previously-set due date. */
    val clearDue: Boolean = false,
    /** Null = leave status unchanged. */
    val completed: Boolean? = null,
) {
    init {
        require(
            title != null || notes != null || dueEpochMs != null ||
                clearDue || completed != null,
        ) { "TaskPatch must change at least one field" }
    }
}

/** Local record of a confirmed external task creation (phase §26, §31). */
data class TaskExecutionRecord(
    /** Stable action key: `messageId|actionType|targetKey`. */
    val actionKey: String,
    val taskListId: String,
    val googleTaskId: String,
    val idempotencyKey: String,
    val title: String,
    val createdAtEpochMs: Long,
)

const val DEFAULT_TASK_LIST_ID = "@default"
