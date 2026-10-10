# AGENTS.md - Project Guidelines for Nightdream

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

- **Intent & Bundle Safety**: Always guard against null Intents and Bundles when extracting extras or iterating over bundle keys:
  ```java
  if (intent == null) return;
  Bundle bundle = intent.getExtras();
  if (bundle != null) { ... }
  ```

- **Import Organization**: Run optimize imports when finished making changes to source files.

## General Development Rules
- Follow existing Java/Android idioms and architectural patterns in the codebase.
- Use built-in project utilities where available.
