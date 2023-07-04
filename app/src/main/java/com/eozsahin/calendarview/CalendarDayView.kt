package com.eozsahin.calendarview

import android.location.Location
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit

private const val HOUR_IN_MINS = 60
private const val DAY_IN_HOURS = 24
private const val TIME_SLOT_MINS = 5L

data class Event(
    val id: String = UUID.randomUUID().toString(),
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val title: String,
    val color: Color,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UIEvent) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

val events = listOf(
    Event(
        id = "1",
        startTime = LocalDateTime.of(2016, 2, 15, 9, 0, 0, 0),
        endTime = LocalDateTime.of(2016, 2, 15, 10, 30, 0, 0),
        title = "Emre's meeting -- Green",
        color = Color.Green
    ),
    Event(
        id = "2",
        startTime = LocalDateTime.of(2016, 2, 15, 10, 0, 0),
        endTime = LocalDateTime.of(2016, 2, 15, 11, 0,0),
        title = "Emre's first conflic -- Blue",
        color = Color.Blue
    ),
    Event(
        id = "3",
        startTime = LocalDateTime.of(2016, 2, 15, 13, 0, 0, 0),
        endTime = LocalDateTime.of(2016, 2, 15, 14, 0, 0, 0),
        title = "Emre's meeting -- Red",
        color = Color.Red
    ),
    Event(
        id = "4",
        startTime = LocalDateTime.of(2016, 2, 15, 14, 0, 0, 0),
        endTime = LocalDateTime.of(2016, 2, 15, 15, 45, 0, 0),
        title = "Emre's meeting -- DarkGray",
        color = Color.DarkGray
    ),
)

@Preview(showBackground = true)
@Composable
fun CalendarDayView(
//    events: List<Event>,
//    date: LocalDate,
) {
//    val uiEvents = remember(events) {
//        events.map { it.toUIEvent() }.sortedBy { it.source.startTime }
//    }
    val collection = remember(events) {
        val numSlots = (HOUR_IN_MINS * DAY_IN_HOURS) / TIME_SLOT_MINS
        val timeSlots = (0..numSlots).map {
            val startMins = it * TIME_SLOT_MINS
            val startTime = getLocalTimeWithMins(startMins)
            val endTime = startTime.plusMinutes(TIME_SLOT_MINS)

            val slotStart = LocalDateTime.of(LocalDate.of(2016, 2, 15), startTime)
            val slotEnd = LocalDateTime.of(LocalDate.of(2016, 2, 15), endTime)
            val slotEvents: MutableList<UIEvent> = mutableListOf()

            events.forEach {event ->
                val eventStart = event.startTime
                val eventEnd = event.endTime
                val isInsideSlot = eventStart.isBefore(slotStart) && eventEnd.isAfter(slotStart) ||
                        eventStart.isAfter(slotStart) && eventEnd.isBefore(slotStart) ||
                        eventStart.isBefore(slotEnd) && eventEnd.isAfter(slotEnd)

                if (isInsideSlot) {
                    slotEvents.add(event.toUIEvent())
                }
            }


            TimeSlot(
                startTime = slotStart,
                endTime = slotEnd,
                events = slotEvents
            )
        }.toList()
        timeSlots.map { slot ->
            if (slot.events.isNotEmpty()) {
               return@map slot
            }

            slot.events.forEach {

            }
        }

        timeSlots.forEach {
        }

        CalendarDayViewCollection(timeSlots = timeSlots)
    }
    var uiEvents: List<UIEvent> = remember(collection) {
        val eventConflicts: MutableMap<UIEvent, MutableSet<String>> = mutableMapOf()
        val maxConflicts: MutableMap<UIEvent, Int> = mutableMapOf()
        collection.timeSlots.forEach { slots ->
            if (slots.events.isEmpty()) {
                return@forEach
            }

            slots.events.forEach { event ->
                val slotConflicts = (slots.events - event).map { it.id }
                val slotConflictCount = slotConflicts.size
                val currentConflictCount = maxConflicts[event] ?: 0
                val currentConflictSet = eventConflicts[event] ?: mutableSetOf()
                currentConflictSet.addAll(slotConflicts)

                maxConflicts[event] = slotConflictCount.coerceAtLeast(currentConflictCount)
                eventConflicts[event] = currentConflictSet
            }
        }

        val list = events.map {
            val uiEvent = it.toUIEvent()
            val conflicts = eventConflicts[uiEvent]
            if (conflicts.isNullOrEmpty()) {
                return@map uiEvent
            }

            UIEvent(
                id = it.id,
                source = it,
                isDisplayed = false,
                horizontalIndex = 0,
                maxCollisionForGivenTimeSlot = maxConflicts[uiEvent] ?: 0,
                conflictedEventIds = conflicts.toList()
            )
        }
//        list.forEach {
//            Log.i("emre", "event: $it")
//        }

        val visited: MutableList<UIEvent> = mutableListOf()
        list.forEach { event ->
            val available = (0..event.maxCollisionForGivenTimeSlot + 1).map { true }.toMutableList()
            event.conflictedEventIds.forEach { conflictedEventId ->
                val conflict = visited.firstOrNull { conflictedEventId == it.id } ?: return@forEach

                if (conflict.isDisplayed) {
                    available[conflict.horizontalIndex] = false
                }
            }

            val horizontalIndex = available.withIndex().firstOrNull { it.value }?.index ?: 0

            val newVisitedItem = event.copy(
                isDisplayed = true,
                horizontalIndex = horizontalIndex
            )
            visited.add(newVisitedItem)
        }
        visited.forEach {
            Log.i("emre", "displayed event: $it")
        }
        visited
    }

    Column(
        Modifier
            .fillMaxWidth()
            .height((24 * HOUR_DP.value).dp)
            .verticalScroll(rememberScrollState())) {
        Row {
            Column(Modifier.padding(horizontal = 8.dp)) {
                (8..24).forEach {
                    Column(Modifier.height(HOUR_DP)) {
                        Text(text = "$it")
                    }
                }
            }
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                uiEvents.forEach { event ->
                    Box(
                        modifier = Modifier
                            .size(maxWidth / (event.maxCollisionForGivenTimeSlot + 1), event.findHeight())
                            .dpOffset(event.findStartOffSet())
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
                        .dpOffset(
                            findStartOffset(
                                eventStart = LocalDateTime.now()
                            )
                        ),
                    color = Color.DarkGray
                )
            }
        }
    }
}

data class DpOffset(val x: Dp, val y: Dp)

fun findStartOffset(
    dayStart: LocalDateTime = LocalDateTime.of(2016, 2, 15, 8, 0, 0),
    eventStart: LocalDateTime
): DpOffset {
    val durationMins = ChronoUnit.MINUTES.between(dayStart, eventStart)

    // This would have to calculate the collisions as well
    // dp needs to be converted to pixels here
    return DpOffset(0.dp, (durationMins * MIN_PER_DP.value).dp)
}

fun UIEvent.findStartOffSet(
    dayStart: LocalDateTime = LocalDateTime.of(2016, 2, 15, 8, 0, 0),
    maxWidth: Dp,
): DpOffset {
    val durationMins = ChronoUnit.MINUTES.between(dayStart, this.source.startTime)
    val startY = (durationMins * MIN_PER_DP.value).dp
    val startX = (maxWidth / this.maxCollisionForGivenTimeSlot) * horizontalIndex

    return DpOffset(startX, startY)
}

fun findStartOffsetForEvent(
    dayStart: LocalDateTime = LocalDateTime.of(2016, 2, 15, 8, 0, 0),
    event: UIEvent
): DpOffset {
    return findStartOffset(dayStart, event.source.startTime)
}

fun UIEvent.findHeight(): Dp {
    val start = this.source.startTime
    val end = this.source.endTime
    val durationInMins = ChronoUnit.MINUTES.between(start, end)
    return (MIN_PER_DP.value * durationInMins).dp
}

fun Event.getCollisions(allEvents: List<Event>): Int {
    // this shouldn't be greater than 3
    return 1
}

fun UIEvent.calculateCollisions(allEvents: List<UIEvent>) {

}

fun getLocalTimeWithMins(totalMinutes: Long): LocalTime {
    if (totalMinutes == (HOUR_IN_MINS * DAY_IN_HOURS).toLong()) {
        return LocalTime.of(23, 59, 0)
    }

    val hour = totalMinutes / HOUR_IN_MINS
    val min = totalMinutes % HOUR_IN_MINS
    return LocalTime.of(hour.toInt(), min.toInt(), 0)
}

fun Modifier.dpOffset(dpOffset: DpOffset) = this.offset(dpOffset.x, dpOffset.y)

private fun Event.toUIEvent() = UIEvent(this.id, this)

data class UIEvent(
    val id: String,
    val source: Event,
    var isDisplayed: Boolean = false,
    var horizontalIndex: Int = 0,
    var maxCollisionForGivenTimeSlot: Int = 0,
    var conflictedEventIds: List<String> = emptyList()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UIEvent) return false

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}

data class CalendarDayViewCollection(
    val timeSlots: List<TimeSlot>
)

data class TimeSlot(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val events: List<UIEvent>
)