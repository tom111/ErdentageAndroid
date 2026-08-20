// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: GPL-3.0-or-later

package de.eigenraum.erdentage.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider as DayNightColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import de.eigenraum.erdentage.ErdentageApplication
import de.eigenraum.erdentage.MainActivity
import de.eigenraum.erdentage.R
import de.eigenraum.erdentage.domain.DayCountSnapshot
import de.eigenraum.erdentage.domain.MilestoneCandidate
import de.eigenraum.erdentage.domain.MilestoneKind
import de.eigenraum.erdentage.domain.PersonTint
import de.eigenraum.erdentage.ui.theme.InkSlateDark
import de.eigenraum.erdentage.ui.theme.InkSlateLight
import de.eigenraum.erdentage.ui.theme.surfaceColor
import de.eigenraum.erdentage.ui.person.captionResource
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.first

class ErdentageGlanceWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(120.dp, 120.dp),
            DpSize(280.dp, 120.dp),
            DpSize(280.dp, 280.dp),
        ),
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val state = (context.applicationContext as ErdentageApplication)
            .container.peopleRepository.state.first()
        val snapshot = WidgetSnapshotFactory.create(
            state = state,
            selection = WidgetSelectionStore(context).read(appWidgetId),
            at = Instant.now(),
            zoneId = ZoneId.systemDefault(),
        )
        val strings = WidgetStrings(
            erdentage = context.getString(R.string.app_name),
            empty = context.getString(R.string.widget_empty),
            captions = MilestoneKind.entries.associateWith { kind ->
                context.getString(kind.captionResource)
            },
        )
        provideContent {
            ErdentageWidgetContent(snapshot, strings)
        }
    }
}

class ErdentageWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ErdentageGlanceWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        WidgetSelectionStore(context).delete(appWidgetIds)
        super.onDeleted(context, appWidgetIds)
    }

    override fun onDisabled(context: Context) {
        WidgetUpdateScheduler.cancel(context)
        super.onDisabled(context)
    }
}

@Composable
private fun ErdentageWidgetContent(
    snapshot: DayCountSnapshot,
    strings: WidgetStrings,
) {
    val size = LocalSize.current
    val tint = snapshot.tint ?: PersonTint.SAND
    val background = if (snapshot.hasBirthDate) {
        DayNightColorProvider(tint.surfaceColor(false), tint.surfaceColor(true))
    } else {
        DayNightColorProvider(Color(0xFFF2F2F2), Color(0xFF24262D))
    }
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(background)
            .clickable(actionStartActivity<MainActivity>())
            .padding(14.dp),
        contentAlignment = Alignment.Center,
    ) {
        when {
            !snapshot.hasBirthDate -> EmptyWidget(strings)
            size.width < 220.dp -> SmallWidget(snapshot, strings)
            size.height < 220.dp -> MediumWidget(snapshot, strings)
            else -> LargeWidget(snapshot, strings)
        }
    }
}

@Composable
private fun SmallWidget(snapshot: DayCountSnapshot, strings: WidgetStrings) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = snapshot.personName,
            style = widgetTextStyle(12.sp, FontWeight.Normal),
            maxLines = 1,
        )
        Text(
            text = grouped(snapshot.days),
            style = widgetTextStyle(38.sp, FontWeight.Bold),
            maxLines = 1,
        )
        Text(
            text = strings.erdentage,
            style = widgetTextStyle(12.sp, FontWeight.Normal),
            maxLines = 1,
        )
    }
}

@Composable
private fun MediumWidget(snapshot: DayCountSnapshot, strings: WidgetStrings) {
    Row(modifier = GlanceModifier.fillMaxSize()) {
        CountBlock(snapshot, strings, modifier = GlanceModifier.defaultWeight().fillMaxHeight())
        if (snapshot.milestones.isNotEmpty()) {
            Spacer(GlanceModifier.width(12.dp))
            Column(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                verticalAlignment = Alignment.Vertical.CenterVertically,
            ) {
                snapshot.milestones.take(2).forEach { milestone ->
                    MilestoneBlock(milestone, strings)
                    Spacer(GlanceModifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun LargeWidget(snapshot: DayCountSnapshot, strings: WidgetStrings) {
    Column(modifier = GlanceModifier.fillMaxSize()) {
        CountBlock(snapshot, strings, modifier = GlanceModifier.fillMaxWidth())
        Spacer(GlanceModifier.height(14.dp))
        val milestones = snapshot.milestones.take(6)
        milestones.chunked(2).forEach { row ->
            Row(modifier = GlanceModifier.fillMaxWidth()) {
                MilestoneBlock(row[0], strings, modifier = GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(12.dp))
                if (row.size > 1) {
                    MilestoneBlock(row[1], strings, modifier = GlanceModifier.defaultWeight())
                } else {
                    Spacer(GlanceModifier.defaultWeight())
                }
            }
            Spacer(GlanceModifier.height(10.dp))
        }
    }
}

@Composable
private fun CountBlock(
    snapshot: DayCountSnapshot,
    strings: WidgetStrings,
    modifier: GlanceModifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = snapshot.personName,
            style = widgetTextStyle(12.sp, FontWeight.Normal),
            maxLines = 1,
        )
        Text(
            text = grouped(snapshot.days),
            style = widgetTextStyle(42.sp, FontWeight.Bold),
            maxLines = 1,
        )
        Text(
            text = strings.erdentage,
            style = widgetTextStyle(12.sp, FontWeight.Normal),
            maxLines = 1,
        )
    }
}

@Composable
private fun MilestoneBlock(
    milestone: MilestoneCandidate,
    strings: WidgetStrings,
    modifier: GlanceModifier = GlanceModifier,
) {
    Column(modifier = modifier) {
        milestone.kinds.forEach { kind ->
            Text(
                text = strings.captions.getValue(kind),
                style = widgetTextStyle(10.sp, FontWeight.Normal, TextAlign.Start),
                maxLines = 1,
            )
        }
        Text(
            text = grouped(milestone.target),
            style = widgetTextStyle(16.sp, FontWeight.Bold, TextAlign.Start),
            maxLines = 1,
        )
        Text(
            text = milestone.date.format(WidgetDateFormatter),
            style = widgetTextStyle(11.sp, FontWeight.Normal, TextAlign.Start),
            maxLines = 1,
        )
    }
}

@Composable
private fun EmptyWidget(strings: WidgetStrings) {
    Column(
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = strings.empty,
            style = widgetTextStyle(14.sp, FontWeight.Bold),
            maxLines = 2,
        )
    }
}

private fun widgetTextStyle(
    size: androidx.compose.ui.unit.TextUnit,
    weight: FontWeight,
    align: TextAlign = TextAlign.Center,
): TextStyle = TextStyle(
    color = DayNightColorProvider(InkSlateLight, InkSlateDark),
    fontSize = size,
    fontWeight = weight,
    textAlign = align,
)

private fun grouped(value: Int): String =
    NumberFormat.getIntegerInstance(Locale.GERMANY).format(value)

private val WidgetDateFormatter = DateTimeFormatter
    .ofPattern("d. MMM uuuu", Locale.GERMANY)

private data class WidgetStrings(
    val erdentage: String,
    val empty: String,
    val captions: Map<MilestoneKind, String>,
)
