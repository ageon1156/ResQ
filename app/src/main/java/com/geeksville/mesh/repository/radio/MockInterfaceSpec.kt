

package com.geeksville.mesh.repository.radio

import javax.inject.Inject

class MockInterfaceSpec @Inject constructor(
    private val factory: MockInterfaceFactory
) : InterfaceSpec<MockInterface> {
    override fun createInterface(rest: String): MockInterface {
        return factory.create(rest)
    }

    override fun addressValid(rest: String): Boolean = true
}

