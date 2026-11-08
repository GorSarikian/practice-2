param([string]$Dotnet = 'dotnet')
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
& "$PSScriptRoot/build.ps1"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
. "$PSScriptRoot/scripts/compare-graphs.ps1"
New-Item -ItemType Directory -Force generated/native | Out-Null
$targets = @{
    newtonsoft = 'Newtonsoft.Json'
    logging = 'Microsoft.Extensions.DependencyInjection.Abstractions'
    serilog = 'Microsoft.Extensions.Logging.Abstractions'
}
foreach ($name in @('newtonsoft', 'logging', 'serilog')) {
    $project = "examples/native/$name/Compare.csproj"
    & $Dotnet restore $project --configfile examples/native/NuGet.Config
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    & $Dotnet list $project package --include-transitive |
        Tee-Object -FilePath "generated/native/$name-packages.txt"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    & $Dotnet nuget why $project $targets[$name] |
        Tee-Object -FilePath "generated/native/$name-why.txt"
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Compare-PackageGraph $name
}
