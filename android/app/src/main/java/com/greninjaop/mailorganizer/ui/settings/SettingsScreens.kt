package com.greninjaop.mailorganizer.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.greninjaop.mailorganizer.core.AppDispatchers
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.local.ConnectionState
import com.greninjaop.mailorganizer.data.prefs.AccountSelection
import com.greninjaop.mailorganizer.data.prefs.ThemeMode
import com.greninjaop.mailorganizer.data.prefs.ThemePreferences
import com.greninjaop.mailorganizer.data.repository.AccountRepository
import com.greninjaop.mailorganizer.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import com.greninjaop.mailorganizer.ui.components.JellyBubble
import com.greninjaop.mailorganizer.ui.components.MoEmptyState
import com.greninjaop.mailorganizer.ui.components.pillCoversIcon
import com.greninjaop.mailorganizer.ui.components.rememberJellyBubbleState
import com.greninjaop.mailorganizer.ui.components.segmentCenters
import com.greninjaop.mailorganizer.ui.components.segmentPillHalfWidth
import com.greninjaop.mailorganizer.ui.mail.AccountAvatar
import com.greninjaop.mailorganizer.ui.mail.MailFormatting
import com.greninjaop.mailorganizer.ui.navigation.AppDestinations
import com.greninjaop.mailorganizer.ui.theme.MoCard
import com.greninjaop.mailorganizer.ui.theme.MoSpacing
import kotlin.math.abs
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Settings destination — Phase 11 (phase §39).
 *
 * Only sections backed by implemented functionality: Accounts (the
 * expandable account list at the top of the panel), Appearance
 * (theme), Privacy, Integrations, About. Nothing speculative, nothing
 * that pretends to configure unbuilt features.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appearanceViewModel: AppearanceViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit,
    accounts: List<AccountRecord> = emptyList(),
    selection: AccountSelection = AccountSelection.Unified,
    onSelectAccount: (accountId: String) -> Unit = {},
    onSelectUnified: () -> Unit = {},
    onAddAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            // Accounts (2026-10-16): the account section is the top
            // of this panel — the current account, with a chevron
            // that expands the signed-in list inline (pushing the
            // settings below down). No separate "Accounts" row and
            // no Gmail rows (Manage your Google Account, storage).
            AccountSection(
                accounts = accounts,
                selection = selection,
                onSelectAccount = onSelectAccount,
                onSelectUnified = onSelectUnified,
                onAddAccount = onAddAccount,
            )
            HorizontalDivider(
                modifier = Modifier.padding(
                    horizontal = MoSpacing.md,
                    vertical = MoSpacing.sm,
                ),
            )
            SettingsRow(
                icon = Icons.Filled.Lock,
                title = "Privacy Center",
                subtitle = "Data audit, Google scopes & security hardening",
                onClick = { onNavigate(AppDestinations.PRIVACY) },
            )
            // Phase 12: rules & corrections management (§9).
            SettingsRow(
                icon = Icons.AutoMirrored.Filled.List,
                title = "Rules",
                subtitle = "Automatic sorting and your corrections",
                onClick = { onNavigate(AppDestinations.RULES) },
            )
            SettingsRow(
                icon = Icons.Filled.Build,
                title = "Integrations",
                subtitle = "Calendar, Tasks and more",
                onClick = { onNavigate(AppDestinations.INTEGRATIONS) },
            )
            // Phase 20: noise, newsletter & cleanup system (§34–§37).
            SettingsRow(
                icon = Icons.Filled.MailOutline,
                title = "Cleanup & Newsletters",
                subtitle = "Review newsletters, notifications and low-value mail",
                onClick = { onNavigate(AppDestinations.CLEANUP) },
            )
            // Phase 25: Analytics & Insights
            SettingsRow(
                icon = Icons.Filled.Info,
                title = "Analytics & Insights",
                subtitle = "Local workload, categories, sources & attention patterns",
                onClick = { onNavigate(AppDestinations.ANALYTICS) },
            )
            // Phase 26: Optional AI Fallback Architecture
            SettingsRow(
                icon = Icons.Filled.Build,
                title = "AI Assistance (Optional)",
                subtitle = "Optional secondary fallback for ambiguous emails",
                onClick = { onNavigate(AppDestinations.AI_SETTINGS) },
            )
            // Phase 27: Advanced Automation
            SettingsRow(
                icon = Icons.Filled.Build,
                title = "Advanced Automation",
                subtitle = "Trigger-based rules, safe local actions & execution history",
                onClick = { onNavigate(AppDestinations.AUTOMATION) },
            )
            AppearanceSection(viewModel = appearanceViewModel)
            // Gmail-style footer (2026-10-16): the informational pages
            // are NOT rows in this list — only real settings are.
            SettingsFooter(onNavigate = onNavigate)
        }
    }
}

/**
 * The account section at the top of the Settings panel (2026-10-16).
 *
 * Collapsed it is a single layer: the current selection — the single
 * account, or "All Accounts" (unified) with the default account's DP.
 * There is no "Accounts" label: the account itself IS the section,
 * like Gmail's switcher header. Tapping the header (the chevron on
 * its right is the affordance) expands the signed-in list inline —
 * the other accounts, "All Accounts" when a single account is
 * current, and "Add an account" — pushing the settings below down.
 * Tapping an account switches the whole app to it and closes the
 * panel; tapping "Add an account" opens the Connect flow.
 *
 * No unread counts on the rows: we have no real unread-count source
 * — counts are never fabricated. No "Manage your Google Account" /
 * storage rows: those are Google-Account features we do not have.
 */
@Composable
private fun AccountSection(
    accounts: List<AccountRecord>,
    selection: AccountSelection,
    onSelectAccount: (accountId: String) -> Unit,
    onSelectUnified: () -> Unit,
    onAddAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Collapsed by default: the panel is the Settings page, and the
    // settings below stay visible until the user asks for the list.
    var expanded by remember { mutableStateOf(false) }

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

    Column(modifier = modifier.fillMaxWidth()) {
        // ---- Header: current selection + chevron ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(horizontal = MoSpacing.md, vertical = MoSpacing.sm)
                .semantics {
                    contentDescription = if (expanded) {
                        "Collapse account list"
                    } else {
                        "Expand account list"
                    }
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (currentAccount != null) {
                AccountAvatar(
                    account = currentAccount,
                    size = MoSpacing.xxxl,
                )
            } else {
                // No accounts at all: a generic person mark.
                Box(
                    modifier = Modifier
                        .size(MoSpacing.xxxl)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(MoSpacing.xxl),
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
            Icon(
                imageVector = if (expanded) {
                    Icons.Filled.KeyboardArrowUp
                } else {
                    Icons.Filled.KeyboardArrowDown
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // ---- Collapsible body: the signed-in list + Add ----
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
                            .clickable { onSelectUnified() }
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
                            .clickable { onSelectAccount(account.accountId) }
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAddAccount() }
                        .padding(
                            horizontal = MoSpacing.md,
                            vertical = MoSpacing.sm,
                        )
                        .semantics { contentDescription = "Add an account" },
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
                        text = "Add an account",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        }
    }
}

/**
 * Gmail-style footer at the very bottom of Settings (2026-10-16):
 * small centred links — "Privacy Policy • Terms of Service" — plus a
 * small centred About paragraph beneath. The informational pages are
 * NOT buttons/rows in the Settings list; only real settings are
 * (Privacy Center, Rules, Integrations, Cleanup, Analytics, AI,
 * Automation, Appearance). Each link opens its document screen. The
 * weighted spacer pushes the footer to the bottom when the content
 * is shorter than the screen; when it overflows, the footer simply
 * follows the last row.
 */
@Composable
private fun ColumnScope.SettingsFooter(onNavigate: (String) -> Unit) {
    Spacer(Modifier.weight(1f))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = MoSpacing.lg, bottom = MoSpacing.xs),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FooterLink(
            text = "Privacy Policy",
            onClick = { onNavigate(AppDestinations.PRIVACY_POLICY) },
        )
        Text(
            text = "•",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = MoSpacing.xs),
        )
        FooterLink(
            text = "Terms of Service",
            onClick = { onNavigate(AppDestinations.TERMS) },
        )
    }
    Text(
        text = "Mailstack — your Gmail, organized on your device.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = MoSpacing.md, end = MoSpacing.md, bottom = MoSpacing.lg),
    )
}

@Composable
private fun FooterLink(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "$text. Opens the $text screen." },
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    // 2026-10-14 (Expressive redesign): plain row -> MoCard.
    MoCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MoSpacing.md, vertical = MoSpacing.xs)
            .semantics { contentDescription = "$title. $subtitle." },
        onClick = onClick,
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MoSpacing.md, vertical = MoSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(MoSpacing.md))
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    }
}

/** ViewModel for the Accounts screen. */
class AccountsViewModel(
    private val accountRepository: AccountRepository,
    private val activeAccountPreferences: com.greninjaop.mailorganizer.data.prefs.ActiveAccountPreferences,
    private val integrationManager: com.greninjaop.mailorganizer.domain.integrations.IntegrationManager?,
    private val dispatchers: AppDispatchers,
    private val clock: () -> Long = System::currentTimeMillis,
    private val syncStateRepository: com.greninjaop.mailorganizer.data.repository.SyncStateRepository? = null,
    private val authRepository: com.greninjaop.mailorganizer.data.auth.AuthRepository? = null,
) : ViewModel() {
    val accounts: StateFlow<List<AccountRecord>> =
        accountRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val syncStatesByAccountId: StateFlow<Map<String, String>> =
        if (syncStateRepository != null) {
            accounts.flatMapLatest { list ->
                if (list.isEmpty()) {
                    flowOf(emptyMap())
                } else {
                    val flows = list.map { acc ->
                        syncStateRepository.observe(acc.accountId).map { record ->
                            acc.accountId to com.greninjaop.mailorganizer.data.sync.SyncTimeFormatter.formatLastSynced(
                                record?.lastSuccessfulSyncEpochMs,
                                clock(),
                            )
                        }
                    }
                    combine(flows) { pairs -> pairs.toMap() }
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
        } else {
            MutableStateFlow(emptyMap())
        }

    val activeSelection: StateFlow<com.greninjaop.mailorganizer.data.prefs.AccountSelection> =
        activeAccountPreferences.activeSelection
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified,
            )

    /** The active account id when a single account is active (null if Unified). */
    val activeAccountId: StateFlow<String?> =
        combine(accounts, activeSelection) { list, selection ->
            when (selection) {
                is com.greninjaop.mailorganizer.data.prefs.AccountSelection.Single -> {
                    list.firstOrNull { it.accountId == selection.accountId && it.isEnabled }?.accountId
                }
                com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified -> null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun selectAccount(accountId: String?) {
        viewModelScope.launch(dispatchers.io) {
            val target = if (accountId != null) {
                com.greninjaop.mailorganizer.data.prefs.AccountSelection.Single(accountId)
            } else {
                com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified
            }
            activeAccountPreferences.setActiveSelection(target)
        }
    }

    fun toggleEnabled(account: AccountRecord) {
        viewModelScope.launch(dispatchers.io) {
            val newEnabled = !account.isEnabled
            accountRepository.setEnabled(account.accountId, newEnabled)
            // If the currently active single account was disabled, fallback to next or unified
            val currentSel = activeAccountPreferences.activeSelection.first()
            if (!newEnabled && currentSel is com.greninjaop.mailorganizer.data.prefs.AccountSelection.Single && currentSel.accountId == account.accountId) {
                activeAccountPreferences.setActiveSelection(com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified)
            }
        }
    }

    fun disconnectAccount(accountId: String) {
        viewModelScope.launch(dispatchers.io) {
            // Crash fix (2026-10-12): every cleanup step is best-effort. A
            // throw in any of them (revocation, integration metadata, FTS
            // rows) used to escape this coroutine and crash the app, leaving
            // the account undeleted. The account row itself is always removed.
            runCatching {
                // Phase 3: revoke Google's grant + clear encrypted tokens first.
                authRepository?.disconnect(com.greninjaop.mailorganizer.core.AccountId(accountId))
            }.onFailure { e ->
                com.greninjaop.mailorganizer.core.MoLogger.w(
                    "AccountsViewModel",
                    "Auth disconnect failed: ${e.javaClass.simpleName}",
                )
            }
            runCatching {
                // Integration cleanup
                integrationManager?.handleAccountRemoved(accountId)
            }.onFailure { e ->
                com.greninjaop.mailorganizer.core.MoLogger.w(
                    "AccountsViewModel",
                    "Integration cleanup failed: ${e.javaClass.simpleName}",
                )
            }
            // Delete account cascades to all account-owned rows in Room and cleans FTS.
            // This step must run even if cleanup above failed.
            runCatching {
                accountRepository.deleteById(accountId)
            }.onFailure { e ->
                com.greninjaop.mailorganizer.core.MoLogger.e(
                    "AccountsViewModel",
                    "Account delete failed: ${e.javaClass.simpleName}: ${e.message}",
                    e,
                )
            }
            // If deleted was active, fallback to unified
            runCatching {
                val currentSel = activeAccountPreferences.activeSelection.first()
                if (currentSel is com.greninjaop.mailorganizer.data.prefs.AccountSelection.Single && currentSel.accountId == accountId) {
                    activeAccountPreferences.setActiveSelection(com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified)
                }
            }.onFailure { e ->
                com.greninjaop.mailorganizer.core.MoLogger.w(
                    "AccountsViewModel",
                    "Active-selection fallback failed: ${e.javaClass.simpleName}",
                )
            }
        }
    }

    fun addLocalAccount(email: String, displayName: String?) {
        val trimmed = email.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch(dispatchers.io) {
            val now = clock()
            val id = "local-" + abs(trimmed.lowercase().hashCode())
            val record = AccountRecord(
                accountId = id,
                emailAddress = trimmed,
                displayName = displayName?.trim()?.takeIf { it.isNotEmpty() },
                createdAtEpochMs = now,
                updatedAtEpochMs = now,
                connectionState = ConnectionState.DISCONNECTED, // Honest local state until Phase 3
                isEnabled = true,
            )
            accountRepository.upsert(record)
        }
    }
}

/** Factory for the Accounts screen. */
class AccountsViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AccountsViewModel(
            accountRepository = container.accountRepository,
            activeAccountPreferences = container.activeAccountPreferences,
            integrationManager = container.integrationManager,
            dispatchers = container.dispatchers,
            syncStateRepository = container.syncStateRepository,
            authRepository = container.authRepository,
        ) as T
    }
}

/**
 * Accounts destination — Phase 11 (phase §35–§37).
 *
 * Lists connected accounts with identity and connection state. Adding an
 * account honestly states that OAuth connection arrives with Phase 3 —
 * no fake login form (§35, editor-rules). Real account switching is
 * Phase 18's scope; until then the active account is shown, never hidden
 * (§36, §37).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: AccountsViewModel,
    onBack: () -> Unit,
    onConnectAccount: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val accounts by viewModel.accounts.collectAsState()
    val activeSelection by viewModel.activeSelection.collectAsState()
    val syncStates by viewModel.syncStatesByAccountId.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var disconnectTarget by remember { mutableStateOf<AccountRecord?>(null) }

    if (showAddDialog) {
        AddLocalAccountDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { email, name ->
                showAddDialog = false
                viewModel.addLocalAccount(email, name)
            },
        )
    }

    disconnectTarget?.let { account ->
        AlertDialog(
            onDismissRequest = { disconnectTarget = null },
            title = { Text("Disconnect account?") },
            text = {
                Text(
                    "Disconnecting ${account.displayName ?: account.emailAddress} removes its synced data from this device. Other accounts will remain completely unaffected.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = account.accountId
                        disconnectTarget = null
                        viewModel.disconnectAccount(id)
                    },
                ) {
                    Text("Disconnect", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { disconnectTarget = null }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                title = { Text("Accounts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    TextButton(onClick = { showAddDialog = true }) {
                        Text("Add Local")
                    }
                },
            )
        },
    ) { padding ->
        if (accounts.isEmpty()) {
            MoEmptyState(
                title = "No accounts connected",
                message = "Connect your Gmail account using the official Google " +
                    "sign-in flow. Mailstack requests read-only access — it cannot " +
                    "send, delete, or modify your emails.",
                actionLabel = "Connect Gmail",
                onAction = onConnectAccount,
                modifier = Modifier.padding(padding),
            )
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                // Unified Inbox option
                val isUnified = activeSelection is com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified
                // 2026-10-14 (Expressive redesign): plain row -> MoCard.
                MoCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MoSpacing.md, vertical = MoSpacing.xs),
                    onClick = { viewModel.selectAccount(null) },
                ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MoSpacing.md, vertical = MoSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.MailOutline,
                        contentDescription = null,
                        modifier = Modifier.size(MoSpacing.xxl),
                        tint = if (isUnified) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(MoSpacing.md))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "All Accounts (Unified Inbox)",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isUnified) FontWeight.Bold else FontWeight.Normal,
                            )
                            if (isUnified) {
                                Spacer(Modifier.width(MoSpacing.xs))
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Text(
                            text = "View combined threads across all enabled accounts",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                }


                accounts.forEach { account ->
                    val isAccountActive = when (val sel = activeSelection) {
                        is com.greninjaop.mailorganizer.data.prefs.AccountSelection.Single -> sel.accountId == account.accountId
                        com.greninjaop.mailorganizer.data.prefs.AccountSelection.Unified -> false
                    }
                    // 2026-10-14 (Expressive redesign): plain row -> MoCard.
                    MoCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MoSpacing.md, vertical = MoSpacing.xs)
                            .semantics {
                                contentDescription = buildString {
                                    append(account.displayName ?: account.emailAddress)
                                    if (isAccountActive) append(". Active account.")
                                    append(" Connection: ${account.connectionState.name.lowercase()}.")
                                    if (!account.isEnabled) append(" Account is disabled.")
                                }
                            },
                        onClick = {
                            if (account.isEnabled) {
                                viewModel.selectAccount(account.accountId)
                            }
                        },
                    ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MoSpacing.md, vertical = MoSpacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AccountAvatar(account = account, size = MoSpacing.xxl)
                        Spacer(Modifier.width(MoSpacing.md))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = account.displayName ?: account.emailAddress,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isAccountActive) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (isAccountActive) {
                                    Spacer(Modifier.width(MoSpacing.xs))
                                    Text(
                                        text = "Active",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            Text(
                                text = account.emailAddress,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = connectionText(account),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            val lastSync = syncStates[account.accountId]
                            if (!lastSync.isNullOrEmpty()) {
                                Text(
                                    text = "Last synced: $lastSync",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        IconButton(onClick = { disconnectTarget = account }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Disconnect account ${account.emailAddress}",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    }
                }
                Spacer(Modifier.height(MoSpacing.md))
                // The real Google sign-in must stay reachable even when local/demo
                // accounts exist — the empty-state "Connect Gmail" action only
                // shows when the account list is completely empty.
                OutlinedButton(
                    onClick = onConnectAccount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = MoSpacing.md),
                ) {
                    Text("Connect Google account")
                }
                Spacer(Modifier.height(MoSpacing.xs))
                Text(
                    text = "Connect your real Gmail with the official Google sign-in flow. Local accounts are for testing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = MoSpacing.md),
                )
                Spacer(Modifier.height(MoSpacing.xl))
            }
        }
    }
}

@Composable
private fun AddLocalAccountDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String?) -> Unit,
) {
    var accountEmail by remember { mutableStateOf("") }
    var accountDisplayName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Local Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MoSpacing.sm)) {
                TextField(
                    value = accountEmail,
                    onValueChange = { accountEmail = it },
                    label = { Text("Email address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextField(
                    value = accountDisplayName,
                    onValueChange = { accountDisplayName = it },
                    label = { Text("Display name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(accountEmail, accountDisplayName.takeIf { it.isNotBlank() }) },
                enabled = accountEmail.isNotBlank() && accountEmail.contains("@"),
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

private fun connectionText(account: AccountRecord): String = when (account.connectionState) {
    ConnectionState.CONNECTED -> "Connected" + (
        account.lastSyncEpochMs?.let {
            " · synced ${MailFormatting.relativeTime(it, System.currentTimeMillis())}"
        } ?: ""
        )
    ConnectionState.CONNECTING -> "Connecting…"
    ConnectionState.TOKEN_EXPIRED -> "Sign-in expired — reconnect in Phase 3"
    ConnectionState.DISABLED -> "Disabled"
    ConnectionState.DISCONNECTED -> "Not connected"
}

/**
 * Privacy Policy destination (2026-10-16) — the document the Settings
 * footer's "Privacy Policy" link opens. This is the policy text, NOT
 * the Privacy Center (the functional audit/controls screen, which
 * stays as its own Settings row). Only claims what is actually
 * implemented. No privacy theater.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                title = { Text("Privacy Policy") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MoSpacing.md),
        ) {
            PolicyPoint(
                title = "Local-first",
                body = "Classification, company detection, priority, and search all " +
                    "run on this device. Your email content is never sent to " +
                    "Mail Organizer servers — there are none.",
            )
            PolicyPoint(
                title = "Gmail is the source of truth",
                body = "Mail Organizer keeps a local copy for offline use, search, " +
                    "and organization. The authoritative copy of your mail stays " +
                    "in Gmail.",
            )
            PolicyPoint(
                title = "Account isolation",
                body = "Every stored item is scoped to the Gmail account it came " +
                    "from. Accounts never mix.",
            )
            PolicyPoint(
                title = "No credential storage by Mail Organizer",
                body = "Google sign-in (Phase 3) will use the official Google " +
                    "authentication flow. Mail Organizer never asks for your " +
                    "Google password.",
            )
            PolicyPoint(
                title = "No remote AI",
                body = "There is no AI fallback sending your mail anywhere. " +
                    "Intelligence is deterministic and on-device.",
            )
            Spacer(Modifier.height(MoSpacing.xl))
        }
    }
}

@Composable
private fun PolicyPoint(title: String, body: String) {
    // 2026-10-14 (Expressive redesign): plain column -> MoCard.
    MoCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = MoSpacing.xs),
    ) {
    Column(modifier = Modifier.padding(horizontal = MoSpacing.md, vertical = MoSpacing.sm)) {
        Text(text = title, style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(MoSpacing.xs))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    }
}

/**
 * Terms of Service destination (2026-10-16) — the document the
 * Settings footer's "Terms of Service" link opens. Plain, factual
 * statements about how the app works. Nothing is claimed that the
 * app doesn't do.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                title = { Text("Terms of Service") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = MoSpacing.md),
        ) {
            PolicyPoint(
                title = "What Mailstack is",
                body = "Mailstack is a personal email organizer for your Gmail. " +
                    "It organizes, classifies, and surfaces actions from your " +
                    "mail on your device.",
            )
            PolicyPoint(
                title = "Gmail is the source of truth",
                body = "Your mail stays in Gmail. Mailstack keeps a local copy " +
                    "for offline use, search, and organization. Mailstack never " +
                    "sends or modifies mail without your explicit action.",
            )
            PolicyPoint(
                title = "Your data stays on your device",
                body = "Classification, company detection, priority, and search " +
                    "run on this device. Mailstack has no servers and does " +
                    "not sell or share your data.",
            )
            PolicyPoint(
                title = "Google sign-in",
                body = "Signing in uses Google's official authentication flow. " +
                    "Mailstack never asks for your Google password. Calendar " +
                    "and Tasks access is granted only when you enable those " +
                    "integrations, through Google's permission screen.",
            )
            PolicyPoint(
                title = "Your responsibility",
                body = "You are responsible for the accounts you connect and " +
                    "the actions you confirm (such as sending mail or " +
                    "deleting local data). Disconnecting an account removes " +
                    "its local data from this device; your Gmail is untouched.",
            )
            PolicyPoint(
                title = "No warranty",
                body = "Mailstack is provided as-is, without warranty of any " +
                    "kind. Organization, classification, and suggested actions " +
                    "are best-effort — always check before acting on them.",
            )
            Spacer(Modifier.height(MoSpacing.xl))
        }
    }
}

/**
 * Integrations destination — Phase 17 (phase §20).
 *
 * The Phase 11 placeholder lived here; the real Integration Manager UI
 * now lives in `ui.integrations` (IntegrationsScreen + detail). This file
 * keeps no integration UI anymore.
 */

/** ViewModel for the Appearance section (theme). */
class AppearanceViewModel(
    private val themePreferences: ThemePreferences,
    private val dispatchers: AppDispatchers,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = themePreferences.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch(dispatchers.io) {
            themePreferences.setThemeMode(mode)
        }
    }
}

/** Factory for the Appearance section. */
class AppearanceViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppearanceViewModel(
            themePreferences = container.themePreferences,
            dispatchers = container.dispatchers,
        ) as T
    }
}

/**
 * Appearance settings — the one real setting implemented (theme).
 */
@Composable
fun AppearanceSection(
    viewModel: AppearanceViewModel,
    modifier: Modifier = Modifier,
) {
    val themeMode by viewModel.themeMode.collectAsState()

    // 2026-10-14 (Expressive redesign): plain section -> MoCard.
    // The segmented control inside is untouched.
    MoCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MoSpacing.md, vertical = MoSpacing.sm),
    ) {
    Column(modifier = Modifier.padding(horizontal = MoSpacing.md)) {
        Text(
            text = "Appearance",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(vertical = MoSpacing.sm),
        )
        // 2026-10-16: jelly-bubble picker (same bubble as the bottom
        // nav) replaces the stock SegmentedButton row.
        AppearanceSegmentedControl(
            selectedMode = themeMode,
            onSelect = viewModel::setThemeMode,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(MoSpacing.md))
    }
    }
}

/**
 * Appearance theme picker — System / Light / Dark — with the same
 * jelly bubble as the bottom nav ([JellyBubble]): one pill slides
 * between the three options with squash-and-stretch physics instead
 * of each option flashing its own selected fill. The checkmark and
 * label tint follow the pill — they switch the moment the bubble
 * covers the segment — so bubble + content read as one unit. The
 * checkmark's slot is always laid out (it fades/scales in), so the
 * label never jumps when the check appears.
 *
 * Deliberate deviations from stock M3 SegmentedButton: no dividers
 * between segments (a sliding thumb + divider lines fight each other
 * — every sliding-thumb reference drops them). Shape, colours, and
 * label style stay M3: capsule outline border, secondaryContainer
 * pill, onSecondaryContainer selected content, labelLarge.
 *
 * The theme itself flips instantly on tap (like the page switch under
 * the nav bubble); the pill just animates over the new theme's colours.
 */
@Composable
private fun AppearanceSegmentedControl(
    selectedMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = ThemeMode.entries
    val selectedIndex = modes.indexOf(selectedMode).coerceAtLeast(0)
    val density = LocalDensity.current
    val insetPx = with(density) { PillInset.toPx() }
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    val centers = remember(modes.size, containerWidthPx) {
        segmentCenters(containerWidthPx, modes.size)
    }
    val segmentWidthPx = if (modes.isEmpty()) 0f else containerWidthPx / modes.size
    val pillHalfWidthPx = segmentPillHalfWidth(segmentWidthPx, insetPx)
    val bubble = rememberJellyBubbleState()
    // No ripple on the segments: the sliding pill IS the tap feedback.
    // The default clickable ripple is a rectangle, and it drew a
    // square box behind the pill on every tap (2026-10-16).
    val interactionSource = remember { MutableInteractionSource() }
    val targetCenter = centers.getOrNull(selectedIndex) ?: 0f

    Box(
        modifier
            .fillMaxWidth()
            .height(ControlHeight)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(percent = 50),
            )
            .clip(RoundedCornerShape(percent = 50))
            .onPlaced { coords -> containerWidthPx = coords.size.width.toFloat() },
    ) {
        JellyBubble(
            state = bubble,
            targetCenterPx = targetCenter,
            targetHalfWidthPx = pillHalfWidthPx,
            height = ControlHeight - PillInset * 2,
            topOffset = PillInset,
        )
        Row(Modifier.fillMaxSize()) {
            modes.forEachIndexed { index, mode ->
                val covered = pillCoversIcon(bubble.centerPx, centers[index], pillHalfWidthPx)
                val checkAlpha by animateFloatAsState(
                    targetValue = if (covered) 1f else 0f,
                    animationSpec = tween(150),
                    label = "checkAlpha",
                )
                val checkScale by animateFloatAsState(
                    targetValue = if (covered) 1f else 0.6f,
                    animationSpec = tween(150),
                    label = "checkScale",
                )
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            role = Role.RadioButton,
                            indication = null,
                            interactionSource = interactionSource,
                        ) {
                            if (index != selectedIndex) onSelect(mode)
                        }
                        .semantics {
                            contentDescription = "${mode.name.lowercase()} theme"
                            selected = index == selectedIndex
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(CheckSize),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(CheckSize)
                                    .graphicsLayer {
                                        alpha = checkAlpha
                                        scaleX = checkScale
                                        scaleY = checkScale
                                    },
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                        Spacer(Modifier.width(CheckGap))
                        Text(
                            text = mode.name.lowercase()
                                .replaceFirstChar { it.titlecase() },
                            style = MaterialTheme.typography.labelLarge,
                            color = if (covered) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }
            }
        }
    }
}

/** Material 3 SegmentedButton height. */
private val ControlHeight = 48.dp

/** The bubble floats 4dp inside the capsule on every side. */
private val PillInset = 4.dp

/** Material 3 SegmentedButton selected-check size. */
private val CheckSize = 18.dp

/** Gap between the check and the label. */
private val CheckGap = 6.dp
