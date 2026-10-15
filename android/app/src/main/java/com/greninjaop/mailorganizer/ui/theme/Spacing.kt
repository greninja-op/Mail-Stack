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
     * Mail + company row geometry (2026-10-15): back to Gmail parity —
     * the 2026-10-13 35% enlargement left every row with dead space
     * (120dp cards for ~60dp of content), so rows are compact again and
     * the space goes to showing more mail. All lists share these tokens
     * so the sizing is identical in Mail, company detail, thread view,
     * search, and Companies.
     *
     * Gmail sizes: mail row 88dp, row avatar 40dp, company row 64dp,
     * thread header 48dp, search row 72dp.
     */
    val rowMinHeight: Dp = 88.dp
    val rowAvatar: Dp = 40.dp
    val companyRowMinHeight: Dp = 64.dp
    val messageHeaderMinHeight: Dp = 48.dp
    val searchRowMinHeight: Dp = 72.dp

    /** The grid unit every token above is a multiple of. */
    val grid: Dp = 4.dp
}
