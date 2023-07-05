package com.eozsahin.calendarview.ui.models

import android.util.Log
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eozsahin.calendarview.DpOffset
import com.eozsahin.calendarview.Event
import com.eozsahin.calendarview.MIN_PER_DP
import com.eozsahin.calendarview.toUIEvent
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal data class UIEvent(
    val id: String,
    val source: Event,
    var isDisplayed: Boolean = false,
    var horizontalIndex: Int = 0,
    var maxCollisionForGivenTimeSlot: Int = 0,
    var conflictedEventIds: List<String> = emptyList()
) {
    override fun equals(other: Any?) = (other as? UIEvent)?.id == id
    override fun hashCode() = id.hashCode()
}

internal fun UIEvent.findStartOffSet(
    dayStart: LocalDateTime,
    maxWidth: Dp,
): DpOffset {
    val durationMins = ChronoUnit.MINUTES.between(dayStart, this.source.startTime)
    val startY = (durationMins * MIN_PER_DP.value).dp
    val startX = (maxWidth / (this.maxCollisionForGivenTimeSlot + 1)) * horizontalIndex

    return DpOffset(startX, startY)
}

internal fun UIEvent.findHeight(): Dp {
    val start = this.source.startTime
    val end = this.source.endTime
    val durationInMins = ChronoUnit.MINUTES.between(start, end)
    return (MIN_PER_DP.value * durationInMins).dp
}

internal fun UIEvent.findWidth(maxWidth: Dp) = maxWidth / (this.maxCollisionForGivenTimeSlot + 1)

internal fun prepareUIEvents(
    sortedEvents: List<Event>,
    collection: CalendarDayViewCollection
): List<UIEvent> {
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

    val list = sortedEvents.map {
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
                if (conflict.horizontalIndex < available.size) {
                    available[conflict.horizontalIndex] = false
                }
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
    return visited
}

