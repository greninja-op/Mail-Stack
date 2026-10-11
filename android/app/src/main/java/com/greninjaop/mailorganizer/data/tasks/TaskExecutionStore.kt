package com.greninjaop.mailorganizer.data.tasks

import com.greninjaop.mailorganizer.core.JsonParser
import com.greninjaop.mailorganizer.core.JsonParser.long
import com.greninjaop.mailorganizer.core.JsonParser.obj
import com.greninjaop.mailorganizer.core.JsonParser.string
import com.greninjaop.mailorganizer.core.MoLogger
import java.io.File

/**
 * Local record of confirmed Google Tasks creations (Phase 16 §26, §31).
 *
 * Persists, per account:
 * - the user's selected destination task list (default `@default`;
 *   phase §12–§13 — the choice is the user's, never auto-picked from
 *   API order),
 * - one [TaskExecutionRecord] per confirmed action, so future
 *   update/complete/delete/reconcile flows can find the Google task
 *   without reconstructing the relationship from the marker alone.
 *
 * Backed by one small JSON file per account under [dir] (created on
 * demand). Google task IDs are not secrets — they are opaque Google
 * identifiers — so plain app-private storage is appropriate. No tokens
 * ever pass through here.
 *
 * All public functions are total: I/O failures are logged and reported
 * as nulls/defaults, never thrown. The serialization is pure and
 * unit-tested via a temp directory (pure JVM).
 */
class TaskExecutionStore(
    private val dir: File,
) {

    /** The user's chosen destination list, or `@default` when unset. */
    fun getSelectedTaskListId(accountId: String): String {
        val state = readState(accountId)
        return state.selectedTaskListId.ifBlank { DEFAULT_TASK_LIST_ID }
    }

    fun setSelectedTaskListId(accountId: String, taskListId: String) {
        val state = readState(accountId)
        writeState(accountId, state.copy(selectedTaskListId = taskListId))
    }

    fun recordExecution(accountId: String, record: TaskExecutionRecord) {
        val state = readState(accountId)
        val executions = state.executions.toMutableMap()
        executions[record.actionKey] = record
        writeState(accountId, state.copy(executions = executions))
    }

    fun findExecution(accountId: String, actionKey: String): TaskExecutionRecord? =
        readState(accountId).executions[actionKey]

    fun findByTaskId(accountId: String, googleTaskId: String): TaskExecutionRecord? =
        readState(accountId).executions.values.firstOrNull { it.googleTaskId == googleTaskId }

    /**
     * Account-removal cleanup boundary (phase §10): clears ONLY this
     * account's Tasks metadata. Never touches other accounts.
     */
    fun clearAccount(accountId: String) {
        try {
            fileFor(accountId).delete()
        } catch (t: Throwable) {
            MoLogger.w(TAG, "clearAccount failed: ${t.javaClass.simpleName}")
        }
    }

    // ---------- Persistence ----------

    internal data class AccountState(
        val selectedTaskListId: String = DEFAULT_TASK_LIST_ID,
        val executions: Map<String, TaskExecutionRecord> = emptyMap(),
    )

    private fun fileFor(accountId: String): File {
        val safe = accountId.map { c ->
            if (c.isLetterOrDigit() || c == '-' || c == '_') c else '_'
        }.joinToString("").take(64).ifBlank { "unknown" }
        return File(dir, "tasks-$safe.json")
    }

    private fun readState(accountId: String): AccountState {
        return try {
            val file = fileFor(accountId)
            if (!file.exists()) return AccountState()
            val text = file.readText(Charsets.UTF_8)
            decodeState(text) ?: AccountState()
        } catch (t: Throwable) {
            MoLogger.w(TAG, "readState failed: ${t.javaClass.simpleName}")
            AccountState()
        }
    }

    private fun writeState(accountId: String, state: AccountState) {
        try {
            if (!dir.exists()) dir.mkdirs()
            val file = fileFor(accountId)
            val tmp = File(dir, file.name + ".tmp")
            tmp.writeText(encodeState(state), Charsets.UTF_8)
            // Atomic replace: never leave a half-written state file.
            if (!tmp.renameTo(file)) {
                file.delete()
                tmp.renameTo(file)
            }
        } catch (t: Throwable) {
            MoLogger.w(TAG, "writeState failed: ${t.javaClass.simpleName}")
        }
    }

    // ---------- Pure JSON codec (unit-tested indirectly) ----------

    internal fun encodeState(state: AccountState): String = buildString {
        append("{\"selectedTaskListId\":\"")
        append(jsonEscape(state.selectedTaskListId))
        append("\",\"executions\":{")
        state.executions.entries.forEachIndexed { index, (key, r) ->
            if (index > 0) append(",")
            append("\"").append(jsonEscape(key)).append("\":{")
            append("\"taskListId\":\"").append(jsonEscape(r.taskListId)).append("\",")
            append("\"googleTaskId\":\"").append(jsonEscape(r.googleTaskId)).append("\",")
            append("\"idempotencyKey\":\"").append(jsonEscape(r.idempotencyKey)).append("\",")
            append("\"title\":\"").append(jsonEscape(r.title)).append("\",")
            append("\"createdAtEpochMs\":").append(r.createdAtEpochMs)
            append("}")
        }
        append("}}")
    }

    internal fun decodeState(text: String): AccountState? {
        val root = JsonParser.parse(text) ?: return null
        val selected = root.string("selectedTaskListId") ?: DEFAULT_TASK_LIST_ID
        val executions = mutableMapOf<String, TaskExecutionRecord>()
        val execObj = root.obj("executions")
        if (execObj != null) {
            for ((key, value) in execObj.fields) {
                val taskListId = value.string("taskListId") ?: continue
                val googleTaskId = value.string("googleTaskId")?.ifBlank { null } ?: continue
                executions[key] = TaskExecutionRecord(
                    actionKey = key,
                    taskListId = taskListId,
                    googleTaskId = googleTaskId,
                    idempotencyKey = value.string("idempotencyKey").orEmpty(),
                    title = value.string("title").orEmpty(),
                    createdAtEpochMs = value.long("createdAtEpochMs") ?: 0L,
                )
            }
        }
        return AccountState(selectedTaskListId = selected, executions = executions)
    }

    private companion object {
        const val TAG = "TaskExecutionStore"
    }
}
