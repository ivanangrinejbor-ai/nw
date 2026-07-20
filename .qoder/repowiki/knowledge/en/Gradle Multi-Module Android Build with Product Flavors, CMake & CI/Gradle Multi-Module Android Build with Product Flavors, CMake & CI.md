---
kind: build_system
name: Gradle Multi-Module Android Build with Product Flavors, CMake & CI
category: build_system
scope:
    - '**'
source_files:
    - build.gradle
    - settings.gradle
    - gradle.properties
    - catroid/build.gradle
    - core/build.gradle
    - desktop-runtime/build.gradle
    - vncclient/build.gradle
    - lunoscript-annotations/build.gradle
    - lunoscript-processor/build.gradle
    - Jenkinsfile
    - fastlane/Fastfile
    - catroid/gradle/code_quality_tasks.gradle
    - catroid/gradle/release_fastlane_tasks.gradle
---

## What system/approach is used

The project uses a **multi-module Gradle workspace** (AGP 8.7.3, Kotlin 2.0.21) that builds:
- An Android application (`:catroid`) with many product flavors and build types
- A pure-JVM runtime core (`:core`) shared between Android and desktop
- A Windows desktop player (`:desktop-runtime`) using libGDX LWJGL3
- Two KSP-based annotation modules (`:lunoscript-annotations`, `:lunoscript-processor`)
- An Android library with native code (`:vncclient`)

Android NDK/CMake is used for native components in both `:catroid` and `:vncclient`. Release packaging to Google Play is handled by Fastlane lanes; CI runs on Jenkins inside a Docker container with an emulator.

## Key files and packages

- **Workspace root**: `build.gradle`, `settings.gradle`, `gradle.properties`, `gradlew`
- **Android app module**: `catroid/build.gradle` — defines all flavors, build types, signing, CMake, asset assembly, and custom tasks
- **Shared core**: `core/build.gradle` — pure JVM module depending on libGDX + Retrofit
- **Desktop runtime**: `desktop-runtime/build.gradle` — produces a fat JAR with `Main-Class: org.catrobat.catroid.stage.DesktopStage`
- **KSP modules**: `lunoscript-annotations/build.gradle`, `lunoscript-processor/build.gradle`
- **VNC client**: `vncclient/build.gradle` — Android library with its own CMakeLists.txt
- **CI pipeline**: `Jenkinsfile` (and `Jenkinsfile.*` variants) — multi-stage Groovy pipeline running in `catrobat/catrobat-android:api33`
- **Release automation**: `fastlane/Fastfile` — per-flavor upload/promote lanes
- **Code quality**: `catroid/gradle/code_quality_tasks.gradle` (Checkstyle, PMD, Detekt), configs under `catroid/config/`
- **Flavor-specific Gradle fragments**: `catroid/gradle/release_fastlane_tasks.gradle`, `standalone_apk_tasks.gradle`, `setup_jacoco.gradle`, `emulator.gradle`

## Architecture and conventions

### Module layout
```
settings.gradle → include ':catroid', ':core', ':desktop-runtime', ':lunoscript-annotations', ':lunoscript-processor', ':vncclient'
```
- `:core` exposes the `RuntimeServices` contract; `:catroid` and `:desktop-runtime` each provide platform implementations.
- `:lunoscript-annotations` declares `@LunoClass/@LunoFunction/@LunoProperty`; `:lunoscript-processor` generates a registry via KSP.
- `:vncclient` is a standalone Android library consumed by `:catroid`.

### Android flavor matrix
`catroid/build.gradle` defines a single `default` dimension with flavors:
- `danvex` (default), `catroid`, `createAtSchool`, `embroideryDesigner`, `lunaAndCat`, `phiro`, `pocketCodeBeta`, `mindstorms`, `standalone`, `runtime`
Each flavor can override `applicationIdSuffix`, feature flags, resources, and manifest placeholders. The `runtime` flavor additionally has a `template` build type that strips heavy assets (Python, ONNX, TensorFlow libs) from the APK.

Build types: `debug`, `release`, `signedRelease` (inherits release + signing config), `template` (inherits debug but minified).

### Versioning & metadata
- `defaultVersionCode = 130`, `defaultVersionName = "2.2.0"` live in `catroid/build.gradle`.
- `versionCode` is incremented via `automationScripts/increaseVersionByOne.py`; version names are bumped manually.
- Git commit SHA and branch are injected as `BuildConfig.GIT_COMMIT_INFO` at build time.

### Native / CMake integration
Both `:catroid` and `:vncclient` declare `externalNativeBuild { cmake { path ... } }`. The catroid module also extracts libGDX native `.so`s into `src/main/jniLibs/<abi>/` via a `copyAndroidNatives` task wired to every `package*` task.

### Asset assembly tricks
- For `debug`/`template` variants, `packageAppClasses${Variant}` jars the variant's compiled classes into `app-classes.jar` and registers it as an asset source dir so the runtime can load plugins at runtime.
- `copyPluginLibs${Variant}` copies selected dependencies (gdx, xstream, koin-core, fragment, activity, core) into variant-specific asset dirs.
- `merge*Assets` tasks are hooked to depend on the matching flavor's `copyPluginLibs*` and `packageAppClasses*` only (not all flavors).
- `copyTemplateApk` builds the `runtime/template` APK and drops it as `assets/template_runtime.apk`; `copyDesktopTemplate` embeds `template_win.zip` and `build_exe.bat` for the in-app Windows player generator.

### Signing
A `signedRelease` build type inherits `release` and adds a keystore whose password/alias are supplied via `-PsigningKeystorePassword -PsigningKeyAlias -PsigningKeyPassword`. If not provided, it falls back to `external/debug.keystore`.

### Code quality & testing
- `check.dependsOn checkstyle pmd detekt` — all run against `src/**` excluding `gen/` and `build/`.
- Unit tests use Robolectric + Mockito; instrumented tests use Espresso/UIAutomator and run on an AVD started by Jenkins.
- JaCoCo coverage is generated for unit and instrumented suites; reports are published by Jenkins.

### CI (Jenkins)
- Runs in `catrobat/catrobat-android:api33` with KVM passthrough.
- Parallel stages: APK build (+ optional Paintroid local AAR), static analysis, unit tests, instrumented suites (local headless, testrunner, quarantine, RTL, outgoing network calls), PR-trigger suite.
- Emulators are created per stage, killed after completion, and logs/coverage zipped.
- Artifacts are renamed with `${BRANCH}-${BUILD_NUMBER}` and archived.

### Desktop packaging
`desktop-runtime/build.gradle` creates a fat JAR named `player.jar` with `Main-Class: org.catrobat.catroid.stage.DesktopStage`. Additional Windows packaging helpers (`build_exe.bat`, `embed_payload.ps1`, `write_launch4j_xml.ps1`) sit alongside the module and are copied into Android assets by `copyDesktopTemplate`.

## Rules developers should follow

- **Add a new flavor**: define it in `android.productFlavors` in `catroid/build.gradle`, add a `src/<flavor>/` source set if needed, and update the `flavorsWithoutGoogleServices` list if the flavor does not ship its own `google-services.json`.
- **Change Java/Kotlin target**: keep `sourceCompatibility/targetCompatibility = 11` and `kotlinOptions.jvmTarget = "11"` consistent across all modules.
- **Introduce a new native dependency**: add it to the `natives` configuration in `catroid/build.gradle` and ensure ABI directories exist under `src/main/jniLibs/` (the `copyAndroidNatives` task will extract them automatically).
- **Run code quality locally**: `./gradlew checkstyle pmd detekt lintCatroidDebug` before pushing.
- **Build all flavors**: `./gradlew assembleCreateAtSchoolDebug assembleLunaAndCatDebug assemblePhiroDebug assembleEmbroideryDesignerDebug assemblePocketCodeBetaDebug assembleMindstormsDebug` (or set `BUILD_ALL_FLAVOURS=true` in Jenkins).
- **Generate screenshots**: `./gradlew generateScreenshotsCatroid` (requires Fastlane/screengrab installed).
- **Publish to Play Store**: `./gradlew uploadAPKToPlayStore` or call the corresponding Fastlane lane directly.
- **Desktop player**: build with `./gradlew :desktop-runtime:jar` then run `java -jar desktop-runtime/build/libs/player.jar`.
- **Independent/local builds**: pass `-Pindependent='My App Name'` to get a unique app ID and name without touching manifests.
