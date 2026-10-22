package com.monayem.crono.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: Schedule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: Subject): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleEntry(scheduleEntry: ScheduleEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExamEntry(examEntry: ExamEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClassTestEntry(classTestEntry: ClassTestEntry)

    @Update
    suspend fun updateSubject(subject: Subject)

    @Update
    suspend fun updateScheduleEntry(scheduleEntry: ScheduleEntry)

    @Update
    suspend fun updateExamEntry(examEntry: ExamEntry)

    @Update
    suspend fun updateClassTestEntry(classTestEntry: ClassTestEntry)

    @Delete
    suspend fun deleteSubject(subject: Subject)

    @Delete
    suspend fun deleteScheduleEntry(scheduleEntry: ScheduleEntry)

    @Delete
    suspend fun deleteExamEntry(examEntry: ExamEntry)

    @Delete
    suspend fun deleteClassTestEntry(classTestEntry: ClassTestEntry)

    @Query("SELECT * FROM schedules LIMIT 1")
    fun getSchedule(): Flow<Schedule?>

    @Transaction
    @Query("SELECT * FROM subjects WHERE scheduleId = :scheduleId")
    fun getSubjectsWithScheduleEntries(scheduleId: Int): Flow<List<SubjectWithScheduleEntries>>

    @Transaction
    @Query("SELECT * FROM subjects WHERE scheduleId = :scheduleId")
    fun getSubjectsWithExams(scheduleId: Int): Flow<List<SubjectWithExams>>

    @Transaction
    @Query("SELECT * FROM subjects WHERE scheduleId = :scheduleId")
    fun getSubjectsWithClassTests(scheduleId: Int): Flow<List<SubjectWithClassTests>>

    @Query("SELECT * FROM subjects WHERE scheduleId = :scheduleId")
    fun getSubjects(scheduleId: Int): Flow<List<Subject>>

    @Query("SELECT * FROM schedule_entries")
    fun getAllScheduleEntries(): Flow<List<ScheduleEntry>>

    @Query("SELECT * FROM exam_entries")
    fun getAllExamEntries(): Flow<List<ExamEntry>>

    @Query("SELECT * FROM class_test_entries")
    fun getAllClassTestEntries(): Flow<List<ClassTestEntry>>

    @Query("SELECT * FROM schedule_entries WHERE subjectId = :subjectId")
    fun getScheduleEntries(subjectId: Int): Flow<List<ScheduleEntry>>

    @Query("DELETE FROM schedule_entries WHERE subjectId = :subjectId")
    suspend fun deleteScheduleEntriesForSubject(subjectId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScheduleEntries(scheduleEntries: List<ScheduleEntry>)
}
