

package com.geeksville.mesh.repository.radio

import dagger.assisted.AssistedFactory

@AssistedFactory
interface TCPInterfaceFactory {
    fun create(rest: String): TCPInterface
}
