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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
 * submit. The trailing action is the account avatar on EVERY tab —
 * there is no overflow menu (Gmail parity, 2026-10-16): tapping the
 * avatar opens the account switcher sheet, and Settings, Privacy,
 * Integrations and Accounts all live inside the Settings page. The
 * DP sits at the far right on every tab, so removing the old menu
 * also resized the pill (it fills the freed space).
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
    onOpenAccounts: () -> Unit,
    account: AccountRecord?,
    modifier: Modifier = Modifier,
) {

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

    // The account DP is ONE size on every tab (2026-10-16 fix): Mail
    // used 32dp (xxl) while the other tabs fell back to the avatar's
    // 24dp (xl) default, so the DP visibly enlarged on Mail and
    // shrank everywhere else. 40dp (xxxl) = the mail row avatar's
    // size, so the top-bar DP matches the avatars in the emails.
    val avatarSize = MoSpacing.xxxl

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .statusBarsPadding()
                // 16dp edge margin, both sides: the search pill (and the
                // hamburger on Mail) never touches the screen edges.
                // The pre-hoist bar was a Material3 TopAppBar, which
                // carries this inset itself; the hoisted Row must add
                // it explicitly, or the pill expands edge-to-edge when
                // the hamburger retracts (2026-10-16 fix).
                .padding(horizontal = 16.dp)
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
                        if (mailState?.isUnified == true) {
                            // Unified inbox: no single active account,
                            // so show the DEFAULT account's DP
                            // (earliest created) — the old envelope
                            // icon read as "mail", not "accounts", and
                            // gave no way to tell whose mailbox this
                            // is (2026-10-16 fix). Tapping opens
                            // Accounts, like every avatar.
                            val defaultAccount = mailState?.accountsById
                                ?.values
                                ?.minWithOrNull(
                                    compareBy<AccountRecord> { it.createdAtEpochMs }
                                        .thenBy { it.accountId },
                                )
                            if (defaultAccount != null) {
                                IconButton(onClick = onOpenAccounts) {
                                    AccountAvatar(
                                        account = defaultAccount,
                                        size = avatarSize,
                                    )
                                }
                            } else {
                                // No accounts at all: keep the envelope
                                // as the Accounts entry point.
                                IconButton(onClick = onOpenAccounts) {
                                    Icon(
                                        imageVector = Icons.Filled.MailOutline,
                                        contentDescription = "Accounts. Tap to switch account.",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(avatarSize),
                                    )
                                }
                            }
                        } else {
                            val activeAccount = mailState?.activeAccount
                            if (activeAccount != null) {
                                IconButton(onClick = onOpenAccounts) {
                                    AccountAvatar(
                                        account = activeAccount,
                                        size = avatarSize,
                                    )
                                }
                            }
                        }
                    } else {
                        // No overflow menu (Gmail parity, 2026-10-16):
                        // the avatar is the only trailing action. It
                        // sits at the far right, and the pill fills
                        // the space the old menu used to take.
                        if (account != null) {
                            IconButton(onClick = onOpenAccounts) {
                                AccountAvatar(
                                    account = account,
                                    size = avatarSize,
                                    modifier = Modifier.clip(CircleShape).clickable(
                                        onClick = onOpenAccounts,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
