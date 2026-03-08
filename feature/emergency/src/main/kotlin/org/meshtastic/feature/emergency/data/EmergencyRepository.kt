

package org.meshtastic.feature.emergency.data

import kotlinx.coroutines.withContext
import org.meshtastic.core.di.CoroutineDispatchers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmergencyRepository @Inject constructor(
    private val jsonDataSource: EmergencyJsonDataSource,
    private val dispatchers: CoroutineDispatchers,
) {
    @Volatile
    private var cachedData: EmergencyGuideData? = null

    suspend fun getGuideData(): Result<EmergencyGuideData> = withContext(dispatchers.io) {
        cachedData?.let { return@withContext Result.success(it) }
        runCatching {
            jsonDataSource.loadEmergencyGuideData().also { cachedData = it }
        }.onFailure {
        }
    }

    fun getFirstAidTopic(id: String): FirstAidTopic? =
        cachedData?.firstAid?.get(id)

    fun getDisasterSurvivalTopic(id: String): DisasterSurvivalTopic? =
        cachedData?.disasterSurvival?.get(id)

    fun getBasicSurvivalTopic(id: String): BasicSurvivalTopic? =
        cachedData?.basicSurvival?.get(id)
}

