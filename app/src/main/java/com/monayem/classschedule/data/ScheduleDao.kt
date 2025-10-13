package com.monayem.classschedule.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: Schedule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleEntry(scheduleEntry: ScheduleEntry)

    @Update
    suspend fun updateSubject(subject: Subject)

    @Update
    suspend fun updateScheduleEntry(scheduleEntry: ScheduleEntry)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Delete
    suspend fun deleteScheduleEntry(scheduleEntry: ScheduleEntry)

    @Query("SELECT * FROM schedules LIMIT 1")
    fun getSchedule(): Flow<Schedule?>

    @Transaction
    @Query("SELECT * FROM subjects WHERE scheduleId = :scheduleId")
    fun getSubjectsWithScheduleEntries(scheduleId: Int): Flow<List<SubjectWithScheduleEntries>>

    @Query("SELECT * FROM subjects WHERE scheduleId = :scheduleId")
    fun getSubjects(scheduleId: Int): Flow<List<Subject>>

    @Query("SELECT * FROM schedule_entries")
    fun getAllScheduleEntries(): Flow<List<ScheduleEntry>>

    @Query("SELECT * FROM schedule_entries WHERE subjectId = :subjectId")
    fun getScheduleEntries(subjectId: Int): Flow<List<ScheduleEntry>>

    @Query("DELETE FROM schedule_entries WHERE subjectId = :subjectId")
    suspend fun deleteScheduleEntriesForSubject(subjectId: Int)

    @Transaction
    suspend fun saveScheduleForSubject(subjectId: Int, entries: List<ScheduleEntry>) {
        deleteScheduleEntriesForSubject(subjectId)
        insertScheduleEntries(entries)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleEntries(scheduleEntries: List<ScheduleEntry>)
}
