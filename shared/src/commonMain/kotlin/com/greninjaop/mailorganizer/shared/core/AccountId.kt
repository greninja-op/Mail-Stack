package com.greninjaop.mailorganizer.shared.core

/**
 * First-class account boundary. Every mailbox / sync / intelligence
 * operation is scoped to an [AccountId]. Never mix account data.
 */
data class AccountId(val value: String) {
    init {
        require(value.isNotBlank()) { "AccountId must not be blank" }
    }

    override fun toString(): String = value
}
