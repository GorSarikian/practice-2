param([switch]$Test)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$compiler = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/javac.exe' } else { 'javac' }
$runtime = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
New-Item -ItemType Directory -Force build/classes | Out-Null
$sources = @(Get-ChildItem src -Recurse -Filter '*.java' | ForEach-Object FullName)
if ($Test) { $sources += @(Get-ChildItem tests -Recurse -Filter '*.java' | ForEach-Object FullName) }
& $compiler --release 17 -encoding UTF-8 -d build/classes @sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($Test) {
    & $runtime '-Dfile.encoding=UTF-8' -cp build/classes nugetgraph.AllTests
    exit $LASTEXITCODE
}
