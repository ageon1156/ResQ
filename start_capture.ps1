$ADB  = "C:\Users\searc\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$DEV1 = "adb-ZA222XYK4Q-3Bs0TK._adb-tls-connect._tcp"  # edge60pro
$DEV2 = "adb-ZD222KRTPM-OBvy32._adb-tls-connect._tcp"  # edge50pro
$LOG1 = "$PSScriptRoot\logcat_edge60pro_live.txt"
$LOG2 = "$PSScriptRoot\logcat_edge50pro_live.txt"

# Get PIDs from ps (more reliable than pidof)
function Get-MeshPid($adb, $dev) {
    $lines = & $adb -s $dev shell "ps" 2>$null
    foreach ($l in $lines) {
        if ($l -match "geeksville") {
            if ($l -match '^\S+\s+(\d+)') { return $Matches[1] }
        }
    }
    return $null
}

$pid60 = Get-MeshPid $ADB $DEV1
$pid50 = Get-MeshPid $ADB $DEV2

Write-Host "edge60pro Meshtastic PID: $pid60" -ForegroundColor Cyan
Write-Host "edge50pro Meshtastic PID: $pid50" -ForegroundColor Yellow

if (-not $pid60) { Write-Host "ERROR: Meshtastic not running on edge60pro" -ForegroundColor Red; exit 1 }
if (-not $pid50) { Write-Host "ERROR: Meshtastic not running on edge50pro" -ForegroundColor Red; exit 1 }

# Kill old adb logcat processes
Get-Process -Name "adb" -ErrorAction SilentlyContinue | Stop-Process -Force -ErrorAction SilentlyContinue
Start-Sleep -Milliseconds 600

# Clear log files and buffers
"" | Out-File $LOG1 -Encoding UTF8
"" | Out-File $LOG2 -Encoding UTF8
& $ADB -s $DEV1 logcat -c 2>$null
& $ADB -s $DEV2 logcat -c 2>$null
Start-Sleep -Milliseconds 400

# Capture with --pid so only Meshtastic lines go to files
$proc1 = Start-Process -FilePath $ADB `
    -ArgumentList "-s", $DEV1, "logcat", "-v", "threadtime", "--pid", $pid60 `
    -RedirectStandardOutput $LOG1 -NoNewWindow -PassThru

$proc2 = Start-Process -FilePath $ADB `
    -ArgumentList "-s", $DEV2, "logcat", "-v", "threadtime", "--pid", $pid50 `
    -RedirectStandardOutput $LOG2 -NoNewWindow -PassThru

"$pid60" | Out-File "$PSScriptRoot\.mesh_pids.txt"
"$pid50" | Out-File "$PSScriptRoot\.mesh_pids.txt" -Append
"$($proc1.Id),$($proc2.Id)" | Out-File "$PSScriptRoot\.logcat_pids.txt"

Write-Host "Capture started: logcat->60pro PID=$pid60, logcat->50pro PID=$pid50" -ForegroundColor Green
