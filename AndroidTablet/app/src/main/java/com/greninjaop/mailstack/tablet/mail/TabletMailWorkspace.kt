package com.greninjaop.mailstack.tablet.mail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val FolderChipsMinWidth = 280.dp

/**
 * Tablet mail list and conversation.
 *
 * The shell owns the drag divider and passes [listFraction], [showList], and
 * [showDetail]. When both panes are on, they share one row by that fraction.
 * When only one is on, that pane uses the full width. Choosing a row shows
 * its body in the detail pane.
 *
 * Copy is [TabletDemoMailbox]. Nothing is sent.
 */
@Composable
fun TabletMailWorkspace(
    listFraction: Float = 0.4f,
    showList: Boolean = true,
    showDetail: Boolean = true,
    stackOnSelect: Boolean = false,
) {
    val fraction = if (listFraction.isFinite()) listFraction.coerceIn(0.2f, 0.8f) else 0.4f
    var messages by remember { mutableStateOf(TabletDemoMailbox.seed()) }
    var folder by remember { mutableStateOf(TabletMailFolder.Primary) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val listVisible = if (stackOnSelect) selectedId == null else showList
    val detailVisible = if (stackOnSelect) selectedId != null else showDetail

    Row(modifier = Modifier.fillMaxSize()) {
        if (listVisible) {
            TabletMailListPane(
                messages = messages,
                folder = folder,
                selectedId = selectedId,
                onFolder = { folder = it },
                onSelect = { id ->
                    selectedId = id
                    messages = messages.map { message ->
                        if (message.id == id && message.unread) message.copy(unread = false) else message
                    }
                },
                onToggleStar = { id ->
                    messages = messages.map { message ->
                        if (message.id == id) message.copy(starred = !message.starred) else message
                    }
                },
                modifier = Modifier
                    .weight(if (detailVisible) fraction else 1f)
                    .fillMaxHeight(),
            )
        }
        if (detailVisible) {
            TabletMailDetailPane(
                message = messages.firstOrNull { it.id == selectedId },
                onBack = if (stackOnSelect) {
                    { selectedId = null }
                } else {
                    null
                },
                modifier = Modifier
                    .weight(if (listVisible) (1f - fraction) else 1f)
                    .fillMaxHeight(),
            )
        }
    }
}

@Composable
private fun TabletMailListPane(
    messages: List<TabletDemoMessage>,
    folder: TabletMailFolder,
    selectedId: String?,
    onFolder: (TabletMailFolder) -> Unit,
    onSelect: (String) -> Unit,
    onToggleStar: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val inFolder = messages.filter { it.folder == folder }
    val spam = messages.filter { it.folder == TabletMailFolder.Spam }
    BoxWithConstraints(modifier) {
        val showChips = maxWidth >= FolderChipsMinWidth
        Column(Modifier.fillMaxSize()) {
            Text(
                text = TabletDemoMailbox.Label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (showChips) {
                FolderChips(selected = folder, onFolder = onFolder)
            }
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                if (inFolder.isEmpty()) {
                    item(key = "empty-folder") {
                        Text(
                            text = "No demo messages in this folder",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp),
                        )
                    }
                } else {
                    items(inFolder, key = { it.id }) { message ->
                        TabletMailRow(
                            message = message,
                            selected = message.id == selectedId,
                            onClick = { onSelect(message.id) },
                            onToggleStar = { onToggleStar(message.id) },
                        )
                    }
                }
                if (spam.isNotEmpty() && folder != TabletMailFolder.Spam) {
                    item(key = "spam-heading") {
                        Text(
                            text = "Spam",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 4.dp),
                        )
                    }
                    items(spam, key = { it.id }) { message ->
                        TabletMailRow(
                            message = message,
                            selected = message.id == selectedId,
                            onClick = { onSelect(message.id) },
                            onToggleStar = { onToggleStar(message.id) },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderChips(
    selected: TabletMailFolder,
    onFolder: (TabletMailFolder) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TabletMailFolder.chips.forEach { chip ->
            FilterChip(
                selected = selected == chip,
                onClick = { onFolder(chip) },
                label = { Text(chip.label) },
            )
        }
    }
}

@Composable
private fun TabletMailDetailPane(
    message: TabletDemoMessage?,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    LaunchedEffect(message?.id) {
        scroll.scrollTo(0)
    }
    if (message == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "No conversation selected",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(horizontal = 24.dp, vertical = 20.dp),
    ) {
        if (onBack != null) {
            Text(
                text = "Back",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clickable(onClick = onBack)
                    .padding(bottom = 12.dp),
            )
        }
        Text(
            text = message.subject,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = message.sender,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = message.address,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = message.timeLabel,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
        Text(
            text = message.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 20.dp),
        )
        Text(
            text = "Local demo. Nothing is sent from this device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}
