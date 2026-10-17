package com.example.core.result

import kotlin.coroutines.cancellation.CancellationException

/**
 * Functional result type for strictly typed domain outcomes.
 * Guarantees that exceptions do not cross layer boundaries and that
 * Coroutines CancellationException is never swallowed.
 */
sealed interface Outcome<out T, out E> {
    data class Success<out T>(val value: T) : Outcome<T, Nothing>
    data class Failure<out E>(val error: E) : Outcome<Nothing, E>

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Failure -> null
    }

    fun errorOrNull(): E? = when (this) {
        is Success -> null
        is Failure -> error
    }
}

inline fun <T, E, R> Outcome<T, E>.map(transform: (T) -> R): Outcome<R, E> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(value))
    is Outcome.Failure -> this
}

inline fun <T, E, R> Outcome<T, E>.flatMap(transform: (T) -> Outcome<R, E>): Outcome<R, E> = when (this) {
    is Outcome.Success -> transform(value)
    is Outcome.Failure -> this
}

inline fun <T, E> Outcome<T, E>.onFailure(action: (E) -> Unit): Outcome<T, E> {
    if (this is Outcome.Failure) action(error)
    return this
}

inline fun <T, E> Outcome<T, E>.onSuccess(action: (T) -> Unit): Outcome<T, E> {
    if (this is Outcome.Success) action(value)
    return this
}

/**
 * Safely executes a suspending block, re-throwing CancellationException to maintain
 * structured concurrency while catching other exceptions.
 */
inline fun <T, E> suspendRunOutcome(
    errorMapper: (Throwable) -> E,
    block: () -> T
): Outcome<T, E> {
    return try {
        Outcome.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Outcome.Failure(errorMapper(e))
    }
}

inline fun <T, E> safeRunCatching(
    onError: (Throwable) -> E,
    block: () -> T
): Outcome<T, E> = suspendRunOutcome(onError, block)

