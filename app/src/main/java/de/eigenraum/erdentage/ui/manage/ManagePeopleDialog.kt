// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.ui.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import de.eigenraum.erdentage.R
import de.eigenraum.erdentage.domain.ErdentageLimits
import de.eigenraum.erdentage.domain.Person
import de.eigenraum.erdentage.ui.icons.ErdentageIcons
import de.eigenraum.erdentage.ui.theme.LocalDarkTheme
import de.eigenraum.erdentage.ui.theme.surfaceColor
import java.time.DateTimeException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagePeopleDialog(
    people: List<Person>,
    selectedPersonId: String?,
    onDismiss: () -> Unit,
    onAddManual: () -> Unit,
    onImportContact: () -> Unit,
    onEdit: (Person) -> Unit,
    onSelect: (Int) -> Unit,
    onDelete: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<Person?>(null) }
    var addMenuExpanded by remember { mutableStateOf(false) }

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
                        title = { Text(stringResource(R.string.people_title)) },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.close),
                                )
                            }
                        },
                        actions = {
                            Box {
                                IconButton(
                                    onClick = { addMenuExpanded = true },
                                    enabled = people.size < ErdentageLimits.MAX_PEOPLE,
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = stringResource(R.string.add_person),
                                    )
                                }
                                DropdownMenu(
                                    expanded = addMenuExpanded,
                                    onDismissRequest = { addMenuExpanded = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.new_person)) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Add, contentDescription = null)
                                        },
                                        onClick = {
                                            addMenuExpanded = false
                                            onAddManual()
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.import_contact)) },
                                        leadingIcon = {
                                            Icon(
                                                ErdentageIcons.Group,
                                                contentDescription = null,
                                            )
                                        },
                                        onClick = {
                                            addMenuExpanded = false
                                            onImportContact()
                                        },
                                    )
                                }
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
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        itemsIndexed(
                            items = people,
                            key = { _, person -> person.id },
                        ) { index, person ->
                            PersonManagementRow(
                                person = person,
                                index = index,
                                peopleCount = people.size,
                                selected = person.id == selectedPersonId,
                                onSelect = {
                                    onSelect(index)
                                    onDismiss()
                                },
                                onEdit = { onEdit(person) },
                                onDelete = { pendingDelete = person },
                                onMove = onMove,
                            )
                            if (index < people.lastIndex) HorizontalDivider()
                        }
                        if (people.size >= ErdentageLimits.MAX_PEOPLE) {
                            item {
                                Text(
                                    text = stringResource(
                                        R.string.max_people,
                                        ErdentageLimits.MAX_PEOPLE,
                                    ),
                                    modifier = Modifier.padding(20.dp),
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

    pendingDelete?.let { person ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = {
                Text(stringResource(R.string.delete_person_title, person.displayName))
            },
            text = { Text(stringResource(R.string.delete_person_message)) },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        onDelete(person.id)
                    },
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
        )
    }
}

@Composable
private fun PersonManagementRow(
    person: Person,
    index: Int,
    peopleCount: Int,
    selected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    val darkTheme = LocalDarkTheme.current
    ListItem(
        headlineContent = { Text(person.displayName) },
        supportingContent = {
            validBirthDate(person)?.let { date ->
                Text(
                    stringResource(
                        R.string.person_birth_date,
                        date.format(ManagementDateFormatter),
                    ),
                )
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(person.resolvedTint(index).surfaceColor(darkTheme), CircleShape),
            )
        },
        trailingContent = {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = stringResource(R.string.selected_person),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                PersonRowMenu(
                    name = person.displayName,
                    canMoveUp = index > 0,
                    canMoveDown = index < peopleCount - 1,
                    onEdit = onEdit,
                    onMoveUp = { onMove(index, index - 1) },
                    onMoveDown = { onMove(index, index + 1) },
                    onDelete = onDelete,
                )
            }
        },
        modifier = Modifier.clickable(onClick = onSelect),
    )
}

@Composable
private fun PersonRowMenu(
    name: String,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                Icons.Default.MoreVert,
                contentDescription = stringResource(R.string.row_options, name),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit_person)) },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                onClick = {
                    expanded = false
                    onEdit()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.move_up)) },
                leadingIcon = {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                },
                enabled = canMoveUp,
                onClick = {
                    expanded = false
                    onMoveUp()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.move_down)) },
                leadingIcon = {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                },
                enabled = canMoveDown,
                onClick = {
                    expanded = false
                    onMoveDown()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete_person)) },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

private fun validBirthDate(person: Person): LocalDate? = try {
    LocalDate.of(person.birthYear, person.birthMonth, person.birthDay)
} catch (_: DateTimeException) {
    null
}

private val ManagementDateFormatter = DateTimeFormatter
    .ofLocalizedDate(FormatStyle.MEDIUM)
    .withLocale(Locale.GERMANY)
