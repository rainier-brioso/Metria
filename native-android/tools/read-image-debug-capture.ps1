param(
    [string]$Serial
)

# Read one member-authorized, debug-only capture. Never write raw output to the repository.
$metriaAdb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
if (-not (Test-Path -LiteralPath $metriaAdb)) { throw 'Android SDK ADB was not found.' }

if ([string]::IsNullOrWhiteSpace($Serial)) {
    $devices = @(& $metriaAdb devices -l | ForEach-Object {
        if ($_ -match '^(.*?)\s+device\s+product:') { $Matches[1] }
    })
    if ($devices.Count -ne 1) { throw 'Connect exactly one device or pass its current exact serial with -Serial.' }
    $Serial = $devices[0]
}

function Read-Exact([IO.BinaryReader]$reader, [int]$count) {
    $bytes = $reader.ReadBytes($count)
    if ($bytes.Length -ne $count) { throw 'The debug capture is incomplete.' }
    return ,$bytes
}

function Read-Int32BE([IO.BinaryReader]$reader) {
    $bytes = Read-Exact $reader 4
    [Array]::Reverse($bytes)
    return [BitConverter]::ToInt32($bytes, 0)
}

function Read-Int64BE([IO.BinaryReader]$reader) {
    $bytes = Read-Exact $reader 8
    [Array]::Reverse($bytes)
    return [BitConverter]::ToInt64($bytes, 0)
}

function Read-ShortUtf8([IO.BinaryReader]$reader) {
    $count = Read-Int32BE $reader
    if ($count -lt 0 -or $count -gt 32) { throw 'Invalid debug capture field length.' }
    return [Text.Encoding]::UTF8.GetString((Read-Exact $reader $count))
}

$metriaTempDir = Join-Path ([IO.Path]::GetTempPath()) ('metria-image-debug-' + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $metriaTempDir -ErrorAction Stop | Out-Null
$metriaBinary = Join-Path $metriaTempDir 'capture.bin'

try {
    if ($Serial -notmatch '^[A-Za-z0-9_.:() -]+$') { throw 'The device serial contains unsupported characters.' }
    $null = & $metriaAdb -s $Serial shell run-as app.metria ls no_backup/debug-image-capture/capture.bin 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw 'No readable image capture is available. Enable capture before the run and keep Metria open until it is read.'
    }
    $start = [Diagnostics.ProcessStartInfo]::new()
    $start.FileName = $metriaAdb
    $start.Arguments = '-s "' + $Serial + '" exec-out run-as app.metria cat no_backup/debug-image-capture/capture.bin'
    $start.UseShellExecute = $false
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $start.CreateNoWindow = $true
    $process = [Diagnostics.Process]::new()
    $process.StartInfo = $start
    try {
        if (-not $process.Start()) { throw 'ADB did not start.' }
        $output = [IO.File]::Create($metriaBinary)
        try { $process.StandardOutput.BaseStream.CopyTo($output) }
        finally { $output.Dispose() }
        $process.WaitForExit()
        if ($process.ExitCode -ne 0) { throw 'The debug capture could not be read through run-as.' }
    } finally {
        $process.Dispose()
    }
    if ((Get-Item -LiteralPath $metriaBinary).Length -eq 0) {
        throw 'No readable image capture is available. Enable capture in Developer diagnostics before the run and keep Metria open.'
    }
    $stream = [IO.File]::OpenRead($metriaBinary)
    try {
        $reader = [IO.BinaryReader]::new($stream)
        if ((Read-Int32BE $reader) -ne 0x4D494341 -or (Read-Int32BE $reader) -ne 1) {
            throw 'Unrecognized image capture format.'
        }
        $capturedAt = Read-Int64BE $reader
        $duration = Read-Int64BE $reader
        $age = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() - $capturedAt
        if ($age -lt 0 -or $age -gt 1800000) {
            & $metriaAdb -s $Serial shell run-as app.metria rm no_backup/debug-image-capture/capture.bin | Out-Null
            throw 'The image capture expired and was removed.'
        }
        $operation = Read-ShortUtf8 $reader
        $category = Read-ShortUtf8 $reader
        $outcome = Read-ShortUtf8 $reader
        $responseLength = Read-Int32BE $reader
        if ($responseLength -lt 0 -or $responseLength -gt 64000) { throw 'Invalid model response length.' }
        $response = [Text.Encoding]::UTF8.GetString((Read-Exact $reader $responseLength))
        $imageLength = Read-Int32BE $reader
        if ($imageLength -lt 0 -or $imageLength -gt 131072) { throw 'Invalid reduced photo length.' }
        $imageBytes = Read-Exact $reader $imageLength
        if ($stream.Position -ne $stream.Length) { throw 'Unexpected trailing capture data.' }
    } finally {
        $reader.Dispose()
    }

    Write-Output "Captured UTC: $([DateTimeOffset]::FromUnixTimeMilliseconds($capturedAt).UtcDateTime.ToString('u'))"
    Write-Output "Operation: $operation | Outcome: $outcome | Category: $category | Duration: ${duration} ms"
    Write-Output 'Model response (private development capture):'
    Write-Output $response
    if ($imageLength -gt 0) {
        $photoPath = Join-Path $metriaTempDir 'reduced-test-photo.jpg'
        [IO.File]::WriteAllBytes($photoPath, $imageBytes)
        Write-Output "Reduced photo (temporary, outside repository): $photoPath"
    }
} finally {
    if (Test-Path -LiteralPath $metriaBinary) { Remove-Item -LiteralPath $metriaBinary -Force }
    if (-not (Test-Path -LiteralPath (Join-Path $metriaTempDir 'reduced-test-photo.jpg'))) {
        Remove-Item -LiteralPath $metriaTempDir
    }
}
