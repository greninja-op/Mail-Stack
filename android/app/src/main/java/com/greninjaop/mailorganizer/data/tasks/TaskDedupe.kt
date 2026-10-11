package com.greninjaop.mailorganizer.data.tasks

import java.security.MessageDigest

/**
 * Deterministic task identity for duplicate prevention (Phase 16 §26)
 * and retry safety (§27).
 *
 * Google Tasks has no client-supplied idempotency key, so Mailstack
 * derives one and embeds it as a compact marker line in the created
 * task's notes:
 *
 * ```
 * [mailstack:task:9f2ac41bd77e03c1]
 * ```
 *
 * Recovery strategy, documented per §27:
 * 1. Before creating: scan the destination list for the marker. If a
 *    task already carries it (earlier attempt succeeded but the
 *    response was lost, or the user double-confirmed), adopt that task
 *    instead of creating a duplicate.
 * 2. Create with the marker in notes (exactly one HTTP attempt).
 * 3. If the attempt fails with a network error *after* the request may
 *    have been sent, scan again for the marker before reporting
 *    failure — a retry later repeats step 1, so it can never duplicate.
 *
 * The key binds account + task list + action: the same action proposed
 * for a different account or list is a different task (phase §10, §32).
 * All functions are pure — unit-tested.
 */
object TaskDedupe {

    private const val MARKER_PREFIX = "[mailstack:task:"
    private const val MARKER_SUFFIX = "]"
    private const val KEY_HEX_CHARS = 16

    /**
     * Stable dedupe key for one action in one list of one account.
     * [actionKey] is `messageId|actionType|targetKey` (the same shape as
     * `ActionCandidate.id`), so re-generated candidates map to the same
     * key and can never spawn duplicates.
     */
    fun idempotencyKey(accountId: String, taskListId: String, actionKey: String): String {
        val material = "$accountId\n$taskListId\n$actionKey"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(material.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
            .take(KEY_HEX_CHARS)
    }

    /** The marker line embedded in created tasks' notes. */
    fun marker(idempotencyKey: String): String =
        "$MARKER_PREFIX$idempotencyKey$MARKER_SUFFIX"

    /**
     * Extracts the dedupe key from task notes, or null when absent.
     * Tolerates surrounding text — the marker just has to appear.
     */
    fun extractKey(notes: String?): String? {
        if (notes.isNullOrEmpty()) return null
        val start = notes.indexOf(MARKER_PREFIX)
        if (start < 0) return null
        val end = notes.indexOf(MARKER_SUFFIX, start + MARKER_PREFIX.length)
        if (end < 0) return null
        val key = notes.substring(start + MARKER_PREFIX.length, end).trim()
        if (key.isEmpty() || !key.all { it.isDigit() || it in 'a'..'f' }) return null
        return key
    }

    /** True when [notes] carry exactly [idempotencyKey]'s marker. */
    fun hasKey(notes: String?, idempotencyKey: String): Boolean =
        extractKey(notes) == idempotencyKey

    /**
     * Finds the first task in [tasks] carrying [idempotencyKey]'s marker,
     * or null. Used by the pre-create scan and post-failure recovery.
     */
    fun findByKey(tasks: List<GoogleTask>, idempotencyKey: String): GoogleTask? =
        tasks.firstOrNull { hasKey(it.notes, idempotencyKey) }
}
