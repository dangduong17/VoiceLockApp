# Base Kit: copy into any project that lacks it

Replace `{pkg}` with the app's root package. These files are the foundation; feature code assumes they exist.
Dependencies: Koin (`koin-android`), Navigation Component, ViewBinding, Coroutines, Timber, Material.

## base/BaseViewModel.kt

```kotlin
package {pkg}.base

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
```

## base/BaseFragment.kt

```kotlin
package {pkg}.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModelForClass
import {pkg}.base.navigator.Navigator
import {pkg}.base.navigator.NavigatorImpl
import timber.log.Timber
import kotlin.reflect.KClass

typealias Inflate<Binding> = (LayoutInflater, ViewGroup?, Boolean) -> Binding

abstract class BaseFragment<Binding : ViewBinding, VM : ViewModel>(
    private val inflate: Inflate<Binding>,
    private val viewModelClass: KClass<VM>,
) : Fragment() {
    // App-wide shared state, if the project has one:
    // val commonViewModel: CommonViewModel by activityViewModel()

    private var _navigator: Navigator? = null
    val navigator: Navigator
        get() =
            checkNotNull(_navigator) {
                "Fragment $this navigator cannot be accessed before onViewCreated()"
            }

    private var _binding: Binding? = null
    val binding: Binding
        get() =
            checkNotNull(_binding) {
                "Fragment $this binding cannot be accessed before onCreateView() or after onDestroyView()"
            }

    protected val bindingOrNull: Binding?
        get() = _binding

    open val viewModel: VM by viewModelForClass(viewModelClass)

    private var loadingDialog: LoadingDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = inflate.invoke(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        val navController = findNavController()
        _navigator = NavigatorImpl(navController, lifecycle, navController.currentDestination?.id ?: 0)
        init(view, savedInstanceState)
        subscribeObserver(view)
    }

    abstract fun init(view: View, savedInstanceState: Bundle?)

    abstract fun subscribeObserver(view: View)

    override fun onDestroyView() {
        hideLoading()
        _binding = null
        super.onDestroyView()
    }

    /**
     * Show or hide loading dialog
     */
    fun showHideLoading(isShow: Boolean) {
        if (isShow) showLoading() else hideLoading()
    }

    private fun showLoading() {
        if (loadingDialog == null || loadingDialog?.isVisible == false) {
            loadingDialog?.dismiss()
            loadingDialog = LoadingDialog()
            loadingDialog?.show(childFragmentManager)
        }
    }

    private fun hideLoading() {
        loadingDialog?.dismiss()
        loadingDialog = null
    }

    fun onSystemBack(action: () -> Unit) {
        activity?.onBackPressedDispatcher?.addCallback(this, true) {
            action.invoke()
        }
    }
}

fun Fragment.doActionWhenResume(action: () -> Unit) {
    if (isResumed) {
        action()
        return
    }
    lifecycle.addObserver(
        object : LifecycleEventObserver {
            override fun onStateChanged(
                source: LifecycleOwner,
                event: Lifecycle.Event,
            ) {
                if (event == Lifecycle.Event.ON_RESUME) {
                    action.invoke()
                    lifecycle.removeObserver(this)
                }
            }
        },
    )
}

private fun Fragment.launchWithExceptionHandler(
    dispatcher: CoroutineDispatcher,
    onError: (Throwable) -> Unit = { },
    block: suspend CoroutineScope.() -> Unit,
): Job {
    val exceptionHandler =
        CoroutineExceptionHandler { _, throwable ->
            Timber.e("${this::class.java.simpleName} error: $throwable")
            lifecycleScope.launch(Dispatchers.Main) {
                onError(throwable)
            }
        }
    return lifecycleScope.launch(dispatcher + exceptionHandler, block = block)
}

fun Fragment.launchIO(
    onError: (Throwable) -> Unit = { },
    block: suspend CoroutineScope.() -> Unit,
): Job = launchWithExceptionHandler(Dispatchers.IO, onError, block)

fun Fragment.launchDefault(
    onError: (Throwable) -> Unit = { },
    block: suspend CoroutineScope.() -> Unit,
): Job = launchWithExceptionHandler(Dispatchers.Default, onError, block)

fun Fragment.launchMain(
    onError: (Throwable) -> Unit = { },
    block: suspend CoroutineScope.() -> Unit,
): Job = launchWithExceptionHandler(Dispatchers.Main, onError, block)
```

## base/navigator/Navigator.kt + NavigatorImpl.kt

Crash-safe navigation: it only acts while the fragment is still the current destination, so double taps and lifecycle transitions cannot throw "navigation action cannot be found". If the fragment isn't resumed yet, the action is deferred to `ON_RESUME`.

```kotlin
package {pkg}.base.navigator

import android.os.Bundle

interface Navigator {
    fun getCurrentDestinationId(): Int

    /**
     * Navigate to a specific route with optional arguments, optionally clearing the back stack
     */
    fun navigateTo(
        actionId: Int,
        bundle: Bundle? = null,
        clearBackStack: Boolean = false,
    )

    fun navigateUp()

    fun popBackStack(
        destinationId: Int,
        inclusive: Boolean,
    )

    /**
     * Check if current screen was navigated from specific destination
     */
    fun isCameFrom(destinationId: Int): Boolean
}
```

```kotlin
package {pkg}.base.navigator

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavOptions

class NavigatorImpl(
    private val navController: NavController,
    private val lifecycle: Lifecycle,
    private val currentDestinationId: Int,
) : Navigator {

    private fun isAtCurrentDestination(): Boolean =
        navController.currentDestination?.id == currentDestinationId

    private fun safeAction(action: NavController.() -> Unit) {
        if (!isAtCurrentDestination()) return
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            runCatching { navController.action() }
            return
        }
        lifecycle.addObserver(
            object : LifecycleEventObserver {
                override fun onStateChanged(
                    source: LifecycleOwner,
                    event: Lifecycle.Event,
                ) {
                    when (event) {
                        Lifecycle.Event.ON_RESUME -> {
                            lifecycle.removeObserver(this)
                            if (isAtCurrentDestination()) {
                                runCatching { navController.action() }
                            }
                        }

                        Lifecycle.Event.ON_DESTROY -> lifecycle.removeObserver(this)

                        else -> {
                            // Do nothing
                        }
                    }
                }
            },
        )
    }

    override fun getCurrentDestinationId(): Int = navController.currentDestination?.id ?: 0

    override fun navigateTo(
        actionId: Int,
        bundle: Bundle?,
        clearBackStack: Boolean,
    ) {
        val navOptions =
            if (clearBackStack) {
                NavOptions
                    .Builder()
                    .setPopUpTo(navController.graph.startDestinationId, true)
                    .build()
            } else {
                null
            }
        safeAction { navigate(actionId, bundle, navOptions) }
    }

    override fun navigateUp() {
        safeAction { navigateUp() }
    }

    override fun popBackStack(
        destinationId: Int,
        inclusive: Boolean,
    ) {
        safeAction { popBackStack(destinationId, inclusive) }
    }

    override fun isCameFrom(destinationId: Int): Boolean =
        navController.previousBackStackEntry?.destination?.id == destinationId
}
```

## base/BaseListAdapter.kt

```kotlin
package {pkg}.base

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncDifferConfig
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Shared singleton executor for all ListAdapter diff calculations.
 * Prevents thread leak from creating new executor per adapter.
 */
private val diffExecutor: ExecutorService by lazy {
    Executors.newSingleThreadExecutor { r ->
        Thread(r, "ListAdapter-Diff-Thread").apply {
            isDaemon = true
        }
    }
}

interface BaseRecyclerAdapter<Item : Any, VB : ViewBinding> {
    fun bindView(
        binding: VB,
        item: Item,
        position: Int,
    )
}

abstract class BaseListAdapter<Item : Any, Binding : ViewBinding>(
    diffCallback: DiffUtil.ItemCallback<Item>,
) : ListAdapter<Item, BaseViewHolder<Binding>>(
        AsyncDifferConfig
            .Builder(diffCallback)
            .setBackgroundThreadExecutor(diffExecutor)
            .build(),
    ),
    BaseRecyclerAdapter<Item, Binding> {

    abstract fun inflateBinding(
        inflater: LayoutInflater,
        parent: ViewGroup,
        viewType: Int,
    ): Binding

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BaseViewHolder<Binding> {
        val inflater = LayoutInflater.from(parent.context)
        return BaseViewHolder(inflateBinding(inflater, parent, viewType))
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<Binding>,
        position: Int,
    ) {
        val item = getItem(position)
        if (item != null) {
            bindView(holder.binding, item, position)
        }
    }
}

open class BaseViewHolder<Binding : ViewBinding>(
    val binding: Binding,
) : RecyclerView.ViewHolder(binding.root)

inline fun <T> createDiffCallback(
    crossinline areItemsTheSame: (T, T) -> Boolean,
    crossinline areContentsTheSame: (T, T) -> Boolean,
): DiffUtil.ItemCallback<T> =
    object : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(
            oldItem: T & Any,
            newItem: T & Any,
        ): Boolean = areItemsTheSame(oldItem, newItem)

        override fun areContentsTheSame(
            oldItem: T & Any,
            newItem: T & Any,
        ): Boolean = areContentsTheSame(oldItem, newItem)
    }
```

## base/BaseDialogFragment.kt

Full-screen transparent window, so the custom layout draws its own card. Tapping outside the first child dismisses it when `isCancelable`.

```kotlin
package {pkg}.base

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding
import timber.log.Timber

abstract class BaseDialogFragment<T : ViewBinding>(
    private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> T,
) : DialogFragment() {
    private var _binding: T? = null

    protected val binding: T
        get() =
            checkNotNull(_binding) {
                "DialogFragment ${this::class.simpleName} binding cannot be accessed before onCreateView() or after onDestroyView()"
            }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        Timber.d("${this::class.simpleName} onCreateView")
        _binding = inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        setupDialogWindow()
        initData(savedInstanceState)
        initView(savedInstanceState)
        addEvent(savedInstanceState)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDialogWindow() {
        dialog?.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
            )
            decorView.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    val inputMethodManager =
                        v.context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    inputMethodManager?.hideSoftInputFromWindow(v.windowToken, 0)
                    if (isCancelable && isTouchOutsideContent(event)) {
                        dismiss()
                    }
                }
                false
            }
        }
    }

    protected open fun getContentView(): View? = (binding.root as? ViewGroup)?.getChildAt(0)

    private fun isTouchOutsideContent(event: MotionEvent): Boolean =
        runCatching {
            val contentView = getContentView() ?: return@runCatching false
            val rect = Rect()
            contentView.getGlobalVisibleRect(rect)
            !rect.contains(event.rawX.toInt(), event.rawY.toInt())
        }.getOrDefault(false)

    open fun initData(savedInstanceState: Bundle?) {}

    open fun initView(savedInstanceState: Bundle?) {}

    open fun addEvent(savedInstanceState: Bundle?) {}

    override fun onDestroyView() {
        Timber.d("${this::class.simpleName} onDestroyView")
        dialog?.window?.decorView?.setOnTouchListener(null)
        _binding = null
        super.onDestroyView()
    }

    fun show(manager: FragmentManager) {
        show(manager, tag)
    }

    override fun show(
        manager: FragmentManager,
        tag: String?,
    ) {
        if (isVisible) return
        runCatching {
            manager.beginTransaction().remove(this).commit()
            super.show(manager, tag)
        }
    }

    override fun dismiss() {
        runCatching { super.dismiss() }
    }
}
```

## base/BaseBottomSheetDialogFragment.kt

```kotlin
package {pkg}.base

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import androidx.viewbinding.ViewBinding
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

abstract class BaseBottomSheetDialogFragment<T : ViewBinding>(
    private val inflate: (LayoutInflater, ViewGroup?, Boolean) -> T,
) : BottomSheetDialogFragment() {
    private var _binding: T? = null

    protected val binding: T
        get() =
            checkNotNull(_binding) {
                "BottomSheetDialogFragment ${this::class.simpleName} binding cannot be accessed before onCreateView() or after onDestroyView()"
            }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initData(savedInstanceState)
        initView(savedInstanceState)
        addEvent(savedInstanceState)
    }

    open fun initData(savedInstanceState: Bundle?) {}

    open fun initView(savedInstanceState: Bundle?) {}

    open fun addEvent(savedInstanceState: Bundle?) {}

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
        super.onCreateDialog(savedInstanceState).apply {
            setOnShowListener {
                findViewById<View>(R.id.design_bottom_sheet)
                    ?.setBackgroundResource(android.R.color.transparent)
            }
        }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun show(manager: FragmentManager) {
        show(manager, tag)
    }

    override fun show(
        manager: FragmentManager,
        tag: String?,
    ) {
        if (isVisible) return
        runCatching {
            manager.beginTransaction().remove(this).commit()
            super.show(manager, tag)
        }
    }

    override fun dismiss() {
        runCatching { super.dismiss() }
    }
}
```

## base/LoadingDialog.kt

Needs `res/layout/dialog_loading.xml`: a `FrameLayout` with a centered `ProgressBar`.

```kotlin
package {pkg}.base

import android.os.Bundle
import {pkg}.databinding.DialogLoadingBinding

class LoadingDialog : BaseDialogFragment<DialogLoadingBinding>(DialogLoadingBinding::inflate) {
    override fun initView(savedInstanceState: Bundle?) {
        isCancelable = false
    }
}
```

## util/Result.kt (data layer)

```kotlin
package {pkg}.util

sealed class Result<T> {
    data class Success<T>(
        val data: T,
    ) : Result<T>()

    data class Error<T>(
        val error: Throwable,
    ) : Result<T>()
}

inline fun <T> Result<T>.onSuccess(block: (T) -> Unit): Result<T> = if (this is Result.Success) also { block(data) } else this

inline fun <T> Result<T>.onError(block: (Throwable) -> Unit): Result<T> = if (this is Result.Error) also { block(error) } else this

fun <T> Result<T>.asSuccessOrNull(): T? = (this as? Result.Success)?.data

inline fun <A, T> Result<T>.map(transform: (T) -> A): Result<A> =
    when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> Result.Error(error)
    }
```

## util/ApiExtensions.kt (UI layer: UiState + handleApiCall)

```kotlin
package {pkg}.util

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import {pkg}.base.launchIO

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
```

## util/Utils.kt

```kotlin
package {pkg}.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import {pkg}.base.BaseBottomSheetDialogFragment
import {pkg}.base.BaseDialogFragment
import {pkg}.base.doActionWhenResume

fun Fragment.safeShowDialog(
    dialog: BaseDialogFragment<out ViewBinding>?,
    fragmentManager: FragmentManager = childFragmentManager,
) {
    if (dialog == null) return
    doActionWhenResume { dialog.show(fragmentManager) }
}

fun Fragment.safeShowBottomSheet(
    dialog: BaseBottomSheetDialogFragment<out ViewBinding>?,
    fragmentManager: FragmentManager = childFragmentManager,
) {
    if (dialog == null) return
    doActionWhenResume { dialog.show(fragmentManager) }
}

inline fun <T> Flow<T>.collectFlowOnView(
    owner: LifecycleOwner,
    state: Lifecycle.State = Lifecycle.State.STARTED,
    crossinline onCollect: suspend (T) -> Unit,
) = owner.lifecycleScope.launch {
    owner.repeatOnLifecycle(state) {
        collect { onCollect(it) }
    }
}
```

## util/ViewExtensions.kt (minimum set)

```kotlin
package {pkg}.util

import android.graphics.Rect
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment

fun View.setPreventDoubleClick(
    debounceTime: Long = 500,
    action: () -> Unit,
) {
    setOnClickListener(
        object : View.OnClickListener {
            private var lastClickTime: Long = 0

            override fun onClick(v: View?) {
                if (SystemClock.elapsedRealtime() - lastClickTime < debounceTime) return
                action()
                lastClickTime = SystemClock.elapsedRealtime()
            }
        },
    )
}

/**
 * Debounced click with 0.9x press-scale animation, for primary CTAs
 */
fun View.setPreventDoubleClickScaleView(
    debounceTime: Long = 500,
    action: () -> Unit,
) {
    setOnTouchListener(
        object : View.OnTouchListener {
            private var lastClickTime: Long = 0
            private var rect: Rect? = null

            override fun onTouch(
                v: View,
                event: MotionEvent,
            ): Boolean {
                fun setScale(scale: Float) {
                    v.scaleX = scale
                    v.scaleY = scale
                }

                when {
                    event.action == MotionEvent.ACTION_DOWN -> {
                        rect = Rect(v.left, v.top, v.right, v.bottom)
                        setScale(0.9f)
                    }

                    rect?.contains(v.left + event.x.toInt(), v.top + event.y.toInt()) == false -> {
                        // moved out of the view
                        setScale(1f)
                        return false
                    }

                    event.action == MotionEvent.ACTION_UP -> {
                        setScale(1f)
                        if (SystemClock.elapsedRealtime() - lastClickTime >= debounceTime) {
                            lastClickTime = SystemClock.elapsedRealtime()
                            action()
                        }
                    }
                }
                return true
            }
        },
    )
}

fun Fragment.displayToast(msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}
```

## util/ParcelableEx.kt, util/BundleKey.kt, util/Constant.kt

```kotlin
package {pkg}.util

import android.os.Build
import android.os.Bundle
import android.os.Parcelable

inline fun <reified T : Parcelable> Bundle.parcelable(key: String): T? =
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> getParcelable(key, T::class.java)
        else -> @Suppress("DEPRECATION") getParcelable<T>(key)
    }
```

```kotlin
package {pkg}.util

object BundleKey {
    const val KEY_BOOKMARK_ID = "KEY_BOOKMARK_ID"
}
```

```kotlin
package {pkg}.util

object Constant {
    const val DEFAULT_PAGE_SIZE = 20
}
```

## data/model/ApiObjectResponseData.kt (standard API envelope)

```kotlin
package {pkg}.data.model

import com.google.gson.annotations.SerializedName

data class ApiObjectResponseData<T>(
    @SerializedName("message")
    var message: String,
    @SerializedName("data")
    var dataResponse: T,
    @SerializedName("status")
    var status: Int,
    @SerializedName("paging")
    var paging: ApiPagingInfo?,
)

data class ApiPagingInfo(
    @SerializedName("current_page")
    var currentPage: Int,
    @SerializedName("page_size")
    var pageSize: Int,
    @SerializedName("total")
    var total: Int,
    @SerializedName("total_pages")
    var totalPages: Int,
)
```

Adjust the field names to the backend's actual envelope, but keep one shared wrapper type.

## di/ (layered Koin modules)

Koin version note: on Koin 3.5.x `viewModelOf` is imported from `org.koin.androidx.viewmodel.dsl`; on Koin 4.x it's `org.koin.core.module.dsl.viewModelOf`.

```kotlin
// di/AppModule.kt
val appModules: List<Module> =
    listOf(
        // Core system dependencies
        coreModule,
        // Data layer
        networkModule,
        databaseModule,
        repositoryModule,
        // Domain layer
        useCaseModule,
        // UI layer
        viewModelModule,
    )

// di/UseCaseModule.kt: one module per feature, aggregated
val useCaseModule = module { includes(bookmarkUseCaseModule) }

// app/MyApplication.kt
startKoin {
    androidContext(this@MyApplication)
    modules(appModules)
}
```

Network module: `single<Gson> { GsonBuilder().setLenient().create() }`, an OkHttp client with `HeaderInterceptor` + `HttpLoggingInterceptor` (BODY in DEBUG, NONE in release) + 10MB cache + 30s timeouts, a Retrofit with `BuildConfig.BASE_URL`, and `single<ApiInterface> { get<Retrofit>().create(ApiInterface::class.java) }`. Third-party APIs that carry secret keys get their own client, registered under `named("xxxOkHttpClient")`, with no Chucker and logging set to NONE.
