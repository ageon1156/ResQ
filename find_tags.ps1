$log = Get-Content "C:\Users\searc\StudioProjects\beta1\logcat_edge60pro_live.txt"

# Show ONLY the high-signal events: connection state changes, SilentNode events, pings, stops/starts
$critical = "SilentNodeDetector|onConnectionChanged|Starting connect|Stopping connect|" +
            "Creating mesh service|Trying to start service|Ping sent|marked SILENT|" +
            "CONFIRMED SILENT|came back online|Node.*SILENT|Broadcast silent|" +
            "cleanupStalePins|Auto-removed|Auto-placed|Firmware heartbeat|" +
            "BLE connection state changed|Broadcasting connection state|" +
            "Lifecycle:|Lost device|DeviceSleep|Disconnected|Connected"

$log | Where-Object { $_ -match $critical } | Where-Object {
    # exclude BLE characteristic/descriptor/GATT noise
    $_ -notmatch "GattCallback|onChar|Descriptor|onSearch|requestConn|configMTU|onConnectionUpdated|" +
                 "PHY changed|CentralManager|BLE PHY|logRadio|keepAlive|logRadioSub|Found logRadio"
} | Select-Object -Last 100
