package com.greninjaop.mailorganizer.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.ui.theme.MoSpacing

/**
 * Skeleton loading (2026-10-16) — Boneyard-style: the placeholder is a
 * pixel-faithful ghost of the real UI, not generic boxes. Each skeleton
 * row reuses the real mail row's geometry (same 88dp min height, same
 * padding, same 40dp avatar, same three text lines), so the layout does
 * not jump when the real content replaces it. The shimmer is hand-rolled
 * (an infinite gradient sweep) — no third-party shimmer library, no CSS.
 *
 * The mail list has exactly three states: content, empty, skeleton
 * (loading). The skeleton shows while the first batch is loading; the
 * empty state shows only once loading has finished with no mail.
 */

/** A shimmering placeholder bar — the "bone" of one text line or chip. */
@Composable
fun SkeletonBar(
    widthFraction: Float,
    height: Dp = 14.dp,
    shimmer: Brush,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(7.dp))
            .background(shimmer),
    )
}

/**
 * One skeleton mail row, mirroring [MailRowLayout]'s geometry exactly:
 * 40dp avatar circle, sender line + time chip, subject line, snippet
 * line. Same row height, same padding, same gaps — so the switch from
 * skeleton to real rows is a pure content swap, never a layout shift.
 */
@Composable
fun MailRowSkeleton(
    shimmer: Brush,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MoSpacing.rowMinHeight)
            .padding(horizontal = MoSpacing.md, vertical = MoSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(MoSpacing.rowAvatar)
                .clip(CircleShape)
                .background(shimmer),
        )
        Spacer(Modifier.width(MoSpacing.md))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(MoSpacing.xxs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonBar(widthFraction = 0.45f, height = 16.dp, shimmer = shimmer)
                Spacer(Modifier.width(MoSpacing.xs))
                SkeletonBar(widthFraction = 0.12f, height = 12.dp, shimmer = shimmer)
            }
            SkeletonBar(widthFraction = 0.75f, height = 14.dp, shimmer = shimmer)
            SkeletonBar(widthFraction = 0.58f, height = 14.dp, shimmer = shimmer)
        }
    }
}

/** The loading state of the mail list: a screenful of skeleton rows. */
@Composable
fun MailListSkeleton(
    modifier: Modifier = Modifier,
    rows: Int = 10,
) {
    val shimmer = rememberShimmerBrush()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = "Loading mail" },
        contentPadding = PaddingValues(
            horizontal = MoSpacing.md,
            vertical = MoSpacing.sm,
        ),
        verticalArrangement = Arrangement.spacedBy(MoSpacing.xs),
        userScrollEnabled = false,
    ) {
        items(rows) {
            MailRowSkeleton(shimmer = shimmer)
        }
    }
}

/**
 * The shimmer sweep: a light diagonal band travelling left-to-right
 * across the placeholder colour, on a slow loop. Built from the theme's
 * surfaceContainerHigh so it reads the same in light and dark mode.
 */
@Composable
private fun rememberShimmerBrush(): Brush {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = Color.White.copy(alpha = 0.35f)
    val transition = rememberInfiniteTransition(label = "skeleton-shimmer")
    val offset by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Restart,
        ),
        label = "skeleton-offset",
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(offset * 600f, 0f),
        end = Offset(offset * 600f + 400f, 200f),
    )
}
