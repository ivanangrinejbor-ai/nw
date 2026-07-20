---
kind: logging_system
name: Ad-hoc Logging with Android Log, Platform Logger, and ErrorLog
category: logging_system
scope:
    - '**'
source_files:
    - core/src/main/java/org/catrobat/catroid/util/Logger.kt
    - catroid/src/main/java/org/catrobat/catroid/utils/ErrorLog.kt
    - catroid/src/main/java/org/catrobat/catroid/apkbuild/RuntimeApp.kt
    - catroid/src/main/java/org/catrobat/catroid/koin/CatroidKoinHelper.kt
---

The repository does not implement a unified logging framework. Instead, it uses several ad-hoc approaches scattered across modules:

1. **Android `android.util.Log`** — Used directly throughout the Android app (`catroid/src/main/java`) and all test code for debug/info/error traces. No central logger wrapper is used in production code.

2. **Core module `Logger` object** (`core/src/main/java/org/catrobat/catroid/util/Logger.kt`) — A minimal platform-agnostic helper that prints to `System.out` (debug/info) and `System.err` (error). It is intended as a cross-platform bridge but appears unused by the main Android app.

3. **`ErrorLog` utility** (`catroid/src/main/java/org/catrobat/catroid/utils/ErrorLog.kt`) — A user-facing error reporter that shows a toast via `StageActivity.messageHandler` and writes the error text to `NeoCatroidError.txt` in the public Downloads directory on the device. Called from many action classes and runtime components to surface unhandled exceptions to end users.

4. **`java.util.logging.Logger`** — Used only in the legacy Bluetooth test server (`catroidBluetoothTestServer`).

5. **Koin integration** — Koin's Android logger is wired in `RuntimeApp` and `CatroidKoinHelper` via `koin.androidLogger`, providing dependency-injection lifecycle logs.

6. **Python logging** — The embedded Python 3.12 runtime ships its own `logging` module under `src/main/assets/python3.12/logging`; LunoScript interpreter output is routed through `ErrorLog.log`.

There is no centralized log-level configuration, no structured logging fields, no external sink (file/network), and no consistent tag/naming convention across modules. Developers should prefer `ErrorLog.log` for user-visible errors and `android.util.Log` for internal debugging; the core `Logger` object exists as a potential cross-platform abstraction but is not adopted.