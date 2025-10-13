package com.monayem.classschedule.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.monayem.classschedule.data.Schedule
import com.monayem.classschedule.data.ScheduleDao
import com.monayem.classschedule.data.ScheduleEntry
import com.monayem.classschedule.data.Subject
import com.monayem.classschedule.data.SubjectWithScheduleEntries
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScheduleViewModel(private val dao: ScheduleDao) : ViewModel() {

    val schedule = dao.getSchedule().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val subjects: StateFlow<List<Subject>> = schedule.flatMapLatest { currentSchedule ->
        if (currentSchedule == null) flowOf(emptyList()) else dao.getSubjects(currentSchedule.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    private val subjectsWithEntries: StateFlow<List<SubjectWithScheduleEntries>> = schedule.flatMapLatest { currentSchedule ->
        if (currentSchedule == null) flowOf(emptyList()) else dao.getSubjectsWithScheduleEntries(currentSchedule.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduleByDay: StateFlow<Map<String, List<Triple<String, Subject, ScheduleEntry>>>> = subjectsWithEntries.map { list ->
        list.flatMap { subjectWithEntries ->
                subjectWithEntries.scheduleEntries.map { entry ->
                    Triple(entry.dayOfWeek.uppercase(), subjectWithEntries.subject, entry)
                }
            }
            .groupBy { it.first }
            .toSortedMap(compareBy { day ->
                listOf("SATURDAY", "SUNDAY", "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY").indexOf(day)
            })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val legendSubjects: StateFlow<List<Subject>> = subjectsWithEntries.map { list ->
        list.map { it.subject }.distinctBy { it.name }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSchedule(scheduleName: String) {
        viewModelScope.launch { dao.insertSchedule(Schedule(scheduleName = scheduleName)) }
    }

    fun addSubject(scheduleId: Int, name: String, code: String, color: Int) {
        viewModelScope.launch { dao.insertSubject(Subject(scheduleId = scheduleId, name = name, code = code, color = color)) }
    }

    fun updateSubject(subject: Subject) {
        viewModelScope.launch { dao.updateSubject(subject) }
    }

    fun deleteSubject(subject: Subject) {
        viewModelScope.launch { dao.deleteSubject(subject) }
    }

    fun addScheduleEntry(entry: ScheduleEntry) {
        viewModelScope.launch { dao.insertScheduleEntry(entry) }
    }

    fun updateScheduleEntry(entry: ScheduleEntry) {
        viewModelScope.launch { dao.updateScheduleEntry(entry) }
    }

    fun deleteScheduleEntry(entry: ScheduleEntry) {
        viewModelScope.launch { dao.deleteScheduleEntry(entry) }
    }
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