// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.ui

import android.app.DatePickerDialog
import android.content.DialogInterface
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.eigenraum.erdentage.R
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun EmptyStartScreen(
    onBirthDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(R.string.empty_birth_date),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(
            onClick = {
                val today = LocalDate.now(ZoneId.systemDefault())
                val preset = today.minusDays(5_000)
                DatePickerDialog(
                    context,
                    R.style.ThemeOverlay_Erdentage_DatePicker,
                    { _, year, month, day ->
                        onBirthDateSelected(LocalDate.of(year, month + 1, day))
                    },
                    preset.year,
                    preset.monthValue - 1,
                    preset.dayOfMonth,
                ).apply {
                    datePicker.maxDate = today
                        .plusDays(1)
                        .atStartOfDay(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli() - 1
                    setOnShowListener {
                        getButton(DialogInterface.BUTTON_NEGATIVE)
                            .setText(R.string.cancel)
                        getButton(DialogInterface.BUTTON_POSITIVE)
                            .setText(R.string.apply)
                    }
                }.show()
            },
        ) {
            Text(stringResource(R.string.set_birth_date))
        }
    }
}
