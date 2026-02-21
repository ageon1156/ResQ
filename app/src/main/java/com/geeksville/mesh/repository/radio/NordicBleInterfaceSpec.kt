

package com.geeksville.mesh.repository.radio

import co.touchlab.kermit.Logger
import com.geeksville.mesh.repository.bluetooth.BluetoothRepository
import org.meshtastic.core.model.util.anonymize
import javax.inject.Inject

class NordicBleInterfaceSpec
@Inject
constructor(
    private val factory: NordicBleInterfaceFactory,
    private val bluetoothRepository: BluetoothRepository,
) : InterfaceSpec<NordicBleInterface> {
    override fun createInterface(rest: String): NordicBleInterface = factory.create(rest)

    override fun addressValid(rest: String): Boolean {
        val allPaired = bluetoothRepository.state.value.bondedDevices.map { it.address }.toSet()
        return if (!allPaired.contains(rest)) {
            Logger.w { "Ignoring stale bond to ${rest.anonymize}" }
            false
        } else {
            true
        }
    }
}

