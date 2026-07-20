---
kind: error_handling
name: Ad-hoc Java Exception Hierarchy with No Centralized Error System
category: error_handling
scope:
    - '**'
source_files:
    - core/src/main/java/org/catrobat/catroid/exceptions/ProjectException.java
    - core/src/main/java/org/catrobat/catroid/exceptions/CompatibilityProjectException.java
    - core/src/main/java/org/catrobat/catroid/exceptions/LoadingProjectException.java
    - core/src/main/java/org/catrobat/catroid/exceptions/OutdatedVersionProjectException.java
    - catroid/src/androidTest/java/org/catrobat/catroid/test/BricksHelpUrlTest.java
---

This repository does not implement a centralized, cross-cutting error handling system. Instead, errors are handled in an ad-hoc manner across the codebase:

1. **Project loading exceptions**: A small hierarchy exists under `core/src/main/java/org/catrobat/catroid/exceptions/` containing `ProjectException`, `CompatibilityProjectException`, `LoadingProjectException`, and `OutdatedVersionProjectException`. These are thrown during project parsing/loading and caught by callers (e.g., `ProjectManagerTest.java`).

2. **Scattered try-catch blocks**: The majority of error handling consists of inline `try { ... } catch (Exception e)` or specific exception catches (`IOException`, `InterruptedException`, `CloneNotSupportedException`) scattered throughout Android test code, Bluetooth server code, and UI tests. There is no global exception handler, middleware, or error propagation pattern.

3. **No Kotlin-specific error types**: Despite significant Kotlin usage, there are no custom Kotlin `sealed class` error types, Result wrappers, or coroutines-based error handling patterns observed.

4. **No panic/recover strategy**: As this is a JVM/Android codebase, there is no equivalent to Go's panic/recover. Unchecked exceptions propagate normally.

5. **No logging framework integration**: While a `Logger.kt` utility exists in `core/src/main/java/org/catrobat/catroid/util/`, it appears to be a simple wrapper rather than a structured logging system integrated with error handling.

The error handling approach is best described as "opportunistic" — each module handles its own errors locally without a unified strategy, making it difficult to consistently surface, categorize, or present errors to users across the application.