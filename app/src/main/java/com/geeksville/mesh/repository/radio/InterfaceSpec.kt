

package com.geeksville.mesh.repository.radio

interface InterfaceSpec<T : IRadioInterface> {
    fun createInterface(rest: String): T

    fun addressValid(rest: String): Boolean = true
}
