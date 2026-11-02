// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import de.eigenraum.erdentage.domain.PersonTint

@Composable
fun PersonTintBackground(
    tint: PersonTint,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val background = MaterialTheme.colorScheme.background
    val surface = tint.surfaceColor(LocalDarkTheme.current)
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                0.0f to surface,
                0.5f to background,
                1.0f to background,
            ),
        ),
    ) {
        content()
    }
}
