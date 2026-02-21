# All lines in the log files are already Meshtastic-only (captured with --pid).
# Just filter for events relevant to connect/disconnect/silent-node detection.

$LOG1    = "$PSScriptRoot\logcat_edge60pro_live.txt"
$LOG2    = "$PSScriptRoot\logcat_edge50pro_live.txt"
$OUTFILE = "$PSScriptRoot\silent_events.txt"

$keywords = 'SilentNode|silent node|marked SILENT|CONFIRMED SILENT|came back online|' +
            'Detector started|Detector stop|cleanupStalePins|autoPlace|autoRemove|' +
            'Broadcast silent|SILENCE_REPORT|HEARTBEAT_PING|MESH_EXIT|fireNotif|' +
            'Ping sent|missedPings|missed pings|notificationFired|trackedNodes|' +
            'Lost device|lostDevice|radioConnected|startConnect|stopConnect|' +
            'onDisconnect|onConnect|reconnect|disconnected|Disconnected|' +
            'BLE.*lost|lost.*BLE|interface.*stop|interface.*start|' +
            'MeshService.*start|MeshService.*stop|service.*start|service.*stop|' +
            'ONLINE|broadcast.*alert|alert.*broadcast|' +
            'start\b|stop\b|connect\b|disconnect\b|lost\b'

"" | Out-File $OUTFILE -Encoding UTF8

$pos1 = (Get-Content $LOG1 -ErrorAction SilentlyContinue).Count
$pos2 = (Get-Content $LOG2 -ErrorAction SilentlyContinue).Count

$header = "$(Get-Date -Format 'HH:mm:ss') === MONITOR READY === 60pro(line $pos1) | 50pro(line $pos2)"
Write-Host $header -ForegroundColor Green
$header | Out-File $OUTFILE -Append -Encoding UTF8

$iters = 0
while ($iters -lt 1500) {
    Start-Sleep -Milliseconds 300
    $iters++

    $c1 = Get-Content $LOG1 -ErrorAction SilentlyContinue
    if ($c1 -and $c1.Count -gt $pos1) {
        $newLines = $c1[$pos1..($c1.Count - 1)]
        foreach ($l in $newLines) {
            if ($l -match $keywords) {
                $ts  = Get-Date -Format 'HH:mm:ss'
                $out = "[$ts][60PRO] $l"
                Write-Host $out -ForegroundColor Cyan
                $out | Out-File $OUTFILE -Append -Encoding UTF8
            }
        }
        $pos1 = $c1.Count
    }

    $c2 = Get-Content $LOG2 -ErrorAction SilentlyContinue
    if ($c2 -and $c2.Count -gt $pos2) {
        $newLines = $c2[$pos2..($c2.Count - 1)]
        foreach ($l in $newLines) {
            if ($l -match $keywords) {
                $ts  = Get-Date -Format 'HH:mm:ss'
                $out = "[$ts][50PRO] $l"
                Write-Host $out -ForegroundColor Yellow
                $out | Out-File $OUTFILE -Append -Encoding UTF8
            }
        }
        $pos2 = $c2.Count
    }
}

"$(Get-Date -Format 'HH:mm:ss') Monitor timeout" | Out-File $OUTFILE -Append -Encoding UTF8
