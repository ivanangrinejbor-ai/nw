---
kind: dependency_management
name: Gradle Multi-Module Dependency Management with Centralized Version Catalog
category: dependency_management
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
---

The NeoCatroid repository uses a Gradle multi-module build to manage dependencies across Android, JVM, and native components. The dependency management system is centralized through the root project's `build.gradle` file, which defines common repositories and version properties that are inherited by all submodules.

**Centralized Repository Configuration:**
The root `build.gradle` configures multiple Maven repositories including Google's Maven repository, Maven Central, Huawei's repository (for HMS services), JitPack (for GitHub-hosted libraries), and local Maven cache. This configuration is applied globally via the `allprojects.repositories` block, ensuring consistent dependency resolution across all modules.

**Version Management Strategy:**
Dependency versions are centrally managed in the root `build.gradle` file using an `ext` block that defines global version variables like `kotlin_version`, `koin_version`, and `lifecycle_version`. Individual module `build.gradle` files reference these shared versions, promoting consistency across the codebase. For example, the `:catroid` module imports versions from the root project using `rootProject.koin_version` and `rootProject.lifecycle_version`.

**Multi-Platform Module Structure:**
The repository is organized into several distinct modules:
- `:catroid` - Main Android application with extensive third-party dependencies
- `:core` - Platform-agnostic Kotlin library shared between Android and desktop runtime
- `:desktop-runtime` - Windows desktop player using libGDX LWJGL backend
- `:vncclient` - Android library for VNC functionality with native C++ components
- `:lunoscript-annotations` and `:lunoscript-processor` - Annotation processing toolchain

**Android-Specific Dependencies:**
The main `:catroid` module includes a comprehensive set of Android dependencies including Firebase services, Google Play Services, Room database, Retrofit networking, Glide image loading, and various UI libraries. It also manages native dependencies through libGDX's platform-specific artifacts and excludes conflicting transitive dependencies to prevent build conflicts.

**Build Configuration Properties:**
The `gradle.properties` file contains important build configuration including AndroidX support, Jetifier enablement, Gradle daemon settings, and memory allocation for both Gradle and Kotlin daemons. It also includes KAPT-specific configurations for Java 17+ compatibility.

**Local Development Support:**
The system supports local development through `mavenLocal()` repository inclusion and conditional property flags like `paintroidLocal=true` that allow developers to use locally published artifacts instead of remote versions.