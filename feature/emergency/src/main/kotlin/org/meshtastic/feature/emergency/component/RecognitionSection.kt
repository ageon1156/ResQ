

package org.meshtastic.feature.emergency.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.meshtastic.core.strings.Res
import org.meshtastic.core.strings.emergency_does_this_match
import org.meshtastic.core.strings.emergency_recognition

private val RecognitionBlue = Color(0xFF42A5F5)

@Composable
fun RecognitionSection(
    signs: List<String>,
    modifier: Modifier = Modifier,
) {
    if (signs.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = RecognitionBlue.copy(alpha = 0.1f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Visibility,
                    contentDescription = null,
                    tint = RecognitionBlue,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(Res.string.emergency_recognition),
                    color = RecognitionBlue,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(Res.string.emergency_does_this_match),
                color = Color(0xFF90CAF9),
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            signs.forEach { sign ->
                Text(
                    text = "\u2022 $sign",
                    color = Color(0xFFE0E0E0),
                    fontSize = 15.sp,
                    modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp),
                )
            }
        }
    }
}

