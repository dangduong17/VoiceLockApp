# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Clean build
./gradlew clean assembleDebug

# Check lint
./gradlew lint
```

## Architecture Overview

**VoiceLock** is an Android project using a 3-layer architecture (UI Layer + Domain Layer + Data Layer) with MVVM pattern. Built with Kotlin, ViewBinding/DataBinding, Koin DI, and Navigation Component (single-Activity).

Data flow: `Fragment -> ViewModel -> UseCase -> Repository -> (API/DB)`. ViewModels never call repositories directly; they depend on single-responsibility UseCases.

### Modules

- **`:app`** - Main application module (`com.example.voicelock`)

### Key Tech Stack

| Component | Technology |
|-----------|-----------|
| DI | Koin (NOT Hilt/Dagger) |
| Navigation | Jetpack Navigation Component |
| Network | Retrofit2 + OkHttp3 + Gson |
| Database | Room |
| Image Loading | Glide |
| Logging | Timber |
| Coroutines | Kotlin Coroutines + Flow + StateFlow |
| Min SDK | 25, Target/Compile SDK 35/37 |
| JVM | Java 11/17 |

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

- Generic params: `<ViewBinding, ViewModel>`
- Constructor params: `inflate` function reference and ViewModel `KClass`
- Override `init(view: View)` for setup and `subscribeObserver(view: View)` for Flow collection
- Provides `binding`, `viewModel`, `navigator`
- Built-in `showHideLoading(Boolean)` and `onSystemBack { }`

### BaseViewModel

- Generic params: `<State, Event>` with `initialState`
- Built-in `_uiState`/`uiState` (StateFlow) and `_uiEvent`/`uiEvent` (Channel)
- Use `setState { copy(...) }` to update state, `setEvent(event)` to emit one-shot events
- Each screen defines its own `data class XxxUiState(...)` passed as `initialState`

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

Use the built-in coroutine launchers (they include exception handling):

```kotlin
// In Fragment (from BaseFragment.kt extensions):
launchIO { }       // IO dispatcher
launchMain { }     // Main dispatcher
launchDefault { }  // Default dispatcher

// In ViewModel (from BaseViewModel.kt extensions):
launchIO { }
launchMain { }
launchDefault { }
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

- **Location**: `domain/usecase/{featureName}/`
- **Naming**: `{Action}{Entity}UseCase` (e.g. `GetInstalledAppsUseCase`, `GetLanguageUseCase`)
- **Single Responsibility**: each UseCase does exactly ONE operation, exposed via a single `operator fun invoke(...)`
- **Return type**: `Flow<Result<T>>` (same shape as Repository, so it stays compatible with `handleApiCall`)
- A UseCase depends on one or more Repositories via constructor injection. It must NOT reference the UI/ViewModel layer.

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

- Interface + Impl pattern in `data/repository/{featureName}Repository/`
- All functions return `Flow<Result<T>>` (using custom `com.example.voicelock.util.Result`)
- Implementation uses `.flowOn(Dispatchers.IO)` and `.catch { emit(Result.Error(it)) }`

### Models

- **DtoModel** (`{Entity}DtoModel`): Data from API/DB, used in Repository layer
- **UIModel** (`{Entity}UIModel`): Transformed data for UI layer
- Extension function `fun XxxDtoModel.toPresentation(): XxxUIModel` for mapping

### API Call Handling

Use `handleApiCall` extensions from `ApiExtensions.kt`. In a ViewModel, `apiCall` invokes a UseCase (not a Repository):

```kotlin
// With manual state management (preferred for screen-level state):
handleApiCall(
    apiCall = { getDataUseCase() },
    onSuccess = { data -> setState { copy(items = data.map { it.toPresentation() }) } },
    onError = { throwable -> setState { copy(error = throwable) } },
)

// With automatic UiState<T> management:
handleApiCall(stateFlow, apiCall = { getDataUseCase() })
```

## Dependency Injection (Koin)

Modules defined in `di/` package, aggregated in `AppModule.kt`:

- `coreModule` - System & DataStore singletons
- `networkModule` - Gson, OkHttp, Retrofit, ApiInterface
- `databaseModule` - Room database + DAOs
- `repositoryModule` - Repository bindings (Impl bind Interface)
- `useCaseModule` - UseCase registrations via `factoryOf(::XxxUseCase)`
- `viewModelModule` - ViewModel registrations via `viewModelOf(::XxxViewModel)`

The `useCaseModule` is registered in `appModules` between `repositoryModule` and `viewModelModule`.

Register new ViewModel: add `viewModelOf(::NewViewModel)` in `viewModelModule`.
Register new UseCase: add `factoryOf(::NewUseCase)` in `useCaseModule`.
Register new Repository: add `singleOf(::NewRepositoryImpl) bind NewRepository::class` in `repositoryModule`.

## Critical Conventions

- **Click listeners**: Use `setPreventDoubleClick { }` or `setPreventDoubleClickScaleView { }` instead of `setOnClickListener`
- **Dialog show**: Use `safeShowDialog(dialog)` / `safeShowBottomSheet(dialog)` from `Utils.kt`
- **Fragment params**: Never pass via constructor. Use `arguments` Bundle or shared ViewModel
- **Adapter listeners**: Never pass via constructor. Use `setListener()` method
- **Navigation**: Use `navigator.navigateTo(actionId)` / `navigator.navigateUp()` (Navigator interface wraps NavController)
- **UseCase over Repository**: ViewModels must depend on single-responsibility UseCases, never inject or call a Repository directly
