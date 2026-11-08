function Assert-EqualSet($Expected, $Actual, [string]$Label) {
    $difference = @(Compare-Object @($Expected | Sort-Object -Unique) @($Actual | Sort-Object -Unique))
    if ($difference.Count -ne 0) {
        $difference | Format-Table | Out-String | Write-Output
        throw ('Graph mismatch: ' + $Label)
    }
}

function Compare-PackageGraph([string]$Name) {
    $runtime = if ($env:JAVA_HOME) { Join-Path $env:JAVA_HOME 'bin/java.exe' } else { 'java' }
    $lines = & $runtime '-Dfile.encoding=UTF-8' -cp build/classes nugetgraph.Main stage3 "examples/$Name.toml"
    if ($LASTEXITCODE -ne 0) { throw ('Java graph failed for ' + $Name) }
    $nodes = @()
    $edges = @()
    foreach ($line in $lines) {
        $pair = $line -split ': ', 2
        $node = $pair[0].ToLowerInvariant()
        $nodes += $node
        foreach ($child in ($pair[1] -split ', ')) {
            if ($child) { $edges += ($node + ' -> ' + $child.ToLowerInvariant()) }
        }
    }
    $assets = Get-Content "examples/native/$Name/obj/project.assets.json" -Raw | ConvertFrom-Json
    $target = $assets.targets.'net8.0'
    $resolved = @{}
    foreach ($property in $target.PSObject.Properties) {
        $id = ($property.Name -split '/')[0].ToLowerInvariant()
        $resolved[$id] = $property.Name.Replace('/', '@').ToLowerInvariant()
    }
    $nativeEdges = @()
    foreach ($property in $target.PSObject.Properties) {
        foreach ($child in $property.Value.dependencies.PSObject.Properties) {
            $nativeEdges += ($property.Name.Replace('/', '@').ToLowerInvariant() + ' -> ' +
                $resolved[$child.Name.ToLowerInvariant()])
        }
    }
    Assert-EqualSet @($resolved.Values) $nodes "$Name nodes"
    Assert-EqualSet $nativeEdges $edges "$Name edges"
    Write-Output "PASS $Name : $($nodes.Count) nodes, $($edges.Count) edges match native restore"
}
