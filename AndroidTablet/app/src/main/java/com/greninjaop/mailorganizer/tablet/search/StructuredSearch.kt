package com.greninjaop.mailorganizer.tablet.search

import com.greninjaop.mailorganizer.tablet.data.AccountId
import com.greninjaop.mailorganizer.tablet.data.ThreadSummary

data class StructuredQuery(
    val freeText: String? = null,
    val isUnread: Boolean? = null,
    val isStarred: Boolean? = null,
)

object StructuredSearch {
    fun search(threads: List<ThreadSummary>, accountId: AccountId, query: StructuredQuery): List<ThreadSummary> {
        return threads.filter { t ->
            if (t.accountId != accountId) return@filter false
            if (query.isUnread != null && t.unread != query.isUnread) return@filter false
            if (query.isStarred != null && t.starred != query.isStarred) return@filter false
            val q = query.freeText?.lowercase()?.trim().orEmpty()
            if (q.isNotEmpty()) {
                val hay = (t.subject + " " + t.snippet + " " + t.senderName).lowercase()
                if (q !in hay) return@filter false
            }
            true
        }
    }
}
