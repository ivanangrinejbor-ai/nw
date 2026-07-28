param(
    [string]$Jar,
    [string]$Payload,
    [string]$EntryName = "embedded_project.ncpp"
)

# Inject the (already NCPP-encrypted) project into player.jar as a normal zip entry,
# BEFORE launch4j wraps it into the exe. This keeps the exe a valid self-executing zip
# (DesktopStage reads the project via getResourceAsStream), unlike an appended footer
# which pushes the zip EOCD past the 64 KB tail and breaks `java -jar`.
#
# STREAMING implementation: build a fresh jar, copy entries entry-by-entry, then stream
# the payload straight from disk. A huge project (hundreds of MB) is therefore never held
# in memory - ZipArchive Update mode + [File]::ReadAllBytes would OOM/hang on ~700 MB.

if (-not (Test-Path $Jar)) { Write-Host "  inject: jar not found: $Jar"; exit 1 }
if (-not (Test-Path $Payload)) { Write-Host "  inject: payload not found: $Payload"; exit 1 }

Add-Type -AssemblyName System.IO.Compression | Out-Null
Add-Type -AssemblyName System.IO.Compression.FileSystem | Out-Null

$tmp = "$Jar.tmp"
if (Test-Path $tmp) { Remove-Item $tmp -Force }

$srcFs = $null; $srcZip = $null; $dstFs = $null; $dstZip = $null; $payFs = $null
try {
    $srcFs = [System.IO.File]::OpenRead($Jar)
    $srcZip = New-Object System.IO.Compression.ZipArchive($srcFs, [System.IO.Compression.ZipArchiveMode]::Read)
    $dstFs = [System.IO.File]::Open($tmp, [System.IO.FileMode]::CreateNew, [System.IO.FileAccess]::Write)
    $dstZip = New-Object System.IO.Compression.ZipArchive($dstFs, [System.IO.Compression.ZipArchiveMode]::Create)

    $buf = New-Object byte[] (1048576)  # 1 MB streaming buffer

    # 1) Copy every existing jar entry (streamed), skipping any stale injected payload.
    foreach ($e in $srcZip.Entries) {
        if ($e.FullName -eq $EntryName) { continue }
        $ne = $dstZip.CreateEntry($e.FullName, [System.IO.Compression.CompressionLevel]::Optimal)
        $inS = $e.Open()
        $outS = $ne.Open()
        while (($n = $inS.Read($buf, 0, $buf.Length)) -gt 0) { $outS.Write($buf, 0, $n) }
        $outS.Close(); $inS.Close()
    }

    # 2) Add the project payload (stored uncompressed - already encrypted), streamed
    #    straight from the file so it is never fully loaded into memory.
    $pe = $dstZip.CreateEntry($EntryName, [System.IO.Compression.CompressionLevel]::NoCompression)
    $peS = $pe.Open()
    $payFs = [System.IO.File]::OpenRead($Payload)
    $total = [int64]0
    while (($n = $payFs.Read($buf, 0, $buf.Length)) -gt 0) { $peS.Write($buf, 0, $n); $total += $n }
    $peS.Close()

    # Close/flush everything (in order) BEFORE swapping the jar in.
    $dstZip.Dispose(); $dstZip = $null
    $dstFs.Close(); $dstFs = $null
    $srcZip.Dispose(); $srcZip = $null
    $srcFs.Close(); $srcFs = $null
    $payFs.Close(); $payFs = $null

    Move-Item -Force $tmp $Jar
    Write-Host "  injected $total bytes as '$EntryName' (streamed) into $Jar"
} finally {
    # On the error path, close whatever is still open and drop the partial temp jar so
    # the original $Jar is never left corrupted.
    if ($dstZip) { $dstZip.Dispose() }
    if ($dstFs) { $dstFs.Close() }
    if ($srcZip) { $srcZip.Dispose() }
    if ($srcFs) { $srcFs.Close() }
    if ($payFs) { $payFs.Close() }
    if (Test-Path $tmp) { Remove-Item $tmp -Force -ErrorAction SilentlyContinue }
}
