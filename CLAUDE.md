# Koda VK development guide

Koda VK is an Android music player built with Kotlin, Compose, Material 3 Expressive and Media3. VK Music is its only user-facing online source.

## Rules

- Never hardcode secrets or account tokens.
- Keep VK network/auth code under `data/vk/`; Compose must depend on models and view-model state only.
- Store the VK session only through `VkSessionStore`.
- Preserve Koda's theme-derived colors, player styles, queue behavior and background playback.
- Handle signed-out, loading, empty, error and expired-session states.
- Do not add YouTube or other online sources to the user-facing application.
- Build and test before handoff. Never operate an emulator or device without explicit permission.
- Do not add AI attribution to commits.
- APK-changing commits end with `Changelog:` and user-visible bullets.
- Do not use `git worktree`; use `git switch` or `git checkout` in this directory.

## Checks

```bash
./gradlew compileDebugKotlin
./gradlew testDebugUnitTest
./gradlew assembleRelease
```

Release signing comes from ignored local properties or CI environment variables. Never commit the signing key or passwords.
