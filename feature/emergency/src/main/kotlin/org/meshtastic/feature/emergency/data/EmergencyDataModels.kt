

package org.meshtastic.feature.emergency.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EmergencyGuideData(
    @SerialName("app_metadata") val appMetadata: AppMetadata,
    @SerialName("first_aid") val firstAid: Map<String, FirstAidTopic>,
    @SerialName("disaster_survival") val disasterSurvival: Map<String, DisasterSurvivalTopic>,
    @SerialName("basic_survival") val basicSurvival: Map<String, BasicSurvivalTopic>,
    @SerialName("emergency_contacts") val emergencyContacts: EmergencyContactsData? = null,
    @SerialName("legal_disclaimers") val legalDisclaimers: LegalDisclaimers,
)

@Serializable
data class AppMetadata(
    val title: String = "",
    val version: String = "",
    @SerialName("offline_capable") val offlineCapable: Boolean = true,
    val sources: List<String> = emptyList(),
    val disclaimer: String = "",
    @SerialName("target_audience") val targetAudience: String = "",
    val language: String = "",
    @SerialName("last_updated") val lastUpdated: String = "",
)

@Serializable
data class ActionStep(
    val order: Int,
    val action: String,
    val details: String = "",
)

@Serializable
data class FirstAidTopic(
    val category: String = "",
    val recognition: List<String> = emptyList(),
    val steps: List<ActionStep> = emptyList(),
    @SerialName("do") val doList: List<String> = emptyList(),
    @SerialName("do_not") val doNotList: List<String> = emptyList(),
    @SerialName("danger_signs_escalate") val dangerSignsEscalate: List<String> = emptyList(),
    @SerialName("escalation_action") val escalationAction: String = "",
    val source: String = "",
)

@Serializable
data class DisasterSurvivalTopic(
    @SerialName("hazard_type") val hazardType: String = "",
    val before: List<String> = emptyList(),
    val during: List<String> = emptyList(),
    val after: List<String> = emptyList(),
    @SerialName("do") val doList: List<String> = emptyList(),
    @SerialName("do_not") val doNotList: List<String> = emptyList(),
    val source: String = "",
)

@Serializable
data class BasicSurvivalTopic(
    val purpose: String = "",
    val steps: List<ActionStep> = emptyList(),
    @SerialName("do") val doList: List<String> = emptyList(),
    @SerialName("do_not") val doNotList: List<String> = emptyList(),
    val source: String = "",
)

@Serializable
data class EmergencyContactsData(
    @SerialName("primary_emergency") val primaryEmergency: EmergencyContactSection? = null,
    @SerialName("disaster_management") val disasterManagement: EmergencyContactSection? = null,
    @SerialName("regional_response") val regionalResponse: EmergencyContactSection? = null,
)

@Serializable
data class EmergencyContactSection(
    val category: String = "",
    val contacts: List<EmergencyContact> = emptyList(),
)

@Serializable
data class EmergencyContact(
    val service: String = "",
    val number: String? = null,
    val numbers: List<String>? = null,
)

@Serializable
data class LegalDisclaimers(
    @SerialName("general_disclaimer") val generalDisclaimer: String = "",
    @SerialName("offline_limitation") val offlineLimitation: String = "",
    @SerialName("training_recommendation") val trainingRecommendation: String = "",
    @SerialName("liability_protection") val liabilityProtection: String = "",
    @SerialName("escalation_emphasis") val escalationEmphasis: String = "",
    @SerialName("medical_limitation") val medicalLimitation: String = "",
    @SerialName("sources_transparency") val sourcesTransparency: String = "",
    @SerialName("version_note") val versionNote: String = "",
    @SerialName("accessibility_note") val accessibilityNote: String = "",
)

