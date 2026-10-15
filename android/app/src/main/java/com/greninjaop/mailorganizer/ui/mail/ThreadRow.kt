package com.greninjaop.mailorganizer.ui.mail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.core.company.CompanyDetectionInput
import com.greninjaop.mailorganizer.core.company.CompanyDetector
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.ui.companies.CompanyLogo
import com.greninjaop.mailorganizer.ui.theme.GlassCard
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * One row in the mail list — Gmail conversation-row fidelity (2026-10-12).
 *
 * Layout mirrors the Gmail Android app:
 * - 40dp circular sender avatar at the left (deterministic theme-container
 *   color, sender initial).
 * - Three text lines: sender + timestamp / subject / snippet, with the
 *   unread distinction carried by bold sender+subject and a primary-colored
 *   timestamp (Gmail's own unread treatment).
 * - Star indicator on the right edge, display-only (read-only phase, §6).
 * - No dividers between rows — Gmail has none.
 * - Min height 88dp: slightly roomier than before, still a generous
 *   48dp+ touch target (§42).
 *
 * Long text is ellipsis-bounded so one rogue email can't break the layout
 * (§11). Every row keeps its full content description for screen readers.
 */
@Composable
fun ThreadRow(
    item: ThreadItem,
    account: AccountRecord?,
    showAccountIndicator: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = System.currentTimeMillis()
    val timeText = remember(item.timestampEpochMs, now / 60_000) {
        MailFormatting.relativeTime(item.timestampEpochMs, now)
    }
    val description = remember(item) {
        buildString {
            append(if (item.unread) "Unread email" else "Email")
            append(" from ${item.senderDisplay}")
            append(", subject: ${MailFormatting.subjectDisplay(item.subject)}")
            if (item.snippet.isNotBlank()) append(", ${item.snippet}")
            if (timeText.isNotBlank()) append(", $timeText")
            if (item.messageCount > 1) append(", ${item.messageCount} messages")
            item.priority?.visuals()?.let { append(", ${it.contentDescription}") }
        }
    }
    MailRowLayout(
        sender = item.senderDisplay,
        senderKey = item.threadId,
        senderDomain = item.senderDomain,
        subject = MailFormatting.subjectDisplay(item.subject),
        snippet = item.snippet,
        timeText = timeText,
        unread = item.unread,
        showStar = item.anyStarred,
        showAttachment = item.hasAttachment,
        messageCount = item.messageCount,
        priority = item.priority,
        conversationState = item.conversationState,
        account = account,
        showAccountIndicator = showAccountIndicator,
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = description },
    )
}

/** Row variant for the Starred destination (message-centric). */
@Composable
fun StarredMessageRow(
    item: MessageItem,
    account: AccountRecord?,
    showAccountIndicator: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = System.currentTimeMillis()
    val timeText = remember(item.timestampEpochMs, now / 60_000) {
        MailFormatting.relativeTime(item.timestampEpochMs, now)
    }
    val sender = remember(item.fromName, item.fromAddress) {
        MailFormatting.senderDisplay(item.fromName, item.fromAddress)
    }
    val description = remember(item) {
        "Starred email from $sender, subject: " +
            "${MailFormatting.subjectDisplay(item.subject)}, $timeText"
    }
    MailRowLayout(
        sender = sender,
        senderKey = item.messageId,
        senderDomain = remember(item.fromAddress) {
            CompanyDetector.detect(
                CompanyDetectionInput(
                    fromAddress = item.fromAddress,
                    fromName = item.fromName,
                ),
            )?.normalizedDomain
        },
        subject = MailFormatting.subjectDisplay(item.subject),
        snippet = item.snippet,
        timeText = timeText,
        unread = item.unread,
        showStar = true,
        showAttachment = item.attachments.isNotEmpty(),
        messageCount = 1,
        priority = item.priority,
        account = account,
        showAccountIndicator = showAccountIndicator,
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = description },
    )
}

@Composable
internal fun MailRowLayout(
    sender: String,
    senderKey: String,
    /** Canonical company domain (null = personal sender → letter avatar). */
    senderDomain: String? = null,
    subject: String,
    snippet: String,
    timeText: String,
    unread: Boolean,
    showStar: Boolean,
    showAttachment: Boolean,
    messageCount: Int,
    priority: Priority?,
    conversationState: com.greninjaop.mailorganizer.core.conversation.ConversationState? = null,
    account: AccountRecord?,
    showAccountIndicator: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val emphasis = if (unread) FontWeight.Bold else FontWeight.Normal
    // 2026-10-13 (Breeze redesign): each mail row is a translucent glass
    // card over the background artwork — no dividers, no flat rows.
    GlassCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MoSpacing.rowMinHeight),
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MoSpacing.md, vertical = MoSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        if (showAccountIndicator && account != null) {
            AccountAvatar(account = account, size = MoSpacing.xxl)
            Spacer(Modifier.width(MoSpacing.sm))
        }
        // Company senders show their logo everywhere a mail row appears
        // (2026-10-13); personal senders keep the letter avatar.
        if (senderDomain != null) {
            CompanyLogo(domain = senderDomain, name = sender, size = MoSpacing.rowAvatar)
        } else {
            SenderAvatar(
                key = senderKey,
                displayName = sender,
                emailAddress = sender,
                size = MoSpacing.rowAvatar,
            )
        }
        Spacer(Modifier.width(MoSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            // Line 1: sender (left) + timestamp (right edge).
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = sender,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = emphasis),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (timeText.isNotBlank()) {
                    Spacer(Modifier.width(MoSpacing.xs))
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (unread) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
            Spacer(Modifier.width(MoSpacing.xxs))
            // Line 2: subject.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = subject,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = emphasis),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (priority != null && priority.visuals() != null) {
                    Spacer(Modifier.width(MoSpacing.xs))
                    PriorityBadge(priority = priority)
                }
            }
            // Line 3: snippet.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = snippet.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (showAttachment) {
                    Spacer(Modifier.width(MoSpacing.xs))
                    // No paperclip in the bundled core icon set: a compact text
                    // chip is honest and needs no invented icon.
                    Text(
                        text = "Files",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (messageCount > 1) {
                    Spacer(Modifier.width(MoSpacing.xs))
                    Text(
                        text = "$messageCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            contentDescription = "$messageCount messages in thread"
                        },
                    )
                }
                if (conversationState != null && conversationState != com.greninjaop.mailorganizer.core.conversation.ConversationState.NO_ACTION) {
                    Spacer(Modifier.width(MoSpacing.xs))
                    ConversationStateBadge(state = conversationState)
                }
            }
        }
        // Right edge: star indicator, display-only (read-only phase, §6).
        if (showStar) {
            Spacer(Modifier.width(MoSpacing.sm))
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = "Starred",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(MoSpacing.lg),
            )
        }
    }
    }
}
