
package com.geeksville.mesh.service

import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("LongParameterList")
@Singleton
class MeshRouter
@Inject
constructor(
    val dataHandler: MeshDataHandler,
    val configHandler: MeshConfigHandler,
    val tracerouteHandler: MeshTracerouteHandler,
    val neighborInfoHandler: MeshNeighborInfoHandler,
    val configFlowManager: MeshConfigFlowManager,
    val mqttManager: MeshMqttManager,
    val actionHandler: MeshActionHandler,
    val silentNodeDetector: SilentNodeDetector,
) {
    fun start(scope: CoroutineScope) {
        dataHandler.start(scope)
        configHandler.start(scope)
        tracerouteHandler.start(scope)
        neighborInfoHandler.start(scope)
        configFlowManager.start(scope)
        actionHandler.start(scope)
        silentNodeDetector.start(scope)
    }
}

