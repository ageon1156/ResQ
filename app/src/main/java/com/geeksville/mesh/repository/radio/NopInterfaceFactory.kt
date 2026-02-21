

package com.geeksville.mesh.repository.radio

import dagger.assisted.AssistedFactory

@AssistedFactory
interface NopInterfaceFactory {
    fun create(rest: String): NopInterface
}
