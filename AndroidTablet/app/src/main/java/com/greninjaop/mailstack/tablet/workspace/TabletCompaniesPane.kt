package com.greninjaop.mailstack.tablet.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Companies named on the local demo mailbox.
 *
 * Wide panes keep the company list beside that company's messages.
 * Narrow panes show the list, then the messages. Pin order is view state
 * only and is not saved as mail.
 */
@Composable
fun TabletCompaniesPane() {
    val colors = workspaceColors()
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var pinnedRaw by rememberSaveable { mutableStateOf("") }
    val pinned = pinnedRaw.split('|').filter { it.isNotEmpty() }.toSet()
    val companies = companyRows(pinned)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        if (companies.isEmpty()) {
            WorkspaceEmpty(
                title = "No companies are named on these messages.",
                message = "Company names come only from the local demo mailbox.",
                colors = colors,
            )
        } else {
            val wide = maxWidth >= WorkspaceSideBySideMin
            val shown = shownCompany(wide, selected, companies)
            if (wide) {
                Row(modifier = Modifier.fillMaxSize()) {
                    CompanyList(
                        companies = companies,
                        selected = shown,
                        pinned = pinned,
                        colors = colors,
                        onSelect = { selected = it },
                        onTogglePin = { name -> pinnedRaw = toggleToken(pinned, name) },
                        modifier = Modifier
                            .width(listWidth(maxWidth))
                            .fillMaxHeight(),
                    )
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(colors.outline),
                    )
                    CompanyMessages(
                        name = shown,
                        colors = colors,
                        showBack = false,
                        onBack = {},
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            } else if (selected == null) {
                CompanyList(
                    companies = companies,
                    selected = null,
                    pinned = pinned,
                    colors = colors,
                    onSelect = { selected = it },
                    onTogglePin = { name -> pinnedRaw = toggleToken(pinned, name) },
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CompanyMessages(
                    name = selected,
                    colors = colors,
                    showBack = true,
                    onBack = { selected = null },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun CompanyList(
    companies: List<CompanyRow>,
    selected: String?,
    pinned: Set<String>,
    colors: WorkspaceColors,
    onSelect: (String) -> Unit,
    onTogglePin: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp),
    ) {
        companies.forEachIndexed { index, company ->
            CompanyRowView(
                company = company,
                selected = company.name == selected,
                pinned = company.name in pinned,
                colors = colors,
                onSelect = { onSelect(company.name) },
                onTogglePin = { onTogglePin(company.name) },
            )
            if (index < companies.lastIndex) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 68.dp)
                        .height(1.dp)
                        .background(colors.outline),
                )
            }
        }
    }
}

@Composable
private fun CompanyRowView(
    company: CompanyRow,
    selected: Boolean,
    pinned: Boolean,
    colors: WorkspaceColors,
    onSelect: () -> Unit,
    onTogglePin: () -> Unit,
) {
    val countLabel = workspaceMessageCount(company.count)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) colors.card else colors.background),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 64.dp)
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Shows this company's messages",
                    onClick = onSelect,
                )
                .semantics(mergeDescendants = true) {
                    contentDescription = "${company.name}, $countLabel"
                }
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = company.initial,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSecondaryContainer,
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = company.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (selected) colors.brand else colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        TextButton(
            onClick = onTogglePin,
            modifier = Modifier.semantics {
                contentDescription = if (pinned) {
                    "Unpin ${company.name}. Pin order stays on this screen only."
                } else {
                    "Pin ${company.name}. Keeps this company at the top of this list."
                }
            },
        ) {
            Text(
                text = if (pinned) "Unpin" else "Pin",
                color = if (pinned) colors.brand else colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CompanyMessages(
    name: String?,
    colors: WorkspaceColors,
    showBack: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (name == null) {
        WorkspaceEmpty(
            title = "Select a company",
            message = "Its messages from the local demo mailbox will show here.",
            colors = colors,
            modifier = modifier,
        )
        return
    }
    val messages = WorkspaceDemo.messages.filter { it.company == name }
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(horizontal = if (showBack) 4.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to companies",
                        tint = colors.onSurface,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = workspaceMessageCount(messages.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        if (messages.isEmpty()) {
            WorkspaceEmpty(
                title = "No messages for this company.",
                message = "Nothing in the local demo mailbox is filed under this name.",
                colors = colors,
                modifier = Modifier.weight(1f),
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                messages.forEach { message ->
                    WorkspaceMessageRow(message = message, colors = colors)
                }
            }
        }
    }
}

private data class CompanyRow(
    val name: String,
    val count: Int,
) {
    val initial: String
        get() = name.firstOrNull()?.toString()?.uppercase() ?: "?"
}

private fun companyRows(pinned: Set<String>): List<CompanyRow> {
    val counts = linkedMapOf<String, Int>()
    for (message in WorkspaceDemo.messages) {
        val name = message.company?.takeIf { it.isNotEmpty() } ?: continue
        counts[name] = (counts[name] ?: 0) + 1
    }
    return counts
        .map { (name, count) -> CompanyRow(name, count) }
        .sortedWith(
            compareByDescending<CompanyRow> { it.name in pinned }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name },
        )
}

private fun shownCompany(
    wide: Boolean,
    selected: String?,
    companies: List<CompanyRow>,
): String? {
    if (!wide) return selected
    if (selected != null && companies.any { it.name == selected }) return selected
    return companies.firstOrNull()?.name
}

private fun listWidth(paneWidth: Dp): Dp {
    val preferred = paneWidth * 0.38f
    return when {
        preferred < 280.dp -> 280.dp
        preferred > 420.dp -> 420.dp
        else -> preferred
    }
}

private fun toggleToken(current: Set<String>, id: String): String {
    val next = current.toMutableSet()
    if (!next.add(id)) next.remove(id)
    return next.sorted().joinToString("|")
}
