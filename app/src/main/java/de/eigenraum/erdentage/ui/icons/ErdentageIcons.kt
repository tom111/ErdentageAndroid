// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object ErdentageIcons {
    val Group: ImageVector by lazy {
        ImageVector.Builder(
            name = "Group",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(16.5f, 10.5f)
                curveTo(17.88f, 10.5f, 19f, 9.38f, 19f, 8f)
                curveTo(19f, 6.62f, 17.88f, 5.5f, 16.5f, 5.5f)
                curveTo(16.15f, 5.5f, 15.82f, 5.57f, 15.52f, 5.7f)
                curveTo(15.83f, 6.39f, 16f, 7.16f, 16f, 8f)
                curveTo(16f, 8.84f, 15.83f, 9.61f, 15.52f, 10.3f)
                curveTo(15.82f, 10.43f, 16.15f, 10.5f, 16.5f, 10.5f)
                close()

                moveTo(7.5f, 11f)
                curveTo(9.16f, 11f, 10.5f, 9.66f, 10.5f, 8f)
                curveTo(10.5f, 6.34f, 9.16f, 5f, 7.5f, 5f)
                curveTo(5.84f, 5f, 4.5f, 6.34f, 4.5f, 8f)
                curveTo(4.5f, 9.66f, 5.84f, 11f, 7.5f, 11f)
                close()

                moveTo(7.5f, 12.75f)
                curveTo(4.25f, 12.75f, 1.5f, 14.45f, 1.5f, 16.55f)
                verticalLineTo(19f)
                horizontalLineTo(13.5f)
                verticalLineTo(16.55f)
                curveTo(13.5f, 14.45f, 10.75f, 12.75f, 7.5f, 12.75f)
                close()

                moveTo(16.5f, 12.5f)
                curveTo(16f, 12.5f, 15.5f, 12.56f, 15.03f, 12.66f)
                curveTo(16.27f, 13.61f, 17f, 14.91f, 17f, 16.55f)
                verticalLineTo(19f)
                horizontalLineTo(22.5f)
                verticalLineTo(16.55f)
                curveTo(22.5f, 14.45f, 19.75f, 12.5f, 16.5f, 12.5f)
                close()
            }
        }.build()
    }

    val Remove: ImageVector by lazy {
        ImageVector.Builder(
            name = "Remove",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(fill = SolidColor(Color.Black)) {
                moveTo(19f, 13f)
                horizontalLineTo(5f)
                verticalLineTo(11f)
                horizontalLineTo(19f)
                close()
            }
        }.build()
    }
}
