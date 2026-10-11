package com.greninjaop.mailorganizer.domain.calendar

import com.greninjaop.mailorganizer.MoResult
import com.greninjaop.mailorganizer.core.AccountId
import com.greninjaop.mailorganizer.core.MoLogger
import com.greninjaop.mailorganizer.core.actions.ActionCandidate
import com.greninjaop.mailorganizer.core.actions.ExternalEffect
import com.greninjaop.mailorganizer.data.integrations.RealCalendarIntegrationAdapter
import com.greninjaop.mailorganizer.data.local.ActionType
import com.greninjaop.mailorganizer.data.repository.IntelligenceRepository
import com.greninjaop.mailorganizer.domain.actions.ActionExecutor
import com.greninjaop.mailorganizer.domain.actions.ExecutionReceipt
import com.greninjaop.mailorganizer.domain.temporal.TemporalPayloadJson

/**
 * Action Engine executor for MEETING actions (Phase 15 §15).
 *
 * Plugs the real Calendar integration into the Phase 14 engine:
 * ```
 * proposal → user review → explicit confirmation (engine)
 *   → validate → execute → verified receipt → COMPLETED
 * ```
 * [execute] is only ever called by [com.greninjaop.mailorganizer.domain.actions.ReviewActionUseCase.confirm]
 * after the user explicitly confirmed in the confirmation dialog (§25,
 * §26). Nothing here runs during sync, parsing, classification, or in
 * the background.
 *
 * Contract notes:
 * - [supports] / [validate] are pure and total. [validate] checks the
 *   candidate is well-formed without side effects; connectivity and
 *   calendar resolution happen in [execute] (they need I/O, and this
 *   interface's validate is non-suspending by design).
 * - [execute] never mixes accounts: the proposal is bound to the
 *   candidate's account and the adapter re-validates the binding (§36).
 * - Failures are typed [MoResult.Failure]s — never fake success. The
 *   engine marks the action FAILED and nothing is recorded as done.
 */
class CalendarActionExecutor(
    private val adapter: RealCalendarIntegrationAdapter,
    private val intelligence: IntelligenceRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ActionExecutor {

    override fun supports(actionType: ActionType): Boolean =
        actionType == ActionType.MEETING

    override fun validate(candidate: ActionCandidate): MoResult<Unit> =
        candidateReady(candidate)

    override suspend fun execute(candidate: ActionCandidate): MoResult<ExecutionReceipt> {
        val ready = candidateReady(candidate)
        if (ready is MoResult.Failure) return MoResult.Failure(ready.error)

        val accountId = AccountId(candidate.accountId)

        // Resolve the target calendar: primary writable calendar wins;
        // explicit selection UI can override per-proposal in the future
        // (Phase 15 §37). This call also proves the integration is
        // genuinely connected — auth/scope failures surface here.
        val calendars = when (val r = adapter.listWritableCalendars(accountId)) {
            is MoResult.Success -> r.value
            is MoResult.Failure -> {
                MoLogger.w(TAG, "Calendar not usable for execution: ${r.error.javaClass.simpleName}")
                return MoResult.Failure(r.error)
            }
        }
        val target = calendars.firstOrNull { it.primary } ?: calendars.firstOrNull()
            ?: return MoResult.Failure(
                com.greninjaop.mailorganizer.MoError.InvalidConfiguration(
                    "No writable calendar found on this account.",
                ),
            )

        // Resolve Phase 13 temporal details for faithful event data
        // (§19–§24). Falls back to candidate-level data when the source
        // item can't be resolved — never invents values.
        val temporal = resolveTemporal(candidate)
        val proposal = CalendarProposalBuilder.build(
            candidate = candidate,
            temporal = temporal,
            calendarId = target.id,
        ) ?: return MoResult.Failure(
            com.greninjaop.mailorganizer.MoError.InvalidConfiguration(
                "Couldn't prepare the calendar event from this action.",
            ),
        )

        when (val v = CalendarProposalBuilder.validate(proposal)) {
            is ProposalValidation.Invalid -> {
                MoLogger.i(TAG, "Proposal invalid, not executing: ${v.reason}")
                return MoResult.Failure(
                    com.greninjaop.mailorganizer.MoError.InvalidConfiguration(v.reason),
                )
            }
            ProposalValidation.Valid -> Unit
        }

        return when (val r = adapter.createEvent(accountId, proposal)) {
            is MoResult.Success -> {
                val event = r.value
                MoLogger.i(TAG, "Executed calendar action: event ${event.id}")
                MoResult.Success(
                    ExecutionReceipt(
                        executorName = "calendar",
                        externalId = event.id,
                        executedAtEpochMs = clock(),
                        summary = "Added \"${proposal.title}\" to ${target.summary}.",
                    ),
                )
            }
            is MoResult.Failure -> {
                MoLogger.w(TAG, "Calendar execution failed: ${r.error.javaClass.simpleName}")
                MoResult.Failure(r.error)
            }
        }
    }

    /**
     * Pure candidate checks: right type, calendar effect, usable ids,
     * and a start time. No I/O, no side effects.
     */
    private fun candidateReady(candidate: ActionCandidate): MoResult<Unit> {
        if (candidate.actionType != ActionType.MEETING) {
            return MoResult.Failure(
                com.greninjaop.mailorganizer.MoError.InvalidConfiguration(
                    "Calendar executor only handles meeting actions.",
                ),
            )
        }
        if (candidate.externalEffect != ExternalEffect.CALENDAR) {
            return MoResult.Failure(
                com.greninjaop.mailorganizer.MoError.InvalidConfiguration(
                    "This action isn't a calendar proposal.",
                ),
            )
        }
        if (candidate.accountId.isBlank() || candidate.messageId.isBlank()) {
            return MoResult.Failure(
                com.greninjaop.mailorganizer.MoError.InvalidConfiguration(
                    "The action is missing its account or message reference.",
                ),
            )
        }
        if ((candidate.dueDateEpochMs ?: 0L) <= 0) {
            return MoResult.Failure(
                com.greninjaop.mailorganizer.MoError.InvalidConfiguration(
                    "The action has no start time.",
                ),
            )
        }
        if (candidate.title.isBlank()) {
            return MoResult.Failure(
                com.greninjaop.mailorganizer.MoError.InvalidConfiguration(
                    "The action has no title.",
                ),
            )
        }
        return MoResult.Success(Unit)
    }

    /**
     * Resolves the temporal item behind the candidate via its targetKey
     * ("temporal:{itemId}"). Total: any failure yields null and the
     * caller falls back to candidate-level data.
     */
    private suspend fun resolveTemporal(candidate: ActionCandidate): ResolvedTemporal? {
        return try {
            val itemId = candidate.targetKey.removePrefix("temporal:").toLongOrNull()
                ?: return null
            val record = intelligence.getExtractedItems(candidate.messageId)
                .firstOrNull { it.id == itemId }
                ?: return null
            val decoded = TemporalPayloadJson.decode(record.payload) ?: return null
            val start = record.dueDateEpochMs ?: return null
            ResolvedTemporal(
                title = record.title,
                startEpochMs = start,
                endEpochMs = decoded.endEpochMs?.takeIf { it > start },
                isDateOnly = decoded.isDateOnly,
                timeZoneId = decoded.timezoneId.ifBlank { java.util.TimeZone.getDefault().id },
                location = decoded.location?.ifBlank { null },
                meetingUrl = decoded.meetingUrl?.ifBlank { null },
            )
        } catch (t: Throwable) {
            MoLogger.w(TAG, "Temporal resolution failed: ${t.javaClass.simpleName}")
            null
        }
    }

    private companion object {
        const val TAG = "CalendarActionExecutor"
    }
}
