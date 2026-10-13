package com.greninjaop.mailorganizer.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized spacing tokens — Phase 1 (source of truth: phase-01 §22).
 *
 * Strict 4dp grid. Do not introduce off-grid values (13dp, 17dp, …) without
 * a genuine component-specific reason documented at the call site.
 */
object MoSpacing {
    val xxs: Dp = 4.dp
    val xs: Dp = 8.dp
    val sm: Dp = 12.dp
    val md: Dp = 16.dp
    val lg: Dp = 20.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 40.dp
    val huge: Dp = 48.dp
    val giant: Dp = 64.dp

    /**
     * Mail + company row geometry (2026-10-13): every mail/company row is
     * 35% roomier than the old sizes — the ROW grows, not the gap between
     * rows. All lists share these tokens so the sizing is identical in
     * Mail, company detail, thread view, search, and Companies.
     *
     * Old → new: mail row 88 → 120dp, row avatar 40 → 54dp, company row
     * 64 → 86dp, thread header 48 → 65dp, search row 72 → 97dp.
     */
    val rowMinHeight: Dp = 120.dp
    val rowAvatar: Dp = 54.dp
    val companyRowMinHeight: Dp = 86.dp
    val messageHeaderMinHeight: Dp = 65.dp
    val searchRowMinHeight: Dp = 97.dp

    /** The grid unit every token above is a multiple of. */
    val grid: Dp = 4.dp
}
