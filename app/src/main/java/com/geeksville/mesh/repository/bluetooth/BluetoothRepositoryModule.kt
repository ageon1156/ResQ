

package com.geeksville.mesh.repository.bluetooth

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.native
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BluetoothRepositoryModule {
    @Provides
    @Singleton
    fun provideCentralManager(@ApplicationContext context: Context, coroutineScope: CoroutineScope): CentralManager =
        CentralManager.native(context, coroutineScope)

    @Provides
    @Singleton
    fun provideSingletonCoroutineScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

