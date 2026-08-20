// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage

import android.app.DatePickerDialog
import android.content.BroadcastReceiver
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import de.eigenraum.erdentage.app.ErdentageViewModel
import de.eigenraum.erdentage.platform.launchTextShare
import de.eigenraum.erdentage.platform.launchCalendarHandoff
import de.eigenraum.erdentage.platform.ContactImportLogic
import de.eigenraum.erdentage.platform.ContactPersonDraft
import de.eigenraum.erdentage.platform.SelectedContact
import de.eigenraum.erdentage.platform.SelectedContactReader
import de.eigenraum.erdentage.platform.withGermanLocale
import de.eigenraum.erdentage.ui.ErdentageApp
import de.eigenraum.erdentage.ui.theme.ErdentageTheme
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private var contactDraft by mutableStateOf<ContactPersonDraft?>(null)
    private var timeChangeReceiverRegistered = false
    private val timeChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            viewModel.refreshNow()
        }
    }

    private val contactPicker = registerForActivityResult(
        ActivityResultContracts.PickContact(),
    ) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val selected = withContext(Dispatchers.IO) {
                SelectedContactReader(contentResolver).read(uri)
            }
            val today = LocalDate.now(ZoneId.systemDefault())
            val completeDate = ContactImportLogic.completeDate(selected.birthday, today)
            if (completeDate != null) {
                contactDraft = ContactPersonDraft(selected.name, completeDate)
            } else {
                showContactBirthdayPicker(selected)
            }
        }
    }

    private val viewModel: ErdentageViewModel by viewModels {
        ErdentageViewModel.Factory(
            (application as ErdentageApplication).container.peopleRepository,
        )
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withGermanLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ErdentageTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                if (state == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                    )
                } else {
                    ErdentageApp(
                        state = requireNotNull(state),
                        onAddFirstPerson = viewModel::addFirstPerson,
                        onAddPerson = viewModel::addPerson,
                        onUpdatePerson = viewModel::updatePerson,
                        onDeletePerson = viewModel::deletePerson,
                        onMovePerson = viewModel::movePerson,
                        onPageSelected = viewModel::selectPage,
                        onCalendarHandoff = { action ->
                            if (launchCalendarHandoff(this@MainActivity, action.event)) {
                                viewModel.recordCalendarHandoff(
                                    personId = action.personId,
                                    milestoneKey = action.milestoneKey,
                                    visibleKeys = action.visibleKeys,
                                )
                            } else {
                                Toast.makeText(
                                    this@MainActivity,
                                    R.string.calendar_app_missing,
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        },
                        contactDraft = contactDraft,
                        onPickContact = ::launchContactPicker,
                        onContactDraftConsumed = { contactDraft = null },
                        onShare = { text ->
                            if (!launchTextShare(
                                    context = this@MainActivity,
                                    text = text,
                                    chooserTitle = getString(R.string.share_chooser_title),
                                )
                            ) {
                                Toast.makeText(
                                    this@MainActivity,
                                    R.string.share_app_missing,
                                    Toast.LENGTH_LONG,
                                ).show()
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onUiStarted()
        ContextCompat.registerReceiver(
            this,
            timeChangeReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_DATE_CHANGED)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        timeChangeReceiverRegistered = true
    }

    override fun onStop() {
        if (timeChangeReceiverRegistered) {
            unregisterReceiver(timeChangeReceiver)
            timeChangeReceiverRegistered = false
        }
        viewModel.onUiStopped()
        super.onStop()
    }

    private fun launchContactPicker() {
        try {
            contactPicker.launch(null)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, R.string.contacts_app_missing, Toast.LENGTH_LONG).show()
        }
    }

    private fun showContactBirthdayPicker(contact: SelectedContact) {
        val today = LocalDate.now(ZoneId.systemDefault())
        val initial = ContactImportLogic.initialDate(contact.birthday, today)
        DatePickerDialog(
            this,
            R.style.ThemeOverlay_Erdentage_DatePicker,
            { _, year, month, day ->
                contactDraft = ContactPersonDraft(
                    name = contact.name,
                    birthDate = LocalDate.of(year, month + 1, day),
                )
            },
            initial.year,
            initial.monthValue - 1,
            initial.dayOfMonth,
        ).apply {
            datePicker.maxDate = today
                .plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli() - 1
            setOnShowListener {
                getButton(DialogInterface.BUTTON_NEGATIVE).setText(R.string.cancel)
                getButton(DialogInterface.BUTTON_POSITIVE).setText(R.string.apply)
            }
        }.show()
    }
}
