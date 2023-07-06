package com.eozsahin.calendarview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eozsahin.calendarview.ui.models.*
import com.eozsahin.calendarview.ui.theme.Typography
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

internal val MINUTES_PER_HOUR = TimeUnit.HOURS.toMinutes(1).toInt()
internal val HOURS_PER_DAY = TimeUnit.DAYS.toHours(1).toInt()
internal val MIN_PER_DP = 1.5.dp
internal val HOUR_DP = MIN_PER_DP * MINUTES_PER_HOUR

data class Event(
    val id: String = UUID.randomUUID().toString(),
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val isAllDayEvent: Boolean = false,
    val title: String,
    val color: Color,
) {
    override fun equals(other: Any?) = (other as? Event)?.id == id
    override fun hashCode() = id.hashCode()
}

private val DUMMYDATE = LocalDate.of(2016, 2, 15)

val events = listOf(
    Event(
        id = "allday",
        startTime = LocalDateTime.of(DUMMYDATE, LocalTime.of(4, 15, 0)),
        endTime = LocalDateTime.of(DUMMYDATE, LocalTime.of(23, 15, 0)),
        title = "Allday",
        color = Color.Yellow,
        isAllDayEvent = true
    ),

    Event(
        id = "A",
        startTime = LocalDateTime.of(DUMMYDATE, LocalTime.of(8, 15, 0, 0)),
        endTime = LocalDateTime.of(DUMMYDATE, LocalTime.of(10, 30, 0, 0)),
        title = "A",
        color = Color.Green
    ),
    Event(
        id = "B",
        startTime = LocalDateTime.of(DUMMYDATE, LocalTime.of(9, 15, 0)),
        endTime = LocalDateTime.of(DUMMYDATE, LocalTime.of(10, 0, 0)),
        title = "B",
        color = Color.Cyan
    ),
    Event(
        id = "L",
        startTime = LocalDateTime.of(DUMMYDATE, LocalTime.of(9, 45, 0)),
        endTime = LocalDateTime.of(DUMMYDATE, LocalTime.of(10, 30, 0)),
        title = "L",
        color = Color.Magenta
    ),
    Event(
        id = "Z",
        startTime = LocalDateTime.of(DUMMYDATE, LocalTime.of(10, 30, 0)),
        endTime = LocalDateTime.of(DUMMYDATE, LocalTime.of(12, 0, 0)),
        title = "Z",
        color = Color.Blue
    ),
    Event(
        id = "C",
        startTime = LocalDateTime.of(DUMMYDATE, LocalTime.of(10, 15, 0)),
        endTime = LocalDateTime.of(DUMMYDATE, LocalTime.of(12, 30 ,0)),
        title = "C",
        color = Color.LightGray
    ),
    Event(
        id = "D",
        startTime = LocalDateTime.of(DUMMYDATE, LocalTime.of(12, 15, 0)),
        endTime = LocalDateTime.of(DUMMYDATE, LocalTime.of(13, 15, 0)),
        title = "D",
        color = Color.Red
    ),
    Event(
        id = "F",
        startTime = LocalDateTime.of(DUMMYDATE, LocalTime.of(10, 40, 0)),
        endTime = LocalDateTime.of(DUMMYDATE, LocalTime.of(12, 50, 0)),
        title = "F",
        color = Color.DarkGray
    ),
    Event(
        id = "K",
        startTime = LocalDateTime.of(DUMMYDATE, LocalTime.of(12, 45, 0)),
        endTime = LocalDateTime.of(DUMMYDATE, LocalTime.of(13, 15, 0)),
        title = "K",
        color = Color.Gray
    ),
)

@Composable
fun CalendarDayView(
    events: List<Event>,
    date: LocalDate,
    modifier: Modifier = Modifier,
    defaultDayStart: LocalTime = LocalTime.of(7, 0, 0),
) {
    val sortedRegularEvents = remember(events) {
        events.filter { !it.isAllDayEvent }
            .sortedBy { it.startTime }
    }
    val dayStart: LocalDateTime = remember(sortedRegularEvents) {
        sortedRegularEvents.firstOrNull()?.startTime?.minusHours(1)?.truncatedTo(ChronoUnit.HOURS)
            ?: LocalDateTime.of(date, defaultDayStart)
    }
    val collection = remember(sortedRegularEvents) {
        generateCalendarDayViewCollection(sortedRegularEvents)
    }
    val regularUIEvents: List<UIEvent> = remember(collection) {
        prepareUIEvents(sortedRegularEvents, collection)
    }
    val allDayUIEvents = remember(events) {
        events.filter { it.isAllDayEvent }
            .map { it.toUIEvent() }
    }

    val currentTimeIndicatorHeight = findStartHeight(
        dayStart = dayStart,
        eventStart = LocalDateTime.of(
            DUMMYDATE, LocalDateTime.now().toLocalTime())
    )

    Box {
        Column(
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Row(Modifier.padding(top = 30.dp)) {
                Column(Modifier.padding(horizontal = 8.dp)) {
                    ((dayStart.hour)..HOURS_PER_DAY).forEach {
                        Column(Modifier.height(HOUR_DP)) {
                            Text(text = "$it:00", style = Typography.labelMedium)
                        }
                    }
                }
                Box(Modifier.width(2.dp).fillMaxHeight().background(Color.Black))
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    regularUIEvents.forEach { event ->
                        Box(
                            modifier = Modifier
                                .size(event.findWidth(maxWidth), event.findHeight())
                                .dpOffset(event.findStartOffSet(dayStart, maxWidth))
                                .clip(RoundedCornerShape(4.dp))
                                .background(event.source.color)
                                .clickable { }
                                .padding(8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(event.source.title)
                        }
                    }
                    Divider(
                        modifier = Modifier
                            .offset(0.dp, currentTimeIndicatorHeight),
                        color = Color.DarkGray
                    )
                }
            }
        }
        Surface(shadowElevation = 8.dp) {


            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .background(MaterialTheme.colorScheme.background),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("all-day", modifier = Modifier.padding(end = 8.dp),
                    style = Typography.labelSmall
                    )

                Column(Modifier.weight(1f)) {
                    allDayUIEvents.forEach {
                        Text(it.source.title, modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray)
                            .clickable {  }
                            .padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarDayViewPreview() {
    CalendarDayView(events = events, date = DUMMYDATE)
}

data class DpOffset(val x: Dp, val y: Dp)

fun findStartHeight(
    dayStart: LocalDateTime,
    eventStart: LocalDateTime
): Dp {
    val durationMins = ChronoUnit.MINUTES.between(dayStart, eventStart)
    return (durationMins * MIN_PER_DP.value).dp
}

fun Modifier.dpOffset(dpOffset: DpOffset) = this.offset(dpOffset.x, dpOffset.y)

internal fun Event.toUIEvent() = UIEvent(this.id, this)

