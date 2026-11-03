// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.ui.together

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.eigenraum.erdentage.R
import de.eigenraum.erdentage.domain.TogetherKind
import de.eigenraum.erdentage.ui.icons.ErdentageIcons

/**
 * Family-level settings for the „Zusammen“ page: how many rows each
 * Wechselwirkungs-Kategorie contributes. Deliberately not per person, and a
 * count instead of a toggle plus a count: zero means the category is off.
 * Changes apply immediately; there is no save button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TogetherSettingsDialog(
    limits: Map<TogetherKind, Int>,
    onLimitChange: (TogetherKind, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Scaffold(
                modifier = Modifier.safeDrawingPadding(),
                topBar = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.together_settings_title)) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.close),
                                )
                            }
                        },
                    )
                },
            ) { contentPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .widthIn(max = 720.dp)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(start = 20.dp, top = 8.dp, end = 20.dp, bottom = 40.dp),
                    ) {
                        item {
                            Text(
                                text = stringResource(R.string.together_limits_section),
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleSmall,
                            )
                        }
                        TogetherKind.entries.sortedBy { it.rarityRank }.forEach { kind ->
                            item(key = kind.name) {
                                LimitRow(
                                    kind = kind,
                                    value = limits[kind] ?: kind.defaultLimit,
                                    onValueChange = { onLimitChange(kind, it) },
                                )
                            }
                        }
                        item {
                            Text(
                                text = stringResource(R.string.together_limits_footer),
                                modifier = Modifier.padding(top = 8.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One category: the count with its stepper and the explanation of what the category is. */
@Composable
private fun LimitRow(
    kind: TogetherKind,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    val resources = LocalResources.current
    val label = stringResource(kind.labelResource)
    val explanation = stringResource(kind.explanationResource)
    val spokenValue = when (value) {
        0 -> stringResource(R.string.together_limit_off)
        else -> pluralStringResource(R.plurals.together_limit_rows, value, value)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics {
                // The explanation is part of the label, not dropped: it is
                // the one sentence that says what the category is.
                contentDescription = "$label. $explanation"
                stateDescription = spokenValue
            },
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = if (value == 0) stringResource(R.string.together_limit_off) else value.toString(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
                fontFamily = FontFamily.Monospace,
            )
            IconButton(
                onClick = { onValueChange(value - 1) },
                enabled = value > TogetherKind.LIMIT_RANGE.first,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = ErdentageIcons.Remove,
                    contentDescription = resources.getString(R.string.together_fewer_rows, label),
                )
            }
            IconButton(
                onClick = { onValueChange(value + 1) },
                enabled = value < TogetherKind.LIMIT_RANGE.last,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = resources.getString(R.string.together_more_rows, label),
                )
            }
        }
        Text(
            text = explanation,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
