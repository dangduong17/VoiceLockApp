package com.example.voicelock.base

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

abstract class BaseViewModel<State, Event>(
    initialState: State,
    open val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState = _uiState.asStateFlow()

    private val _uiEvent = Channel<Event>(Channel.BUFFERED)
    val uiEvent = _uiEvent.receiveAsFlow()

    protected fun getCurrentState() = _uiState.value

    protected fun setState(reduce: State.() -> State) {
        _uiState.update { it.reduce() }
    }

    protected suspend fun setEvent(event: Event) {
        _uiEvent.send(event)
    }
}

/**
 * Launch a coroutine with exception handling
 */
private fun ViewModel.launchWithExceptionHandler(
    dispatcher: CoroutineDispatcher,
    onError: (Throwable) -> Unit = { },
    block: suspend CoroutineScope.() -> Unit,
): Job {
    val exceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            Timber.e("${this::class.java.simpleName} error: $throwable")
            viewModelScope.launch(Dispatchers.Main) {
                onError(throwable)
            }
        }
    return viewModelScope.launch(dispatcher + exceptionHandler, block = block)
}

fun ViewModel.launchIO(
    onError: (Throwable) -> Unit = { },
    block: suspend CoroutineScope.() -> Unit,
): Job = launchWithExceptionHandler(Dispatchers.IO, onError, block)

fun ViewModel.launchDefault(
    onError: (Throwable) -> Unit = { },
    block: suspend CoroutineScope.() -> Unit,
): Job = launchWithExceptionHandler(Dispatchers.Default, onError, block)

fun ViewModel.launchMain(
    onError: (Throwable) -> Unit = { },
    block: suspend CoroutineScope.() -> Unit,
): Job = launchWithExceptionHandler(Dispatchers.Main, onError, block)
