package com.greninjaop.mailorganizer.ui.mail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.greninjaop.mailorganizer.ui.theme.MoSpacing
import kotlin.math.abs

/**
 * Gmail-style circular sender avatar (54dp): sender initial on a
 * deterministic theme-container color derived from the sender key.
 *
 * Same discipline as [AccountAvatar]: the color always comes from the
 * theme's container palette (never a fixed hue), so it stays readable in
 * light and dark mode and never carries meaning on its own.
 */
@Composable
fun SenderAvatar(
    key: String,
    displayName: String?,
    emailAddress: String,
    modifier: Modifier = Modifier,
    size: Dp = MoSpacing.rowAvatar,
) {
    val containers = listOf(
        MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer,
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer,
        MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer,
    )
    val index = remember(key) { abs(key.hashCode()) % containers.size }
    val initial = remember(displayName, emailAddress) {
        MailFormatting.avatarInitial(displayName, emailAddress)
    }
    val senderName = remember(displayName, emailAddress) {
        MailFormatting.senderDisplay(displayName, emailAddress)
    }
    val (container, onContainer) = containers[index]
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(container)
            .semantics { contentDescription = "Sender: $senderName" },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            color = onContainer,
        )
    }
}
