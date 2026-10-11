package com.greninjaop.mailorganizer.data.tasks

import com.greninjaop.mailorganizer.core.actions.ActionCandidate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Structured task proposal (Phase 16 §16).
 *
 * Built from an [ActionCandidate] — and only from information the
 * candidate actually carries. Nothing is invented: no made-up due
 * dates (§19–§20), no email-body dumps in notes (§18), no fabricated
 * source links.
 *
 * The proposal is what the user reviews in the confirmation UI
 * (§24) before anything touches the Google Tasks API.
 */
data class TaskProposal(
    val accountId: String,
    val taskListId: String,
    val taskListName: String?,
    val title: String,
    /** Human-friendly notes; the dedupe marker is added at send time. */
    val notes: String?,
    /** Null = unknown (§20); the task is still proposable without one. */
    val dueEpochMs: Long?,
    val sourceMessageId: String,
    val sourceThreadId: String,
    val sourceActionId: String,
    /**
     * Stable dedupe key: `messageId|actionType|targetKey` — the same
     * shape as `ActionCandidate.id`, so re-generated candidates for the
     * same action map to the same key (§26).
     */
    val actionKey: String,
) {
    /** Problems that must block execution (surfaced by the executor). */
    fun problems(): List<String> = buildList {
        if (accountId.isBlank()) add("Missing account.")
        if (taskListId.isBlank()) add("No task list selected.")
        if (title.isBlank()) add("Missing title.")
        if (actionKey.isBlank()) add("Missing action identity.")
    }

    val isExecutable: Boolean get() = problems().isEmpty()
}

/**
 * Builds a [TaskProposal] from an action candidate (phase §16–§21).
 * Pure function — unit-tested.
 *
 * Title hierarchy (§17): the candidate's explicit title first (Phase 14
 * already derives it deterministically: explicit action → deadline /
 * action description → email subject → safe sender + type); a short
 * description-derived fallback; never a bare "Task".
 */
fun buildTaskProposal(
    candidate: ActionCandidate,
    taskListId: String,
    taskListName: String?,
): TaskProposal {
    val title = candidate.title.trim().ifEmpty {
        candidate.description.trim().take(TITLE_FALLBACK_CHARS).trim()
    }.ifEmpty {
        "Action from Mail Organizer"
    }

    val notes = buildNotes(candidate)

    return TaskProposal(
        accountId = candidate.accountId,
        taskListId = taskListId,
        taskListName = taskListName,
        title = title,
        notes = notes,
        dueEpochMs = candidate.dueDateEpochMs,
        sourceMessageId = candidate.messageId,
        sourceThreadId = candidate.threadId,
        sourceActionId = candidate.id,
        actionKey = "${candidate.messageId}|${candidate.actionType.name}|${candidate.targetKey}",
    )
}

/**
 * Notes per §18: useful context only — a short description, the source,
 * and the due moment with its exact time when known (§21). Never the
 * email body, never credentials, never internal database ids, never a
 * fabricated link. The dedupe marker is appended at send time by
 * [GoogleTasksHttp.withDedupeMarker], not here.
 */
private fun buildNotes(candidate: ActionCandidate): String? {
    val parts = ArrayList<String>(3)
    val description = candidate.description.trim().take(NOTES_MAX_CHARS).trim()
    if (description.isNotEmpty() && !description.equals(candidate.title.trim(), ignoreCase = true)) {
        parts.add(description)
    }
    val source = buildString {
        append("Source: Mail Organizer")
        if (candidate.dueDateEpochMs != null) {
            append("\nDue: ").append(formatDueForNotes(candidate.dueDateEpochMs))
        }
    }
    parts.add(source)
    return parts.joinToString("\n\n").ifBlank { null }
}

/**
 * Human due label for notes, preserving the exact local time (§21):
 * e.g. "18 October 2026, 10:30 am". The API `due` field carries only
 * the date; this label keeps the time from being silently lost.
 */
internal fun formatDueForNotes(epochMs: Long): String {
    val dateTime = SimpleDateFormat("d MMMM yyyy, h:mm a", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }
    val dateOnly = SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).apply {
        timeZone = TimeZone.getDefault()
    }
    // Date-precision intelligence is stored at local midnight; don't
    // render a misleading "12:00 am" for those.
    val cal = java.util.Calendar.getInstance(TimeZone.getDefault(), Locale.getDefault())
    cal.timeInMillis = epochMs
    val isMidnight = cal.get(java.util.Calendar.HOUR_OF_DAY) == 0 &&
        cal.get(java.util.Calendar.MINUTE) == 0
    return if (isMidnight) {
        dateOnly.format(Date(epochMs))
    } else {
        dateTime.format(Date(epochMs))
    }
}

private const val TITLE_FALLBACK_CHARS = 80
private const val NOTES_MAX_CHARS = 300
