package com.greninjaop.mailorganizer.domain.tasks

import com.greninjaop.mailorganizer.MoError
import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.actions.ActionCandidate
import com.greninjaop.mailorganizer.core.actions.ExternalEffect
import com.greninjaop.mailorganizer.core.integrations.IntegrationCapability
import com.greninjaop.mailorganizer.core.integrations.IntegrationId
import com.greninjaop.mailorganizer.data.local.ActionType
import com.greninjaop.mailorganizer.data.tasks.TaskProposal
import com.greninjaop.mailorganizer.data.tasks.TasksRepository
import com.greninjaop.mailorganizer.data.tasks.buildTaskProposal
import com.greninjaop.mailorganizer.domain.actions.ActionExecutor
import com.greninjaop.mailorganizer.domain.actions.ExecutionReceipt
import com.greninjaop.mailorganizer.domain.integrations.IntegrationManager

/**
 * Action Engine executor for REMINDER → Google Tasks (Phase 16 §15).
 *
 * Wiring (done by the parent in `AppContainer` — this file only defines
 * the executor):
 * ```kotlin
 * val createTaskExecutor = CreateTaskActionExecutor(tasksRepository, integrationManager)
 * val actionExecutorRegistry = ActionExecutorRegistry(listOf(createTaskExecutor))
 * ```
 * where `tasksRepository` is built as
 * ```kotlin
 * TasksRepository(authRepository, GoogleTasksHttp(),
 *     TaskExecutionStore(File(appContext.filesDir, "tasks")))
 * ```
 *
 * Flow: proposal → user review → explicit confirmation (all in
 * `ReviewActionUseCase`, Phase 14) → [execute] → verified result.
 * [execute] is only ever called after explicit user confirmation; this
 * class never initiates a creation on its own (phase §25).
 *
 * Account isolation (phase §32): the candidate's [ActionCandidate.accountId]
 * selects the token, the task list, and the execution record for every
 * step. A proposal created under Account A can never execute with
 * Account B's credentials — there is no fallback, no "active account".
 */
class CreateTaskActionExecutor(
    private val tasks: TasksRepository,
    private val integrations: IntegrationManager,
    private val clock: () -> Long = System::currentTimeMillis,
) : ActionExecutor {

    override fun supports(actionType: ActionType): Boolean =
        actionType == ActionType.REMINDER

    /**
     * Pre-execution validation without side effects. In addition to
     * well-formedness, requires [ExternalEffect.TASKS]: a REMINDER
     * candidate marked NONE is an internal suggestion and must never
     * reach the Tasks API through this executor.
     */
    override fun validate(candidate: ActionCandidate): MoResult<Unit> {
        if (!supports(candidate.actionType)) {
            return MoResult.Failure(
                MoError.InvalidConfiguration("Not a reminder action."),
            )
        }
        if (candidate.externalEffect != ExternalEffect.TASKS) {
            return MoResult.Failure(
                MoError.InvalidConfiguration("This action isn't a Tasks action."),
            )
        }
        if (candidate.accountId.isBlank()) {
            return MoResult.Failure(MoError.InvalidConfiguration("Missing account."))
        }
        if (candidate.title.isBlank() && candidate.description.isBlank()) {
            return MoResult.Failure(MoError.InvalidConfiguration("Nothing to put in the task."))
        }
        return MoResult.Success(Unit)
    }

    override suspend fun execute(candidate: ActionCandidate): MoResult<ExecutionReceipt> {
        when (val v = validate(candidate)) {
            is MoResult.Failure -> return v
            is MoResult.Success -> Unit
        }

        // Defense in depth: re-check the integration is genuinely usable
        // right now (CONNECTED ⇒ usable token + tasks scope, verified by
        // the adapter's probe — never a declaration alone).
        val capable = try {
            integrations.isCapable(
                IntegrationId.TASKS,
                IntegrationCapability.CREATE_TASK,
                candidate.accountId,
            )
        } catch (t: Throwable) {
            MoLogger.w(TAG, "Capability check failed: ${t.javaClass.simpleName}")
            false
        }
        if (!capable) {
            return MoResult.Failure(
                MoError.Integration(
                    integration = IntegrationId.TASKS.value,
                    message = "Google Tasks isn't connected for this account.",
                ),
            )
        }

        val accountId = com.greninjaop.mailorganizer.core.AccountId(candidate.accountId)
        val taskListId = tasks.selectedTaskListId(accountId)
        val taskListName = resolveListName(accountId, taskListId)
        val proposal = buildTaskProposal(candidate, taskListId, taskListName)
        val problems = proposal.problems()
        if (problems.isNotEmpty()) {
            return MoResult.Failure(
                MoError.InvalidConfiguration(problems.joinToString(" ")),
            )
        }

        return when (
            val created = tasks.createTask(
                accountId = accountId,
                taskListId = proposal.taskListId,
                actionKey = proposal.actionKey,
                title = proposal.title,
                notes = proposal.notes,
                dueEpochMs = proposal.dueEpochMs,
            )
        ) {
            is MoResult.Success -> {
                val task = created.value
                MoLogger.i(TAG, "Task created: ${task.id} (list=$taskListId)")
                MoResult.Success(
                    ExecutionReceipt(
                        executorName = "Google Tasks",
                        externalId = task.id,
                        executedAtEpochMs = clock(),
                        summary = "Task created in ${taskListName ?: "Google Tasks"}: ${task.title}",
                    ),
                )
            }
            is MoResult.Failure -> {
                MoLogger.w(TAG, "Task creation failed: ${created.error}")
                MoResult.Failure(created.error)
            }
        }
    }

    /**
     * Best-effort display name for the destination list (confirmation
     * copy shows names, never raw ids — phase §12). Null when the list
     * can't be resolved; the id is still used for the API call.
     */
    private suspend fun resolveListName(
        accountId: com.greninjaop.mailorganizer.core.AccountId,
        taskListId: String,
    ): String? {
        if (taskListId == com.greninjaop.mailorganizer.data.tasks.DEFAULT_TASK_LIST_ID) {
            return "My Tasks"
        }
        return try {
            when (val lists = tasks.taskLists(accountId)) {
                is MoResult.Success ->
                    lists.value.firstOrNull { it.id == taskListId }?.displayTitle
                is MoResult.Failure -> null
            }
        } catch (t: Throwable) {
            MoLogger.w(TAG, "List-name lookup failed: ${t.javaClass.simpleName}")
            null
        }
    }

    private companion object {
        const val TAG = "CreateTaskExecutor"
    }
}

/** Builds the executor's proposal for tests/preview without executing. */
fun buildReminderTaskProposal(
    candidate: ActionCandidate,
    taskListId: String,
    taskListName: String?,
): TaskProposal = buildTaskProposal(candidate, taskListId, taskListName)
