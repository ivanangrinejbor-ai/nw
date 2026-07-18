param(
    [string]$Root,
    [string]$AppDir,
    [string]$IconIco
)

$ErrorActionPreference = 'Stop'
$r = $Root
$a = $AppDir

if (Test-Path "$a\NeoCatroid.exe") {
    if (Test-Path "$r\NeoCatroid.exe") { Remove-Item "$r\NeoCatroid.exe" -Force }
    Copy-Item "$a\NeoCatroid.exe" "$r\NeoCatroid.exe" -Force
    Write-Host "  - Runnable app (root): $r\NeoCatroid.exe"
}
if (Test-Path "$a\player.jar") {
    if (Test-Path "$r\player.jar") { Remove-Item "$r\player.jar" -Force }
    Copy-Item "$a\player.jar" "$r\player.jar" -Force
}
if (Test-Path "$a\project.zip") {
    if (Test-Path "$r\project.zip") { Remove-Item "$r\project.zip" -Force }
    Copy-Item "$a\project.zip" "$r\project.zip" -Force
    Write-Host "  - Project: $r\project.zip"
}
if (Test-Path "$a\jre") {
    if (Test-Path "$r\jre") { Remove-Item "$r\jre" -Recurse -Force }
    Copy-Item "$a\jre" "$r\jre" -Recurse -Force
}
Get-ChildItem $r -Directory | Where-Object { $_.Name -notin 'icon','jre','build','launch4j','src' } | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
Write-Host "  - Icon: $IconIco"
