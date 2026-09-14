package com.freemind.ai.util

sealed interface AppError {
    data object Network : AppError
    data object RateLimited : AppError
    data object Unauthorized : AppError
    data object NotConfigured : AppError
    data class Unknown(val message: String?) : AppError
}

sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}

inline fun <T> AppResult<T>.onSuccess(action: (T) -> Unit): AppResult<T> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <T> AppResult<T>.onFailure(action: (AppError) -> Unit): AppResult<T> {
    if (this is AppResult.Failure) action(error)
    return this
}
