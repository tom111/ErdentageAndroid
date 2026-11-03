// SPDX-FileCopyrightText: 2026 Thomas Kahle
// SPDX-License-Identifier: Apache-2.0

package de.eigenraum.erdentage.ui.overview

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.eigenraum.erdentage.R
import de.eigenraum.erdentage.app.OverviewEventUiState
import de.eigenraum.erdentage.domain.MilestoneKind
import de.eigenraum.erdentage.ui.person.todayLabelResource
import de.eigenraum.erdentage.ui.theme.LocalDarkTheme
import de.eigenraum.erdentage.ui.theme.inkSlateColor
import de.eigenraum.erdentage.ui.theme.surfaceColor
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OverviewPage(
    events: List<OverviewEventUiState>,
    modifier: Modifier = Modifier,
) {
    val darkTheme = LocalDarkTheme.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(overviewSky(darkTheme)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(bottom = 72.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.upcoming_title),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 36.dp, bottom = 20.dp)
                    .semantics { heading() },
                color = inkSlateColor(darkTheme),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
            )
            if (events.isEmpty()) {
                EmptyCloud(darkTheme)
            } else {
                CloudTimeline(events = events, darkTheme = darkTheme)
            }
        }
    }
}

@Composable
private fun CloudTimeline(
    events: List<OverviewEventUiState>,
    darkTheme: Boolean,
) {
    val density = LocalDensity.current
    val windowHeight = with(density) {
        LocalWindowInfo.current.containerSize.height.toDp()
    }
    val textScale = density.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val availableWidth = maxWidth
        val landscape = windowHeight < 500.dp
        val wide = maxWidth >= 600.dp && !landscape
        val bubbleWidth = when {
            wide -> 290.dp
            landscape -> 200.dp
            else -> 188.dp
        }.coerceAtMost(maxWidth - 16.dp)
        val baseBubbleHeight = when {
            wide -> 195.dp
            landscape -> 128.dp
            else -> 132.dp
        }
        val bubbleHeight = baseBubbleHeight * textScale
        val baseRowStep = when {
            wide -> 155.dp
            landscape -> 84.dp
            else -> 96.dp
        }
        val rowStep = baseRowStep * textScale
        val jitter = when {
            wide -> 34.dp
            landscape -> 20.dp
            else -> 16.dp
        }
        val layoutWidth = when {
            wide -> minOf(maxWidth * 0.9f, 900.dp)
            landscape -> minOf(maxWidth * 0.76f, 700.dp)
            else -> minOf(maxWidth, 500.dp)
        }
        val layoutStart = (maxWidth - layoutWidth) / 2
        val canvasHeight = rowStep * (events.size - 1).coerceAtLeast(0) +
            bubbleHeight + 28.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight),
        ) {
            events.forEachIndexed { index, event ->
                val id = event.cloudId
                val shape = remember(id) {
                    cloudShape(OverviewCloudMath.fnv1a(id))
                }
                val horizontalJitter = OverviewCloudMath.jitter(id + "-x", jitter)
                val verticalJitter = OverviewCloudMath.jitter(id + "-y", jitter / 2)
                val baseX = if (index % 2 == 0) {
                    layoutStart + 4.dp
                } else {
                    layoutStart + layoutWidth - bubbleWidth - 4.dp
                }
                CloudBubble(
                    event = event,
                    darkTheme = darkTheme,
                    wide = wide,
                    shape = shape,
                    modifier = Modifier
                        .offset(
                            x = (baseX + horizontalJitter)
                                .coerceIn(4.dp, availableWidth - bubbleWidth - 4.dp),
                            y = rowStep * index + 8.dp + verticalJitter,
                        )
                        .width(bubbleWidth)
                        .height(bubbleHeight),
                )
            }
        }
    }
}

@Composable
private fun CloudBubble(
    event: OverviewEventUiState,
    darkTheme: Boolean,
    wide: Boolean,
    shape: Shape,
    modifier: Modifier,
) {
    val tint = event.person.resolvedTint(event.personIndex)
    val ink = inkSlateColor(darkTheme)
    val fill = tint.surfaceColor(darkTheme).copy(alpha = if (darkTheme) 0.94f else 0.96f)
    val glow = tint.surfaceColor(darkTheme).copy(alpha = if (darkTheme) 0.75f else 0f)
    val tags = event.milestone.kinds.filterNot { kind ->
        kind == MilestoneKind.TAUSENDER || kind == MilestoneKind.ZEHNTAUSENDER
    }
    Surface(
        modifier = modifier.shadow(
            elevation = if (darkTheme) 14.dp else 0.dp,
            shape = shape,
            ambientColor = glow,
            spotColor = glow,
        ),
        shape = shape,
        color = fill,
        contentColor = ink,
        border = BorderStroke(
            width = 1.5.dp,
            color = ink.copy(alpha = if (darkTheme) 0.35f else 0.9f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 14.dp)
                .semantics(mergeDescendants = true) {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = event.person.displayName,
                color = ink.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                maxLines = 2,
                style = if (wide) {
                    MaterialTheme.typography.titleMedium
                } else {
                    MaterialTheme.typography.bodySmall
                },
            )
            Text(
                text = NumberFormat.getIntegerInstance(Locale.GERMANY)
                    .format(event.milestone.target),
                color = ink,
                fontSize = if (wide) 28.sp else 22.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Text(
                text = event.milestone.date.format(OverviewDateFormatter),
                color = ink.copy(alpha = 0.75f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                style = MaterialTheme.typography.labelSmall,
            )
            tags.forEach { kind ->
                Text(
                    text = stringResource(kind.todayLabelResource),
                    color = ink.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            event.birthday?.let { birthday ->
                Text(
                    text = birthday.short,
                    color = ink.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

@Composable
private fun EmptyCloud(darkTheme: Boolean) {
    val ink = inkSlateColor(darkTheme)
    val shape = remember { cloudShape(0xE3E11E) }
    Surface(
        modifier = Modifier
            .padding(top = 24.dp)
            .width(240.dp)
            .height(110.dp)
            .shadow(
                elevation = if (darkTheme) 12.dp else 0.dp,
                shape = shape,
                ambientColor = Color.White.copy(alpha = if (darkTheme) 0.18f else 0f),
                spotColor = Color.White.copy(alpha = if (darkTheme) 0.18f else 0f),
            ),
        shape = shape,
        color = if (darkTheme) Color(0xFF2E3342) else Color.White.copy(alpha = 0.9f),
        border = BorderStroke(1.5.dp, ink.copy(alpha = if (darkTheme) 0.35f else 0.9f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.no_upcoming_milestones),
                color = ink.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

internal fun overviewSky(darkTheme: Boolean): Brush {
    val colors = if (darkTheme) {
        listOf(Color(0xFF0A0F2E), Color(0xFF121738), Color(0xFF2E2138))
    } else {
        listOf(Color(0xFFBDDFF7), Color(0xFFE0F0FF), Color(0xFFFCF7ED))
    }
    return Brush.verticalGradient(
        0.0f to colors[0],
        0.55f to colors[1],
        0.85f to colors[2],
        1.0f to colors[2],
    )
}

internal fun cloudShape(seed: Long): Shape = GenericShape { size, _ ->
    var state = seed
    fun nextUnit(): Float {
        state += -7046029254386353131L
        var value = state
        value = (value xor (value ushr 30)) * -4658895280553007687L
        value = (value xor (value ushr 27)) * -7723592293110705685L
        value = value xor (value ushr 31)
        return ((value ushr 11).toDouble() / (1L shl 53).toDouble()).toFloat()
    }

    val center = Offset(size.width / 2f, size.height / 2f)
    val radiusX = size.width / 2f * 0.94f
    val radiusY = size.height / 2f * 0.9f
    val points = List(9) { index ->
        val angle = (Math.PI * 2.0 * index / 9.0).toFloat()
        val wobble = 1f + (nextUnit() - 0.5f) * 0.12f
        Offset(
            x = center.x + cos(angle) * radiusX * wobble,
            y = center.y + sin(angle) * radiusY * wobble,
        )
    }
    fun midpoint(first: Offset, second: Offset): Offset = Offset(
        (first.x + second.x) / 2f,
        (first.y + second.y) / 2f,
    )
    val start = midpoint(points[0], points[1])
    moveTo(start.x, start.y)
    points.indices.forEach { index ->
        val point = points[(index + 1) % points.size]
        val next = points[(index + 2) % points.size]
        val mid = midpoint(point, next)
        quadraticTo(point.x, point.y, mid.x, mid.y)
    }
    close()
}

internal object OverviewCloudMath {
    fun fnv1a(value: String): Long {
        var hash = -3750763034362895579L
        value.encodeToByteArray().forEach { byte ->
            hash = hash xor (byte.toLong() and 0xff)
            hash *= 1099511628211L
        }
        return hash
    }

    fun jitter(key: String, range: Dp): Dp {
        val positive = fnv1a(key).ushr(11)
        val unit = positive.toDouble() / (1L shl 53).toDouble()
        return range * ((unit * 2.0 - 1.0).toFloat())
    }
}

private val OverviewEventUiState.cloudId: String
    get() = "${person.id}-${milestone.target}-${milestone.date.toInstant()}"

private val OverviewDateFormatter = DateTimeFormatter
    .ofPattern("d. MMM uuuu", Locale.GERMANY)
