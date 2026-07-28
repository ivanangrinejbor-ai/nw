param(
    [string]$OutFile,
    [string]$Icon,
    [string]$XmlPath,
    [string]$Jar
)

# Only emit <icon> when a real icon file exists. A missing/invalid icon must NOT
# break the build: launch4j+windres fail hard on a bad .ico, so in that case we
# omit the tag entirely and the exe is built with launch4j's default icon.
$iconLine = ""
if ($Icon -and (Test-Path $Icon) -and ((Get-Item $Icon).Length -gt 0)) {
    $iconLine = "  <icon>$Icon</icon>`r`n"
}

$xml = @"
<launch4jConfig>
  <headerType>gui</headerType>
  <dontWrapJar>false</dontWrapJar>
  <jar>$Jar</jar>
  <cmdLine>project.zip</cmdLine>
  <outfile>$OutFile</outfile>
  <chdir>.</chdir>
$iconLine  <jre>
    <path>jre</path>
    <minVersion>11</minVersion>
    <initialHeapSize>512</initialHeapSize>
    <maxHeapSize>4096</maxHeapSize>
  </jre>
</launch4jConfig>
"@

Set-Content -Path $XmlPath -Value $xml -Encoding UTF8
Write-Host "  wrote launch4j config: $XmlPath"
