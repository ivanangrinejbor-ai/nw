param(
    [string]$OutFile,
    [string]$Icon,
    [string]$XmlPath
)

$xml = @"
<launch4jConfig>
  <headerType>gui</headerType>
  <dontWrapJar>true</dontWrapJar>
  <jar>player.jar</jar>
  <cmdLine>project.zip</cmdLine>
  <outfile>$OutFile</outfile>
  <icon>$Icon</icon>
  <classPath>
    <mainClass>org.catrobat.catroid.stage.DesktopStage</mainClass>
  </classPath>
  <jre>
    <path>jre</path>
    <minVersion>11</minVersion>
    <initialHeapSize>512</initialHeapSize>
    <maxHeapSize>4096</maxHeapSize>
  </jre>
</launch4jConfig>
"@

Set-Content -Path $XmlPath -Value $xml -Encoding UTF8
Write-Host "  wrote launch4j config: $XmlPath"
