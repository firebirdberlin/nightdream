# AGENTS.md - Project Guidelines for Nightdream

## Project Overview

- Android app (package `com.firebirdberlin.nightdream`, UI classes in `.ui`), written in Java.
- Do not introduce Kotlin unless explicitly requested.
- `minSdk 24`, `targetSdk`/`compileSdk 37`. Do not add version checks for API levels <= 24.
- **Simplify obsolete version checks**: When touching code that contains `Build.VERSION.SDK_INT`
  checks that are always true or always false because of `minSdk 24`, simplify them:
  - `SDK_INT >= X` with X <= 24 (e.g. `LOLLIPOP`, `M`, `N`) is always true: keep only the
    "true" branch and remove the check and the `else` branch.
  - `SDK_INT < X` with X <= 24 is always false: remove the check and its body, keep the
    `else` branch.
  - `SDK_INT <= 24` is **not** always false (it is true on API 24). Leave it alone and
    only simplify it if it can be proven redundant.
  - Also remove the then-unneeded `@RequiresApi`/`@TargetApi` annotations, unused
    `Build` imports and `lint` suppressions for those levels.
  - Do this only in code you are changing anyway. Do not sweep through unrelated files.
- Source/target compatibility is **Java 8**. Do not use `var`, `List.of`, `Map.of`, `record`,
  text blocks, `String.isBlank()` or other Java 9+ language features/APIs.
  `java.time` is allowed (core library desugaring is enabled).
- Do not add new dependencies without asking. Prefer libraries already in use:
  Volley (networking), Gson (JSON), Media3/ExoPlayer incl. HLS (audio/radio),
  WorkManager, Flexbox, Material, DataBinding.
- New source files start with the GPLv3 license header used by neighbouring files.

## Build Flavors

- There are two flavors: `full` (with Google Play Services) and `noGms`.
- Shared code (`src/main`) must **not** reference Google Play Services, Billing, Cast or
  Play Review classes. Put such code in `src/full` and provide an alternative or stub in
  `src/noGms`.
- Point out when a change compiles in only one flavor.

## Coding Standards & Utilities

- **String Emptiness Checks**: Avoid manual null and empty checks such as `str != null && !str.isEmpty()`. Instead, use `Utility.isEmpty(str)` or `!Utility.isEmpty(str)`.
  - **Specific Rule**: Replace the pattern:
    ```java
    preset.drawableResName != null && !preset.drawableResName.isEmpty()
    ```
    with:
    ```java
    !Utility.isEmpty(preset.drawableResName)
    ```

- **Collection & Map Emptiness Checks**: Avoid manual null and empty checks on collections or maps (e.g., `list == null || list.isEmpty()`). Instead, use `Utility.isEmpty(collection)` or `Utility.isEmpty(map)`.

- **Logging `TAG` Standardization**: Standardize all logging `TAG` definitions across classes to use class reflection:
  ```java
  private static final String TAG = ClassName.class.getSimpleName();
  ```
  Apply this to new and touched classes. Do not rewrite TAGs in unrelated parts of a file.

- **Intent & Bundle Safety**: Always guard against null Intents and Bundles when extracting extras or iterating over bundle keys:
  ```java
  if (intent == null) return;
  Bundle bundle = intent.getExtras();
  if (bundle != null) { ... }
  ```

- **Import Organization**: Run optimize imports when finished making changes to source files.
  No wildcard imports and no unused imports.

- **Nullability**: Use `androidx.annotation.NonNull` / `@Nullable` on parameters and return
  values of new public methods.

- **Comments**: Write comments in English and only where the "why" is not obvious.

## Threading & Lifecycle

- Run background work with `ExecutorService` plus `new Handler(Looper.getMainLooper())`,
  or use WorkManager for persistent work.
- Do not introduce new `AsyncTask` usages (deprecated). Do not use `new Handler()` without
  an explicit `Looper`.
- No file I/O, network access or bitmap decoding on the main thread.
- Everything registered in `onStart`/`onResume` (receivers, sensor listeners, callbacks,
  posted runnables) must be removed in the matching counterpart method.
- Do not keep `Activity`/`View` references in static fields. Check that a view is still
  attached before updating the UI from a callback.
- Avoid empty `catch` blocks. Catch specific exceptions and log or handle them.

## UI & Resources

- All user-visible text goes into `strings.xml`. Missing translations do not fail the build
  (lint `MissingTranslation` is disabled), but mention when translations are missing.
- Use `Utility.dpToPx(context, ...)` or `dimens.xml` instead of hardcoded pixel values.
  Colors belong in `colors.xml` or themes.
- Do not allocate objects in `onDraw`. Scale bitmaps and be aware of out-of-memory risks.
- Layouts must work in portrait and landscape and on tablets (the app is often used as a
  desk clock).
- RenderScript is disabled and must not be used.

## Night-Clock Specifics

- The device often runs all night: avoid unnecessary wake locks, timers, animations and
  frequent sensor updates.
- Do not break dimming, brightness handling or burn-in protection (e.g. `moveAround`).
- Change alarm logic (`AlarmHandlerService` and related code) only when explicitly asked,
  and describe the risks.

## Radio / Network Features

- Use Volley for network requests. Debounce autocomplete/search input and cancel outdated
  requests.
- Deliver results to the UI only on the main thread. Handle empty results, timeouts and
  missing connectivity.
- Do not trust stream URLs or server responses blindly. Play audio through Media3/ExoPlayer
  and handle playback errors in the player listener.
- Request and release audio focus correctly.

## Release Builds & Security

- Release builds are minified (`minifyEnabled`, `shrinkResources`). When adding Gson models,
  reflection or similar, add the required rules to `proguard-project.txt`.
- Never hardcode API keys or tokens. The OpenWeatherMap key comes from `local.properties`
  via `BuildConfig.API_KEY_OWM`. Never print or commit `local.properties`.
- Prefer HTTPS. Allow cleartext traffic only after asking.

## Testing

- Tests use JUnit 4 (`returnDefaultValues = true`). Mockito and Robolectric are not available.
- Keep new logic in plain Java classes without Android dependencies where possible so it can
  be unit tested.
- Do not delete or weaken existing tests to make them pass.

## General Development Rules

- Follow existing Java/Android idioms and architectural patterns in the codebase.
- Use built-in project utilities (`Utility`, `Settings`, `Graphics`, ...) where available.
  If you are unsure a helper exists, say so instead of inventing it.
- Keep changes minimal and focused on the task. No unrelated reformatting, renaming or
  refactoring, especially in the very large `ClockLayout.java` and `NightDreamUI.java`.
- Do not remove or implement open `TODO` comments unless asked.
- Match the style of the surrounding file (indentation, brace style, naming).
