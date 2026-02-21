

package com.geeksville.mesh.repository.radio

import dagger.assisted.AssistedFactory

@AssistedFactory
interface SerialInterfaceFactory {
    fun create(rest: String): SerialInterface
}
