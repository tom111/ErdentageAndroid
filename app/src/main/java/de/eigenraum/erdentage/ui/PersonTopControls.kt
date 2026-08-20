// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.eigenraum.erdentage.R
import de.eigenraum.erdentage.app.PersonPageUiState
import de.eigenraum.erdentage.domain.PersonName
import de.eigenraum.erdentage.ui.icons.ErdentageIcons
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PersonTopControls(
    state: PersonPageUiState,
    onManagePeople: () -> Unit,
    onEditPerson: () -> Unit,
    onShare: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val shareText = currentShareText(state)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onManagePeople,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = ErdentageIcons.Group,
                contentDescription = stringResource(R.string.manage_people),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(modifier = Modifier.weight(1f))

        IconButton(
            onClick = { onShare(shareText) },
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = stringResource(R.string.share_days),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = onEditPerson,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(R.string.edit_person),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun currentShareText(state: PersonPageUiState): String {
    val count = NumberFormat.getIntegerInstance(Locale.GERMANY).format(state.snapshot.days)
    val milestone = state.todayKinds.isNotEmpty()
    return if (PersonName.isOwner(state.person.name)) {
        stringResource(
            if (milestone) R.string.share_owner_milestone else R.string.share_owner_regular,
            count,
        )
    } else {
        stringResource(
            if (milestone) R.string.share_named_milestone else R.string.share_named_regular,
            state.person.displayName,
            count,
        )
    }
}
