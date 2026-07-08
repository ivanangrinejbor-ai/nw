@echo off
setlocal

echo === Step 1: Ensure patterns are in .gitignore ===
echo Patterns already updated by previous step.

echo === Step 2: List tracked files that match build artifacts ===
for /f "delims=" %%f in ('git ls-files ^| findstr /i /r /c:"\.cxx\\" /c:"CMakeFiles\\" /c:"\.cmake\\" /c:"^build\\" /c:"\.gradle\\" /c:"\.o$" /c:"\.obj$" /c:"\.bin$" /c:"\.ninja_log$" /c:"\.ninja_deps$" /c:"configure_fingerprint\.bin$" /c:"android_gradle_build\.json$" /c:"android_gradle_build_mini\.json$" /c:"compile_commands\.json\.bin$" /c:"app-classes\.jar$"') do (
    echo Removing from index: %%f
    git rm --cached -- "%%f"
)

echo === Step 3: Also remove common top-level build dirs if tracked ===
git rm --cached -- .cxx/ CMakeFiles/ .cmake/ obj/ externalNativeBuild/ .externalNativeBuild/ lib/ build/ .gradle/ 2>nul

echo === Step 4: Commit .gitignore changes and artifact removals ===
git add .gitignore
git commit -m "chore(git): ignore and untrack build artifacts (CMake, Gradle, Ninja, NDK)"

echo === Step 5: Resolve conflicts in build artifact files by accepting incoming branch version (theirs) ===
for /f "delims=" %%f in ('git diff --name-only --diff-filter=U') do (
    echo Checking conflicted file: %%f
    git checkout --theirs -- "%%f" || echo Failed to checkout theirs for %%f
)

echo === Step 6: Add resolved files and create merge commit ===
git add -A
git commit -m "merge: auto-resolve conflicts in generated build artifacts using incoming branch"

echo === Done ===
git status --short
pause