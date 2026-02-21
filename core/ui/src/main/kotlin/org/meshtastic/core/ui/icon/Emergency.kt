

package org.meshtastic.core.ui.icon

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val MeshtasticIcons.Emergency: ImageVector
    get() {
        if (emergency != null) {
            return emergency!!
        }
        emergency =
            ImageVector.Builder(
                name = "Emergency",
                defaultWidth = 24.dp,
                defaultHeight = 24.dp,
                viewportWidth = 960f,
                viewportHeight = 960f,
            )
                .apply {
                    path(fill = SolidColor(Color(0xFFE3E3E3))) {
                        
                        moveTo(480f, 880f)
                        quadToRelative(-7f, 0f, -13f, -1f)
                        reflectiveQuadToRelative(-12f, -3f)
                        quadToRelative(-135f, -45f, -215f, -166.5f)
                        reflectiveQuadTo(160f, 444f)
                        verticalLineToRelative(-204f)
                        quadToRelative(0f, -26f, 17f, -45.5f)
                        reflectiveQuadToRelative(43f, -27.5f)
                        lineToRelative(240f, -80f)
                        quadToRelative(10f, -4f, 20f, -4f)
                        reflectiveQuadToRelative(20f, 4f)
                        lineToRelative(240f, 80f)
                        quadToRelative(26f, 8f, 43f, 27.5f)
                        reflectiveQuadToRelative(17f, 45.5f)
                        verticalLineToRelative(204f)
                        quadToRelative(0f, 143f, -80f, 264.5f)
                        reflectiveQuadTo(505f, 876f)
                        quadToRelative(-6f, 2f, -12f, 3f)
                        reflectiveQuadToRelative(-13f, 1f)
                        close()
                        
                        moveTo(420f, 560f)
                        horizontalLineToRelative(120f)
                        verticalLineToRelative(-80f)
                        horizontalLineToRelative(80f)
                        verticalLineToRelative(-120f)
                        horizontalLineToRelative(-80f)
                        verticalLineToRelative(-80f)
                        horizontalLineToRelative(-120f)
                        verticalLineToRelative(80f)
                        horizontalLineToRelative(-80f)
                        verticalLineToRelative(120f)
                        horizontalLineToRelative(80f)
                        close()
                    }
                }
                .build()
        return emergency!!
    }

private var emergency: ImageVector? = null

