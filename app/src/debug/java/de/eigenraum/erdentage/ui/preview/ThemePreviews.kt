// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.eigenraum.erdentage.R
import de.eigenraum.erdentage.domain.PersonTint
import de.eigenraum.erdentage.ui.theme.ErdentageTheme
import de.eigenraum.erdentage.ui.theme.PersonTintBackground
import java.text.NumberFormat
import java.util.Locale

private const val PhoneDevice = "spec:width=393dp,height=852dp,dpi=440"

@Preview(
    name = "Person light",
    device = PhoneDevice,
    locale = "de",
    showSystemUi = true,
)
@Composable
private fun PersonLightPreview() {
    ErdentageTheme(darkTheme = false) {
        VisualFoundationPreview(tint = PersonTint.SKY)
    }
}

@Preview(
    name = "Person dark",
    device = PhoneDevice,
    locale = "de",
    showSystemUi = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun PersonDarkPreview() {
    ErdentageTheme(darkTheme = true) {
        VisualFoundationPreview(tint = PersonTint.MINT)
    }
}

@Composable
private fun VisualFoundationPreview(tint: PersonTint) {
    val person = PreviewPeople.first { it.resolvedTint(0) == tint }
    PersonTintBackground(
        tint = tint,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 24.dp, vertical = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
            )
            Text(
                text = person.displayName,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = NumberFormat.getIntegerInstance(Locale.GERMANY).format(
                    if (tint == PersonTint.SKY) 16_796 else 5_041,
                ),
                modifier = Modifier.padding(top = 40.dp),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.displayLarge,
            )
            Text(
                text = stringResource(
                    if (tint == PersonTint.SKY) {
                        R.string.preview_today_catalan
                    } else {
                        R.string.preview_today_square
                    },
                ),
                modifier = Modifier.padding(top = 2.dp),
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
