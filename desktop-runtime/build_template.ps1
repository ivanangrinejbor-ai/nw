param(
    [string]$BundleDir,
    [string]$IconIco,
    [string]$ZipOut
)

if (Test-Path $IconIco) { Copy-Item $IconIco "$BundleDir\icon.ico" -Force }

if (Test-Path $ZipOut) { Remove-Item $ZipOut -Force }
Compress-Archive -Path "$BundleDir\*" -DestinationPath $ZipOut -Force
Write-Host "  template bundle rebuilt: $ZipOut ($([math]::Round((Get-Item $ZipOut).Length/1MB,1)) MB)"
