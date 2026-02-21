

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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
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
import org.meshtastic.core.strings.emergency_do
import org.meshtastic.core.strings.emergency_do_not

private val DoGreen = Color(0xFF4CAF50)
private val DoNotRed = Color(0xFFE53935)

@Composable
fun DoDoNotSection(
    doList: List<String>,
    doNotList: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (doList.isNotEmpty()) {
            SectionHeader(
                title = stringResource(Res.string.emergency_do),
                color = DoGreen,
            )
            doList.forEach { item ->
                BulletItem(text = item, icon = Icons.Rounded.Check, color = DoGreen)
            }
        }
        if (doNotList.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            SectionHeader(
                title = stringResource(Res.string.emergency_do_not),
                color = DoNotRed,
            )
            doNotList.forEach { item ->
                BulletItem(text = item, icon = Icons.Rounded.Close, color = DoNotRed)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Text(
        text = title,
        color = color,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun BulletItem(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                color = Color(0xFFE0E0E0),
                fontSize = 15.sp,
            )
        }
    }
}

