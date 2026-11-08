$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
& "$PSScriptRoot/build.ps1"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$runtime = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
$source = Get-Content examples/test.toml -Raw
$cases = [ordered]@{
    package = @('package = "A"', 'package = "a"')
    repository = @('repository = "graph.txt"', 'repository = "missing.txt"')
    test_mode = @('test_mode = true', 'test_mode = "true"')
    output = @('test.png', 'test.svg')
    ascii_tree = @('ascii_tree = true', 'ascii_tree = 1')
    max_depth = @('max_depth = 5', 'max_depth = -1')
    filter = @('filter = ""', 'filter = false')
}
$temporary = Join-Path $PSScriptRoot 'examples/error-demo.tmp.toml'
try {
    foreach ($entry in $cases.GetEnumerator()) {
        Write-Output ("--- Invalid parameter: " + $entry.Key)
        $source.Replace($entry.Value[0], $entry.Value[1]) |
            Set-Content -LiteralPath $temporary -Encoding UTF8
        & $runtime '-Dfile.encoding=UTF-8' -cp build/classes nugetgraph.Main stage1 $temporary
        if ($LASTEXITCODE -ne 1) { throw ('Expected exit 1 for ' + $entry.Key) }
    }
} finally { Remove-Item -LiteralPath $temporary -ErrorAction SilentlyContinue }
exit 0
