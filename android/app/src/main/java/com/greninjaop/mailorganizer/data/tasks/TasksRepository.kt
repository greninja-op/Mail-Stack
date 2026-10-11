package com.greninjaop.mailorganizer.data.tasks

import android.content.Intent
import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.data.auth.AuthRepository
import com.greninjaop.mailorganizer.data.auth.GoogleAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Google Tasks use-case layer (Phase 16 §13).
 *
 * Sits between the Action Engine / integration adapter and the
 * [GoogleTasksHttp] wire client:
 *
 * ```
 * Presentation → Use Case → Action Engine → Tasks Action Adapter
 *     → TasksRepository → GoogleTasksHttp → Google Tasks API
 * ```
 *
 * Rules:
 * - Bearer tokens come ONLY from `AuthRepository.getValidAccessToken`
 *   (per account, per call). This class never touches token storage
 *   directly and never logs tokens.
 * - Every operation is account-scoped: [accountId] selects the token,
 *   the task list, and the execution record. Accounts are never mixed
 *   (phase §9–§10, §32).
 * - Creation is idempotent (phase §26–§27): see [createTask].
 * - Nothing here runs without explicit user confirmation — that gate
 *   lives in the Action Engine (`ReviewActionUseCase`); this layer
 *   only executes what it is asked to.
 */
class TasksRepository(
    private val auth: AuthRepository,
    private val http: GoogleTasksHttp = GoogleTasksHttp(),
    private val store: TaskExecutionStore? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    // ---------- Task lists (phase §11–§12) ----------

    /**
     * The user's task lists. Never auto-selects: the caller (UI) shows
     * names and lets the user choose; [selectedTaskListId] holds the
     * choice, defaulting to the API's `@default` alias.
     */
    suspend fun taskLists(accountId: AccountId): MoResult<List<GoogleTaskList>> =
        withToken(accountId) { token -> http.listTaskLists(token) }

    /** The user's chosen destination list id (default `@default`). */
    fun selectedTaskListId(accountId: AccountId): String =
        store?.getSelectedTaskListId(accountId.value) ?: DEFAULT_TASK_LIST_ID

    fun setSelectedTaskListId(accountId: AccountId, taskListId: String) {
        require(taskListId.isNotBlank()) { "taskListId must not be blank" }
        store?.setSelectedTaskListId(accountId.value, taskListId)
    }

    /**
     * Lightweight proof that the tasks scope is actually usable
     * (phase §29 honesty): a single `tasklists.list` call. Used by the
     * integration adapter to report CONNECTED only when true.
     */
    suspend fun probeAccess(accountId: AccountId): MoResult<Unit> =
        withToken(accountId) { token ->
            when (val r = http.listTaskLists(token)) {
                is MoResult.Success -> MoResult.Success(Unit)
                is MoResult.Failure -> MoResult.Failure(r.error)
            }
        }

    // ---------- Create (idempotent; phase §24–§27) ----------

    /**
     * Creates a task from a confirmed action. Idempotent:
     *
     * 1. If this [actionKey] already has a recorded execution, verify the
     *    task still exists and return it (a 404 falls through to
     *    re-create — the user may have deleted it in Google Tasks).
     * 2. Scan the destination list for the dedupe marker. If found, a
     *    previous attempt succeeded but its response was lost — adopt
     *    that task instead of creating a duplicate.
     * 3. Insert exactly once with the marker in notes.
     * 4. If the insert fails with a network error *after* the request
     *    may have been sent, scan once more for the marker before
     *    reporting failure — a later retry repeats step 2, so it can
     *    never duplicate.
     *
     * [actionKey] is `messageId|actionType|targetKey` — stable across
     * re-generated candidates for the same action.
     */
    suspend fun createTask(
        accountId: AccountId,
        taskListId: String,
        actionKey: String,
        title: String,
        notes: String?,
        dueEpochMs: Long?,
    ): MoResult<GoogleTask> = withToken(accountId) { token ->
        require(taskListId.isNotBlank()) { "taskListId must not be blank" }
        require(actionKey.isNotBlank()) { "actionKey must not be blank" }
        require(title.isNotBlank()) { "title must not be blank" }
        val key = TaskDedupe.idempotencyKey(accountId.value, taskListId, actionKey)

        // 1. Recorded execution fast path.
        val recorded = store?.findExecution(accountId.value, actionKey)
        if (recorded != null && recorded.taskListId == taskListId) {
            when (val existing = http.getTask(token, taskListId, recorded.googleTaskId)) {
                is MoResult.Success -> return@withToken MoResult.Success(existing.value)
                is MoResult.Failure -> {
                    // 404 = user deleted it elsewhere → fall through and
                    // re-create. Anything else is reported honestly.
                    if (existing.error !is MoError.Api || existing.error.code != 404) {
                        return@withToken MoResult.Failure(existing.error)
                    }
                    MoLogger.i(TAG, "Recorded task gone (404); re-creating")
                }
            }
        }

        // 2. Marker scan: did an earlier attempt actually succeed?
        findByMarker(token, taskListId, key)?.let { found ->
            record(accountId, actionKey, taskListId, found.id, key, title)
            return@withToken MoResult.Success(found)
        }

        // 3. Exactly one insert attempt.
        val draft = TaskDraft(
            title = title,
            notes = notes,
            dueEpochMs = dueEpochMs,
            idempotencyKey = key,
        )
        when (val created = http.insertTask(token, taskListId, draft)) {
            is MoResult.Success -> {
                record(accountId, actionKey, taskListId, created.value.id, key, title)
                MoResult.Success(created.value)
            }
            is MoResult.Failure -> {
                // 4. Network failure after a possibly-sent request:
                // one recovery scan before admitting failure.
                if (created.error is MoError.Network) {
                    findByMarker(token, taskListId, key)?.let { found ->
                        MoLogger.i(TAG, "Recovered task after network failure: ${found.id}")
                        record(accountId, actionKey, taskListId, found.id, key, title)
                        return@withToken MoResult.Success(found)
                    }
                }
                MoResult.Failure(created.error)
            }
        }
    }

    // ---------- Update / delete (phase §31 preparation) ----------

    /**
     * Updates a task previously created by Mailstack. The caller must
     * resolve [googleTaskId] from [TaskExecutionStore] (or the marker
     * scan) — ids are never guessed.
     */
    suspend fun updateTask(
        accountId: AccountId,
        taskListId: String,
        googleTaskId: String,
        patch: TaskPatch,
    ): MoResult<GoogleTask> = withToken(accountId) { token ->
        http.patchTask(token, taskListId, googleTaskId, patch)
    }

    /** Marks a Mailstack-created task completed / reopens it. */
    suspend fun setTaskCompleted(
        accountId: AccountId,
        taskListId: String,
        googleTaskId: String,
        completed: Boolean,
    ): MoResult<GoogleTask> =
        updateTask(accountId, taskListId, googleTaskId, TaskPatch(completed = completed))

    /** Deletes a Mailstack-created task. Explicit user action only. */
    suspend fun deleteTask(
        accountId: AccountId,
        taskListId: String,
        googleTaskId: String,
    ): MoResult<Unit> = withToken(accountId) { token ->
        http.deleteTask(token, taskListId, googleTaskId)
    }

    /** Local execution lookup for update/delete/reconcile flows. */
    fun findExecution(
        accountId: AccountId,
        actionKey: String,
    ): TaskExecutionRecord? = store?.findExecution(accountId.value, actionKey)

    /**
     * Clears this account's local Tasks metadata (selected list +
     * execution records). Used by the integration adapter's disconnect;
     * never touches other accounts.
     */
    fun clearAccountData(accountId: AccountId) {
        store?.clearAccount(accountId.value)
    }

    // ---------- Incremental authorization (phase §37) ----------

    /**
     * Intent that adds the Tasks scope to an already-connected account
     * without disturbing Gmail/Calendar grants. The UI starts this
     * intent when the adapter reports PERMISSION_REQUIRED; the normal
     * OAuth redirect flow completes it. Never called during the initial
     * Phase 3 sign-in (least privilege — the tasks scope is requested
     * only here, on demand).
     */
    fun incrementalAuthIntent(accountId: AccountId): Intent =
        auth.createIncrementalAuthIntent(
            accountId,
            listOf(GoogleAuthRepository.SCOPE_TASKS),
        )

    // ---------- Internals ----------

    /**
     * Fetches a valid token for [accountId] (refreshing when needed via
     * the auth layer) and runs [block]. Null token → honest auth error;
     * the caller (UI) then routes to reconnect / incremental auth.
     */
    private suspend fun <T> withToken(
        accountId: AccountId,
        block: suspend (token: String) -> MoResult<T>,
    ): MoResult<T> = withContext(Dispatchers.IO) {
        val token = try {
            auth.getValidAccessToken(accountId)
        } catch (t: Throwable) {
            MoLogger.w(TAG, "getValidAccessToken failed: ${t.javaClass.simpleName}")
            null
        } ?: return@withContext MoResult.Failure(
            MoError.Authentication("Google Tasks isn't connected for this account."),
        )
        try {
            block(token)
        } catch (t: Throwable) {
            MoLogger.w(TAG, "Tasks operation failed: ${t.javaClass.simpleName}")
            MoResult.Failure(MoError.Unexpected(t))
        }
    }

    /**
     * Scans the destination list for a task carrying [key]'s marker.
     * Bounded pagination (never an unbounded crawl). Returns the match
     * or null. Network/auth failures → null (the caller reports the
     * original error, not the scan's).
     */
    private suspend fun findByMarker(
        token: String,
        taskListId: String,
        key: String,
    ): GoogleTask? {
        var pageToken: String? = null
        repeat(MAX_MARKER_SCAN_PAGES) {
            val page = when (val r = http.listTasks(token, taskListId, pageToken)) {
                is MoResult.Success -> r.value
                is MoResult.Failure -> return null
            }
            TaskDedupe.findByKey(page.tasks, key)?.let { return it }
            pageToken = page.nextPageToken ?: return null
        }
        return null
    }

    private fun record(
        accountId: AccountId,
        actionKey: String,
        taskListId: String,
        googleTaskId: String,
        idempotencyKey: String,
        title: String,
    ) {
        try {
            store?.recordExecution(
                accountId.value,
                TaskExecutionRecord(
                    actionKey = actionKey,
                    taskListId = taskListId,
                    googleTaskId = googleTaskId,
                    idempotencyKey = idempotencyKey,
                    title = title,
                    createdAtEpochMs = clock(),
                ),
            )
        } catch (t: Throwable) {
            // The task itself was created — a local bookkeeping failure
            // must not turn success into failure (the marker scan covers
            // future dedupe). Logged, not thrown.
            MoLogger.w(TAG, "recordExecution failed: ${t.javaClass.simpleName}")
        }
    }

    private companion object {
        const val TAG = "TasksRepository"
        const val MAX_MARKER_SCAN_PAGES = 5
    }
}
