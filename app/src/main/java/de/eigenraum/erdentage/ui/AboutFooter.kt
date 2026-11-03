// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.ui

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import de.eigenraum.erdentage.R

/**
 * The „Über“ footer: project line, version, mail and Mastodon. It lives in
 * the people list, the closest thing the app has to a root screen, rather
 * than in the person editor, where everything else is a property of one
 * person (iOS 1.4.1).
 */
@Composable
fun AboutFooter(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val packageInfo = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val versionCode = if (Build.VERSION.SDK_INT >= 28) {
        packageInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
    }
    val linkColor = MaterialTheme.colorScheme.primary
    val feedback = buildAnnotatedString {
        append(stringResource(R.string.settings_feedback_prefix))
        append(' ')
        withLink(
            LinkAnnotation.Url(
                url = FeedbackMailUri,
                linkInteractionListener = { uriHandler.openUri(FeedbackMailUri) },
            ),
        ) {
            pushStyle(SpanStyle(color = linkColor))
            append(stringResource(R.string.settings_feedback_mail))
            pop()
        }
        append(' ')
        append(stringResource(R.string.settings_feedback_or))
        append(' ')
        withLink(
            LinkAnnotation.Url(
                url = MastodonUri,
                linkInteractionListener = { uriHandler.openUri(MastodonUri) },
            ),
        ) {
            pushStyle(SpanStyle(color = linkColor))
            append(stringResource(R.string.settings_feedback_mastodon))
            pop()
        }
    }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(
                R.string.settings_about,
                packageInfo.versionName ?: "?",
                versionCode,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            text = feedback,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

private const val FeedbackMailUri =
    "mailto:thomas.kahle@jpberlin.de?subject=Feedback%20Erdentage"
private const val MastodonUri = "https://mathe.social/@tomkalei"
