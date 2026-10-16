package com.greninjaop.mailorganizer.ui.mail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.data.local.Priority
import com.greninjaop.mailorganizer.data.local.PriorityRecord
import com.greninjaop.mailorganizer.ui.theme.MoSpacing
import com.greninjaop.mailorganizer.ui.theme.rememberMoWarningColors

/**
 * Priority presentation (Phase 9).
 *
 * Only HIGH and CRITICAL get a visible indicator — NORMAL is the default
 * and LOW is deliberately quiet (badges on everything would be noise).
 * Every indicator carries a text label (never color alone) and a content
 * description for screen readers.
 */
data class PriorityVisuals(
    val label: String,
    val contentDescription: String,
)

/** Maps a [Priority] to its row visuals, or null when no badge is shown. */
fun Priority.visuals(): PriorityVisuals? = when (this) {
    Priority.CRITICAL -> PriorityVisuals(
        label = "Critical",
        contentDescription = "Critical priority",
    )
    Priority.HIGH -> PriorityVisuals(
        label = "High priority",
        contentDescription = "High priority",
    )
    Priority.NORMAL, Priority.LOW -> null
}

/**
 * Compact priority badge for mail rows. Renders nothing for NORMAL/LOW.
 *
 * 2026-10-16 (Expressive redesign): solid amber warning pill —
 * pale amber container + amber-brown text in light mode, deep amber
 * container + amber text in dark mode. The label text still
 * carries the meaning (never color alone).
 */
@Composable
fun PriorityBadge(
    priority: Priority,
    modifier: Modifier = Modifier,
) {
    val visuals = remember(priority) { priority.visuals() } ?: return
    val warning = rememberMoWarningColors()
    Surface(
        shape = RoundedCornerShape(MoSpacing.xs),
        color = warning.container,
        contentColor = warning.content,
        modifier = modifier.semantics {
            contentDescription = visuals.contentDescription
        },
    ) {
        Text(
            text = visuals.label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(
                horizontal = MoSpacing.xs,
                vertical = MoSpacing.xxs,
            ),
        )
    }
}

/**
 * Priority with an expandable "Why this priority?" explanation (Phase 9).
 * The reason comes from the persisted [PriorityRecord.reason] — recorded at
 * compute time, so the UI never needs raw email content to explain it.
 * Only shown for HIGH/CRITICAL (NORMAL/LOW are quiet by design).
 */
@Composable
fun PriorityWithExplanation(
    priority: PriorityRecord,
    modifier: Modifier = Modifier,
) {
    if (priority.priority.visuals() == null) return
    var showWhy by remember(priority.messageId) { mutableStateOf(false) }
    val visuals = remember(priority.priority) { priority.priority.visuals()!! }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(MoSpacing.sm))
                .clickable { showWhy = !showWhy }
                .padding(vertical = MoSpacing.xs)
                .semantics {
                    contentDescription =
                        "${visuals.contentDescription}. " +
                            if (showWhy) "Hide explanation." else "Show why."
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PriorityBadge(priority = priority.priority)
            Spacer(Modifier.width(MoSpacing.xs))
            Text(
                text = if (showWhy) "Hide why" else "Why?",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        if (showWhy) {
            val reason = priority.reason
                ?: "No reason was recorded for this priority."
            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = MoSpacing.md,
                    top = MoSpacing.xs,
                    bottom = MoSpacing.xs,
                ),
            )
        }
    }
}
/**
 * Action-required filter chip (Phase 9). Toggles the "action required"
 * view — mail classified ACTION_REQUIRED by the deterministic engine.
 *
 * 2026-10-16 (Expressive redesign): solid amber warning chip —
 * amber exclamation icon + amber text on an amber container, in both
 * modes. The label still carries the meaning.
 */
@Composable
fun ActionRequiredFilterChip(
    active: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val warning = rememberMoWarningColors()
    androidx.compose.material3.FilterChip(
        selected = active,
        onClick = onToggle,
        label = {
            Text(
                text = "Action required",
                style = MaterialTheme.typography.labelMedium,
                color = warning.content,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = warning.content,
                modifier = Modifier.size(MoSpacing.md),
            )
        },
        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
            containerColor = warning.container,
            selectedContainerColor = warning.container,
            labelColor = warning.content,
            selectedLabelColor = warning.content,
            iconColor = warning.content,
            selectedLeadingIconColor = warning.content,
        ),
        modifier = modifier.semantics {
            contentDescription = if (active) {
                "Showing action-required mail. Tap to show all mail."
            } else {
                "Show only action-required mail."
            }
        },
    )
}
