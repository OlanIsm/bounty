# Build the installable coursework APK with Android's standard debug signature.
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
Push-Location $repo
try {
    & ./gradlew.bat --no-daemon --max-workers=2 '-Dorg.gradle.jvmargs=-Xmx512m -Dfile.encoding=UTF-8' -PbountyIsolatedTest=false assembleDebug lintDebug
    if ($LASTEXITCODE -ne 0) { throw 'APK build or lint failed.' }
    $metadata = Get-Content app/build/outputs/apk/debug/output-metadata.json -Raw | ConvertFrom-Json
    if ($metadata.applicationId -ne 'id.ac.binus.bounty') { throw 'Refusing to package an isolated test APK.' }
    New-Item -ItemType Directory -Force build | Out-Null
    Copy-Item -LiteralPath app/build/outputs/apk/debug/app-debug.apk -Destination build/Bounty.apk -Force
    Get-Item build/Bounty.apk | Select-Object FullName, Length
    Get-FileHash build/Bounty.apk -Algorithm SHA256
} finally {
    Pop-Location
}
