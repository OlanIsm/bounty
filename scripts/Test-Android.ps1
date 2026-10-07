param(
    [string]$Adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe",
    [string]$Serial = ""
)

# Run from any directory. Only the fixed .testing installation is disposable.
$ErrorActionPreference = 'Stop'
$repo = Split-Path $PSScriptRoot -Parent
$package = 'id.ac.binus.bounty.testing'
$animations = @{}
function Invoke-Adb {
    param([Parameter(ValueFromRemainingArguments)] [string[]]$Command)
    # Transient UI dump readiness can be reported on stderr with a zero exit code.
    $ErrorActionPreference = 'Continue'
    $result = & $Adb -s $Serial @Command 2>&1
    if ($LASTEXITCODE -ne 0) { throw "adb failed: $($Command -join ' '): $result" }
    return ($result -join "`n")
}
function Wait-View([string]$Id, [string]$Text = '') {
    $end = [DateTime]::UtcNow.AddSeconds(45)
    do {
        $dump = Invoke-Adb shell uiautomator dump /sdcard/bounty-testing-ui.xml
        if ($dump -notmatch 'ERROR:') {
            [xml]$tree = Invoke-Adb shell cat /sdcard/bounty-testing-ui.xml
            $found = @($tree.SelectNodes('//node') | Where-Object {
                $_.'resource-id' -eq "${package}:id/$Id" -and (!$Text -or $_.text -eq $Text)
            })
            if ($found.Count) { return }
        }
        Start-Sleep -Milliseconds 250
    } while ([DateTime]::UtcNow -lt $end)
    throw "UI did not show $Id / $Text"
}
function Run-Tests([string]$Filter, [string]$Class, [string]$Report) {
    $output = Invoke-Adb shell am instrument -w -e $Filter $Class "${package}.test/androidx.test.runner.AndroidJUnitRunner"
    $output | Set-Content -Encoding utf8 $Report
    Write-Output $output
    if ($output -notmatch 'OK \(\d+ tests?\)' -or $output -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed') {
        throw "Instrumentation failed; see $Report"
    }
}

if (!(Test-Path -LiteralPath $Adb)) { throw "adb not found: $Adb" }
if (!$Serial) {
    $devices = @(& $Adb devices | Select-String '^([^\s]+)\s+device$' | ForEach-Object { $_.Matches[0].Groups[1].Value })
    if ($devices.Count -ne 1) { throw 'Connect one device, or pass -Serial.' }
    $Serial = $devices[0]
}
Push-Location $repo
try {
    & ./gradlew.bat --no-daemon --max-workers=2 '-Dorg.gradle.jvmargs=-Xmx512m -Dfile.encoding=UTF-8' -PbountyIsolatedTest=true assembleDebug assembleDebugAndroidTest
    if ($LASTEXITCODE -ne 0) { throw 'Isolated APK build failed.' }
    $metadata = Get-Content app/build/outputs/apk/debug/output-metadata.json -Raw | ConvertFrom-Json
    if ($metadata.applicationId -ne $package) { throw 'Refusing to test an APK with the canonical app ID.' }
    foreach ($key in @('window_animation_scale', 'transition_animation_scale', 'animator_duration_scale')) {
        $animations[$key] = (Invoke-Adb shell settings get global $key).Trim()
        $null = Invoke-Adb shell settings put global $key 0
    }
    foreach ($id in @("${package}.test", $package)) {
        if ((Invoke-Adb shell pm list packages $id) -split "`n" -contains "package:$id") {
            $null = Invoke-Adb uninstall $id
        }
    }
    $null = Invoke-Adb install app/build/outputs/apk/debug/app-debug.apk
    $null = Invoke-Adb install app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
    $null = Invoke-Adb shell am start -n "$package/id.ac.binus.bounty.activities.SplashActivity"
    Wait-View login_button
    Write-Output 'PASS: fresh install opens Login.'
    $reports = 'app/build/reports/m12'
    New-Item -ItemType Directory -Force $reports | Out-Null
    # Run feature checks first. The continuous flow runs last, retaining its paid hunter for restart.
    Run-Tests notClass id.ac.binus.bounty.MainFlowTest "$reports/regression.txt"
    Run-Tests class id.ac.binus.bounty.MainFlowTest "$reports/main-flow.txt"
    $null = Invoke-Adb shell am force-stop $package
    $null = Invoke-Adb shell am start -n "$package/id.ac.binus.bounty.activities.SplashActivity"
    Wait-View welcome_text 'Halo, M12 Hunter!'
    Wait-View demo_balance_amount 'Rp70.000'
    # A second process restart must not credit the same approved challenge twice.
    $null = Invoke-Adb shell am force-stop $package
    $null = Invoke-Adb shell am start -n "$package/id.ac.binus.bounty.activities.SplashActivity"
    Wait-View demo_balance_amount 'Rp70.000'
    'PASS: two actual process restarts retain the hunter session and Rp70.000 balance.' |
        Tee-Object -FilePath "$reports/restart.txt"
} finally {
    foreach ($key in $animations.Keys) {
        if ($animations[$key] -eq 'null') { & $Adb -s $Serial shell settings delete global $key | Out-Null }
        else { & $Adb -s $Serial shell settings put global $key $animations[$key] | Out-Null }
    }
    foreach ($id in @("${package}.test", $package)) {
        $installed = (& $Adb -s $Serial shell pm list packages $id) -join "`n"
        if ($installed -split "`n" -contains "package:$id") {
            & $Adb -s $Serial uninstall $id | Out-Null
        }
    }
    & $Adb -s $Serial shell rm -f /sdcard/bounty-testing-ui.xml | Out-Null
    Pop-Location
}
