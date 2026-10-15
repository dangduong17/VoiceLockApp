# AGENTS.md

Cross-tool agent rules for **VoiceLock**. Read natively by Antigravity, Cursor, Claude Code, and Gemini/Codex. These rules are mandatory when generating or modifying code in this repository.

## Project Overview

VoiceLock is an Android project using a **3-layer architecture** (UI Layer + Domain Layer + Data Layer) with the **MVVM** pattern. Built with Kotlin, ViewBinding/DataBinding, Koin DI, and Navigation Component (single-Activity).

Data flow: `Fragment -> ViewModel -> UseCase -> Repository -> (API/DB)`. ViewModels never call repositories directly; they depend on single-responsibility UseCases.

### Modules

- `:app` - Main application module (`com.example.voicelock`)

### Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Kotlin, JVM (Java 11/17) |
| DI | Koin (NOT Hilt/Dagger) |
| Navigation | Jetpack Navigation Component |
| Network | Retrofit2 + OkHttp3 + Gson |
| Database | Room |
| Image Loading | Glide |
| Logging | Timber |
| Async | Kotlin Coroutines + Flow + StateFlow |
| SDK | Min 25, Target/Compile 35/37 |

## Build Commands

```bash
./gradlew assembleDebug       # Build debug APK
./gradlew assembleRelease     # Build release APK
./gradlew test                # Run unit tests
./gradlew clean assembleDebug # Clean build
```

## Base Classes (MANDATORY)

Never use raw Android classes. Always extend from these base classes:

| Instead of | Use |
|-----------|-----|
| `Fragment` | `BaseFragment<Binding, VM>(inflate, vmClass)` |
| `ViewModel` | `BaseViewModel<State, Event>(initialState)` |
| `ListAdapter` | `BaseListAdapter<Item, ViewBinding>(diffCallback)` |
| `DialogFragment` | `BaseDialogFragment<T>(layoutRes)` |
| `BottomSheetDialogFragment` | `BaseBottomSheetDialogFragment<T>(layoutRes)` |

### BaseFragment

- Generic params: `<ViewBinding, ViewModel>`; constructor takes `inflate` function reference and ViewModel `KClass`.
- Override `init(view: View)` for setup and `subscribeObserver(view: View)` for Flow collection.
- Provides `binding`, `viewModel`, `navigator`.
- Built-in `showHideLoading(Boolean)` and `onSystemBack { }`.

### BaseViewModel

- Generic params: `<State, Event>` with `initialState`.
- Built-in `_uiState`/`uiState` (StateFlow) and `_uiEvent`/`uiEvent` (Channel).
- Use `setState { copy(...) }` to update state, `setEvent(event)` to emit one-shot events.
- Each screen defines its own `data class XxxUiState(...)` passed as `initialState`.

## Feature Structure Pattern

Each feature follows a strict 3-file pattern:

```
feature/{featureName}/
├── {Feature}Fragment.kt      # UI + observer subscriptions
├── {Feature}FragmentEx.kt    # Extension functions for click events, view init
├── {Feature}ViewModel.kt     # State management + business logic
├── adapter/                  # RecyclerView adapters
├── dialog/                   # DialogFragments
└── bottomSheet/              # BottomSheetDialogFragments
```

### Fragment Pattern

```kotlin
class HomeFragment : BaseFragment<FragmentHomeBinding, HomeViewModel>(
    FragmentHomeBinding::inflate,
    HomeViewModel::class,
) {
    override fun init(view: View) {
        initView()        // from FragmentEx
        settingEvent()    // from FragmentEx
    }
    override fun subscribeObserver(view: View) {
        viewModel.uiState
            .map { it.someField }
            .distinctUntilChanged()
            .collectFlowOnView(viewLifecycleOwner) { value -> /* update UI */ }
    }
}
```

### FragmentEx Pattern

Extension functions on the Fragment class, separating UI logic:

```kotlin
fun HomeFragment.initView() {
    adapter.setListener(this)
    binding.rvMain.adapter = adapter
}
fun HomeFragment.settingEvent() {
    binding.btnSetting.setPreventDoubleClickScaleView { navigator.navigateTo(R.id.action_...) }
}
```

## Coroutine Extensions

Use the built-in coroutine launchers (they include exception handling). Available in both Fragment (from `BaseFragment.kt`) and ViewModel (from `BaseViewModel.kt`):

```kotlin
launchIO { }       // IO dispatcher
launchMain { }     // Main dispatcher
launchDefault { }  // Default dispatcher
```

**Do NOT use** `viewModelScope.launch()` or `lifecycleScope.launch()` directly.

## State Observation

Always use `collectFlowOnView` with `map + distinctUntilChanged` to observe specific state fields:

```kotlin
viewModel.uiState
    .map { it.fieldName }
    .distinctUntilChanged()
    .collectFlowOnView(viewLifecycleOwner) { value -> /* handle */ }
```

## Domain Layer (UseCase)

ViewModels depend on UseCases, **never on Repositories directly**.

- **Location**: `domain/usecase/{featureName}/`.
- **Naming**: `{Action}{Entity}UseCase` (e.g. `GetInstalledAppsUseCase`, `GetLanguageUseCase`).
- **Single Responsibility**: each UseCase does exactly ONE operation, exposed via a single `operator fun invoke(...)`.
- **Return type**: `Flow<Result<T>>` (same shape as Repository, so it stays compatible with `handleApiCall`).
- A UseCase depends on one or more Repositories via constructor injection and must NOT reference the UI/ViewModel layer.

```kotlin
// domain/usecase/home/GetInstalledAppsUseCase.kt
class GetInstalledAppsUseCase(
    private val installedAppsRepository: InstalledAppsRepository,
) {
    operator fun invoke(): Flow<Result<List<InstalledAppDtoModel>>> =
        installedAppsRepository.getInstalledApps()
}
```

```kotlin
// ViewModel depends on the UseCase, not the Repository
class HomeViewModel(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
) : BaseViewModel<HomeUiState, Nothing>(HomeUiState()) {
    fun getInstalledApps() {
        handleApiCall(
            apiCall = { getInstalledAppsUseCase() },
            onSuccess = { dtoList -> setState { copy(installedApps = dtoList.map { it.toPresentation() }) } },
            onError = { throwable -> setState { copy(error = throwable) } },
        )
    }
}
```

## Data Layer Patterns

### Repository

- Interface + Impl pattern in `data/repository/{featureName}Repository/`.
- All functions return `Flow<Result<T>>` (using custom `com.example.voicelock.util.Result`).
- Implementation uses `.flowOn(Dispatchers.IO)` and `.catch { emit(Result.Error(it)) }`.

### Models

- **DtoModel** (`{Entity}DtoModel`): Data from API/DB, used in the Repository layer.
- **UIModel** (`{Entity}UIModel`): Transformed data for the UI layer.
- Map with extension function `fun XxxDtoModel.toPresentation(): XxxUIModel`.

### API Call Handling

Use `handleApiCall` extensions from `ApiExtensions.kt`:

In a ViewModel, `apiCall` invokes a UseCase (not a Repository):

```kotlin
// Manual state management (preferred for screen-level state):
handleApiCall(
    apiCall = { getDataUseCase() },
    onSuccess = { data -> setState { copy(items = data.map { it.toPresentation() }) } },
    onError = { throwable -> setState { copy(error = throwable) } },
)

// Automatic UiState<T> management:
handleApiCall(stateFlow, apiCall = { getDataUseCase() })
```

## Dependency Injection (Koin)

Modules defined in `di/` package, aggregated in `AppModule.kt`:

| Module | Contents |
|--------|----------|
| `coreModule` | System & DataStore singletons |
| `networkModule` | Gson, OkHttp, Retrofit, ApiInterface |
| `databaseModule` | Room database + DAOs |
| `repositoryModule` | Repository bindings (Impl bind Interface) |
| `useCaseModule` | UseCase registrations via `factoryOf(::XxxUseCase)` |
| `viewModelModule` | ViewModel registrations via `viewModelOf(::XxxViewModel)` |

The `useCaseModule` is registered in `appModules` between `repositoryModule` and `viewModelModule`.

- Register new ViewModel: add `viewModelOf(::NewViewModel)` in `viewModelModule`.
- Register new UseCase: add `factoryOf(::NewUseCase)` in `useCaseModule`.
- Register new Repository: add `singleOf(::NewRepositoryImpl) bind NewRepository::class` in `repositoryModule`.

## Critical Conventions

| Rule | Correct | Wrong |
|------|---------|-------|
| Click listener | `view.setPreventDoubleClick { }` / `setPreventDoubleClickScaleView { }` | `view.setOnClickListener { }` |
| Show dialog | `safeShowDialog(dialog)` / `safeShowBottomSheet(dialog)` | `dialog.show(...)` directly |
| Fragment params | `arguments` Bundle / shared ViewModel | Constructor params |
| Adapter listener | `adapter.setListener(this)` | Constructor lambda |
| Navigation | `navigator.navigateTo(actionId)` / `navigator.navigateUp()` | `findNavController()` directly |
| Coroutines | `launchIO { }` / `launchMain { }` / `launchDefault { }` | `viewModelScope.launch()` / `lifecycleScope.launch()` |
| Data access | ViewModel uses single-responsibility `UseCase` | ViewModel injects/calls `Repository` directly |
