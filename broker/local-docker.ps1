param(
    [ValidateSet('Start', 'Check', 'Usage', 'Build')]
    [string]$Action = 'Start',
    [switch]$CaptureFictionalResponses
)

$ErrorActionPreference = 'Stop'
if ($CaptureFictionalResponses -and $Action -ne 'Start') {
    throw 'Response capture is available only with Start and fictional inputs.'
}
$metriaRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$metriaState = Join-Path $metriaRoot '.local-state'

if ($Action -ne 'Usage') {
    & docker build -f (Join-Path $PSScriptRoot 'Dockerfile.local') -t metria-broker:local $metriaRoot
    if ($LASTEXITCODE -ne 0) { throw 'Docker build failed. Start Docker Desktop with Linux containers and retry.' }
}
if ($Action -eq 'Build') { exit 0 }
if ($Action -eq 'Check') {
    # Disposable state: this check cannot reset or consume the app's quota ledger.
    & docker run --rm --init metria-broker:local --check
} else {
    if ($Action -eq 'Start') {
        Write-Host 'Stop the earlier Python launcher first. This uses the SAME private pass and quota ledger.'
        Write-Host 'The PC stays on. Nebius inference still uses your credits. Ctrl+C closes the tunnel.'
        [System.IO.Directory]::CreateDirectory($metriaState) | Out-Null
        # Windows-created bind-mount directories appear owned by container root.
        # Change only ownership of the two known directories, never their contents.
        # The actual broker remains UID 10001; no host ACL is loosened.
        & docker run --rm --user 0:0 --cap-drop ALL --cap-add CHOWN --security-opt no-new-privileges --entrypoint python --mount "type=bind,source=$metriaState,target=/app/.local-state" metria-broker:local broker/prepare_local_state.py
        if ($LASTEXITCODE -ne 0) { throw 'Local state ownership preparation failed. Existing state was preserved.' }
        $metriaArgs = @('--live', '--account-verified')
        if ($CaptureFictionalResponses) { $metriaArgs += '--capture-fictional-responses' }
        & docker run --rm -it --init --name metria-local --mount "type=bind,source=$metriaState,target=/app/.local-state" metria-broker:local @metriaArgs
    } else {
        if (-not (Test-Path -LiteralPath $metriaState -PathType Container)) { throw 'No local broker state exists.' }
        & docker run --rm --init --mount "type=bind,source=$metriaState,target=/app/.local-state,readonly" metria-broker:local --usage
    }
}
if ($LASTEXITCODE -ne 0) { throw 'Local broker command failed. No automatic inference retry was attempted.' }
