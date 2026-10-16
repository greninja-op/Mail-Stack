package com.greninjaop.mailorganizer.ui.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.ui.components.MoSearchField
import com.greninjaop.mailorganizer.ui.mail.AccountAvatar
import com.greninjaop.mailorganizer.ui.mail.MailScreenState
import com.greninjaop.mailorganizer.ui.mail.SyncUiState
import com.greninjaop.mailorganizer.ui.theme.MoMotion
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * The primary destinations' top bar, hoisted above the NavHost (2026-10-16)
 * — the same reason the bottom bar is hoisted: one bar instance stays
 * mounted across tab switches, so the search pill can reshape itself
 * instead of remounting and snapping.
 *
 * Dynamic search pill: only the Mail tab has the hamburger (the three
 * lines). When the Mail tab becomes current, the hamburger slides in
 * from the left edge and the pill shrinks rightward to make room for it;
 * leaving Mail, the hamburger slides back out to the left and the pill
 * expands to fill the space. The pill never enters from the right —
 * its left edge is pushed by the hamburger. The reshape runs on the
 * same 350ms spec as the page slide and the jelly bubble, so the top
 * bar, the page, and the bubble land together.
 *
 * The search field itself is the same [MoSearchField] on every tab.
 * On Mail it edits the mailbox filter (from [mailState]); on the other
 * tabs it holds a local query and opens the dedicated Search screen on
 * submit. The trailing actions swap by tab (Mail: refresh + account;
 * others: account + overflow) with a short fade.
 *
 * Shown only on primary destinations; pushed routes (thread, settings,
 * ...) keep their own top bars.
 */
@Composable
fun MoPrimaryTopBar(
    isMail: Boolean,
    searchValue: String,
    onSearchValueChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onMenuClick: () -> Unit,
    mailState: MailScreenState?,
    onRefresh: () -> Unit,
    onOpenAccounts: () -> Unit,
    account: AccountRecord?,
    onOpenSecondary: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }

    // The hamburger's width animates 0 ↔ 48dp. The icon inside slides
    // from the left edge (offset = width − 48dp) while the container
    // grows, so it reads as entering from the left and pushing the pill
    // rightward — never as the pill entering from the right. The pill
    // (weight 1f) fills whatever width remains, so it reshapes itself
    // automatically as the hamburger grows or shrinks.
    val hamburgerWidth by animateDpAsState(
        targetValue = if (isMail) 48.dp else 0.dp,
        animationSpec = MoMotion.topBarReshape,
        label = "hamburger-width",
    )

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                .height(64.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(hamburgerWidth)
                    .clipToBounds(),
            ) {
                IconButton(
                    onClick = onMenuClick,
                    enabled = isMail,
                    modifier = Modifier.offset {
                        IntOffset(
                            (hamburgerWidth - 48.dp).roundToPx(),
                            0,
                        )
                    },
                ) {
                    Icon(
                        Icons.Filled.Menu,
                        contentDescription = "Open mailbox menu",
                    )
                }
            }
            MoSearchField(
                value = searchValue,
                onValueChange = onSearchValueChange,
                onSearchSubmit = onSearchSubmit,
                placeholder = "Search mail",
                modifier = Modifier.weight(1f),
            )
            AnimatedContent(
                targetState = isMail,
                transitionSpec = {
                    fadeIn(animationSpec = MoMotion.fadeThroughExit) togetherWith
                        fadeOut(animationSpec = MoMotion.fadeThroughExit)
                },
                label = "top-bar-actions",
            ) { mail ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mail) {
                        val syncing = mailState?.syncUi is SyncUiState.Syncing
                        IconButton(onClick = onRefresh, enabled = !syncing) {
                            Icon(
                                Icons.Filled.Refresh,
                                contentDescription = "Sync now",
                            )
                        }
                        if (mailState?.isUnified == true) {
                            IconButton(onClick = onOpenAccounts) {
                                Icon(
                                    imageVector = Icons.Filled.MailOutline,
                                    contentDescription = "All Accounts (Unified Inbox). Tap to switch account.",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        } else {
                            val activeAccount = mailState?.activeAccount
                            if (activeAccount != null) {
                                IconButton(onClick = onOpenAccounts) {
                                    AccountAvatar(
                                        account = activeAccount,
                                        size = MoSpacing.xxl,
                                    )
                                }
                            }
                        }
                    } else {
                        if (account != null) {
                            IconButton(onClick = onOpenAccounts) {
                                AccountAvatar(
                                    account = account,
                                    modifier = Modifier.clip(CircleShape).clickable(
                                        onClick = onOpenAccounts,
                                    ),
                                )
                            }
                        }
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "More options",
                            )
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                        ) {
                            AppDestinations.secondary
                                .filter { it != AppDestinations.SEARCH }
                                .forEach { route ->
                                    DropdownMenuItem(
                                        text = { Text(AppDestinations.titleFor(route)) },
                                        onClick = {
                                            menuOpen = false
                                            onOpenSecondary(route)
                                        },
                                    )
                                }
                        }
                    }
                }
            }
        }
    }
}
