

package org.meshtastic.feature.emergency

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flood
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Masks
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Tsunami
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.emergency_basic_survival
import org.meshtastic.core.strings.emergency_disaster_survival
import org.meshtastic.core.strings.emergency_first_aid
import org.meshtastic.core.strings.emergency_help
import org.meshtastic.core.strings.emergency_loading_error
import org.meshtastic.feature.emergency.component.EmergencyTopicCard
import org.meshtastic.feature.emergency.data.EmergencyGuideData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyHelpScreen(
    onNavigateToTopic: (section: String, topicId: String) -> Unit,
    viewModel: EmergencyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val disclaimerAccepted by viewModel.disclaimerAccepted.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = {
                        Icon(
                            imageVector = ImageVector.vectorResource(id = org.meshtastic.core.ui.R.drawable.ic_meshtastic),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 16.dp).size(28.dp),
                        )
                    },
                    title = {
                        Text(
                            text = stringResource(Res.string.emergency_help),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 1.sp),
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
                HorizontalDivider(thickness = 1.dp, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            }
        },
    ) { innerPadding ->
            when (val state = uiState) {
                is EmergencyUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                is EmergencyUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.emergency_loading_error),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                is EmergencyUiState.Success -> {
                    if (!disclaimerAccepted) {
                        EmergencyDisclaimerDialog(
                            disclaimerText = state.data.appMetadata.disclaimer,
                            onAccept = viewModel::acceptDisclaimer,
                        )
                    }
                    EmergencyContent(
                        data = state.data,
                        onNavigateToTopic = onNavigateToTopic,
                        contentPadding = innerPadding,
                    )
                }
            }
        }
}

@Composable
private fun EmergencyContent(
    data: EmergencyGuideData,
    onNavigateToTopic: (String, String) -> Unit,
    contentPadding: PaddingValues,
) {
    val firstAidEntries = data.firstAid.entries.toList()
        .sortedBy { if (it.value.category == "Life-Threatening") 0 else 1 }
    val disasterEntries = data.disasterSurvival.entries.toList()
    val survivalEntries = data.basicSurvival.entries.toList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
    ) {
        
        item {
            SectionHeader(title = stringResource(Res.string.emergency_first_aid))
        }
        items(firstAidEntries, key = { "fa_${it.key}" }) { (topicId, topic) ->
            EmergencyTopicCard(
                title = formatTopicTitle(topicId),
                icon = getFirstAidIcon(topicId),
                categoryBadge = topic.category,
                onClick = { onNavigateToTopic("first_aid", topicId) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(title = stringResource(Res.string.emergency_disaster_survival))
        }
        items(disasterEntries, key = { "ds_${it.key}" }) { (topicId, _) ->
            EmergencyTopicCard(
                title = formatTopicTitle(topicId),
                icon = getDisasterIcon(topicId),
                categoryBadge = null,
                onClick = { onNavigateToTopic("disaster_survival", topicId) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionHeader(title = stringResource(Res.string.emergency_basic_survival))
        }
        items(survivalEntries, key = { "bs_${it.key}" }) { (topicId, _) ->
            EmergencyTopicCard(
                title = formatTopicTitle(topicId),
                icon = getSurvivalIcon(topicId),
                categoryBadge = null,
                onClick = { onNavigateToTopic("basic_survival", topicId) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }

        data.emergencyContacts?.let { contacts ->
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(title = "EMERGENCY CONTACTS")
            }

            contacts.primaryEmergency?.let { section ->
                item {
                    EmergencyContactsCard(section = section)
                }
            }

            contacts.disasterManagement?.let { section ->
                item {
                    EmergencyContactsCard(section = section)
                }
            }

            contacts.regionalResponse?.let { section ->
                item {
                    EmergencyContactsCard(section = section)
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

private fun formatTopicTitle(topicId: String): String =
    topicId.replace("_", " ")
        .split(" ")
        .joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }

private fun getFirstAidIcon(topicId: String): ImageVector = when (topicId) {
    "severe_bleeding" -> Icons.Rounded.LocalHospital
    "unconscious_person" -> Icons.Rounded.Psychology
    "cpr_adult" -> Icons.Rounded.VolunteerActivism
    "choking" -> Icons.Rounded.Warning
    "shock" -> Icons.Rounded.Warning
    "burns" -> Icons.Rounded.LocalFireDepartment
    "fractures" -> Icons.Rounded.LocalHospital
    "head_injury" -> Icons.Rounded.Psychology
    "snake_bite" -> Icons.Rounded.Warning
    "heat_stroke" -> Icons.Rounded.LocalFireDepartment
    "hypothermia" -> Icons.Rounded.Opacity
    "drowning" -> Icons.Rounded.WaterDrop
    else -> Icons.Rounded.LocalHospital
}

private fun getDisasterIcon(topicId: String): ImageVector = when (topicId) {
    "earthquake" -> Icons.Rounded.Warning
    "flood" -> Icons.Rounded.Flood
    "fire" -> Icons.Rounded.LocalFireDepartment
    "cyclone_severe_storm" -> Icons.Rounded.Tsunami
    "landslide" -> Icons.Rounded.Warning
    else -> Icons.Rounded.Warning
}

private fun getSurvivalIcon(topicId: String): ImageVector = when (topicId) {
    "temporary_shelter" -> Icons.Rounded.Forest
    "safe_water" -> Icons.Rounded.WaterDrop
    "food_safety" -> Icons.Rounded.Forest
    "hygiene_sanitation" -> Icons.Rounded.Masks
    else -> Icons.Rounded.Forest
}

@Composable
private fun EmergencyContactsCard(
    section: org.meshtastic.feature.emergency.data.EmergencyContactSection,
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = section.category.uppercase(),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        section.contacts.forEach { contact ->
            androidx.compose.foundation.layout.Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contact.service,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f)
                )

                if (contact.number != null) {
                    Text(
                        text = contact.number,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                } else if (contact.numbers != null) {
                    androidx.compose.foundation.layout.Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        contact.numbers.forEach { number ->
                            Text(
                                text = number,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

