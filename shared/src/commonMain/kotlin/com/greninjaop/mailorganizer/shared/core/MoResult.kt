package com.greninjaop.mailorganizer.shared.core

sealed class MoResult<out T> {
    data class Ok<T>(val value: T) : MoResult<T>()
    data class Err(val error: MoError) : MoResult<Nothing>()

    inline fun <R> map(transform: (T) -> R): MoResult<R> = when (this) {
        is Ok -> Ok(transform(value))
        is Err -> this
    }

    fun getOrNull(): T? = (this as? Ok)?.value
}

sealed class MoError {
    abstract val message: String

    data class Network(override val message: String, val cause: Throwable? = null) : MoError()
    data class Auth(override val message: String) : MoError()
    data class Database(override val message: String, val cause: Throwable? = null) : MoError()
    data class NotConfigured(override val message: String) : MoError()
    data class RateLimited(
        override val message: String,
        val daily: Boolean = false,
    ) : MoError()
    data class Unknown(override val message: String, val cause: Throwable? = null) : MoError()
}
