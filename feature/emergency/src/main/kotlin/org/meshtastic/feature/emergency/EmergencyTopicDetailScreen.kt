

package org.meshtastic.feature.emergency

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.emergency_after
import org.meshtastic.core.strings.emergency_before
import org.meshtastic.core.strings.emergency_during
import org.meshtastic.core.strings.emergency_hazard_type
import org.meshtastic.core.strings.emergency_loading_error
import org.meshtastic.core.strings.emergency_purpose
import org.meshtastic.core.strings.emergency_source
import org.meshtastic.core.strings.emergency_steps
import org.meshtastic.feature.emergency.component.DangerWarningBanner
import org.meshtastic.feature.emergency.component.DoDoNotSection
import org.meshtastic.feature.emergency.component.RecognitionSection
import org.meshtastic.feature.emergency.component.StepCard

private val DetailDarkScheme = darkColorScheme(
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onBackground = Color.White,
    onSurface = Color.White,
    primary = Color(0xFF90CAF9),
    onPrimary = Color.Black,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyTopicDetailScreen(
    section: String,
    topicId: String,
    onNavigateUp: () -> Unit,
    viewModel: EmergencyViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val title = formatTopicTitle(topicId)

    MaterialTheme(colorScheme = DetailDarkScheme) {
        Scaffold(
            containerColor = Color(0xFF121212),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF121212),
                    ),
                )
            },
        ) { innerPadding ->
            if (uiState !is EmergencyUiState.Success) {
                Text(
                    text = stringResource(Res.string.emergency_loading_error),
                    color = Color(0xFFE53935),
                    modifier = Modifier.padding(innerPadding).padding(16.dp),
                )
                return@Scaffold
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                when (section) {
                    "first_aid" -> FirstAidDetail(viewModel, topicId)
                    "disaster_survival" -> DisasterSurvivalDetail(viewModel, topicId)
                    "basic_survival" -> BasicSurvivalDetail(viewModel, topicId)
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun FirstAidDetail(viewModel: EmergencyViewModel, topicId: String) {
    val topic = viewModel.getFirstAidTopic(topicId) ?: return

    RecognitionSection(signs = topic.recognition)
    Spacer(modifier = Modifier.height(16.dp))

    DetailSectionTitle(stringResource(Res.string.emergency_steps))
    topic.steps.forEach { step ->
        StepCard(step = step, modifier = Modifier.padding(vertical = 4.dp))
    }
    Spacer(modifier = Modifier.height(16.dp))

    DoDoNotSection(doList = topic.doList, doNotList = topic.doNotList)
    Spacer(modifier = Modifier.height(16.dp))

    DangerWarningBanner(
        dangerSigns = topic.dangerSignsEscalate,
        escalationAction = topic.escalationAction,
    )

    if (topic.source.isNotBlank()) {
        Spacer(modifier = Modifier.height(16.dp))
        SourceText(topic.source)
    }
}

@Composable
private fun DisasterSurvivalDetail(viewModel: EmergencyViewModel, topicId: String) {
    val topic = viewModel.getDisasterSurvivalTopic(topicId) ?: return

    Text(
        text = "${stringResource(Res.string.emergency_hazard_type)}: ${topic.hazardType}",
        color = Color(0xFF90CAF9),
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
    )
    Spacer(modifier = Modifier.height(16.dp))

    if (topic.before.isNotEmpty()) {
        PhaseSection(
            title = stringResource(Res.string.emergency_before),
            items = topic.before,
            color = Color(0xFF66BB6A),
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    if (topic.during.isNotEmpty()) {
        PhaseSection(
            title = stringResource(Res.string.emergency_during),
            items = topic.during,
            color = Color(0xFFFF9800),
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    if (topic.after.isNotEmpty()) {
        PhaseSection(
            title = stringResource(Res.string.emergency_after),
            items = topic.after,
            color = Color(0xFF42A5F5),
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    DoDoNotSection(doList = topic.doList, doNotList = topic.doNotList)

    if (topic.source.isNotBlank()) {
        Spacer(modifier = Modifier.height(16.dp))
        SourceText(topic.source)
    }
}

@Composable
private fun BasicSurvivalDetail(viewModel: EmergencyViewModel, topicId: String) {
    val topic = viewModel.getBasicSurvivalTopic(topicId) ?: return

    if (topic.purpose.isNotBlank()) {
        Text(
            text = "${stringResource(Res.string.emergency_purpose)}: ${topic.purpose}",
            color = Color(0xFFB0B0B0),
            fontSize = 15.sp,
        )
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (topic.steps.isNotEmpty()) {
        DetailSectionTitle(stringResource(Res.string.emergency_steps))
        topic.steps.forEach { step ->
            StepCard(step = step, modifier = Modifier.padding(vertical = 4.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
    }

    DoDoNotSection(doList = topic.doList, doNotList = topic.doNotList)

    if (topic.source.isNotBlank()) {
        Spacer(modifier = Modifier.height(16.dp))
        SourceText(topic.source)
    }
}

@Composable
private fun DetailSectionTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun PhaseSection(title: String, items: List<String>, color: Color) {
    Text(
        text = title,
        color = color,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    items.forEach { item ->
        Text(
            text = "\u2022 $item",
            color = Color(0xFFE0E0E0),
            fontSize = 15.sp,
            modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp),
        )
    }
}

@Composable
private fun SourceText(source: String) {
    Text(
        text = "${stringResource(Res.string.emergency_source)}: $source",
        color = Color(0xFF757575),
        fontSize = 12.sp,
    )
}

private fun formatTopicTitle(topicId: String): String =
    topicId.replace("_", " ")
        .split(" ")
        .joinToString(" ") { word ->
            word.replaceFirstChar { it.uppercase() }
        }

