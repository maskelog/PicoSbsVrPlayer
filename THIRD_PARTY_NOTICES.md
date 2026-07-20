# Third-party notices

This project uses open-source Android and Kotlin components through Gradle. Their original licenses and copyright notices remain applicable.

- AndroidX libraries, including Jetpack Compose, Lifecycle, Navigation and Media3: Apache License 2.0
- Kotlin and kotlinx.coroutines: Apache License 2.0
- Gradle Wrapper: Apache License 2.0

Dependency versions are defined in `gradle/libs.versions.toml` and `app/build.gradle.kts`. Gradle resolves the corresponding license metadata and artifacts from their upstream repositories.

The public build does not include yt-dlp, youtube-dl, youtubedl-android, FFmpeg, or a YouTube download implementation.
