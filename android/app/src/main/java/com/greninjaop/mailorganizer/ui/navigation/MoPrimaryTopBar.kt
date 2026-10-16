package com.greninjaop.mailorganizer.ui.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.ui.components.MoSearchField
import com.greninjaop.mailorganizer.ui.mail.AccountAvatar
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
 * On Mail it edits the mailbox filter; on the other
 * tabs it holds a local query and opens the dedicated Search screen on
 * submit. The trailing action is the account avatar on EVERY tab —
 * there is no overflow menu (Gmail parity, 2026-10-16): tapping the
 * avatar opens the full-screen Settings panel (it slides in from
 * the right; the accounts section is at its top), and Privacy,
 * Integrations and the rest all live inside that panel.
 *
 * The DP NEVER animates (2026-10-16 fix): it used to sit inside an
 * AnimatedContent keyed on the tab, so it crossfaded (flickered) on
 * every tab switch even though the avatar itself never changed. It is
 * now one [AccountAvatar] rendered from the single [account] source on
 * every tab — same account, same size, no animation — so leaving Mail
 * cannot make it flicker. And it is CENTRED in the trailing region
 * (2026-10-16 fix): the avatar sits in a fixed 64dp box spanning from
 * the search pill's end to the screen's right edge, centred inside it,
 * so the gap pill → avatar equals the gap avatar → screen edge (12dp
 * each side). It never sits jammed against the edge.
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
    onOpenSettings: () -> Unit,
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
                // 16dp left margin: the search pill (and the hamburger
                // on Mail) never touches the left screen edge. NO right
                // padding — the trailing avatar region (64dp, below)
                // spans to the screen's right edge, and the avatar is
                // centred inside it (2026-10-16 fix), so the pill's
                // width is unchanged and the avatar is equidistant
                // from the pill and the edge.
                // The pre-hoist bar was a Material3 TopAppBar, which
                // carries this inset itself; the hoisted Row must add
                // it explicitly, or the pill expands edge-to-edge when
                // the hamburger retracts (2026-10-16 fix).
                .padding(start = 16.dp)
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
            // Trailing avatar: NO AnimatedContent (2026-10-16 fix). The
            // avatar is one [AccountAvatar] from the single [account]
            // source on every tab — Mail's unified mode and the other
            // tabs resolve to the same account (the default / selected
            // one), so there is nothing to crossfade between, and the
            // crossfade was the flicker on leaving Mail. No accounts
            // at all → the envelope stays as the Settings entry point.
            //
            // Centred in the 64dp trailing region (2026-10-16 fix):
            // the region spans from the pill's end to the screen's
            // right edge, and the 48dp IconButton (40dp avatar inside)
            // is centred in it — gap pill → avatar = gap avatar →
            // screen edge = 12dp each side. The pill keeps its exact
            // current width: the region replaces the old 48dp button
            // + 16dp Row padding (48 + 16 = 64).
            Box(
                modifier = Modifier.width(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(onClick = onOpenSettings) {
                    if (account != null) {
                        AccountAvatar(
                            account = account,
                            size = avatarSize,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.MailOutline,
                            contentDescription = "Settings. Tap to switch account.",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(avatarSize),
                        )
                    }
                }
            }
        }
    }
}
