package com.monayem.crono.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class UnifiedEvent(
    val id: String,
    val type: EventType,
    val subjectName: String,
    val subjectCode: String,
    val startTime: String,
    val endTime: String,
    val dateOrDay: String,
    val originalSubject: Subject,
    val originalEntry: Any
) {
    enum class EventType { CLASS, CT, EXAM }
}

object TimeUtils {

    /**
     * Converts compact military format strings (e.g., "1123" -> "11:23", "930" -> "09:30")
     * or standard formatted strings ("9:30" -> "09:30", "11:23" -> "11:23") into "HH:mm" format.
     */
    fun parseAndFormatTime(input: String): String {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return ""

        if (trimmed.all { it.isDigit() }) {
            return when (trimmed.length) {
                1, 2 -> trimmed
                3 -> "0${trimmed[0]}:${trimmed.substring(1)}"
                4 -> "${trimmed.substring(0, 2)}:${trimmed.substring(2)}"
                else -> trimmed
            }
        }

        val parts = trimmed.split(":")
        if (parts.size == 2 && parts[0].all { it.isDigit() } && parts[1].all { it.isDigit() }) {
            val hour = parts[0].padStart(2, '0')
            val min = parts[1].padStart(2, '0')
            return "$hour:$min"
        }

        return trimmed
    }

    /**
     * Checks if a formatted time string is valid HH:mm format.
     */
    fun isValidTime(time: String): Boolean {
        val formatted = parseAndFormatTime(time)
        return formatted.matches(Regex("^([01]?[0-9]|2[0-3]):[0-5][0-9]$"))
    }

    fun getMinutesOfDay(timeStr: String): Int? {
        return try {
            val formatted = parseAndFormatTime(timeStr)
            val sdf = SimpleDateFormat("HH:mm", Locale.US)
            val date = sdf.parse(formatted) ?: return null
            val cal = Calendar.getInstance().apply { time = date }
            cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Checks if current time is within [startStr, endStr).
     * An end time of 11:23 means the class ends when 11:23 begins (active through 11:22).
     */
    fun isCurrentTimeInRange(startStr: String, endStr: String, currentTime: Calendar): Boolean {
        val start = getMinutesOfDay(startStr) ?: return false
        val end = getMinutesOfDay(endStr) ?: return false
        val now = currentTime.get(Calendar.HOUR_OF_DAY) * 60 + currentTime.get(Calendar.MINUTE)
        return now in start until end
    }

    /**
     * Checks if current time has reached or passed the given time string.
     */
    fun isTimePassed(timeStr: String, currentTime: Calendar): Boolean {
        val time = getMinutesOfDay(timeStr) ?: return false
        val now = currentTime.get(Calendar.HOUR_OF_DAY) * 60 + currentTime.get(Calendar.MINUTE)
        return now >= time
    }

    fun doEventsOverlap(e1: UnifiedEvent, e2: UnifiedEvent): Boolean {
        val s1 = getMinutesOfDay(e1.startTime) ?: return false
        val end1 = getMinutesOfDay(e1.endTime) ?: return false
        val s2 = getMinutesOfDay(e2.startTime) ?: return false
        val end2 = getMinutesOfDay(e2.endTime) ?: return false
        return s1 < end2 && s2 < end1
    }

    fun findConflictingEventClusters(events: List<UnifiedEvent>): List<List<UnifiedEvent>> {
        if (events.size < 2) return emptyList()
        val clusters = mutableListOf<MutableList<UnifiedEvent>>()
        for (event in events) {
            val matchingCluster = clusters.find { cluster ->
                cluster.any { doEventsOverlap(it, event) }
            }
            if (matchingCluster != null) {
                if (matchingCluster.none { it.id == event.id }) {
                    matchingCluster.add(event)
                }
            } else {
                val overlappingOthers =
                    events.filter { it.id != event.id && doEventsOverlap(it, event) }
                if (overlappingOthers.isNotEmpty()) {
                    val newCluster = mutableListOf(event)
                    newCluster.addAll(overlappingOthers)
                    clusters.add(newCluster)
                }
            }
        }
        return clusters.map { cluster -> cluster.distinctBy { it.id } }.filter { it.size >= 2 }
    }
}
