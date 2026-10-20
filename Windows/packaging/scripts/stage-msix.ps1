# Stage Mailstack MSIX package root from a release build.
# Does NOT sign or pack — requires MakeAppx separately.
# Usage (Developer PowerShell with MSVC env):
#   .\Windows\packaging\scripts\stage-msix.ps1 -ExePath .\target\x86_64-pc-windows-msvc\release\Mailstack.exe

param(
    [Parameter(Mandatory = $true)]
    [string]$ExePath,
    [string]$StageDir = "$PSScriptRoot\..\msix\_stage",
    [string]$ManifestStub = "$PSScriptRoot\..\msix\AppxManifest.xml.stub"
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $ExePath)) {
    throw "Exe not found: $ExePath"
}

New-Item -ItemType Directory -Force -Path $StageDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $StageDir "Assets") | Out-Null

Copy-Item -Force $ExePath (Join-Path $StageDir "Mailstack.exe")
Copy-Item -Force $ManifestStub (Join-Path $StageDir "AppxManifest.xml")

Write-Host "Staged to $StageDir"
Write-Host "TODO: copy StoreLogo.png / Square* logos into Assets\"
Write-Host "TODO: set Identity Publisher to signing cert CN"
Write-Host "Next: MakeAppx pack /d `"$StageDir`" /p Mailstack.msix"
