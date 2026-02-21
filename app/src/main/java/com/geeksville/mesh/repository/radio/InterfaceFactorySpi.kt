

package com.geeksville.mesh.repository.radio

interface InterfaceFactorySpi<T : IRadioInterface> {
    fun create(rest: String): T
}

