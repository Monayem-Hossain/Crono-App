package com.monayem.crono

import com.monayem.crono.data.Subject
import com.monayem.crono.data.TimeUtils
import com.monayem.crono.data.UnifiedEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ExampleUnitTest {

    @Test
    fun testParseAndFormatTime() {
        assertEquals("11:23", TimeUtils.parseAndFormatTime("1123"))
        assertEquals("09:30", TimeUtils.parseAndFormatTime("930"))
        assertEquals("08:30", TimeUtils.parseAndFormatTime("0830"))
        assertEquals("09:30", TimeUtils.parseAndFormatTime("9:30"))
        assertEquals("11:23", TimeUtils.parseAndFormatTime("11:23"))
    }

    @Test
    fun testIsValidTime() {
        assertTrue(TimeUtils.isValidTime("1123"))
        assertTrue(TimeUtils.isValidTime("930"))
        assertTrue(TimeUtils.isValidTime("11:23"))
        assertTrue(TimeUtils.isValidTime("09:30"))
        assertFalse(TimeUtils.isValidTime("2500"))
        assertFalse(TimeUtils.isValidTime("abcd"))
    }

    @Test
    fun testEndTimeLogic() {
        // Class from 10:00 to 11:23
        val cal1122 = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE, 22)
        }
        val cal1123 = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 11)
            set(Calendar.MINUTE, 23)
        }

        // At 11:22, the class ending at 11:23 is ONGOING
        assertTrue(TimeUtils.isCurrentTimeInRange("1000", "1123", cal1122))

        // At 11:23, the class ending at 11:23 is FINISHED
        assertFalse(TimeUtils.isCurrentTimeInRange("1000", "1123", cal1123))
    }

    @Test
    fun testGetDateForDayOfWeek() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 15)
        }
        val fridayDate = getDateForDayOfWeek("FRIDAY", cal)
        assertEquals("16-10-2026", fridayDate)
    }

    @Test
    fun testConflictDetection() {
        val dummySubject = Subject(id = 1, scheduleId = 1, name = "Math", code = "M101", color = 0)
        val event1 = UnifiedEvent(
            "1",
            UnifiedEvent.EventType.CLASS,
            "Math",
            "M101",
            "10:00",
            "11:00",
            "MONDAY",
            dummySubject,
            ""
        )
        val event2 = UnifiedEvent(
            "2",
            UnifiedEvent.EventType.CT,
            "Physics",
            "P101",
            "10:30",
            "11:30",
            "MONDAY",
            dummySubject,
            ""
        )
        val event3 = UnifiedEvent(
            "3",
            UnifiedEvent.EventType.EXAM,
            "Chemistry",
            "C101",
            "14:00",
            "15:00",
            "MONDAY",
            dummySubject,
            ""
        )

        assertTrue(TimeUtils.doEventsOverlap(event1, event2))
        assertFalse(TimeUtils.doEventsOverlap(event1, event3))

        val clusters = TimeUtils.findConflictingEventClusters(listOf(event1, event2, event3))
        assertEquals(1, clusters.size)
        assertEquals(2, clusters[0].size)
    }

    @Test
    fun testRealtimeAutoSelectionWhenPassed() {
        val dummySubject = Subject(id = 1, scheduleId = 1, name = "Math", code = "M101", color = 0)
        val event1 = UnifiedEvent(
            "1",
            UnifiedEvent.EventType.CLASS,
            "Math",
            "M101",
            "10:00",
            "10:30",
            "MONDAY",
            dummySubject,
            ""
        )
        val event2 = UnifiedEvent(
            "2",
            UnifiedEvent.EventType.CLASS,
            "Physics",
            "P101",
            "10:00",
            "11:30",
            "MONDAY",
            dummySubject,
            ""
        )

        val nowMinutes = 10 * 60 + 35 // 10:35 AM

        val activeEvents = listOf(event1, event2).filter { e ->
            val endMin = TimeUtils.getMinutesOfDay(e.endTime) ?: 0
            nowMinutes < endMin
        }

        assertEquals(1, activeEvents.size)
        assertEquals("2", activeEvents.first().id)
    }
}
