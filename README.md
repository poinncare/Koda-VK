<p align="center">
  <img src="icon.svg" width="112" alt="Koda VK logo" />
</p>

<h1 align="center">Koda VK</h1>

<p align="center"><b>VK Music in the expressive Koda player.</b></p>

<p align="center">
  <a href="https://github.com/poinncare/Koda-VK/releases/latest"><img src="https://img.shields.io/github/v/release/poinncare/Koda-VK?style=for-the-badge&label=Download&color=6750A4" alt="Latest release" /></a>
  <a href="https://github.com/poinncare/Koda-VK/releases"><img src="https://img.shields.io/github/downloads/poinncare/Koda-VK/total?style=for-the-badge&color=4C6FFF" alt="Downloads" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/github/license/poinncare/Koda-VK?style=for-the-badge&color=8B4513" alt="GPL-3.0 license" /></a>
</p>

Koda VK is an unofficial Android music player that combines VK Music with Koda's Material 3 Expressive interface. VK is the only online music source exposed by this fork: recommendations, search, the user's library and playlists live in one native interface and play through Koda's queue and background audio service.

## Download

Download the current APK from **[GitHub Releases](https://github.com/poinncare/Koda-VK/releases/latest)**. Android 11 or newer is required.

Choose `arm64-v8a` for almost every modern phone. Use `armeabi-v7a` for an older 32-bit device or `universal` when you are unsure.

## Features

- VK sign-in inside a dedicated browser dialog; the resulting session is encrypted on the device.
- Personal VK Music sections and recommendations.
- Search for tracks, with artist and album views derived from live VK results.
- VK library, liked tracks and playlists.
- Create and delete playlists, add tracks to editable playlists.
- Save or remove tracks from My music.
- Play next, add to queue, shuffle and repeat.
- Koda's background playback, notification, lock-screen controls and eight player styles.
- Dynamic color, light/dark themes, responsive loading, empty and error states.
- Direct HLS playback with automatic URL re-resolution when a VK stream expires.

## Privacy and account security

Koda VK does not contain an access token, password or private API key. Authentication happens on VK's site. Session cookies and the short-lived access token are stored with Android encrypted preferences and are removed when you sign out.

This is an unofficial client and is not affiliated with, endorsed by or sponsored by VK. VK may change or restrict its private music endpoints at any time. Use the app in accordance with the rules that apply to your account and region.

## Building

Install Android SDK Platform 37 and JDK 17, then run:

```bash
./gradlew assembleDebug
```

Release builds read signing values from `local.properties` or the `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` environment variables. The signing keystore itself is intentionally not stored in Git.

## Project structure

- `data/vk/` — native VK auth, API mapping and encrypted session storage.
- `ui/vk/` — Koda-styled home, search, library, playlist and sign-in surfaces.
- `service/MusicService.kt` — shared Media3 playback and stream refresh.
- `ui/player/` — Koda player styles, queue and playback controls.

## Credits and licenses

Koda VK is based on [Koda](https://github.com/Ivorisnoob/Koda) and uses a native Android port of the protocol surface documented by [@toil/vk-audio](https://github.com/ilyhalight/vk-audio). Koda is GPL-3.0; `vk-audio` is MIT. See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

The complete application is distributed under [GPL-3.0](LICENSE).
