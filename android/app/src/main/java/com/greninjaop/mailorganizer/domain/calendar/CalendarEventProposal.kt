package com.greninjaop.mailorganizer.domain.calendar

import com.greninjaop.mailorganizer.core.actions.ActionCandidate
import com.greninjaop.mailorganizer.data.calendar.EventWrite
import java.security.MessageDigest

/**
 * Structured calendar event proposal (Phase 15 §16).
 *
 * Built from a MEETING [ActionCandidate] plus the Phase 13 temporal
 * details. Only data actually available is included — nothing is
 * invented (§16, §19–§21). A proposal is a *preview*, not an action:
 * the engine only executes it after explicit user confirmation (§25).
 */
data class CalendarEventProposal(
    /** Mailstack account the action belongs to — never mixed (§9, §10, §36). */
    val accountId: String,
    /** Target calendar; blank until resolved (primary writable default). */
    val calendarId: String,
    val title: String,
    val description: String,
    val startEpochMs: Long,
    /** Null when unknown — never invented (§21). */
    val endEpochMs: Long?,
    /** True for date-only items (§20). */
    val isAllDay: Boolean,
    val timeZoneId: String,
    /** Verbatim extracted location; never geocoded (§23). */
    val location: String?,
    /** Verbatim extracted meeting URL; never visited (§24). */
    val meetingUrl: String?,
    val sourceMessageId: String,
    val sourceActionId: String,
    /**
     * Deterministic idempotency key, stable across retries for the same
     * (account, calendar, action). Retries can never create duplicates
     * (§27, §29).
     */
    val idempotencyKey: String,
    /** Client-generated iCalUID, stable for the same idempotency key. */
    val iCalUID: String,
) {
    fun toEventWrite(): EventWrite = EventWrite(
        calendarId = calendarId,
        summary = title,
        description = description.ifBlank { null },
        location = location,
        startEpochMs = startEpochMs,
        endEpochMs = endEpochMs,
        isAllDay = isAllDay,
        timeZoneId = timeZoneId,
        iCalUID = iCalUID,
        idempotencyKey = idempotencyKey,
    )
}

/**
 * Temporal details resolved from Phase 13 extraction (never reinterpreted
 * here — §19). Null when the source item can't be resolved; the builder
 * then falls back to candidate-level data only.
 */
data class ResolvedTemporal(
    val title: String,
    val startEpochMs: Long,
    val endEpochMs: Long?,
    val isDateOnly: Boolean,
    val timeZoneId: String,
    val location: String?,
    val meetingUrl: String?,
)

/** Proposal validation verdict (Phase 15 §30: never execute invalid). */
sealed interface ProposalValidation {
    data object Valid : ProposalValidation
    data class Invalid(val reason: String) : ProposalValidation
}

object CalendarProposalBuilder {

    /**
     * Builds a proposal from a MEETING candidate and its resolved
     * temporal details. Returns null when the candidate lacks the
     * minimum executable data (no start time).
     */
    fun build(
        candidate: ActionCandidate,
        temporal: ResolvedTemporal?,
        calendarId: String = "",
    ): CalendarEventProposal? {
        val start = temporal?.startEpochMs ?: candidate.dueDateEpochMs ?: return null
        if (start <= 0) return null

        val title = titleFor(candidate, temporal)
        val isAllDay = temporal?.isDateOnly == true
        val end = temporal?.endEpochMs?.takeIf { it > start }
        val timeZoneId = temporal?.timeZoneId?.ifBlank { null }
            ?: java.util.TimeZone.getDefault().id
        val sourceActionId = candidate.id.ifBlank { return null }
        val key = idempotencyKeyFor(candidate.accountId, calendarId, sourceActionId)

        return CalendarEventProposal(
            accountId = candidate.accountId,
            calendarId = calendarId,
            title = title,
            description = descriptionFor(candidate, temporal),
            startEpochMs = start,
            endEpochMs = end,
            isAllDay = isAllDay,
            timeZoneId = timeZoneId,
            location = temporal?.location?.ifBlank { null },
            meetingUrl = temporal?.meetingUrl?.ifBlank { null },
            sourceMessageId = candidate.messageId,
            sourceActionId = sourceActionId,
            idempotencyKey = key,
            iCalUID = "$key@mailstack.local",
        )
    }

    /**
     * Deterministic idempotency key for (account, calendar, action).
     * Stable across retries and process restarts; distinct per account
     * so Account A's action can never collide with Account B's (§10, §36).
     */
    fun idempotencyKeyFor(
        accountId: String,
        calendarId: String,
        sourceActionId: String,
    ): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val input = "calendar-v1|$accountId|$calendarId|$sourceActionId"
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.take(16).joinToString("") { "%02x".format(it) }
    }

    /**
     * Title strategy (Phase 15 §17):
     * explicit event title → candidate title → safe generic title.
     * Never blank, never a bare "Meeting".
     */
    fun titleFor(candidate: ActionCandidate, temporal: ResolvedTemporal?): String {
        val explicit = temporal?.title?.trim().orEmpty()
        if (explicit.isNotBlank()) return explicit
        val candidateTitle = candidate.title.trim()
        if (candidateTitle.isNotBlank()) return candidateTitle
        return "Calendar event"
    }

    /**
     * Description (Phase 15 §18): minimal provenance only. Never the
     * email body, never tokens, never internal database ids.
     */
    fun descriptionFor(candidate: ActionCandidate, temporal: ResolvedTemporal?): String =
        buildString {
            append("Added from Mailstack.")
            val url = temporal?.meetingUrl?.trim().orEmpty()
            if (url.isNotBlank()) {
                append("\n\nJoin: ").append(url)
            }
        }

    /**
     * Validates a proposal before execution. Invalid proposals are never
     * executed — the action stays unexecuted and the user gets an honest
     * reason (§21, §30).
     */
    fun validate(proposal: CalendarEventProposal): ProposalValidation {
        if (proposal.title.isBlank()) {
            return ProposalValidation.Invalid("The event has no title.")
        }
        if (proposal.startEpochMs <= 0) {
            return ProposalValidation.Invalid("The event has no start time.")
        }
        val end = proposal.endEpochMs
        if (end != null && end <= proposal.startEpochMs) {
            return ProposalValidation.Invalid("The event ends before it starts.")
        }
        if (end == null && !proposal.isAllDay) {
            // §21: never invent a duration. The user must supply the end
            // time (future UI) — until then the action can't execute.
            return ProposalValidation.Invalid(
                "The end time is unknown. The event can't be created without it.",
            )
        }
        if (proposal.calendarId.isBlank()) {
            return ProposalValidation.Invalid("No target calendar was chosen.")
        }
        return ProposalValidation.Valid
    }

    /**
     * Human-readable preview for the confirmation dialog (§25): what
     * will happen, when, in which calendar, and what's unknown. The
     * dialog must show this before the user confirms.
     */
    fun previewText(
        proposal: CalendarEventProposal,
        calendarName: String,
    ): String = buildString {
        append("Add to Calendar?\n\n")
        append(proposal.title).append("\n\n")
        append(formatWhen(proposal)).append("\n")
        append("Calendar: ").append(calendarName).append("\n")
        val location = proposal.location
        if (!location.isNullOrBlank()) {
            append("Location: ").append(location).append("\n")
        }
        val url = proposal.meetingUrl
        if (!url.isNullOrBlank()) {
            append("Join: ").append(url).append("\n")
        }
        if (proposal.isAllDay) {
            append("\nAll-day event (no time was specified).")
        }
    }

    private fun formatWhen(proposal: CalendarEventProposal): String {
        val zone = runCatching { java.time.ZoneId.of(proposal.timeZoneId) }
            .getOrDefault(java.time.ZoneId.systemDefault())
        return if (proposal.isAllDay) {
            val date = java.time.Instant.ofEpochMilli(proposal.startEpochMs)
                .atZone(zone).toLocalDate()
            date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
        } else {
            val fmt = java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy h:mm a")
            val start = java.time.OffsetDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(proposal.startEpochMs), zone,
            ).format(fmt)
            val end = proposal.endEpochMs?.let {
                java.time.OffsetDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(it), zone,
                ).format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
            }
            if (end != null) "$start – $end (${zone.id})" else "$start (${zone.id})"
        }
    }
}
