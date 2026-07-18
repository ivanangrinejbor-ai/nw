param(
    [string]$Exe,
    [string]$Payload
)

if (-not (Test-Path $Exe)) { Write-Host "  embed: exe not found: $Exe"; exit 1 }
if (-not (Test-Path $Payload)) { Write-Host "  embed: payload not found: $Payload"; exit 1 }

$bytes = [System.IO.File]::ReadAllBytes($Payload)
$size = [int64]$bytes.Length
$magic = [System.Text.Encoding]::ASCII.GetBytes("NEOCAT01")
$lenBytes = $size.ToByteArray()

$fs = [System.IO.File]::Open($Exe, [System.IO.FileMode]::Append)
try {
    $fs.Write($bytes, 0, $bytes.Length)
    $fs.Write($lenBytes, 0, 8)
    $fs.Write($magic, 0, 8)
} finally {
    $fs.Close()
}
Write-Host "  embedded $size bytes into $Exe"
