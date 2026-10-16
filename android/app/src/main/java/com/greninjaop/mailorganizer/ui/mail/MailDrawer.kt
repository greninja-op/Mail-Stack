package com.greninjaop.mailorganizer.ui.mail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Gmail-fidelity mailbox drawer (2026-10-16).
 *
 * Hoisted above the whole Scaffold in AppNavGraph, so the sheet + scrim
 * cover the top bar and bottom bar too — exactly like Gmail's drawer.
 * The old per-screen drawer (inside MailScreen) only covered the
 * content below the top bar, and its rows were too large (M3's
 * NavigationDrawerItem defaults: 56dp rows, 16sp label, padded column).
 *
 * Geometry copied from Gmail's drawer (see the reference screenshots):
 * 48dp rows, 14sp labels, 24dp icons, 12dp icon gap, 12dp horizontal
 * inset, full-width selected pill, one divider after the account
 * header, no gaps between rows. No trailing counts — Mailstack has
 * no confirmed unread-count source, and counts are never fabricated.
 *
 * The account header (All Accounts / Unified Inbox, or the single
 * account's avatar + name + email) is kept — it is existing
 * functionality showing which account context the mailbox is in —
 * but compacted to Gmail's density.
 */
@Composable
fun MailDrawer(
    state: MailScreenState,
    onSelect: (MailboxDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalDrawerSheet(modifier = modifier) {
        val account = state.activeAccount
        if (state.isUnified || account != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (state.isUnified) {
                    Icon(
                        imageVector = Icons.Filled.MailOutline,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "All Accounts",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "Unified Inbox",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (account != null) {
                    AccountAvatar(account = account, size = 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = account.displayName ?: account.emailAddress,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = account.emailAddress,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            HorizontalDivider()
        }
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            MailboxDestination.entries.forEach { dest ->
                MailDrawerRow(
                    dest = dest,
                    selected = state.destination == dest,
                    onClick = { onSelect(dest) },
                )
            }
        }
    }
}

/**
 * One drawer row at Gmail's density.
 *
 * M3's NavigationDrawerItem fixes 56dp rows (16dp vertical padding)
 * and offers no padding parameter, so the row is built from M3
 * primitives with the same tokens — full-width 28dp pill,
 * secondaryContainer when selected, labelLarge (14sp), 24dp icon,
 * 12dp icon gap — at Gmail's 48dp height.
 */
@Composable
private fun MailDrawerRow(
    dest: MailboxDestination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        contentColor = if (selected) {
            MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier.fillMaxWidth().height(48.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = dest.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = dest.title,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
            )
        }
    }
}

private val MailboxDestination.icon
    @Composable get() = when (this) {
        MailboxDestination.ALL_INBOX -> Icons.Filled.Email
        MailboxDestination.PRIMARY -> Icons.Filled.MailOutline
        MailboxDestination.PROMOTIONAL -> Icons.Filled.ShoppingCart
        MailboxDestination.SOCIAL -> Icons.Filled.Person
        MailboxDestination.SPAM -> Icons.Filled.Warning
        MailboxDestination.STARRED -> Icons.Filled.Star
    }
