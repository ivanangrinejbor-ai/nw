---
kind: configuration_system
name: Android Build-Time & Runtime Configuration System
category: configuration_system
scope:
    - '**'
source_files:
    - catroid/build.gradle
    - catroid/src/main/java/org/catrobat/catroid/common/Constants.java
    - catroid/src/apktemplate/java/org/catrobat/catroid/common/FlavoredConstants.java
    - gradle.properties
    - catroid/google-services-template.json
---

This repository uses a layered, build-time configuration system centered on Android Gradle's BuildConfig and product flavors to control feature toggles, API endpoints, and per-flavor behavior at compile time. There is no runtime config file loader (no JSON/YAML/env parsing); instead, configuration is baked into the APK via Gradle tasks and consumed through generated constants and resource values.

### What system/approach is used
- Gradle buildConfigField / resValue: Feature flags (FEATURE_*_ENABLED), debug switches, and build metadata are injected as Java/Kotlin constants or Android string resources during compilation.
- Product flavors (danvex, catroid, phiro, embroideryDesigner, lunaAndCat, mindstorms, pocketCodeBeta, standalone, runtime) override applicationId, manifest placeholders, and feature flags per distribution channel.
- Flavor-specific source sets provide flavor overrides for constants and UI strings (e.g. src/<flavor>/java/.../common/FlavoredConstants.java).
- Google Services template copy: google-services-template.json is conditionally copied into flavor source dirs so Firebase resources are generated only where needed; flavors without their own file have the task disabled.
- Runtime user preferences: User-facing settings are persisted via Android SharedPreferences (and androidx.security:security-crypto for sensitive keys). No custom preference schema - standard key/value pairs with constant keys defined in Constants.

### Key files and packages
- catroid/build.gradle - central definition of all buildConfigFields, resValues, productFlavors, buildTypes, signing configs, and asset-packing logic.
- catroid/src/main/java/org/catrobat/catroid/common/Constants.java - centralized runtime constants (file paths, URLs, HTTP codes, OAuth keys) that read from BuildConfig for test/prod URL selection.
- catroid/src/apktemplate/java/org/catrobat/catroid/common/FlavoredConstants.java - flavor-level constants (community URL, external storage folder name, library base URLs). Each flavor has its own copy under src/<flavor>/java/.../common/FlavoredConstants.java.
- gradle.properties - global Gradle JVM args, AndroidX flags, KAPT workarounds, and local-repo toggle (paintroidLocal=true).
- catroid/google-services-template.json - shared Firebase client config template copied into default flavors at build time.
- catroid/src/danvex/google-services.json (when present) - per-flavor Firebase override.
- core/src/main/java/org/catrobat/catroid/runtime/RuntimeServices.kt - platform-agnostic service interfaces used by both Android and desktop runtime (configuration boundary between modules).

### Architecture and conventions
1. Compile-time feature flags: Every capability is gated by a FEATURE_<NAME>_ENABLED boolean declared in defaultConfig and optionally overridden in buildTypes/productFlavors. Source code branches on BuildConfig.FEATURE_* to include/exclude functionality.
2. URL/environment switching: Constants.MAIN_URL_HTTPS selects between production (https://api.neo-catroid.org) and a test endpoint based on BuildConfig.WEB_TEST_FLAG / WEB_TEST_URL, which are supplied via Gradle properties (-PwebTestUrl=...).
3. Flavor isolation: Each flavor can supply its own FlavoredConstants.java and google-services.json; the build script disables Google Services processing for flavors that lack one.
4. Resource-based toggles: Some UI-level toggles are exposed as resValue "string" entries (e.g. FEATURE_EMBROIDERY_PREFERENCES_ENABLED, SNACKBAR_HINTS_ENABLED, DEBUG_MODE) so they can be flipped without recompiling Java sources.
5. User preferences: Application state is stored in SharedPreferences using keys defined in Constants (e.g. GOOGLE_ID_TOKEN, PROJECTNAME_KEY). Sensitive tokens use androidx.security:security-crypto dependency but are still keyed by plain strings.

### Rules developers should follow
- Add new features via buildConfigField: Declare a FEATURE_<NAME>_ENABLED boolean in defaultConfig, then gate the feature behind if (BuildConfig.FEATURE_<NAME>_ENABLED) in code. Override per flavor/buildType if needed.
- Do not hardcode URLs or secrets in source: Put them in buildConfigField/resValue or flavor-specific FlavoredConstants.java. For per-user secrets, use SharedPreferences backed by security-crypto.
- Use Constants for shared runtime constants: Centralize file names, directory names, and URL fragments here; read environment-dependent values from BuildConfig rather than duplicating logic.
- Keep flavor overrides minimal: Only override what differs per flavor (applicationId suffix, FlavoredConstants, google-services.json). Default behavior lives in defaultConfig and main source set.
- Avoid runtime config files: The app does not load JSON/YAML/env at startup. If you need deploy-time knobs, add a buildConfigField or resValue and rebuild.
- When adding a new flavor, create src/<flavor>/java/.../common/FlavoredConstants.java and, if Firebase is needed, place a matching google-services.json there; otherwise rely on the template copy mechanism.