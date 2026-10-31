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
- Source/target compatibility is **Java 17**. Do not use `var`, `List.of`, `Map.of`, `record`,
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
- The `noGms` flavor has no purchase management. All features shall be available.
- Point out when a change compiles in only one flavor.

## Coding Standards & Utilities
- Don't exceed a line length of 100 characters.
- Add {} even for one line statements, except for return statements., e.g. 
```java
  if (condition) {
    statement;
  }
  if (condition) return;

```
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

- **Lambdas**: Use lambda expressions instead of anonymous inner classes (e.g. `new Runnable()`, `new Comparator()`) when implementing functional interfaces.

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

- All user-visible text goes into `strings.xml`. Missing translations do fail the build.
- New strings shall be introduced via string resources in `strings.xml`. Once the feature is
  fully implemented (ask the user) missing translations shall be added. The keys in
  `strings.xml` files shall be sorted alphabetically.
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

## Versioning and Release Notes

This section outlines the process for managing application versions and creating release notes.

### Version Bumping

- **`app/build.gradle`**: The `versionCode` and `versionName` are defined in the `android.defaultConfig` block.
    - `versionCode`: An integer used by the Play Store to determine if an update is available. This should be incremented sequentially for each release.
    - `versionName`: A user-facing string representing the version (e.g., "2.11.40"). This should be incremented logically (e.g., `major.minor.patch`).
- **Process**:
    1. Determine the next `versionName` and `versionCode`.
    2. Update these values in `app/build.gradle`.
    3. Ensure all necessary code changes for the release are complete.

### Changelog Creation

- **Location**: Changelogs are located in `fastlane/metadata/android/en-US/changelogs/`. Each version should have its own changelog file, named sequentially by version code (e.g., `362.txt`, `363.txt`).
- **Contents**: Changelogs shall contains major changes since the last creation of a changelog file,e.g if the new version is 363 but tha last file is 350.txt all changes since version 350 must be considered.
- **Format**: Changelogs should be a user-facing, optimistic message summarizing new features and improvements.
    - Focus on user benefits, not technical details.
    - Keep messages concise, under 250 characters.
    - Use emojis to enhance readability and tone.
    - In case features need more room in the description, shorten stability improvements and bugfixes.
    - Example format:
      ```
      <ul>
          <li>✨ New Feature: [Brief description of feature]</li>
          <li>🚀 Improvements: [Brief description of improvements]</li>
          <li>🌍 Translations: Updated for [list languages]</li>
          <li>🛠️ Stability: Fixes for [mention key fixes]</li>
      </ul>
      ```
- **Process**:
    1. After bumping the version in `app/build.gradle`, create a new changelog file (e.g., `357.txt` if the new `versionCode` is 363).
    2. Write the user-facing changelog message according to the specified format and constraints.
    3. Commit both the `app/build.gradle` changes and the new changelog file.

### Commit Message Format

When bumping versions and updating changelogs, use the following commit message format:

```
Bump version

2.11.39 (361) -> 2.11.40 (362)
```

Replace the version numbers and codes with the actual previous and current versions.
