param(
    [string]$BundleDir,
    [string]$IconIco,
    [string]$ZipOut
)

if (Test-Path $IconIco) { Copy-Item $IconIco "$BundleDir\icon.ico" -Force }

if (Test-Path $ZipOut) { Remove-Item $ZipOut -Force }

# Streaming zip creation — avoids Compress-Archive which loads everything into RAM.
# Uses .NET ZipArchive with 64 KB copy buffer for constant-memory operation.
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$bufferSize = 65536  # 64 KB streaming buffer
$zipStream = $null
$archive = $null
try {
    $zipStream = [System.IO.File]::Create($ZipOut)
    $archive = New-Object System.IO.Compression.ZipArchive($zipStream, [System.IO.Compression.ZipArchiveMode]::Create)

    $allFiles = Get-ChildItem -Path $BundleDir -Recurse -File
    $totalFiles = $allFiles.Count
    $fileIndex = 0

    $buffer = New-Object byte[] $bufferSize
    foreach ($file in $allFiles) {
        $fileIndex++
        $entryName = $file.FullName.Substring($BundleDir.Length + 1).Replace('\', '/')
        $entry = $archive.CreateEntry($entryName, [System.IO.Compression.CompressionLevel]::Fastest)

        $srcStream = $null
        $dstStream = $null
        try {
            $srcStream = [System.IO.File]::Open($file.FullName, [System.IO.FileMode]::Open, [System.IO.FileAccess]::Read, [System.IO.FileShare]::Read)
            $dstStream = $entry.Open()
            while ($true) {
                $n = $srcStream.Read($buffer, 0, $bufferSize)
                if ($n -le 0) { break }
                $dstStream.Write($buffer, 0, $n)
            }
        } finally {
            if ($dstStream) { $dstStream.Close() }
            if ($srcStream) { $srcStream.Close() }
        }

        if ($fileIndex % 10 -eq 0 -or $fileIndex -eq $totalFiles) {
            Write-Host "`r  zipping: $fileIndex / $totalFiles files" -NoNewline
        }
    }
    Write-Host ""
} finally {
    if ($archive) { $archive.Dispose() }
    if ($zipStream) { $zipStream.Close() }
}

$zipSize = [math]::Round((Get-Item $ZipOut).Length / 1MB, 1)
Write-Host "  template bundle rebuilt: $ZipOut ($zipSize MB) - streaming, 64 KB buffer"
