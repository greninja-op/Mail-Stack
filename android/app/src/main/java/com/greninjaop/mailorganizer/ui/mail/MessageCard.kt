package com.greninjaop.mailorganizer.ui.mail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import com.greninjaop.mailorganizer.core.company.CompanyDetectionInput
import com.greninjaop.mailorganizer.core.company.CompanyDetector
import com.greninjaop.mailorganizer.core.temporal.ExtractedTemporal
import com.greninjaop.mailorganizer.domain.write.GmailWriteOperation as WriteOp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.data.local.ClassificationRecord
import com.greninjaop.mailorganizer.data.local.PriorityRecord
import com.greninjaop.mailorganizer.ui.companies.CompanyLogo
import com.greninjaop.mailorganizer.ui.theme.MoCard
import com.greninjaop.mailorganizer.ui.theme.MoSpacing
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Message reveal animation (2026-10-17): expanding a message used to be a
 * bare `if (expanded)` — the body snapped in instantly, a flash, not a
 * reveal. The reveal is now a smooth height + fade, Material 3 standard:
 * the height grows from the top over 350 ms (FastOutSlowIn — fast start,
 * gentle settle), while the content fades in over the first 200 ms so the
 * text never looks stretched. Collapse is a touch faster (300 ms height,
 * 150 ms fade-out — the content leaves first so it never looks squashed).
 * The chevron rotates 0° → 180° over 300 ms on the same easing instead of
 * swapping icons. The "to me ▾" header-details disclosure uses the same
 * enter/exit, so every reveal in the thread feels like one motion.
 */
private val MessageRevealEnter =
    expandVertically(
        expandFrom = Alignment.Top,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
    ) + fadeIn(animationSpec = tween(durationMillis = 200))
private val MessageRevealExit =
    shrinkVertically(
        shrinkTowards = Alignment.Top,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
    ) + fadeOut(animationSpec = tween(durationMillis = 150))
private const val MESSAGE_CHEVRON_DURATION_MS = 300

/**
 * Conversation message as a Gmail-style card (2026-10-12).
 *
 * Each message is an M3 filled-tonal Card (12dp corners): header row with
 * the sender's 40dp avatar, sender name, a "to me ▾" recipient line that
 * expands to the full headers, date at the right edge, star display and
 * the existing overflow menu for write actions. Collapsed messages show
 * only the header (existing expand/collapse behavior, newest expanded).
 *
 * Body text uses typography.bodyLarge (16sp Roboto — Gmail's message font).
 * All Phase 7/9/12/13/22 semantics are preserved: classification chips are
 * now neutral tonal chips, the correction affordance, temporal items, the
 * "Show details" header disclosure, HTML/text rendering, link handling and
 * attachment metadata are unchanged. Write actions stay behind the
 * confirmation dialog exactly as before.
 */
@Composable
fun MessageCard(
    message: MessageItem,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    /** Phase 7: message-level classification; null when not classified yet. */
    classification: ClassificationRecord? = null,
    /** Phase 9: message-level priority; null when not prioritized yet. */
    priority: PriorityRecord? = null,
    /**
     * Phase 12: correction entry point. When non-null, a "Correct" affordance
     * is shown next to the category/priority chips (§4).
     */
    onCorrect: (() -> Unit)? = null,
    /** Phase 13: extracted meetings/deadlines; empty when none extracted. */
    temporalItems: List<ExtractedTemporal> = emptyList(),
    /**
     * Phase 22: per-message Gmail write actions. When non-null and the
     * message is synced with Gmail, an overflow menu offers mark
     * read/unread, star/unstar, archive, and trash — each opening an
     * explicit confirmation dialog first (never executed directly).
     */
    onWriteAction: ((MessageItem, com.greninjaop.mailorganizer.domain.write.GmailWriteOperation) -> Unit)? = null,
    /**
     * Fetch-on-open state (2026-10-13). While LOADING the body is being
     * fetched from Gmail; FAILED keeps the honest "No content available"
     * fallback. Null/absent = no fetch in flight.
     */
    bodyFetchState: BodyFetchState? = null,
) {
    // "Show details" disclosure lives in the header ("to me ▾"); hoisted so
    // the header toggle and the body details share one state.
    var showDetails by remember(message.messageId) { mutableStateOf(false) }
    // 2026-10-16 (Expressive redesign): solid tonal card.
    MoCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = MoSpacing.xs),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            MessageHeader(
                message = message,
                expanded = expanded,
                showDetails = showDetails,
                onToggleDetails = { showDetails = !showDetails },
                onToggleExpanded = onToggleExpanded,
                onWriteAction = onWriteAction,
            )
            AnimatedVisibility(
                visible = expanded,
                enter = MessageRevealEnter,
                exit = MessageRevealExit,
            ) {
                MessageBody(
                    message = message,
                    showDetails = showDetails,
                    classification = classification,
                    priority = priority,
                    onCorrect = onCorrect,
                    temporalItems = temporalItems,
                    onOpenLink = onOpenLink,
                    bodyFetchState = bodyFetchState,
                    modifier = Modifier.padding(
                        start = MoSpacing.md,
                        end = MoSpacing.md,
                        bottom = MoSpacing.md,
                    ),
                )
            }
        }
    }
}

@Composable
private fun MessageHeader(
    message: MessageItem,
    expanded: Boolean,
    showDetails: Boolean,
    onToggleDetails: () -> Unit,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier,
    onWriteAction: ((MessageItem, com.greninjaop.mailorganizer.domain.write.GmailWriteOperation) -> Unit)? = null,
) {
    val now = System.currentTimeMillis()
    val timeText = remember(message.timestampEpochMs, now / 60_000) {
        MailFormatting.relativeTime(message.timestampEpochMs, now)
    }
    val sender = remember(message.fromName, message.fromAddress) {
        MailFormatting.senderDisplay(message.fromName, message.fromAddress)
    }
    // Chevron rotation (2026-10-17): one arrow that rotates 0° → 180° on
    // the reveal easing, instead of swapping Down/Up icons in a flash.
    val detailsChevronRotation by animateFloatAsState(
        targetValue = if (showDetails) 180f else 0f,
        animationSpec = tween(durationMillis = MESSAGE_CHEVRON_DURATION_MS, easing = FastOutSlowInEasing),
        label = "message-details-chevron",
    )
    val expandChevronRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = MESSAGE_CHEVRON_DURATION_MS, easing = FastOutSlowInEasing),
        label = "message-expand-chevron",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MoSpacing.messageHeaderMinHeight)
            .clickable(onClick = onToggleExpanded)
            .padding(horizontal = MoSpacing.md, vertical = MoSpacing.sm)
            .semantics {
                contentDescription =
                    "$sender, ${MailFormatting.subjectDisplay(message.subject)}, " +
                        "${if (expanded) "expanded" else "collapsed"}"
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Company senders show their logo (2026-10-13); personal senders
        // keep the letter avatar.
        val senderDomain = remember(message.fromAddress) {
            CompanyDetector.detect(
                CompanyDetectionInput(
                    fromAddress = message.fromAddress,
                    fromName = message.fromName,
                ),
            )?.normalizedDomain
        }
        if (senderDomain != null) {
            CompanyLogo(domain = senderDomain, name = sender)
        } else {
            SenderAvatar(
                key = message.messageId,
                displayName = message.fromName,
                emailAddress = message.fromAddress,
            )
        }
        Spacer(Modifier.width(MoSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = sender,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = if (message.unread) FontWeight.Bold else FontWeight.Normal,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (timeText.isNotBlank()) {
                    Spacer(Modifier.width(MoSpacing.xs))
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            // Gmail-style recipient line: "to me ▾" expands to full headers.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable(onClick = onToggleDetails)
                    .semantics {
                        contentDescription =
                            if (showDetails) "Hide email details" else "Show email details"
                    },
            ) {
                Text(
                    text = "to me",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(MoSpacing.lg).rotate(detailsChevronRotation),
                )
                if (message.attachments.isNotEmpty()) {
                    Spacer(Modifier.width(MoSpacing.xs))
                    val count = message.attachments.size
                    Text(
                        text = if (count == 1) "1 file" else "$count files",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!expanded) {
                Text(
                    text = message.snippet.ifBlank { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        // Star is display-only (read-only phase, §6).
        if (message.starred) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Starred",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .padding(start = MoSpacing.xs)
                    .size(MoSpacing.lg),
            )
        }
        IconButton(onClick = onToggleExpanded) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse message" else "Expand message",
                modifier = Modifier.rotate(expandChevronRotation),
            )
        }
        // Phase 22: overflow menu with Gmail write actions. Only for synced
        // messages — fixture rows never get write affordances.
        if (onWriteAction != null && message.syncedWithGmail) {
            MessageWriteMenu(message = message, onWriteAction = onWriteAction)
        }
    }
}

/**
 * Overflow menu for per-message Gmail write actions (Phase 22).
 *
 * Every item only *requests* the action — the confirmation dialog owns
 * execution (phase §15). Menu labels reflect current state (e.g. "Mark as
 * unread" when the message is read).
 */
@Composable
private fun MessageWriteMenu(
    message: MessageItem,
    onWriteAction: (MessageItem, com.greninjaop.mailorganizer.domain.write.GmailWriteOperation) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    androidx.compose.foundation.layout.Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.semantics { contentDescription = "Message actions" },
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "Message actions",
            )
        }
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            val items = listOf(
                (if (message.unread) WriteOp.MARK_READ else WriteOp.MARK_UNREAD) to
                    if (message.unread) "Mark as read" else "Mark as unread",
                (if (message.starred) WriteOp.UNSTAR else WriteOp.STAR) to
                    if (message.starred) "Remove star" else "Star",
                WriteOp.ARCHIVE to "Archive",
                WriteOp.TRASH to "Move to trash",
            )
            for ((op, label) in items) {
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onWriteAction(message, op)
                    },
                )
            }
        }
    }
}

@Composable
private fun MessageBody(
    message: MessageItem,
    showDetails: Boolean,
    onOpenLink: (String) -> Unit,
    modifier: Modifier = Modifier,
    classification: ClassificationRecord? = null,
    /** Phase 9: message-level priority; null when not prioritized yet. */
    priority: PriorityRecord? = null,
    /** Phase 12: correction entry point (§4). */
    onCorrect: (() -> Unit)? = null,
    /** Phase 13: extracted meetings/deadlines. */
    temporalItems: List<ExtractedTemporal> = emptyList(),
    /** Fetch-on-open state (2026-10-13); null when no fetch is in flight. */
    bodyFetchState: BodyFetchState? = null,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MoSpacing.sm),
    ) {
        // Phase 7: category chip with explainable "why" (§52, §55).
        if (classification != null) {
            CategoryChipWithExplanation(classification = classification)
        }
        // Phase 9: priority with explainable "why" — same discipline as
        // classification: the reason was recorded at compute time, so the
        // UI never needs raw email content to explain it.
        if (priority != null) {
            PriorityWithExplanation(priority = priority)
        }
        // Phase 13: deadlines & meetings with explainable "why" — same
        // discipline: explanations were recorded at extraction time.
        if (temporalItems.isNotEmpty()) {
            TemporalSection(items = temporalItems, onOpenLink = onOpenLink)
        }
        // Phase 12: "Correct" affordance next to the chips, plus the
        // "Set by you" marker comes from the records' overridden flag (§7).
        if (onCorrect != null && (classification != null || priority != null)) {
            Text(
                text = "Correct",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onCorrect)
                    .padding(vertical = MoSpacing.xs)
                    .semantics { contentDescription = "Correct category or priority" },
            )
        }
        // Full headers, disclosed from the header's "to me ▾" line (§21) —
        // same reveal motion as the body (see MessageRevealEnter/Exit).
        AnimatedVisibility(
            visible = showDetails,
            enter = MessageRevealEnter,
            exit = MessageRevealExit,
        ) {
            HeaderDetails(message)
        }

        val html = message.bodyHtml
        val text = message.bodyText
        when {
            bodyFetchState == BodyFetchState.LOADING && html.isNullOrBlank() && text.isNullOrBlank() -> {
                // Fetch-on-open in flight: honest loading state, never the
                // "no content" fallback while Gmail may still deliver.
                BodyLoadingIndicator()
            }
            !html.isNullOrBlank() && html.length <= LARGE_BODY_CHARS -> {
                val blocks = remember(html) { SafeHtmlRenderer.render(html) }
                if (blocks.isNotEmpty()) {
                    SafeHtmlText(blocks = blocks, onOpenLink = onOpenLink)
                } else {
                    EmptyBodyText()
                }
            }
            !html.isNullOrBlank() -> {
                // Enormous body: plain-text fallback keeps the UI responsive.
                val fallbackText = remember(html) {
                    com.greninjaop.mailorganizer.core.email.HtmlSanitizer.htmlToText(html)
                }
                Text(
                    text = fallbackText,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            !text.isNullOrBlank() -> {
                Text(text = text, style = MaterialTheme.typography.bodyLarge)
            }
            else -> EmptyBodyText()
        }

        if (message.attachments.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(MoSpacing.xs)) {
                message.attachments.forEach { attachment ->
                    AttachmentRow(
                        filename = attachment.filename,
                        mimeType = attachment.mimeType,
                        sizeBytes = attachment.sizeBytes,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBodyText() {
    Text(
        text = "No content available",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Fetch-on-open loading state (2026-10-13): the body is on its way from
 * Gmail. Strict M3 — indeterminate LinearProgressIndicator with a plain
 * status line, no custom artwork.
 */
@Composable
private fun BodyLoadingIndicator() {
    Column(verticalArrangement = Arrangement.spacedBy(MoSpacing.xs)) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        Text(
            text = "Loading message…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun HeaderDetails(message: MessageItem) {
    val fullDate = remember(message.timestampEpochMs) {
        if (message.timestampEpochMs <= 0L) {
            "Unknown date"
        } else {
            DateTimeFormatter.ofPattern("EEE, MMM d, yyyy h:mm a", Locale.US)
                .format(
                    Instant.ofEpochMilli(message.timestampEpochMs)
                        .atZone(ZoneId.systemDefault()),
                )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(MoSpacing.xxs)) {
        DetailRow(label = "From", value = detailAddress(message.fromName, message.fromAddress))
        if (message.toAddresses.isNotEmpty()) {
            DetailRow(label = "To", value = message.toAddresses.joinToString(", "))
        }
        if (message.ccAddresses.isNotEmpty()) {
            DetailRow(label = "Cc", value = message.ccAddresses.joinToString(", "))
        }
        DetailRow(label = "Date", value = fullDate)
    }
}

private fun detailAddress(name: String?, address: String): String {
    val n = name?.trim()?.takeIf { it.isNotEmpty() }
    val a = address.trim()
    return when {
        n != null && a.isNotEmpty() -> "$n <$a>"
        a.isNotEmpty() -> a
        else -> "Unknown sender"
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(MoSpacing.giant),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun AttachmentRow(
    filename: String?,
    mimeType: String,
    sizeBytes: Long,
) {
    val size = remember(sizeBytes) { MailFormatting.formatBytes(sizeBytes) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.semantics {
            contentDescription = "Attachment: ${filename ?: "unnamed"}, " +
                "$mimeType${if (size.isNotBlank()) ", $size" else ""}"
        },
    ) {
        // Text-only row: the bundled core icon set has no paperclip, and the
        // filename + size + type already identify the attachment.
        Column {
            Text(
                text = filename?.takeIf { it.isNotBlank() } ?: "Unnamed attachment",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = listOf(
                mimeType.substringAfter('/').uppercase(Locale.US).takeIf { it.isNotBlank() },
                size.takeIf { it.isNotBlank() },
            ).filterNotNull().joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private const val LARGE_BODY_CHARS = 500_000
