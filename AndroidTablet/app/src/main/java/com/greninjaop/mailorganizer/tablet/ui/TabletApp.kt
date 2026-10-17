package com.greninjaop.mailorganizer.tablet.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.greninjaop.mailorganizer.tablet.data.DemoMailbox
import com.greninjaop.mailorganizer.tablet.data.Destination
import com.greninjaop.mailorganizer.tablet.data.ThreadSummary

@Composable
fun TabletApp() {
    var destination by remember { mutableStateOf(Destination.AllInbox) }
    var selected by remember { mutableStateOf<ThreadSummary?>(null) }
    val threads = remember(destination) {
        DemoMailbox.threads.filter {
            when (destination) {
                Destination.Starred -> it.starred
                Destination.Promotional -> it.senderDomain?.contains("shop") == true
                Destination.Social -> it.senderDomain == null
                Destination.Spam -> false
                else -> true
            }
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail {
            Text(
                "Mailstack",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
            )
            Destination.entries.forEach { dest ->
                NavigationRailItem(
                    selected = destination == dest,
                    onClick = {
                        destination = dest
                        selected = null
                    },
                    icon = { Text(dest.name.take(1)) },
                    label = { Text(dest.name) },
                )
            }
        }
        LazyColumn(modifier = Modifier.weight(1f).fillMaxSize()) {
            items(threads, key = { it.threadId }) { thread ->
                MailRow(thread) { selected = thread }
            }
        }
        Surface(modifier = Modifier.weight(1.2f).fillMaxSize()) {
            val t = selected
            if (t == null) {
                Text("Select a conversation", modifier = Modifier.padding(24.dp))
            } else {
                Detail(t)
            }
        }
    }
}

@Composable
private fun MailRow(thread: ThreadSummary, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            thread.senderName,
            fontWeight = if (thread.unread) FontWeight.Bold else FontWeight.Normal,
        )
        Text(
            thread.subject,
            fontWeight = if (thread.unread) FontWeight.SemiBold else FontWeight.Normal,
        )
        Text(
            thread.snippet,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Detail(thread: ThreadSummary) {
    Column(
        modifier = Modifier.padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(thread.subject, style = MaterialTheme.typography.headlineSmall)
        Text(
            thread.senderName + " <" + thread.senderAddress + ">",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(thread.snippet)
        Text("Remote images never fetched (privacy).", style = MaterialTheme.typography.labelSmall)
        Text(
            "Account " + thread.accountId.value + " (demo — never syncs).",
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
