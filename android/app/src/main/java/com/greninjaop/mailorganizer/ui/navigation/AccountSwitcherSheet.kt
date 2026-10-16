package com.greninjaop.mailorganizer.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.prefs.AccountSelection
import com.greninjaop.mailorganizer.ui.mail.AccountAvatar
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Gmail-style account switcher bottom sheet (2026-10-16).
 *
 * Opens when the top-bar account DP is tapped, on ANY tab. The header
 * shows the current selection — the single account, or "All Accounts"
 * (unified inbox) with the default account's DP — plus a collapse
 * arrow. Expanding shows the account list: tapping an account
 * switches the whole app to it; tapping "All Accounts" switches
 * back to the unified inbox. The current selection is the header,
 * never repeated in the list (Gmail parity).
 *
 * The bottom holds "Add another account" (the Google sign-in flow)
 * and "Settings" (our Settings page — Privacy, Integrations,
 * Accounts and the rest all live inside Settings, so there is no
 * separate overflow menu for them). Gmail's "Manage accounts on
 * this device" / "Manage your Google Account" rows are NOT copied:
 * those are Google-Account features we do not have.
 *
 * No unread counts on the rows: Gmail shows them, but we have no
 * real unread-count source — counts are never fabricated.
 *
 * The sheet opens expanded (like Gmail). The arrow collapses it to
 * just the current account; tapping the arrow again re-opens the
 * list. The collapse is animated (expand/shrink + fade), never a
 * snap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherSheet(
    accounts: List<AccountRecord>,
    selection: AccountSelection,
    onSelectAccount: (accountId: String) -> Unit,
    onSelectUnified: () -> Unit,
    onAddAccount: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Opens expanded, like Gmail's sheet.
    var expanded by remember { mutableStateOf(true) }

    // The current single account, if the selection names one that is
    // still enabled; otherwise the default (earliest) account, so the
    // header never shows a stale or missing account.
    val currentAccount: AccountRecord? = when (selection) {
        is AccountSelection.Single ->
            accounts.firstOrNull { it.accountId == selection.accountId }
                ?: accounts.firstOrNull()
        is AccountSelection.Unified -> accounts.firstOrNull()
    }
    val isUnified = selection is AccountSelection.Unified
    // The list shows the OTHER accounts (never the current one, which
    // is the header). In unified mode every account is "other".
    val otherAccounts = if (isUnified) {
        accounts
    } else {
        accounts.filter { it.accountId != currentAccount?.accountId }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = MoSpacing.md),
        ) {
            // ---- Header: current selection + collapse arrow ----
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MoSpacing.md, vertical = MoSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (currentAccount != null) {
                    AccountAvatar(
                        account = currentAccount,
                        size = 64.dp,
                    )
                } else {
                    // No accounts at all: a generic person mark.
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }
                Spacer(Modifier.width(MoSpacing.md))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (isUnified) {
                            "All Accounts"
                        } else {
                            currentAccount?.displayName ?: currentAccount?.emailAddress
                                ?: "No accounts"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (isUnified) {
                            "View combined threads across all enabled accounts"
                        } else {
                            currentAccount?.emailAddress ?: "Add an account to get started"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .clip(CircleShape)
                        .semantics {
                            contentDescription = if (expanded) {
                                "Collapse account list"
                            } else {
                                "Expand account list"
                            }
                        },
                ) {
                    Icon(
                        imageVector = if (expanded) {
                            Icons.Filled.KeyboardArrowUp
                        } else {
                            Icons.Filled.KeyboardArrowDown
                        },
                        contentDescription = null,
                    )
                }
            }

            // ---- Collapsible body: list + bottom actions ----
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    // "All Accounts" row — only when a single account
                    // is current (in unified mode it IS the header).
                    if (!isUnified) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectUnified()
                                    onDismiss()
                                }
                                .padding(
                                    horizontal = MoSpacing.md,
                                    vertical = MoSpacing.sm,
                                )
                                .semantics {
                                    contentDescription =
                                        "Switch to All Accounts (Unified Inbox)"
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MailOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(MoSpacing.xxl),
                            )
                            Spacer(Modifier.width(MoSpacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = "All Accounts (Unified Inbox)",
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "View combined threads across all enabled accounts",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    otherAccounts.forEach { account ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectAccount(account.accountId)
                                    onDismiss()
                                }
                                .padding(
                                    horizontal = MoSpacing.md,
                                    vertical = MoSpacing.sm,
                                )
                                .semantics {
                                    contentDescription =
                                        "Switch to ${account.displayName ?: account.emailAddress}"
                                },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AccountAvatar(
                                account = account,
                                size = MoSpacing.xxl,
                            )
                            Spacer(Modifier.width(MoSpacing.md))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = account.displayName ?: account.emailAddress,
                                    style = MaterialTheme.typography.bodyLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = account.emailAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = MoSpacing.sm),
                    )

                    // ---- Bottom: Add another account + Settings ----
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onAddAccount()
                                onDismiss()
                            }
                            .padding(
                                horizontal = MoSpacing.md,
                                vertical = MoSpacing.sm,
                            )
                            .semantics {
                                contentDescription = "Add another account"
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = null,
                            )
                        }
                        Spacer(Modifier.width(MoSpacing.md))
                        Text(
                            text = "Add another account",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onOpenSettings()
                                onDismiss()
                            }
                            .padding(
                                horizontal = MoSpacing.md,
                                vertical = MoSpacing.sm,
                            )
                            .semantics {
                                contentDescription = "Open Settings"
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = null,
                            )
                        }
                        Spacer(Modifier.width(MoSpacing.md))
                        Text(
                            text = "Settings",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                    Spacer(Modifier.height(MoSpacing.sm))
                }
            }
        }
    }
}
