package com.example.voicelock.util

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import com.example.voicelock.base.launchIO

sealed interface UiState<out T> {
    data object None : UiState<Nothing>

    data object Loading : UiState<Nothing>

    data class Success<T>(
        val data: T,
    ) : UiState<T>

    data class Error(
        val exception: Throwable,
    ) : UiState<Nothing>
}

/**
 * API call with automatic UiState management (skipIfInProgress prevents duplicate requests)
 */
inline fun <T, R> ViewModel.handleApiCall(
    stateFlow: MutableStateFlow<UiState<T>>,
    crossinline apiCall: suspend () -> Flow<Result<R>>,
    crossinline transform: (R) -> T,
    skipIfInProgress: Boolean = true,
) {
    launchIO {
        if (skipIfInProgress && (stateFlow.value is UiState.Loading || stateFlow.value is UiState.Success)) {
            return@launchIO
        }
        stateFlow.value = UiState.Loading
        apiCall().collect { result ->
            result
                .onSuccess { data -> stateFlow.value = UiState.Success(transform(data)) }
                .onError { exception -> stateFlow.value = UiState.Error(exception) }
        }
    }
}

/**
 * API call with manual state handling (preferred for screen-level UiState)
 */
inline fun <R> ViewModel.handleApiCall(
    crossinline apiCall: suspend () -> Flow<Result<R>>,
    crossinline onSuccess: (R) -> Unit = {},
    crossinline onError: (Throwable) -> Unit = {},
) {
    launchIO {
        apiCall().collect { result ->
            result
                .onSuccess { data -> onSuccess(data) }
                .onError { exception -> onError(exception) }
        }
    }
}

/**
 * API call with data transformation before onSuccess
 */
inline fun <R, T> ViewModel.handleApiCall(
    crossinline apiCall: suspend () -> Flow<Result<R>>,
    crossinline transform: (R) -> T,
    crossinline onSuccess: (T) -> Unit = {},
    crossinline onError: (Throwable) -> Unit = {},
) {
    launchIO {
        apiCall().collect { result ->
            result
                .onSuccess { data -> onSuccess(transform(data)) }
                .onError { exception -> onError(exception) }
        }
    }
}

suspend fun <T> Flow<Result<T>>.getDataOrDefault(defaultValue: T): T =
    first().let { result -> if (result is Result.Success) result.data else defaultValue }

suspend fun <T> Flow<Result<T>>.getDataOrNull(): T? =
    first().let { result -> if (result is Result.Success) result.data else null }

inline fun <T> UiState<T>.handleUiState(
    crossinline onNone: () -> Unit = {},
    crossinline onLoading: () -> Unit = {},
    crossinline onSuccess: (T) -> Unit = {},
    crossinline onError: (Throwable) -> Unit = {},
) {
    when (this) {
        is UiState.None -> onNone()
        is UiState.Loading -> onLoading()
        is UiState.Success -> onSuccess(data)
        is UiState.Error -> onError(exception)
    }
}
