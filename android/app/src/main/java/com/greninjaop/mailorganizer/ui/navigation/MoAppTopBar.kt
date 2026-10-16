package com.greninjaop.mailorganizer.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.ui.components.MoSearchField
import com.greninjaop.mailorganizer.ui.mail.AccountAvatar

/**
 * Shared top app bar for the Phase 11 primary destinations (phase §7,
 * §34–§36): the same Gmail-style search pill as the mailbox (2026-10-13),
 * the account identity (tapping it opens Accounts), and an overflow menu
 * for the secondary destinations (Integrations, Settings, Privacy,
 * Accounts).
 *
 * Submitting the search field opens the dedicated Search screen with the
 * typed query; the current destination stays readable via the selected
 * bottom-nav item (§34).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoAppTopBar(
    account: AccountRecord?,
    onOpenSearch: (String) -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenSecondary: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        title = {
            MoSearchField(
                value = query,
                onValueChange = { query = it },
                onSearchSubmit = { onOpenSearch(query) },
                placeholder = "Search mail",
            )
        },
        modifier = modifier,
        actions = {
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
        },
    )
}
