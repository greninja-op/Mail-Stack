package com.greninjaop.mailorganizer.shared.oauth

object OAuthContracts {
    const val REDIRECT_URI = "com.greninjaop.mailorganizer:/oauth2redirect"
    const val PKCE_METHOD = "S256"
    val BASE_SCOPES = listOf(
        "openid", "email", "profile",
        "https://www.googleapis.com/auth/gmail.readonly",
        "https://www.googleapis.com/auth/calendar.events",
        "https://www.googleapis.com/auth/tasks",
    )
    fun baseExcludesModify(): Boolean = BASE_SCOPES.none { it.contains("gmail.modify") }
}
