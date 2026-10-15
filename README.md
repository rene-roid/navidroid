<img src="assets/icon.svg" width="64" height="64" alt="">

# navidroid

An Android client for a Navidrome server, styled after its Spotify-like web UI.

- Home, playlists, albums, artists, search and library, with the NaviBeat and Explo mixes
  sorted into the same sections ("Made For you", "Your top mixes", ...) as the web UI.
- Several server addresses (LAN and public): all are pinged in parallel and the fastest
  one is used, with failover when one goes down or the network changes.
- Download playlists, albums or Liked Songs for offline use. Pinned collections re-sync on
  launch, so rotating mixes pick up new songs and drop removed ones.
- Plays of downloaded songs are still reported to Navidrome (now playing and scrobbles);
  scrobbles made offline are queued and sent with their original time.
- Synced lyrics, swipe-to-skip artwork, persistent queue, separate Wi-Fi and mobile quality.

## Build

```sh
./gradlew assembleRelease      # app/build/outputs/apk/release/app-release.apk
./gradlew testDebugUnitTest
```

Needs JDK 17+ and the Android SDK (compileSdk 37).
