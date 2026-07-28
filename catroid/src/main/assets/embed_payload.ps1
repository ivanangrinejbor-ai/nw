param(
    [string]$Exe,
    [string]$Payload
)

if (-not (Test-Path $Exe)) { Write-Host "  embed: exe not found: $Exe"; exit 1 }
if (-not (Test-Path $Payload)) { Write-Host "  embed: payload not found: $Payload"; exit 1 }

$magic = [System.Text.Encoding]::ASCII.GetBytes("NEOCAT01")
$bufferSize = 65536  # 64 KB streaming buffer — same as ProjectCrypto.STREAM_BUFFER

# Stream payload into exe without loading the entire file into memory.
# This avoids OOM on large projects (758 MB+).
$payloadStream = $null
$exeStream = $null
try {
    $payloadStream = [System.IO.File]::Open($Payload, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::Read)
    $size = $payloadStream.Length
    $lenBytes = [System.BitConverter]::GetBytes([int64]$size)

    $exeStream = [System.IO.File]::Open($Exe, [System.IO.FileMode]::Append, [System.IO.FileAccess]::Write, [System.IO.FileShare]::None)

    $buffer = New-Object byte[] $bufferSize
    $totalWritten = [int64]0
    $lastPct = -1
    while ($true) {
        $n = $payloadStream.Read($buffer, 0, $bufferSize)
        if ($n -le 0) { break }
        $exeStream.Write($buffer, 0, $n)
        $totalWritten += $n
        $pct = [math]::Floor($totalWritten * 100 / $size)
        if ($pct -ne $lastPct -and $pct % 10 -eq 0) {
            Write-Host "`r  embedding: $pct% ($([math]::Round($totalWritten/1MB,1)) / $([math]::Round($size/1MB,1)) MB)" -NoNewline
            $lastPct = $pct
        }
    }
    Write-Host ""
    $exeStream.Write($lenBytes, 0, 8)
    $exeStream.Write($magic, 0, 8)
} finally {
    if ($payloadStream) { $payloadStream.Close() }
    if ($exeStream) { $exeStream.Close() }
}
Write-Host "  embedded $size bytes into $Exe (streaming, 64 KB buffer)"
