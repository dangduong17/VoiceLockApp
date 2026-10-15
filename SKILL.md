---
name: pion-android-coding-style
description: Use when writing, generating, scaffolding, refactoring or reviewing Kotlin Android code (Fragment, ViewModel, UiState, UiEvent, RecyclerView adapter, DialogFragment, BottomSheet, Room, Retrofit, repository, use case, Koin/Hilt module) in any Android project, including brand-new or empty projects that have no base classes yet.
---

# Pion Android Coding Style

## Overview

Every Android screen is built on one **base kit** (BaseFragment, BaseViewModel, BaseListAdapter, BaseDialogFragment, Navigator, Result/UiState, coroutine launchers, click/flow helpers) and one **feature recipe** (Fragment + FragmentEx + ViewModel + UiState). Code that skips the kit is wrong even if it works.

**Violating the letter of these rules is violating the spirit.** "The project is empty", "the PM wants it today", and "plain AndroidX is more standard" are not exceptions.

## Step 0: Bootstrap the base kit (mandatory in every project)

Look for `BaseFragment`, `BaseViewModel`, `BaseListAdapter`, `BaseDialogFragment`, `Navigator`, `Result`, `UiState`, `launchIO`, `collectFlowOnView` and `setPreventDoubleClick`.

- **All of them exist** → use them as they are. Don't rewrite them.
- **Any of them is missing** → create the missing files first, copied from `base-kit.md` (only the package name changes), then write the feature.

Never fall back to raw `Fragment`, `ViewModel`, `ListAdapter` or `DialogFragment` because "there is no base class yet". Creating the base class is the fix.

- **`BaseViewModel` exists but has no `<State, Event>` generics** (legacy projects) → don't rewrite it, other screens depend on it. Inside the feature's ViewModel declare the same members the generic base would give, with the same names: `private val _uiState = MutableStateFlow(XxxUiState())` / `val uiState = _uiState.asStateFlow()`, `private fun setState(reduce: XxxUiState.() -> XxxUiState)`, and, if the screen has events, `private val _uiEvent = Channel<XxxUiEvent>(Channel.BUFFERED)` / `val uiEvent = _uiEvent.receiveAsFlow()` / `private suspend fun setEvent(event: XxxUiEvent)`.
- **DI**: use what the project already uses. Hilt → `@HiltViewModel class XxxViewModel @Inject constructor(...)`, `@AndroidEntryPoint` on the Fragment, `XxxViewModel::class.java`. Koin → the modules below. Never add a second DI framework.

## Package layout

```
{root}/
├── app/            MyApplication (startKoin), MainActivity, CommonViewModel (app-wide state, optional)
├── base/           BaseFragment, BaseViewModel, BaseListAdapter, BaseDialogFragment,
│   └── navigator/  BaseBottomSheetDialogFragment, LoadingDialog, Navigator, NavigatorImpl
├── data/
│   ├── database/   AppDatabase, dao/, entity/, converter/
│   ├── model/{feature}/   XxxDtoModel, XxxUiModel, XxxUiState, XxxUiEvent, XxxContentState,
│   │                      XxxItem (sealed rows)
│   ├── remote/     ApiInterface, HeaderInterceptor
│   └── repository/{feature}Repository/   XxxRepository + XxxRepositoryImpl
├── di/             AppModule (list), CoreModule, NetworkModule, DatabaseModule,
│                   RepositoryModule, UseCaseModule, ViewModelModule
├── domain/usecase/{feature}/   VerbNounUseCase
├── feature/{feature}/   XxxFragment, XxxFragmentEx, XxxViewModel, adapter/, dialog/, bottomSheet/
└── util/           Result, ApiExtensions (UiState, handleApiCall), Utils, ViewExtensions,
                    BundleKey, Constant, ParcelableEx
```

## Feature recipe (write files in this order)

1. **Entity + DAO** (if local data): `XxxEntity` with `companion object { const val TABLE_NAME }` and `fun XxxEntity.toDtoModel()` in the same file.
2. **DtoModel**: API models carry `@SerializedName`. Put `fun XxxDtoModel.toPresentation(): XxxUiModel` in the **UiModel file**. Synthetic items the API doesn't send (an "All" tab, a "None" option) are built in the UiModel's `companion object { const val ALL_XXX_ID; fun createAllXxx() }`, never in the Fragment (see "Synthetic items" below).
3. **Repository**: an interface plus an `Impl`. Reads return `Flow<Result<T>>` built as `flow {}` / `.map {}` → `.catch { if (e is CancellationException) throw e; emit(Result.Error(e)) }` → `.flowOn(Dispatchers.IO)`.
4. **UseCase** (only when logic combines repositories or is reused): `class VerbNounUseCase(private val repo: XxxRepository) { operator fun invoke(...) }`.
5. **UiState**: `data class XxxUiState(val itemsUiState: UiState<List<XxxUiModel>> = UiState.None, val selectedXxxId: String = …)`. Store only **source data + user selections**. Everything the UI shows that can be computed from them (filtered list, visible layout, flags) is an extension function `fun XxxUiState.getXxx()` / `isXxx()` that combines the fields, never a stored field (see "UiState: source data + combined extensions" below).
6. **ContentState** (when the screen swaps whole layouts: loading / error / empty / content): `sealed class XxxContentState { data object Loading; data object Error; data object Empty; data object Success }`, produced by `fun XxxUiState.getContentState()`.
7. **UiEvent** (one-shot, optional): `sealed class XxxUiEvent { data class ShowX(..) : XxxUiEvent(); data object NavigateToY : XxxUiEvent() }`. Use `Nothing` as the Event type when the screen has no events. Rules in "UiEvent" below.
8. **ViewModel**: `class XxxViewModel(useCases…) : BaseViewModel<XxxUiState, XxxUiEvent>(XxxUiState())`. Use `setState { copy(...) }`, `setEvent(...)`, `handleApiCall(...)`, `launchIO {}`, and keep `Job?` fields for cancellable work, cancelled in `onCleared`. A load writes source data once; selecting a tab/filter only changes the selection field and never reloads.
9. **Adapter**: `BaseListAdapter<Item, Binding>(createDiffCallback(...))`, with `interface Listener` + `setListener()` inside the adapter, and display formatting (dates, counts) done in `bindView`.
10. **Dialog / BottomSheet**: `BaseDialogFragment<XBinding>(XBinding::inflate)`. Callbacks go through `companion fun newInstance(onConfirm: () -> Unit)` or a `Listener` + `setListener()`. Show it with `safeShowDialog(dialog)` / `safeShowBottomSheet(dialog)`.
11. **Fragment**: extends `BaseFragment<FragmentXBinding, XViewModel>(FragmentXBinding::inflate, XViewModel::class)` and implements `XAdapter.Listener`. It holds public `val adapter = XAdapter()` plus screen vars. `init()` only calls FragmentEx functions. `subscribeObserver()` holds every flow collection, and each collection is one line that delegates: `.map { it.getXxx() }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) { updateXxx(it) }`, plus `viewModel.uiEvent.collectFlowOnView(viewLifecycleOwner) { handleUiEvent(it) }`.
12. **FragmentEx**: top-level `fun XFragment.initView()`, `fun XFragment.onBackEvent()`, `fun XFragment.xxxEvent()` (one per clickable area), `fun XFragment.backEvent()`, render functions `fun XFragment.updateXxx(value)` (one per collected value), `fun XFragment.updateContentState(contentState)`, `fun XFragment.handleUiEvent(event)`, navigation helpers, show-dialog helpers.
13. **DI + nav + resources**: register in the DI modules, add `action_xFragment_to_yFragment` to the nav graph, add keys to `BundleKey` and texts to `strings.xml`.

A complete worked feature is in `feature-example.md`. Copy its shape.

## UiState: source data + combined extensions

The ViewModel stores what it loaded and what the user picked. The screen reads **combined** values through extension functions on the UiState, so related values always change together and a selection change never refetches.

```kotlin
data class GamesUiState(
    val gamesUiState: UiState<List<GameUiModel>> = UiState.None,          // source: everything loaded
    val categoriesUiState: UiState<List<CategoryUiModel>> = UiState.None,
    val selectedCategoryId: String = CategoryUiModel.ALL_CATEGORIES_ID,   // selection
)

// Combines source + selection into the list the RecyclerView shows. Non-Success states pass through.
fun GamesUiState.getSelectedGamesUiState(): UiState<List<GameUiModel>> =
    when (gamesUiState) {
        is UiState.Success ->
            if (selectedCategoryId == CategoryUiModel.ALL_CATEGORIES_ID) {
                gamesUiState
            } else {
                UiState.Success(gamesUiState.data.filter { it.categoryId == selectedCategoryId })
            }

        else -> gamesUiState
    }

// Which layout is visible. Use `when` (not handleUiState) because it returns a value.
fun GamesUiState.getContentState(): GamesContentState =
    when (gamesUiState) {
        is UiState.None, is UiState.Loading -> GamesContentState.Loading
        is UiState.Error -> GamesContentState.Error
        is UiState.Success -> if (gamesUiState.data.isEmpty()) GamesContentState.Empty else GamesContentState.Success
    }

// ViewModel: selecting only changes the selection
fun selectCategory(categoryId: String) {
    setState { copy(selectedCategoryId = categoryId) }
}
```

- Don't store `filteredGames` next to `games`, and don't keep separate `isLoading` / `isEmpty` / `showError` booleans. Two fields that must agree will eventually disagree.
- Name derived extensions `getXxx()` (values) / `isXxx()` (flags). Keep them pure and in the UiState file.

### ContentState: one sealed class for mutually exclusive layouts

```kotlin
sealed class GamesContentState {
    data object Loading : GamesContentState()
    data object Error : GamesContentState()
    data object Empty : GamesContentState()
    data object Success : GamesContentState()
}

// FragmentEx: exactly one layout visible, one line each
fun GamesFragment.updateContentState(contentState: GamesContentState) {
    binding.includeLoading.root.isVisible = contentState is GamesContentState.Loading
    binding.includeError.root.isVisible = contentState is GamesContentState.Error
    binding.includeEmpty.root.isVisible = contentState is GamesContentState.Empty
    binding.groupContent.isVisible = contentState is GamesContentState.Success
}

// FragmentEx: data render functions only handle Success; visibility belongs to updateContentState
fun GamesFragment.updateSelectedGamesUiState(selectedGamesUiState: UiState<List<GameUiModel>>) {
    selectedGamesUiState.handleUiState(onSuccess = { games -> adapter.submitList(games) })
}
```

## Synthetic items come from the data layer

Items the API doesn't send but the UI needs (an "All" tab, a "None" option, a default entry) are created by the UiModel and added to the list by the ViewModel. The Fragment renders the list as-is.

```kotlin
data class CategoryUiModel(
    val id: String,
    val name: String,
    @StringRes val nameRes: Int? = null, // localized name for synthetic items; wins over `name`
) {
    companion object {
        const val ALL_CATEGORIES_ID = "all_categories"

        fun createAllCategories(): CategoryUiModel =
            CategoryUiModel(id = ALL_CATEGORIES_ID, name = "", nameRes = R.string.all_games)
    }
}

// ViewModel, in onSuccess
val categoryTabs = listOf(CategoryUiModel.createAllCategories()) + categories
```

When rebuilding a `TabLayout` / `ChipGroup` from state: `clearOnTabSelectedListeners()` → `removeAllTabs()` → add each item, pre-selected from `uiState.value.selectedXxxId` → attach the listener **last**, so the pre-selection doesn't call back into the ViewModel.

## UiEvent: one-shot effects

A UiEvent is something that must happen **once** after the ViewModel decides it: a toast/snackbar, navigation after a save, opening a dialog, scrolling to top. Anything that must still be on screen after rotation is UiState, not an event.

```kotlin
// data/model/games/GamesUiEvent.kt
sealed class GamesUiEvent {
    data object ShowLoadMoreError : GamesUiEvent()
    data class NavigateToDemo(val gameId: String) : GamesUiEvent()
    data class ShowUnlockDialog(val game: GameUiModel) : GamesUiEvent()
}

// ViewModel: setEvent is suspend, so call it inside a coroutine
fun launchDemo(game: GameUiModel) {
    launchIO {
        if (unlockRepository.isUnlocked(game.id)) {
            setEvent(GamesUiEvent.NavigateToDemo(game.id))
        } else {
            setEvent(GamesUiEvent.ShowUnlockDialog(game))
        }
    }
}

// Inside handleApiCall callbacks (plain lambdas) wrap it: onError = { launchIO { setEvent(GamesUiEvent.ShowLoadMoreError) } }

// Fragment.subscribeObserver
viewModel.uiEvent.collectFlowOnView(viewLifecycleOwner) { handleUiEvent(it) }

// FragmentEx: exhaustive `when`, no `else`, so a new event fails to compile until it is handled
fun GamesFragment.handleUiEvent(event: GamesUiEvent) {
    when (event) {
        is GamesUiEvent.ShowLoadMoreError -> displayToast(getString(R.string.cannot_load_more_games))
        is GamesUiEvent.NavigateToDemo -> navigateToDemo(event.gameId)
        is GamesUiEvent.ShowUnlockDialog -> showUnlockDialog(event.game)
    }
}
```

- Name events as what the UI does: `ShowXxx`, `NavigateToXxx`, `ScrollToTop`. `data object` without payload, `data class` with payload (ids or UiModels, never Views or Context).
- A click whose result the Fragment already knows (back, open a static screen) goes straight to FragmentEx (`xxxEvent()` → `navigator.navigateTo`). Round-trip through the ViewModel only when it decides (validation, async result, data lookup).
- The base `Channel(BUFFERED).receiveAsFlow()` delivers each event once, buffered while the view is stopped. Don't replace it with `SharedFlow`, `SingleLiveEvent`, or a `showToast: Boolean` in UiState that the Fragment resets.
- No events on the screen → `BaseViewModel<XxxUiState, Nothing>` and no `uiEvent` collector.

## Quick reference: instead of → use

| Instead of | Use |
|---|---|
| `Fragment()`, `_binding!!`, `onViewCreated` logic | `BaseFragment` + `init()` / `subscribeObserver()` |
| `ViewModel()`, `MutableStateFlow` per field | `BaseViewModel<State, Event>` + one `XxxUiState` + `setState { copy() }` |
| `viewModelScope.launch`, `lifecycleScope.launch`, `launchIn` | `launchIO {}` / `launchMain {}` / `launchDefault {}` |
| Collecting `Result` by hand for a one-shot load | `handleApiCall(apiCall = {…}, onSuccess = {…}, onError = {…})` |
| Cancellable work (search, refresh) | `searchJob?.cancel(); searchJob = launchIO { useCase(k).collect { result -> when (result) { is Result.Success -> …; is Result.Error -> … } } }` |
| Re-collecting a Room Flow each time the view is recreated | Guard `loadX()`: return early if the state is already `Loading`/`Success` (reuse loaded data) |
| `repeatOnLifecycle` boilerplate | `.map { it.field }.distinctUntilChanged().collectFlowOnView(viewLifecycleOwner) {}` |
| `when (uiState)` on UiState | `uiState.handleUiState(onLoading = {}, onSuccess = {}, onError = {})` (use `when` only inside a `getXxx()` that returns a value) |
| `handleUiState` toggling 3–4 layouts in every branch | `XxxContentState` sealed class + `getContentState()` + one `updateContentState()` |
| Storing a filtered list / `isEmpty` flag next to the source list | Source + selection in UiState, combined by `fun XxxUiState.getSelectedXxxUiState()` |
| Selecting a tab/filter reloads data | `setState { copy(selectedXxxId = id) }`; the combined extension recomputes |
| Fragment adds an "All" tab / default item itself | `XxxUiModel.createAllXxx()` in the companion, prepended by the ViewModel |
| Rendering logic inline in `subscribeObserver` | `{ updateXxx(it) }` / `{ handleUiEvent(it) }` delegating to FragmentEx |
| `SharedFlow`, `SingleLiveEvent`, `showToast: Boolean` in UiState | `XxxUiEvent` sealed class + `setEvent()` + `handleUiEvent()` |
| `ListAdapter` + `DiffUtil` object + ViewHolder class | `BaseListAdapter` + `createDiffCallback` + `bindView` |
| `AlertDialog` / `MaterialAlertDialogBuilder` / Fragment Result API | Custom-layout `BaseDialogFragment` + `newInstance(callback)` |
| `setOnClickListener` | `setPreventDoubleClick {}` (primary CTA: `setPreventDoubleClickScaleView {}`) |
| `findNavController().navigate/navigateUp` | `navigator.navigateTo(R.id.action_…, bundleOf(BundleKey.KEY_X to v))` / `navigator.navigateUp()` |
| `requireActivity().onBackPressedDispatcher…` | `onSystemBack { backEvent() }`. On the start destination (Home), `backEvent()` shows an exit dialog or finishes the activity, never `navigateUp()` |
| Constructor params on Fragment/Dialog/Adapter | `arguments` Bundle (`BundleKey`), `newInstance()`, `setListener()` |
| Passing an object between screens | `@Parcelize data class XxxUiModel(...) : Parcelable`, then `bundleOf(BundleKey.KEY_X to model)` and `arguments?.parcelable<XxxUiModel>(BundleKey.KEY_X)` |
| Dialog lambda that must survive rotation | `interface Listener`, resolved as `(parentFragment as? Listener) ?: listener`, with data passed in `arguments` |
| Koin `parametersOf(id)` into the VM | Read `arguments` in `initView()`, then call `viewModel.loadX(id)` |
| `Log.d` | `Timber.tag(TAG).d(...)`, with `companion object { private const val TAG = "Xxx" }` |
| Magic strings / numbers | `BundleKey`, `Constant`, `companion object` constants |
| Hardcoded text / color / dp | `@string/`, `@color/`, `@dimen/_16dp` / `@dimen/_16sp`. If the project has no auto-dimens plugin (htkien.autodimens / sdp), declare these `_Xdp` names in `dimens.xml` |

## Naming

- Files and classes: `XxxFragment`, `XxxFragmentEx.kt`, `XxxViewModel`, `XxxUiState`, `XxxUiEvent`, `XxxContentState`, `XxxDtoModel`, `XxxUiModel`, `XxxEntity`, `XxxDao`, `XxxRepository`/`XxxRepositoryImpl`, `GetAllXxxUseCase`/`DeleteXxxUseCase`/`SaveXxxUseCase`.
- Mappers: `toDtoModel()` (entity → dto, and ui → dto), `toEntity()` (dto → entity, kept in the entity file), `toPresentation()` (dto → ui, kept in the UiModel file).
- View ids: `tvTitle`, `ivBack`, `btnRetry`, `rvItems`, `layoutEmpty`, `layoutAdsBottom`, `includeEmpty`, `progressBarLoading`.
- Layouts: `fragment_x`, `dialog_x`, `layout_item_x`, `layout_bottom_sheet_x`.
- UiState fields that hold async state end in `UiState` (`historiesUiState`). Selections are `selectedXxxId`.
- UiState extensions: `getXxx()` / `isXxx()`. FragmentEx render functions: `updateXxx()` (`updateContentState`, `updateSelectedGamesUiState`). Event dispatcher: `handleUiEvent()`. Synthetic item factories: `createAllXxx()` with `const val ALL_XXX_ID`.

## Kotlin formatting

- Trailing commas. Named arguments whenever a call has 2 or more params. Expression bodies for one-liners.
- `when` lists the remaining cases as `else -> { // Do nothing }`, except over your own sealed classes (`XxxUiEvent`, `XxxContentState`): list every case with no `else`. Use `runCatching {}` around risky parsing or binding.
- Functions that change state are verbs: `loadHistory()`, `deleteHistoryItem()`, `updateSearchKeyword()`, `clearSearch()`.
- Commits use Conventional Commits: `feat(scope): …`, `fix(scope): …`, `chore(scope): …`.

## Red flags: stop and fix

- You typed `: Fragment()`, `: ViewModel()`, `: ListAdapter<`, `: DialogFragment()` or `viewModelScope.launch`
- "No base classes exist, so I'll use plain AndroidX": bootstrap from `base-kit.md` instead
- "The Fragment Result API / AlertDialog is more robust": use the dialog pattern here anyway
- Click handlers or navigation written inside `init()` instead of FragmentEx
- `.catch { emit(Result.Error(it)) }` without rethrowing `CancellationException`
- Several `MutableStateFlow`s in a ViewModel instead of one UiState
- Formatting dates or counts in the UiModel or ViewModel instead of the adapter
- A UiState field that can be computed from other fields (`filteredItems`, `isEmpty`, `showError`)
- A Fragment that builds list items or tabs the data didn't contain
- A toast, navigation or dialog driven by a UiState flag instead of a `XxxUiEvent`
- `when (event)` with an `else` branch
