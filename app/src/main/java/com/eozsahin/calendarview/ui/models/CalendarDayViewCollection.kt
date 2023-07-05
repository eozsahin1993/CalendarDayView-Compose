package com.eozsahin.calendarview.ui.models

import com.eozsahin.calendarview.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private const val TIME_SLOT_MINS = 5L

internal data class CalendarDayViewCollection(
    val timeSlots: List<TimeSlot>
)

internal data class TimeSlot(
    val startTime: LocalDateTime,
    val endTime: LocalDateTime,
    val events: List<UIEvent>
)

internal fun generateCalendarDayViewCollection(
    sortedEvents: List<Event>
): CalendarDayViewCollection {
    val numSlots = (MINUTES_PER_HOUR * HOURS_PER_DAY) / TIME_SLOT_MINS
    val timeSlots = (0..numSlots).map {
        val startMins = it * TIME_SLOT_MINS
        val startTime = getLocalTimeWithMins(startMins)
        val endTime = startTime.plusMinutes(TIME_SLOT_MINS)

        val slotStart = LocalDateTime.of(LocalDate.of(2016, 2, 15), startTime)
        val slotEnd = LocalDateTime.of(LocalDate.of(2016, 2, 15), endTime)
        val slotEvents: MutableList<UIEvent> = mutableListOf()

        sortedEvents.forEach {event ->
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

    return CalendarDayViewCollection(timeSlots = timeSlots)
}

private fun getLocalTimeWithMins(totalMinutes: Long): LocalTime {
    if (totalMinutes == (MINUTES_PER_HOUR * HOURS_PER_DAY).toLong()) {
        return LocalTime.of(23, 59, 0)
    }

    val hour = totalMinutes / MINUTES_PER_HOUR
    val min = totalMinutes % MINUTES_PER_HOUR
    return LocalTime.of(hour.toInt(), min.toInt(), 0)
}