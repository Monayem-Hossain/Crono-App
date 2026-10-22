package com.monayem.crono.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.monayem.crono.data.ClassTestEntry
import com.monayem.crono.data.ExamEntry
import com.monayem.crono.data.Schedule
import com.monayem.crono.data.ScheduleDao
import com.monayem.crono.data.ScheduleEntry
import com.monayem.crono.data.Subject
import com.monayem.crono.data.SubjectWithClassTests
import com.monayem.crono.data.SubjectWithExams
import com.monayem.crono.data.SubjectWithScheduleEntries
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScheduleViewModel(private val dao: ScheduleDao) : ViewModel() {

    val schedule =
        dao.getSchedule().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val subjects: StateFlow<List<Subject>> = schedule.flatMapLatest { currentSchedule ->
        if (currentSchedule == null) flowOf(emptyList()) else dao.getSubjects(currentSchedule.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val subjectsWithEntries: StateFlow<List<SubjectWithScheduleEntries>> =
        schedule.flatMapLatest { currentSchedule ->
            if (currentSchedule == null) flowOf(emptyList()) else dao.getSubjectsWithScheduleEntries(
                currentSchedule.id
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val subjectsWithExams: StateFlow<List<SubjectWithExams>> =
        schedule.flatMapLatest { currentSchedule ->
            if (currentSchedule == null) flowOf(emptyList()) else dao.getSubjectsWithExams(
                currentSchedule.id
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val subjectsWithClassTests: StateFlow<List<SubjectWithClassTests>> =
        schedule.flatMapLatest { currentSchedule ->
            if (currentSchedule == null) flowOf(emptyList()) else dao.getSubjectsWithClassTests(
                currentSchedule.id
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allScheduleEntries: StateFlow<List<ScheduleEntry>> = dao.getAllScheduleEntries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduleByDay: StateFlow<Map<String, List<Triple<String, Subject, ScheduleEntry>>>> =
        subjectsWithEntries.map { list ->
            list.flatMap { subjectWithEntries ->
                subjectWithEntries.scheduleEntries.map { entry ->
                    Triple(entry.dayOfWeek.uppercase(), subjectWithEntries.subject, entry)
                }
            }
                .groupBy { it.first }
                .mapValues { group ->
                    group.value.sortedBy { triple ->
                        val time = triple.third.startTime
                        if (time.contains(":") && time.indexOf(":") == 1) "0$time" else time
                    }
                }
                .toSortedMap(compareBy { day ->
                    listOf(
                        "SATURDAY",
                        "SUNDAY",
                        "MONDAY",
                        "TUESDAY",
                        "WEDNESDAY",
                        "THURSDAY",
                        "FRIDAY"
                    ).indexOf(day)
                })
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val examsSorted: StateFlow<List<Pair<Subject, ExamEntry>>> = subjectsWithExams.map { list ->
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        list.flatMap { subjectWithExams ->
            subjectWithExams.examEntries.map { exam ->
                subjectWithExams.subject to exam
            }
        }.sortedBy {
            try {
                sdf.parse(it.second.date)
            } catch (e: Exception) {
                Date(Long.MAX_VALUE)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val classTestsSorted: StateFlow<List<Pair<Subject, ClassTestEntry>>> =
        subjectsWithClassTests.map { list ->
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
            list.flatMap { swct ->
                swct.classTestEntries.map {
                    swct.subject to it
                }
            }.sortedBy {
                try {
                    sdf.parse(it.second.date)
                } catch (e: Exception) {
                    Date(Long.MAX_VALUE)
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val legendSubjects: StateFlow<List<Subject>> = subjectsWithEntries.map { list ->
        list.map { it.subject }.distinctBy { it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSchedule(scheduleName: String) =
        viewModelScope.launch { dao.insertSchedule(Schedule(scheduleName = scheduleName)) }

    fun addSubject(
        scheduleId: Int,
        name: String,
        code: String,
        color: Int,
        instructor: String,
        note: String = ""
    ) = viewModelScope.launch {
        dao.insertSubject(
            Subject(
                scheduleId = scheduleId,
                name = name,
                code = code,
                color = color,
                instructor = instructor,
                note = note
            )
        )
    }

    fun updateSubject(subject: Subject) = viewModelScope.launch { dao.updateSubject(subject) }
    fun deleteSubject(subject: Subject) = viewModelScope.launch { dao.deleteSubject(subject) }
    fun addScheduleEntry(entry: ScheduleEntry) =
        viewModelScope.launch { dao.insertScheduleEntry(entry) }

    fun updateScheduleEntry(entry: ScheduleEntry) =
        viewModelScope.launch { dao.updateScheduleEntry(entry) }

    fun deleteScheduleEntry(entry: ScheduleEntry) =
        viewModelScope.launch { dao.deleteScheduleEntry(entry) }

    fun addExamEntry(entry: ExamEntry) = viewModelScope.launch { dao.insertExamEntry(entry) }
    fun updateExamEntry(entry: ExamEntry) = viewModelScope.launch { dao.updateExamEntry(entry) }
    fun deleteExamEntry(entry: ExamEntry) = viewModelScope.launch { dao.deleteExamEntry(entry) }
    fun addClassTestEntry(entry: ClassTestEntry) =
        viewModelScope.launch { dao.insertClassTestEntry(entry) }

    fun updateClassTestEntry(entry: ClassTestEntry) =
        viewModelScope.launch { dao.updateClassTestEntry(entry) }

    fun deleteClassTestEntry(entry: ClassTestEntry) =
        viewModelScope.launch { dao.deleteClassTestEntry(entry) }
}

class ScheduleViewModelFactory(private val dao: ScheduleDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ScheduleViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
