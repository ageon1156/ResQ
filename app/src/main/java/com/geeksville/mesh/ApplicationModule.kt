

package com.geeksville.mesh

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.geeksville.mesh.service.MeshServiceNotificationsImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.meshtastic.core.common.BuildConfigProvider
import org.meshtastic.core.di.ProcessLifecycle
import org.meshtastic.core.service.MeshServiceNotifications
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
interface ApplicationModule {

    @Binds fun bindMeshServiceNotifications(impl: MeshServiceNotificationsImpl): MeshServiceNotifications

    companion object {
        @Provides @ProcessLifecycle
        fun provideProcessLifecycleOwner(): LifecycleOwner = ProcessLifecycleOwner.get()

        @Provides
        @ProcessLifecycle
        fun provideProcessLifecycle(@ProcessLifecycle processLifecycleOwner: LifecycleOwner): Lifecycle =
            processLifecycleOwner.lifecycle

        @Singleton
        @Provides
        fun provideBuildConfigProvider(): BuildConfigProvider = object : BuildConfigProvider {
            override val isDebug: Boolean = BuildConfig.DEBUG
            override val applicationId: String = BuildConfig.APPLICATION_ID
            override val versionCode: Int = BuildConfig.VERSION_CODE
            override val versionName: String = BuildConfig.VERSION_NAME
            override val absoluteMinFwVersion: String = BuildConfig.ABS_MIN_FW_VERSION
            override val minFwVersion: String = BuildConfig.MIN_FW_VERSION
        }
    }
}

