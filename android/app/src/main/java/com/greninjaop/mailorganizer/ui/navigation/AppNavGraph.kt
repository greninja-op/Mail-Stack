package com.greninjaop.mailorganizer.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.greninjaop.mailorganizer.data.local.AccountRecord
import com.greninjaop.mailorganizer.data.prefs.AccountSelection
import com.greninjaop.mailorganizer.core.integrations.IntegrationId
import com.greninjaop.mailorganizer.core.integrations.IntegrationSnapshot
import com.greninjaop.mailorganizer.di.AppContainer
import com.greninjaop.mailorganizer.ui.actions.ActionsScreen
import com.greninjaop.mailorganizer.ui.actions.ActionsViewModel
import com.greninjaop.mailorganizer.ui.actions.ActionsViewModelFactory
import com.greninjaop.mailorganizer.ui.auth.ConnectAccountScreen
import com.greninjaop.mailorganizer.ui.auth.ConnectAccountViewModel
import com.greninjaop.mailorganizer.ui.auth.ConnectAccountViewModelFactory
import com.greninjaop.mailorganizer.ui.categories.CategoriesScreen
import com.greninjaop.mailorganizer.ui.categories.CategoriesViewModel
import com.greninjaop.mailorganizer.ui.categories.CategoriesViewModelFactory
import com.greninjaop.mailorganizer.ui.categories.CategoryDetailScreen
import com.greninjaop.mailorganizer.ui.companies.CompaniesScreen
import com.greninjaop.mailorganizer.ui.companies.CompaniesViewModel
import com.greninjaop.mailorganizer.ui.companies.CompaniesViewModelFactory
import com.greninjaop.mailorganizer.ui.companies.CompanyDetailScreen
import com.greninjaop.mailorganizer.ui.home.HomeScreen
import com.greninjaop.mailorganizer.ui.theme.MoMotion
import com.greninjaop.mailorganizer.ui.home.HomeViewModel
import com.greninjaop.mailorganizer.ui.home.HomeViewModelFactory
import com.greninjaop.mailorganizer.ui.integrations.IntegrationsViewModel
import com.greninjaop.mailorganizer.ui.integrations.IntegrationsViewModelFactory
import com.greninjaop.mailorganizer.ui.integrations.UnknownIntegrationScreen
import com.greninjaop.mailorganizer.ui.mail.MailDrawer
import com.greninjaop.mailorganizer.ui.mail.MailScreen
import com.greninjaop.mailorganizer.ui.mail.MailScreenState
import com.greninjaop.mailorganizer.ui.mail.MailViewModel
import com.greninjaop.mailorganizer.ui.mail.MailViewModelFactory
import com.greninjaop.mailorganizer.ui.mail.ThreadScreen
import com.greninjaop.mailorganizer.ui.mail.ThreadViewModel
import com.greninjaop.mailorganizer.ui.mail.ThreadViewModelFactory
import com.greninjaop.mailorganizer.ui.rules.CorrectionViewModel
import com.greninjaop.mailorganizer.ui.rules.CorrectionViewModelFactory
import com.greninjaop.mailorganizer.ui.rules.RuleEditorScreen
import com.greninjaop.mailorganizer.ui.rules.RuleEditorViewModel
import com.greninjaop.mailorganizer.ui.rules.RuleEditorViewModelFactory
import com.greninjaop.mailorganizer.ui.rules.RulesScreen
import com.greninjaop.mailorganizer.ui.rules.RulesViewModel
import com.greninjaop.mailorganizer.ui.rules.RulesViewModelFactory
import com.greninjaop.mailorganizer.ui.search.SearchScreen
import com.greninjaop.mailorganizer.ui.search.SearchViewModel
import com.greninjaop.mailorganizer.ui.search.SearchViewModelFactory
import com.greninjaop.mailorganizer.ui.settings.AccountsScreen
import com.greninjaop.mailorganizer.ui.settings.AccountsViewModel
import com.greninjaop.mailorganizer.ui.settings.AccountsViewModelFactory
import com.greninjaop.mailorganizer.ui.settings.AppearanceViewModel
import com.greninjaop.mailorganizer.ui.settings.AppearanceViewModelFactory
import com.greninjaop.mailorganizer.ui.settings.PrivacyPolicyScreen
import com.greninjaop.mailorganizer.ui.settings.TermsScreen
import com.greninjaop.mailorganizer.ui.settings.SettingsScreen

/**
 * Navigation foundation — Phase 1 (phase-01 §25).
 *
 * Minimal foundation destinations for the full information architecture.
 * Each destination is a real route in the graph; non-home destinations
 * render an honest foundation placeholder until their phase builds them.
 * Later phases add real screens to these routes without a rewrite.
 */
object AppDestinations {
    const val HOME = "home"
    const val MAIL = "mail"
    const val CATEGORIES = "categories"
    const val COMPANIES = "companies"
    const val ACTIONS = "actions"

    const val SEARCH = "search"
    const val INTEGRATIONS = "integrations"
    const val SETTINGS = "settings"
    const val PRIVACY = "privacy"
    const val ACCOUNTS = "accounts"

    /** Privacy Policy document (Settings footer link, 2026-10-16). */
    const val PRIVACY_POLICY = "privacy-policy"

    /** Terms of Service document (Settings footer link, 2026-10-16). */
    const val TERMS = "terms"

    /** Phase 3: Google OAuth connect-account flow. */
    const val CONNECT = "connect"

    /**
     * Import flow (2026-10-12): `import/{accountId}`. Shown after a new
     * account connects so the user can explicitly start the first sync with
     * the animated import experience.
     */
    const val IMPORT = "import/{accountId}"

    fun importRoute(accountId: String): String =
        "import/${android.net.Uri.encode(accountId)}"
    const val CLEANUP = "cleanup"
    const val ANALYTICS = "analytics"
    const val AI_SETTINGS = "settings/ai"

    /**
     * Integration detail route: `integration/{integrationId}` (Phase 17).
     * Unknown ids degrade to a NotFound-style message, never a crash.
     */
    const val INTEGRATION_DETAIL = "integration/{integrationId}"

    fun integrationDetailRoute(integrationId: String): String =
        "integration/$integrationId"

    /** Phase 12: rules list. */
    const val RULES = "rules"

    /**
     * Phase 12: rule editor. [ruleId] is null when creating a new rule.
     * Navigation arg validation (§34): a non-numeric ruleId is treated
     * as "create new" rather than crashing.
     */
    const val RULE_EDITOR = "rules/editor?ruleId={ruleId}"

    fun ruleEditorRoute(ruleId: Long? = null): String =
        if (ruleId == null) "rules/editor" else "rules/editor?ruleId=$ruleId"

    /** Phase 27: Advanced Automation routes. */
    const val AUTOMATION = "automation"
    const val AUTOMATION_EDITOR = "automation/editor?ruleId={ruleId}"

    fun automationEditorRoute(ruleId: String? = null): String =
        if (ruleId == null) "automation/editor" else "automation/editor?ruleId=$ruleId"

    /**
     * Thread route with an optional deep-link focus arg (Phase 6, phase §37):
     * `mail/thread/{threadId}?focusMessageId={messageId}`. Future features
     * (notifications, widgets) can open a specific message without a rewrite.
     */
    const val THREAD = "mail/thread/{threadId}?focusMessageId={focusMessageId}"

    fun threadRoute(threadId: String, focusMessageId: String? = null): String =
        buildString {
            append("mail/thread/").append(threadId)
            if (focusMessageId != null) {
                append("?focusMessageId=").append(focusMessageId)
            }
        }

    /**
     * Search route with an optional initial query (Phase 10): tapping the
     * mailbox search field's IME action opens the dedicated search screen
     * with the typed text (phase §10, §11).
     */
    const val SEARCH_ROUTE = "search?query={query}"

    fun searchRoute(query: String = ""): String =
        "search?query=" + android.net.Uri.encode(query)

    /** Primary destinations, in display order. */
    val primary: List<String> = listOf(HOME, MAIL, CATEGORIES, COMPANIES, ACTIONS)

    /** Secondary destinations, in display order. */
    val secondary: List<String> = listOf(SEARCH, INTEGRATIONS, SETTINGS, PRIVACY, ACCOUNTS)

    /** Every route registered in the graph. */
    val all: List<String> = primary + secondary

    /** Human-readable title for a route. */
    fun titleFor(route: String): String = when (route) {
        HOME -> "Home"
        MAIL -> "Mail"
        CATEGORIES -> "Categories"
        COMPANIES -> "Companies"
        ACTIONS -> "Actions"
        SEARCH -> "Search"
        INTEGRATIONS -> "Integrations"
        SETTINGS -> "Settings"
        PRIVACY -> "Privacy"
        PRIVACY_POLICY -> "Privacy Policy"
        TERMS -> "Terms of Service"
        ACCOUNTS -> "Accounts"
        CLEANUP -> "Cleanup"
        ANALYTICS -> "Analytics"
        else -> route
    }

    /** The future phase that builds this destination's real UI. */
    fun phaseFor(route: String): String = when (route) {
        HOME -> "Phase 11"
        MAIL -> "Phase 6"
        CATEGORIES -> "Phase 11"
        COMPANIES -> "Phase 11"
        ACTIONS -> "Phase 11"
        SEARCH -> "Phase 10"
        INTEGRATIONS -> "Phase 11"
        SETTINGS -> "Phase 11"
        PRIVACY -> "Phase 11"
        ACCOUNTS -> "Phase 11"
        CLEANUP -> "Phase 20"
        ANALYTICS -> "Phase 25"
        else -> "a later phase"
    }

    /** Category detail route: `category/{name}` (MailCategory.name). */
    const val CATEGORY_DETAIL = "category/{categoryName}"

    fun categoryRoute(category: com.greninjaop.mailorganizer.data.local.MailCategory): String =
        "category/${category.name}"

    /** Company detail route: `company/{companyId}`. */
    const val COMPANY_DETAIL = "company/{companyId}"

    fun companyRoute(companyId: String): String =
        "company/" + android.net.Uri.encode(companyId)
}

@Composable
fun AppNavGraph(
    container: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    // 2026-10-13 (redone): single tab-navigation path. Primary tabs use
    // the standard saveState/restoreState pattern — switching tabs keeps
    // each tab's back stack, ViewModel, scroll position and loaded data,
    // so a tab switch never rebuilds or reloads the screen (the old
    // plain navigate() stacked a fresh screen per tap, which read as a
    // refresh). Non-primary routes still push normally.
    val navigatePrimary: (String) -> Unit = { route ->
        if (PrimaryDestination.entries.any { it.route == route }) {
            navController.navigate(route) {
                popUpTo(AppDestinations.HOME) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        } else {
            navController.navigate(route)
        }
    }
    // 2026-10-16 (Expressive redesign): no background artwork — screens
    // sit on the solid theme background (see Color.kt).
    // 2026-10-16 (jelly bubble): the bottom bar is hoisted ABOVE the
    // NavHost so one bar instance stays mounted across tab switches —
    // a per-screen bar remounts on every switch and its pill could only
    // snap, never slide. The bar shows only on primary destinations;
    // pushed routes (thread, settings, ...) get the full screen. The
    // Scaffold applies no window insets of its own: the bar pads itself
    // for the system navigation bar, and each screen owns its
    // remaining insets exactly as before.
    // 2026-10-16 (dynamic search pill): the top bar is hoisted the same
    // way — one search pill stays mounted across tab switches, so it
    // can reshape itself (hamburger slides in from the left, pill
    // shrinks rightward) instead of remounting. The top bar shows only
    // on primary destinations; pushed routes keep their own top bars.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBars = currentRoute in PrimaryDestination.entries.map { it.route }
    val isMail = currentRoute == AppDestinations.MAIL

    // Mail's ViewModel, shared with the Mail destination: same back-stack
    // entry owner, same factory → the same instance MailScreen uses.
    // Only exists while Mail is current.
    val mailVm: MailViewModel? = if (isMail && backStackEntry != null) {
        viewModel(
            viewModelStoreOwner = backStackEntry!!,
            factory = MailViewModelFactory(container),
        )
    } else {
        null
    }
    val mailState = if (mailVm != null) {
        val s by mailVm.state.collectAsState()
        s
    } else {
        null
    }

    // Search query for the non-Mail tabs (Mail's filter lives in
    // MailViewModel). Held here so the hoisted pill keeps its text
    // across tab switches.
    var otherQuery by remember { mutableStateOf("") }
    val searchQuery = if (isMail) mailState?.filterText ?: "" else otherQuery

    // Mailbox drawer: hoisted above the whole Scaffold (2026-10-16), so
    // the sheet + scrim cover the top bar and bottom bar too — exactly
    // like Gmail's drawer. The per-screen drawer (inside MailScreen)
    // only covered the content below the top bar.
    // The hamburger opens it directly via DrawerState — no event flow:
    // the earlier SharedFlow counter replayed its last value and
    // reopened the drawer by itself around tab switches (2026-10-16
    // fix); DrawerState has no replay at all.
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val drawerScope = rememberCoroutineScope()
    // Defensive: the drawer is Mail-only, so make sure it is closed
    // when leaving Mail (it is already closed in practice — the scrim
    // blocks tab switches while it is open — but never leave it open
    // over another tab).
    LaunchedEffect(isMail) {
        if (!isMail) drawerState.close()
    }

    // Account for the top bar's avatar on non-Mail tabs (Mail's
    // actions read it from MailViewModel). A selected account, else
    // the DEFAULT account (earliest created) for unified — the avatar
    // always shows a real DP, never a generic icon (2026-10-16 fix:
    // the envelope icon read as "mail", not "accounts").
    val account: AccountRecord? by remember {
        combine(
            container.accountRepository.observeAll(),
            container.activeAccountPreferences.activeSelection,
        ) { list, selection ->
            val enabled = list.filter { it.isEnabled }
            when (selection) {
                is AccountSelection.Single ->
                    enabled.firstOrNull { it.accountId == selection.accountId }
                        ?: enabled.minWithOrNull(
                            compareBy<AccountRecord> { it.createdAtEpochMs }
                                .thenBy { it.accountId },
                        )
                is AccountSelection.Unified ->
                    enabled.minWithOrNull(
                        compareBy<AccountRecord> { it.createdAtEpochMs }
                            .thenBy { it.accountId },
                    )
            }
        }
    }.collectAsState(initial = null)

    // Tapping the top-bar DP opens the full-screen Settings panel
    // (2026-10-16: the bottom-sheet account switcher was replaced —
    // the panel slides in from the right and fills the screen, with
    // the accounts section at its top). The sheet state + sheet
    // block were removed with it (fix = remove).
    // Mail-scoped: the drawer holds mailbox destinations, so the
    // edge-swipe only opens it on Mail (the hamburger is Mail-only
    // too). Opening it from another tab and picking a destination
    // would set Mail's destination invisibly.
    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isMail,
        drawerContent = {
            // ALWAYS composed — never conditional (2026-10-16 fix for
            // the drawer auto-opening on Mail entry + the freeze on
            // leaving Mail). ModalNavigationDrawer's anchors are
            // derived from the sheet's measured width: when the content
            // was `if (mailState != null)`, entering Mail made the width
            // jump 0 → ~320dp, so the closed offset (0, when empty)
            // suddenly mapped to the OPEN anchor and the drawer opened
            // by itself; leaving Mail collapsed the anchors, the offset
            // went out of bounds, and the scrim stuck over the screen
            // blocking all touches. With the content always present
            // (a default state when not on Mail — the drawer is never
            // openable there anyway), the width and anchors never
            // change, so the drawer only ever opens on a hamburger tap
            // or an edge swipe.
            MailDrawer(
                state = mailState ?: MailScreenState(),
                onSelect = { dest ->
                    mailVm?.setDestination(dest)
                    drawerScope.launch { drawerState.close() }
                },
            )
        },
    ) {
        Scaffold(
            topBar = {
                if (showBars) {
                    MoPrimaryTopBar(
                        isMail = isMail,
                        searchValue = searchQuery,
                        onSearchValueChange = { query ->
                            if (isMail) mailVm?.setFilterText(query)
                            else otherQuery = query
                        },
                        onSearchSubmit = {
                            navController.navigate(
                                AppDestinations.searchRoute(searchQuery),
                            )
                        },
                        onMenuClick = { drawerScope.launch { drawerState.open() } },
                        onOpenSettings = { navController.navigate(AppDestinations.SETTINGS) },
                        account = account,
                    )
                }
            },
            bottomBar = {
                if (showBars) {
                    MoNavBar(
                        currentRoute = currentRoute ?: AppDestinations.HOME,
                        onNavigate = navigatePrimary,
                    )
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { padding ->
        NavHost(
                navController = navController,
                startDestination = AppDestinations.HOME,
                modifier = Modifier.padding(padding),
            // 2026-10-16: tab switches (primary ↔ primary) slide
            // DIRECTIONALLY — the incoming tab's content enters from the
            // side that tab sits on (a tab to the right slides in from the
            // right, and vice versa), so the motion matches the jelly
            // bubble's travel. The slide is opaque (no fade, no scale) and
            // runs on the same 350ms spec as the bubble and the search-pill
            // reshape, so all three land together and the new tab is fully
            // rendered when the bubble stops. The old fade-through (exit
            // 200ms + enter delayed 120ms) left a gap where both screens
            // were semi-transparent, so the platform window background
            // showed through as a white/gray flash — and the animation
            // itself read as a reload. Pushed routes (thread, settings,
            // ...) keep the fade-through. Tab state is preserved via
            // saveState/restoreState (see navigatePrimary), so a switch
            // never reloads a tab.
            enterTransition = {
                // Drilling in from the Settings panel: the deeper
                // screen slides in from the right (2026-10-16).
                if (initialState.destination.route == AppDestinations.SETTINGS) {
                    slideInHorizontally(animationSpec = MoMotion.panelSlide) { it }
                } else if (isTabSwitch(initialState.destination.route, targetState.destination.route)) {
                    val dir = tabSlideDirection(
                        initialState.destination.route,
                        targetState.destination.route,
                    )
                    slideInHorizontally(animationSpec = MoMotion.tabSwitchSlide) { width ->
                        if (dir > 0) width else -width
                    }
                } else {
                    fadeIn(animationSpec = MoMotion.fadeThroughEnter) +
                        scaleIn(initialScale = 0.97f, animationSpec = MoMotion.fadeThroughScaleIn)
                }
            },
            exitTransition = {
                // Opening the Settings panel: the source screen stays
                // put while the panel slides in over it from the
                // right (2026-10-16) — a 0-offset slide is a no-op.
                if (targetState.destination.route == AppDestinations.SETTINGS) {
                    slideOutHorizontally(animationSpec = MoMotion.panelSlide) { 0 }
                } else if (isTabSwitch(initialState.destination.route, targetState.destination.route)) {
                    val dir = tabSlideDirection(
                        initialState.destination.route,
                        targetState.destination.route,
                    )
                    slideOutHorizontally(animationSpec = MoMotion.tabSwitchSlide) { width ->
                        if (dir > 0) -width else width
                    }
                } else {
                    fadeOut(animationSpec = MoMotion.fadeThroughExit) +
                        scaleOut(targetScale = 0.97f, animationSpec = MoMotion.fadeThroughScaleOut)
                }
            },
            popEnterTransition = {
                // Dismissing the Settings panel: the tab underneath
                // is already there — it stays put while the panel
                // slides out to the right (2026-10-16).
                if (initialState.destination.route == AppDestinations.SETTINGS) {
                    slideInHorizontally(animationSpec = MoMotion.panelSlide) { 0 }
                } else if (isTabSwitch(initialState.destination.route, targetState.destination.route)) {
                    val dir = tabSlideDirection(
                        initialState.destination.route,
                        targetState.destination.route,
                    )
                    slideInHorizontally(animationSpec = MoMotion.tabSwitchSlide) { width ->
                        if (dir > 0) width else -width
                    }
                } else {
                    fadeIn(animationSpec = MoMotion.fadeThroughEnter) +
                        scaleIn(initialScale = 0.97f, animationSpec = MoMotion.fadeThroughScaleIn)
                }
            },
            popExitTransition = {
                // Popping back to the Settings panel: the deeper
                // screen slides out to the right while the panel
                // slides back in from the left (2026-10-16).
                if (targetState.destination.route == AppDestinations.SETTINGS) {
                    slideOutHorizontally(animationSpec = MoMotion.panelSlide) { it }
                } else if (isTabSwitch(initialState.destination.route, targetState.destination.route)) {
                    val dir = tabSlideDirection(
                        initialState.destination.route,
                        targetState.destination.route,
                    )
                    slideOutHorizontally(animationSpec = MoMotion.tabSwitchSlide) { width ->
                        if (dir > 0) -width else width
                    }
                } else {
                    fadeOut(animationSpec = MoMotion.fadeThroughExit) +
                        scaleOut(targetScale = 0.97f, animationSpec = MoMotion.fadeThroughScaleOut)
                }
            },
        ) {
            // Phase 11: the real Home dashboard (replaces the Phase 1
            // foundation screen).
            composable(AppDestinations.HOME) {
                val vm: HomeViewModel = viewModel(factory = HomeViewModelFactory(container))
                HomeScreen(
                    viewModel = vm,
                    onNavigate = navigatePrimary,
                    onOpenThread = { threadId ->
                        navController.navigate(AppDestinations.threadRoute(threadId))
                    },
                    onOpenCategory = { category ->
                        navController.navigate(AppDestinations.categoryRoute(category))
                    },
                    onOpenCompany = { companyId ->
                        navController.navigate(AppDestinations.companyRoute(companyId))
                    },
                )
            }
            // Phase 6: the real mailbox. Other destinations keep their honest
            // placeholders until their phase builds them.
            composable(AppDestinations.MAIL) {
                val vm: MailViewModel = viewModel(factory = MailViewModelFactory(container))
                MailScreen(
                    viewModel = vm,
                    onOpenThread = { threadId, focusMessageId ->
                        navController.navigate(
                            AppDestinations.threadRoute(threadId, focusMessageId),
                        )
                    },
                )
            }
            // Phase 11: category browsing + detail.
            composable(AppDestinations.CATEGORIES) {
                val vm: CategoriesViewModel =
                    viewModel(factory = CategoriesViewModelFactory(container))
                CategoriesScreen(
                    viewModel = vm,
                    onNavigate = navigatePrimary,
                    onOpenCategory = { category ->
                        navController.navigate(AppDestinations.categoryRoute(category))
                    },
                    onOpenThread = { threadId ->
                        navController.navigate(AppDestinations.threadRoute(threadId))
                    },
                )
            }
            composable(
                route = AppDestinations.CATEGORY_DETAIL,
                arguments = listOf(
                    navArgument("categoryName") { type = NavType.StringType },
                ),
            ) { entry ->
                val categoryName = entry.arguments?.getString("categoryName")
                val category = runCatching {
                    com.greninjaop.mailorganizer.data.local.MailCategory.valueOf(
                        categoryName ?: "",
                    )
                }.getOrNull()
                if (category == null) {
                    // Unknown category — never crash on a bad deep link.
                    navController.popBackStack()
                } else {
                    val vm: CategoriesViewModel =
                        viewModel(factory = CategoriesViewModelFactory(container))
                    androidx.compose.runtime.LaunchedEffect(category) {
                        vm.selectCategory(category)
                    }
                    CategoryDetailScreen(
                        viewModel = vm,
                        category = category,
                        onOpenThread = { threadId ->
                            navController.navigate(AppDestinations.threadRoute(threadId))
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
            // Phase 11: company browsing + detail.
            composable(AppDestinations.COMPANIES) {
                val vm: CompaniesViewModel =
                    viewModel(factory = CompaniesViewModelFactory(container))
                CompaniesScreen(
                    viewModel = vm,
                    onNavigate = navigatePrimary,
                    onOpenCompany = { companyId ->
                        navController.navigate(AppDestinations.companyRoute(companyId))
                    },
                )
            }
            composable(
                route = AppDestinations.COMPANY_DETAIL,
                arguments = listOf(
                    navArgument("companyId") { type = NavType.StringType },
                ),
            ) { entry ->
                val companyId = entry.arguments?.getString("companyId")
                if (companyId.isNullOrEmpty()) {
                    navController.popBackStack()
                } else {
                    val vm: CompaniesViewModel =
                        viewModel(factory = CompaniesViewModelFactory(container))
                    androidx.compose.runtime.LaunchedEffect(companyId) {
                        vm.selectCompany(companyId)
                    }
                    CompanyDetailScreen(
                        viewModel = vm,
                        onOpenThread = { threadId ->
                            navController.navigate(AppDestinations.threadRoute(threadId))
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
            }
            // Phase 11: actions destination (view-only).
            composable(AppDestinations.ACTIONS) {
                val vm: ActionsViewModel =
                    viewModel(factory = ActionsViewModelFactory(container))
                ActionsScreen(
                    viewModel = vm,
                    onNavigate = navigatePrimary,
                    onOpenThread = { threadId ->
                        navController.navigate(AppDestinations.threadRoute(threadId))
                    },
                )
            }
            // Phase 10: the real search experience (replaces the placeholder).
            composable(
                route = AppDestinations.SEARCH_ROUTE,
                arguments = listOf(
                    navArgument("query") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val initialQuery = entry.arguments?.getString("query").orEmpty()
                val vm: SearchViewModel = viewModel(
                    factory = SearchViewModelFactory(container, initialQuery),
                )
                SearchScreen(
                    viewModel = vm,
                    onOpenThread = { threadId, focusMessageId ->
                        navController.navigate(
                            AppDestinations.threadRoute(threadId, focusMessageId),
                        )
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = AppDestinations.THREAD,
                arguments = listOf(
                    navArgument("threadId") { type = NavType.StringType },
                    navArgument("focusMessageId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                val threadId = entry.arguments?.getString("threadId") ?: return@composable
                val focusMessageId = entry.arguments?.getString("focusMessageId")
                val vm: ThreadViewModel =
                    viewModel(factory = ThreadViewModelFactory(container, threadId))
                val correctionVm: CorrectionViewModel =
                    viewModel(factory = CorrectionViewModelFactory(container))
                ThreadScreen(
                    viewModel = vm,
                    correctionViewModel = correctionVm,
                    focusMessageId = focusMessageId,
                    onBack = { navController.popBackStack() },
                )
            }
            // Phase 12: rules list + editor.
            composable(AppDestinations.RULES) {
                val vm: RulesViewModel =
                    viewModel(factory = RulesViewModelFactory(container))
                RulesScreen(
                    viewModel = vm,
                    onAddRule = { navController.navigate(AppDestinations.ruleEditorRoute()) },
                    onEditRule = { ruleId ->
                        navController.navigate(AppDestinations.ruleEditorRoute(ruleId))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = AppDestinations.RULE_EDITOR,
                arguments = listOf(
                    navArgument("ruleId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { entry ->
                // §34: non-numeric ruleId degrades to "create new", never a crash.
                val ruleId = entry.arguments?.getString("ruleId")?.toLongOrNull()
                val vm: RuleEditorViewModel =
                    viewModel(factory = RuleEditorViewModelFactory(container, ruleId))
                RuleEditorScreen(
                    viewModel = vm,
                    onDone = { navController.popBackStack() },
                    onBack = { navController.popBackStack() },
                )
            }
            // Phase 17: Integration Manager screens — real statuses, honest states.
            composable(AppDestinations.INTEGRATIONS) {
                val vm: IntegrationsViewModel =
                    viewModel(factory = IntegrationsViewModelFactory(container))
                com.greninjaop.mailorganizer.ui.integrations.IntegrationsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onOpenDetail = { id ->
                        navController.navigate(AppDestinations.integrationDetailRoute(id.value))
                    },
                )
            }
            composable(AppDestinations.INTEGRATION_DETAIL) { backStackEntry ->
                val vm: IntegrationsViewModel =
                    viewModel(factory = IntegrationsViewModelFactory(container))
                val rawId = backStackEntry.arguments?.getString("integrationId")
                var snapshot by remember { mutableStateOf<IntegrationSnapshot?>(null) }
                var loaded by remember { mutableStateOf(false) }
                LaunchedEffect(rawId) {
                    snapshot = rawId?.let { vm.snapshotFor(IntegrationId(it)) }
                    loaded = true
                }
                when {
                    !loaded -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }

                    snapshot != null -> com.greninjaop.mailorganizer.ui.integrations.IntegrationDetailScreen(
                        snapshot = snapshot!!,
                        viewModel = vm,
                        onBack = { navController.popBackStack() },
                    )

                    // Unknown integration id: honest not-found, never a crash.
                    else -> UnknownIntegrationScreen(onBack = { navController.popBackStack() })
                }
            }
            composable(
                AppDestinations.SETTINGS,
                // The Settings panel slides in from the right and out
                // to the right, full-screen (2026-10-16) — NOT the
                // fade-through the other pushed routes use.
                enterTransition = {
                    slideInHorizontally(animationSpec = MoMotion.panelSlide) { it }
                },
                exitTransition = {
                    slideOutHorizontally(animationSpec = MoMotion.panelSlide) { -it }
                },
                popEnterTransition = {
                    slideInHorizontally(animationSpec = MoMotion.panelSlide) { -it }
                },
                popExitTransition = {
                    slideOutHorizontally(animationSpec = MoMotion.panelSlide) { it }
                },
            ) {
                val vm: AppearanceViewModel =
                    viewModel(factory = AppearanceViewModelFactory(container))
                val settingsScope = rememberCoroutineScope()
                // The account section at the top of the panel needs
                // the enabled accounts (earliest first, so the first
                // is the default) and the live selection.
                val panelAccounts: List<AccountRecord> by remember {
                    container.accountRepository.observeAll()
                }.collectAsState(initial = emptyList())
                val panelSelection: AccountSelection by remember {
                    container.activeAccountPreferences.activeSelection
                }.collectAsState(initial = AccountSelection.Unified)
                SettingsScreen(
                    appearanceViewModel = vm,
                    onNavigate = navigatePrimary,
                    onBack = { navController.popBackStack() },
                    accounts = panelAccounts
                        .filter { it.isEnabled }
                        .sortedWith(
                            compareBy<AccountRecord> { it.createdAtEpochMs }
                                .thenBy { it.accountId },
                        ),
                    selection = panelSelection,
                    onSelectAccount = { accountId ->
                        settingsScope.launch {
                            container.activeAccountPreferences.setActiveSelection(
                                AccountSelection.Single(accountId),
                            )
                        }
                        navController.popBackStack()
                    },
                    onSelectUnified = {
                        settingsScope.launch {
                            container.activeAccountPreferences.setActiveSelection(
                                AccountSelection.Unified,
                            )
                        }
                        navController.popBackStack()
                    },
                    onAddAccount = { navController.navigate(AppDestinations.CONNECT) },
                )
            }
            composable(AppDestinations.PRIVACY) {
                val vm: com.greninjaop.mailorganizer.ui.privacy.PrivacyViewModel =
                    viewModel(factory = com.greninjaop.mailorganizer.ui.privacy.PrivacyViewModelFactory(container))
                com.greninjaop.mailorganizer.ui.privacy.PrivacyCenterScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                )
            }
            // Settings footer links (2026-10-16): the Privacy Policy
            // and Terms of Service documents. Drilled from Settings,
            // so the NavHost's global transition slides them in from
            // the right — no per-destination transition needed.
            composable(AppDestinations.PRIVACY_POLICY) {
                PrivacyPolicyScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppDestinations.TERMS) {
                TermsScreen(
                    onBack = { navController.popBackStack() },
                )
            }
            composable(AppDestinations.ACCOUNTS) {
                val vm: AccountsViewModel =
                    viewModel(factory = AccountsViewModelFactory(container))
                AccountsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onConnectAccount = { navController.navigate(AppDestinations.CONNECT) },
                )
            }
            // Phase 3: Google OAuth connect-account flow. The screen opens
            // the system browser; the redirect returns via MainActivity.
            composable(AppDestinations.CONNECT) {
                val vm: ConnectAccountViewModel =
                    viewModel(factory = ConnectAccountViewModelFactory(container.authRepository))
                val scope = rememberCoroutineScope()
                ConnectAccountScreen(
                    viewModel = vm,
                    // New accounts (never successfully synced) go to the import
                    // flow; reconnects return to where they came from.
                    onConnected = { accountId ->
                        scope.launch {
                            val record = container.syncStateRepository.ensureForAccount(accountId)
                            if (record.lastSuccessfulSyncEpochMs == null) {
                                navController.navigate(AppDestinations.importRoute(accountId)) {
                                    popUpTo(AppDestinations.CONNECT) { inclusive = true }
                                }
                            } else {
                                navController.popBackStack()
                            }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            // Import flow (2026-10-12): explicit "Import now" + animated
            // flying-mail progress for a newly connected account.
            composable(
                route = AppDestinations.IMPORT,
                arguments = listOf(navArgument("accountId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getString("accountId").orEmpty()
                val vm: com.greninjaop.mailorganizer.ui.import.ImportViewModel =
                    viewModel(
                        factory = com.greninjaop.mailorganizer.ui.import.ImportViewModelFactory(
                            accountId = accountId,
                            accountRepository = container.accountRepository,
                            syncCoordinator = container.syncCoordinator,
                            dispatchers = container.dispatchers,
                        ),
                    )
                com.greninjaop.mailorganizer.ui.import.ImportFlowScreen(
                    viewModel = vm,
                    onDone = {
                        navController.navigate(AppDestinations.MAIL) {
                            popUpTo(AppDestinations.IMPORT) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            // Phase 20: Noise, Newsletter & Cleanup destination
            composable(AppDestinations.CLEANUP) {
                val vm: com.greninjaop.mailorganizer.ui.cleanup.CleanupViewModel =
                    viewModel(factory = com.greninjaop.mailorganizer.ui.cleanup.CleanupViewModelFactory(container))
                com.greninjaop.mailorganizer.ui.cleanup.CleanupScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onOpenThread = { threadId ->
                        navController.navigate(AppDestinations.threadRoute(threadId))
                    },
                )
            }
            // Phase 25: Analytics & Insights destination
            composable(AppDestinations.ANALYTICS) {
                val vm: com.greninjaop.mailorganizer.ui.analytics.AnalyticsViewModel =
                    viewModel(
                        factory = com.greninjaop.mailorganizer.ui.analytics.AnalyticsViewModelFactory(
                            analyticsUseCase = container.analyticsUseCase,
                            accountRepository = container.accountRepository,
                            activeAccountPreferences = container.activeAccountPreferences,
                        ),
                    )
                com.greninjaop.mailorganizer.ui.analytics.AnalyticsScreen(
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                )
            }
            // Phase 26: Optional AI Fallback Settings destination
            composable(AppDestinations.AI_SETTINGS) {
                val vm: com.greninjaop.mailorganizer.ui.settings.AiSettingsViewModel =
                    viewModel(
                        factory = com.greninjaop.mailorganizer.ui.settings.AiSettingsViewModelFactory(
                            preferences = container.aiPreferences,
                            registry = container.aiProviderRegistry,
                            aiFallbackUseCase = container.aiFallbackUseCase,
                            dispatchers = container.dispatchers,
                        ),
                    )
                com.greninjaop.mailorganizer.ui.settings.AiSettingsScreen(
                    viewModel = vm,
                    onNavigate = navigatePrimary,
                    onBack = { navController.popBackStack() },
                )
            }
            // Phase 27: Advanced Automation destinations
            composable(AppDestinations.AUTOMATION) {
                val vm: com.greninjaop.mailorganizer.ui.automation.AutomationViewModel =
                    viewModel(
                        factory = com.greninjaop.mailorganizer.ui.automation.AutomationViewModelFactory(container),
                    )
                com.greninjaop.mailorganizer.ui.automation.AutomationScreen(
                    viewModel = vm,
                    onNavigateToEditor = { ruleId ->
                        navController.navigate(AppDestinations.automationEditorRoute(ruleId))
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = AppDestinations.AUTOMATION_EDITOR,
                arguments = listOf(
                    navArgument("ruleId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                ),
            ) { backStackEntry ->
                val ruleId = backStackEntry.arguments?.getString("ruleId")
                val vm: com.greninjaop.mailorganizer.ui.automation.AutomationViewModel =
                    viewModel(
                        factory = com.greninjaop.mailorganizer.ui.automation.AutomationViewModelFactory(container),
                    )
                com.greninjaop.mailorganizer.ui.automation.AutomationEditorScreen(
                    ruleId = ruleId,
                    viewModel = vm,
                    onBack = { navController.popBackStack() },
                )
            }
        }
        }
        // The bottom-sheet account switcher was removed (2026-10-16):
        // tapping the DP navigates to the Settings panel instead, and
        // the account list lives at the top of that panel.
    }
}

/** True when a navigation moves between two primary tab destinations. */
private fun isTabSwitch(fromRoute: String?, toRoute: String?): Boolean {
    val primary = PrimaryDestination.entries.map { it.route }
    return fromRoute in primary && toRoute in primary
}

/**
 * Direction of a primary tab switch, in bottom-bar order (HOME, MAIL,
 * CATEGORIES, COMPANIES, ACTIONS — left to right): +1 when the target
 * tab sits to the right of the source tab (its content enters from the
 * right), −1 when it sits to the left, 0 when the routes are the same
 * or not both tabs. Only the sign matters — the slide always travels
 * the full screen width.
 */
fun tabSlideDirection(fromRoute: String?, toRoute: String?): Int {
    val from = PrimaryDestination.entries.indexOfFirst { it.route == fromRoute }
    val to = PrimaryDestination.entries.indexOfFirst { it.route == toRoute }
    if (from < 0 || to < 0 || from == to) return 0
    return if (to > from) 1 else -1
}
