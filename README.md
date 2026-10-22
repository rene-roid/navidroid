<div align="center">

<img src="assets/icon.svg" width="96" alt="navidroid" />

# navidroid

**An Android client for Navidrome, styled after its Spotify-like web UI.**

Point it at your Navidrome server and get your library, mixes and playlists in your pocket,
online or off.

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7f52ff?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack_Compose-Material_3-4285f4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/compose)
[![Media3](https://img.shields.io/badge/Media3-ExoPlayer-3ddc84?logo=android&logoColor=white)](https://developer.android.com/media/media3)
[![Android](https://img.shields.io/badge/Android-9%2B-3ddc84?logo=android&logoColor=white)](https://www.android.com)
[![Navidrome](https://img.shields.io/badge/server-Navidrome-0084ff)](https://www.navidrome.org)

</div>

---

## Features

- 🏠 **Home & Library** — home, playlists, albums, artists, search and library, with the NaviBeat and Explo mixes sorted into the same sections ("Made For you", "Your top mixes", ...) as the web UI
- 🌐 **Multiple Servers** — several server addresses (LAN and public) are pinged in parallel and the fastest one is used, with failover when one goes down or the network changes
- ⬇️ **Offline Downloads** — download playlists, albums or Liked Songs; pinned collections re-sync on launch, so rotating mixes pick up new songs and drop removed ones
- 📡 **Offline Scrobbling** — plays of downloaded songs are still reported to Navidrome (now playing and scrobbles); scrobbles made offline are queued and sent with their original time
- 📚 **Your Library** — save albums, artists and playlists; generated mixes get their own tab
- 🎤 **Synced Lyrics** — follow along line by line in the player
- 👆 **Gestures** — swipe to skip (artwork or mini player), swipe down to close the player
- 💾 **Persistent Queue** — picks up where you left off
- 📶 **Quality Settings** — separate streaming quality for Wi-Fi and mobile data

## Tech Stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3, Navigation 3 |
| Playback | Media3 (ExoPlayer + MediaSession), OkHttp data source |
| Images | Coil 3, Palette |
| Server | Navidrome (Subsonic API) |

## Quick Start

Needs **JDK 17+** and the Android SDK (compileSdk 37). Runs on Android 9 (API 28) and newer.

The `github` build is the one to install, as `xyz.yuuki.navidroid`. The `xiaomi` build installs as
`com.netease.cloudmusic`, a music app's package, because Xiaomi's rear screen only works with those.

```sh
./gradlew assembleGithubRelease   # app/build/outputs/apk/github/release/app-github-release.apk
./gradlew testGithubDebugUnitTest
```

## Project Structure

```
navidroid/
├── app/src/main/java/xyz/yuuki/navidroid/
│   ├── Api.kt              # Navidrome/Subsonic client, models, prefs, server racing
│   ├── App.kt              # Application setup, network change handling
│   ├── Mixes.kt            # NaviBeat / Explo mix grouping
│   ├── Offline.kt          # Downloads and pinned collection sync
│   ├── PlaybackService.kt  # Media3 playback service, scrobbling
│   └── ui/                 # Compose screens, player, settings, theme
├── assets/                 # Icon
└── design/                 # UI mockups
```
