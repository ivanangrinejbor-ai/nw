param(
    [string]$Root,
    [string]$AppDir,
    [string]$IconIco
)

$ErrorActionPreference = 'Stop'
$r = $Root
$a = $AppDir

# 1) HARD GATE: do nothing destructive unless the exe was actually built.
#    If launch4j failed, keep every intermediate so the build can be diagnosed.
if (-not (Test-Path "$a\NeoCatroid.exe")) {
    Write-Host "  ERROR: NeoCatroid.exe was NOT built ($a\NeoCatroid.exe missing)."
    Write-Host "  Staging and cleanup SKIPPED - all intermediates kept (see post_log.txt)."
    exit 1
}

# 2) Stage the runnable app + project into the root.
if (Test-Path "$r\NeoCatroid.exe") { Remove-Item "$r\NeoCatroid.exe" -Force }
Copy-Item "$a\NeoCatroid.exe" "$r\NeoCatroid.exe" -Force
Write-Host "  - Runnable app (root): $r\NeoCatroid.exe"

# player.jar is now WRAPPED INTO NeoCatroid.exe (dontWrapJar=false) - do not ship a
# loose jar. Remove a stale one left by a previous (unwrapped) build if present.
if (Test-Path "$r\player.jar") { Remove-Item "$r\player.jar" -Force }
if (Test-Path "$a\project.zip") {
    if (Test-Path "$r\project.zip") { Remove-Item "$r\project.zip" -Force }
    Copy-Item "$a\project.zip" "$r\project.zip" -Force
    Write-Host "  - Project: $r\project.zip"
}
if (Test-Path "$a\jre") {
    if (Test-Path "$r\jre") { Remove-Item "$r\jre" -Recurse -Force }
    Copy-Item "$a\jre" "$r\jre" -Recurse -Force
}

# 3) VERIFY the exe is really in the root (non-empty) BEFORE cleaning anything.
$rootExe = Join-Path $r 'NeoCatroid.exe'
if ((Test-Path $rootExe) -and ((Get-Item $rootExe).Length -gt 0)) {
    # Exe confirmed in place -> safe to remove build junk. Keep only what the
    # launcher needs at runtime (jre) plus the icon folder.
    Get-ChildItem $r -Directory | Where-Object { $_.Name -notin 'jre','icon' } |
        Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
    Write-Host "  - Cleaned build folders (exe verified in root)."
} else {
    Write-Host "  WARNING: NeoCatroid.exe missing/empty in root after staging - cleanup SKIPPED."
    exit 1
}
Write-Host "  - Icon: $IconIco"
