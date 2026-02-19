$ADB = "C:\Users\searc\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$DEV1 = "adb-ZA222XYK4Q-3Bs0TK._adb-tls-connect._tcp"
$DEV2 = "adb-ZD222KRTPM-OBvy32._adb-tls-connect._tcp"
$LOG1 = "$PSScriptRoot\logcat_edge60pro_live.txt"
$LOG2 = "$PSScriptRoot\logcat_edge50pro_live.txt"

# Clear previous logs
"" | Out-File $LOG1 -Encoding UTF8
"" | Out-File $LOG2 -Encoding UTF8

Write-Host "Starting logcat for both devices..." -ForegroundColor Green
Write-Host "edge 60 pro -> $LOG1" -ForegroundColor Cyan
Write-Host "edge 50 pro -> $LOG2" -ForegroundColor Yellow

$job1 = Start-Job -ScriptBlock {
    param($adb, $dev, $log)
    & $adb -s $dev logcat -v threadtime 2>&1 | ForEach-Object {
        "[edge60pro] $_" | Out-File -FilePath $log -Append -Encoding UTF8
    }
} -ArgumentList $ADB, $DEV1, $LOG1

$job2 = Start-Job -ScriptBlock {
    param($adb, $dev, $log)
    & $adb -s $dev logcat -v threadtime 2>&1 | ForEach-Object {
        "[edge50pro] $_" | Out-File -FilePath $log -Append -Encoding UTF8
    }
} -ArgumentList $ADB, $DEV2, $LOG2

Write-Host "Background jobs started: Job $($job1.Id) (60pro), Job $($job2.Id) (50pro)" -ForegroundColor Green
Write-Host "Tailing merged output... (Ctrl+C to stop)" -ForegroundColor Green
Write-Host ("=" * 80)

$pos1 = 0
$pos2 = 0

try {
    while ($true) {
        Start-Sleep -Milliseconds 300

        if (Test-Path $LOG1) {
            $content = Get-Content $LOG1 -Encoding UTF8 -ErrorAction SilentlyContinue
            if ($content -and $content.Count -gt $pos1) {
                $newLines = $content[$pos1..($content.Count - 1)]
                foreach ($l in $newLines) {
                    if ($l) { Write-Host $l -ForegroundColor Cyan }
                }
                $pos1 = $content.Count
            }
        }

        if (Test-Path $LOG2) {
            $content = Get-Content $LOG2 -Encoding UTF8 -ErrorAction SilentlyContinue
            if ($content -and $content.Count -gt $pos2) {
                $newLines = $content[$pos2..($content.Count - 1)]
                foreach ($l in $newLines) {
                    if ($l) { Write-Host $l -ForegroundColor Yellow }
                }
                $pos2 = $content.Count
            }
        }
    }
}
finally {
    Write-Host "Stopping jobs..." -ForegroundColor Red
    Stop-Job $job1, $job2
    Remove-Job $job1, $job2
}
