

package com.geeksville.mesh.repository.radio

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import dagger.multibindings.Multibinds

@Suppress("unused") 
@Module
@InstallIn(SingletonComponent::class)
abstract class RadioRepositoryModule {

    @Multibinds abstract fun interfaceMap(): Map<InterfaceId, @JvmSuppressWildcards InterfaceSpec<*>>

    @[Binds IntoMap InterfaceMapKey(InterfaceId.BLUETOOTH)]
    abstract fun bindBluetoothInterfaceSpec(spec: NordicBleInterfaceSpec): @JvmSuppressWildcards InterfaceSpec<*>

    @[Binds IntoMap InterfaceMapKey(InterfaceId.MOCK)]
    abstract fun bindMockInterfaceSpec(spec: MockInterfaceSpec): @JvmSuppressWildcards InterfaceSpec<*>

    @[Binds IntoMap InterfaceMapKey(InterfaceId.NOP)]
    abstract fun bindNopInterfaceSpec(spec: NopInterfaceSpec): @JvmSuppressWildcards InterfaceSpec<*>

    @[Binds IntoMap InterfaceMapKey(InterfaceId.SERIAL)]
    abstract fun bindSerialInterfaceSpec(spec: SerialInterfaceSpec): @JvmSuppressWildcards InterfaceSpec<*>

    @[Binds IntoMap InterfaceMapKey(InterfaceId.TCP)]
    abstract fun bindTCPInterfaceSpec(spec: TCPInterfaceSpec): @JvmSuppressWildcards InterfaceSpec<*>
}

