package com.greninjaop.mailstack.tablet.mail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.floorMod
import kotlin.math.sin

/** Light brand violet. Dark surfaces use the lighter brand violet so the time stays readable. */
internal val TabletBrandViolet = Color(0xFF5238E8)
internal val TabletBrandVioletOnDark = Color(0xFFB9A8FF)

@Composable
internal fun tabletBrandViolet(): Color =
    if (isSystemInDarkTheme()) TabletBrandVioletOnDark else TabletBrandViolet

/**
 * Flat mail row on the pane background: 40dp letter avatar, sender and time,
 * subject, snippet. Minimum height 88dp. Unread sender and subject are bold.
 * Unread time uses brand violet.
 */
@Composable
fun TabletMailRow(
    message: TabletDemoMessage,
    selected: Boolean,
    onClick: () -> Unit,
    onToggleStar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val emphasis = if (message.unread) FontWeight.Bold else FontWeight.Normal
    val timeColor = if (message.unread) {
        tabletBrandViolet()
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
            )
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .semantics(mergeDescendants = true) { selected = selected }
                .clickable(role = Role.Button, onClick = onClick),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LetterAvatar(message)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = message.sender,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = emphasis,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = message.timeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = timeColor,
                        maxLines = 1,
                    )
                }
                Text(
                    text = message.subject,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = emphasis,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = message.snippet.ifBlank { "—" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(
            onClick = onToggleStar,
            modifier = Modifier.semantics {
                contentDescription = if (message.starred) "Starred" else "Star"
            },
        ) {
            StarMark(
                filled = message.starred,
                tint = if (message.starred) {
                    tabletBrandViolet()
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun StarMark(filled: Boolean, tint: Color) {
    Canvas(
        modifier = Modifier
            .size(20.dp)
            .clearAndSetSemantics {},
    ) {
        val path = starPath(size.minDimension)
        if (filled) {
            drawPath(path, color = tint)
        } else {
            drawPath(path, color = tint, style = Stroke(width = 1.5.dp.toPx()))
        }
    }
}

private fun starPath(extent: Float): Path {
    val path = Path()
    val center = extent / 2f
    val outer = extent / 2f * 0.92f
    val inner = outer * 0.46f
    for (point in 0 until 10) {
        val radius = if (point % 2 == 0) outer else inner
        val angle = -Math.PI / 2.0 + point * Math.PI / 5.0
        val x = center + (radius * cos(angle)).toFloat()
        val y = center + (radius * sin(angle)).toFloat()
        if (point == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

@Composable
private fun LetterAvatar(message: TabletDemoMessage) {
    val containers = listOf(
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
    )
    val (container, onContainer) = containers[floorMod(message.id.hashCode(), containers.size)]
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(container)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message.initial,
            style = MaterialTheme.typography.titleMedium,
            color = onContainer,
        )
    }
}
