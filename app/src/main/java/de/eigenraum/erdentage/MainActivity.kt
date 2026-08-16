// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.eigenraum.erdentage.app.ErdentageViewModel
import de.eigenraum.erdentage.platform.launchTextShare
import de.eigenraum.erdentage.platform.withGermanLocale
import de.eigenraum.erdentage.ui.ErdentageApp
import de.eigenraum.erdentage.ui.theme.ErdentageTheme

class MainActivity : ComponentActivity() {
    private val viewModel: ErdentageViewModel by viewModels {
        ErdentageViewModel.Factory(
            (application as ErdentageApplication).container.peopleRepository,
        )
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(newBase.withGermanLocale())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
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
                        onShare = { text ->
                            launchTextShare(
                                context = this@MainActivity,
                                text = text,
                                chooserTitle = getString(R.string.share_chooser_title),
                            )
                        },
                    )
                }
            }
        }
    }
}
