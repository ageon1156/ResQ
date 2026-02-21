

package com.geeksville.mesh.repository.radio

import dagger.assisted.AssistedFactory

@AssistedFactory
interface NordicBleInterfaceFactory {
    fun create(rest: String): NordicBleInterface
}

