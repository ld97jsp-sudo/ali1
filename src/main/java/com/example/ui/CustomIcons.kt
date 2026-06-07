package com.example.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object CustomIcons {
    val Telegram: ImageVector by lazy {
        ImageVector.Builder(
            name = "Telegram",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF229ED9))) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                reflectiveCurveTo(6.48f, 22f, 12f, 22f)
                reflectiveCurveTo(22f, 17.52f, 22f, 12f)
                reflectiveCurveTo(17.52f, 2f, 12f, 2f)
                close()
                moveTo(16.64f, 8.8f)
                curveTo(16.49f, 10.38f, 15.84f, 14.22f, 15.51f, 15.99f)
                curveTo(15.37f, 16.74f, 15.09f, 16.99f, 14.83f, 17.02f)
                curveTo(14.25f, 17.07f, 13.81f, 16.64f, 13.25f, 16.27f)
                curveTo(12.37f, 15.69f, 11.87f, 15.33f, 11.02f, 14.77f)
                curveTo(10.03f, 14.12f, 10.67f, 13.76f, 11.24f, 13.18f)
                curveTo(11.39f, 13.03f, 13.95f, 10.7f, 14f, 10.49f)
                curveTo(14.01f, 10.46f, 14.01f, 10.35f, 13.93f, 10.29f)
                curveTo(13.85f, 10.23f, 13.74f, 10.25f, 13.66f, 10.27f)
                curveTo(13.54f, 10.29f, 11.7f, 11.51f, 8.12f, 13.95f)
                curveTo(7.6f, 14.31f, 7.12f, 14.48f, 6.7f, 14.47f)
                curveTo(6.23f, 14.46f, 5.33f, 14.21f, 4.67f, 13.99f)
                curveTo(3.85f, 13.72f, 3.2f, 13.58f, 3.25f, 13.12f)
                curveTo(3.28f, 12.88f, 3.62f, 12.63f, 4.27f, 12.38f)
                curveTo(8.26f, 10.65f, 10.93f, 9.5f, 12.28f, 8.95f)
                curveTo(16.09f, 7.39f, 16.88f, 7.12f, 17.4f, 7.11f)
                curveTo(17.51f, 7.11f, 17.77f, 7.14f, 17.94f, 7.28f)
                curveTo(18.08f, 7.4f, 18.12f, 7.56f, 18.14f, 7.68f)
                curveTo(18.12f, 7.75f, 18.12f, 7.89f, 18.11f, 8f)
                close()
            }
        }.build()
    }
}
