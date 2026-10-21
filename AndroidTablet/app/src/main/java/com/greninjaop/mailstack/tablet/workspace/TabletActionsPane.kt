package com.greninjaop.mailstack.tablet.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Demo messages whose subjects mention a task or a pull request.
 *
 * Cards share the row once the pane is wide enough. Opening a card shows
 * the demo body. Nothing is marked complete and nothing is sent.
 */
@Composable
fun TabletActionsPane() {
    val colors = workspaceColors()
    val messages = WorkspaceDemo.messages.filter { message ->
        workspaceSubjectHas(message.subject, "Task") ||
            workspaceSubjectHas(message.subject, "pull request")
    }
    var openId by rememberSaveable { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        if (messages.isEmpty()) {
            WorkspaceEmpty(
                title = "No task or pull request messages in the demo mailbox.",
                message = "Nothing here is sent.",
                colors = colors,
            )
        } else {
            val columns = workspaceColumns(
                availableWidth = maxWidth - WorkspacePanePadding * 2,
                itemCount = messages.size,
                minItem = 300.dp,
                gap = 12.dp,
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(WorkspacePanePadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                messages.chunked(columns).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        row.forEach { message ->
                            ActionCard(
                                message = message,
                                expanded = openId == message.id,
                                colors = colors,
                                onClick = {
                                    openId = if (openId == message.id) null else message.id
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .align(Alignment.Top),
                            )
                        }
                        repeat(columns - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionCard(
    message: WorkspaceDemo.Message,
    expanded: Boolean,
    colors: WorkspaceColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(WorkspaceCardShape)
            .clickable(
                role = Role.Button,
                onClickLabel = "Shows the demo message. Nothing is sent.",
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                contentDescription =
                    "${message.subject}. ${WorkspaceDemo.nothingSent}"
            },
        shape = WorkspaceCardShape,
        color = colors.card,
        contentColor = colors.onSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = message.subject,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface,
            )
            Text(
                text = WorkspaceDemo.nothingSent,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurfaceVariant,
            )
            if (expanded) {
                Text(
                    text = message.sender,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.brand,
                )
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurface,
                )
            }
        }
    }
}
