

package com.geeksville.mesh.repository.radio

import dagger.assisted.AssistedFactory

@AssistedFactory
interface MockInterfaceFactory {
    fun create(rest: String): MockInterface
}
