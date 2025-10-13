package com.monayem.classschedule

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.monayem.classschedule.data.AppDatabase
import com.monayem.classschedule.data.ScheduleEntry
import com.monayem.classschedule.data.Subject
import com.monayem.classschedule.ui.schedule.ScheduleViewModel
import com.monayem.classschedule.ui.schedule.ScheduleViewModelFactory
import com.monayem.classschedule.ui.theme.ClassScheduleTheme
import com.monayem.classschedule.ui.theme.ColorUtils
import kotlinx.coroutines.launch

class SecondActivity : ComponentActivity() {
    private val viewModel: ScheduleViewModel by viewModels {
        ScheduleViewModelFactory(AppDatabase.getDatabase(this).scheduleDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClassScheduleTheme {
                UpdateScheduleScreen(viewModel = viewModel, onNavigateBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScheduleScreen(viewModel: ScheduleViewModel, onNavigateBack: () -> Unit) {
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Update Schedule") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") } }
            )
        }
    ) {
        Column(modifier = Modifier.padding(it)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Courses") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Weekly Schedule") })
            }
            when (selectedTab) {
                0 -> ManageCoursesScreen(viewModel)
                1 -> BuildScheduleScreen(viewModel)
            }
        }
    }
}

@Composable
fun ManageCoursesScreen(viewModel: ScheduleViewModel) {
    val subjects by viewModel.subjects.collectAsState()
    var subjectToEdit by remember { mutableStateOf<Subject?>(null) }
    val schedule by viewModel.schedule.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // This effect ensures a default schedule exists, fixing the bug on physical devices.
    LaunchedEffect(key1 = Unit) {
        if (schedule == null) {
            viewModel.addSchedule("My Schedule")
        }
    }

    if (subjectToEdit != null) {
        EditCourseDialog(
            subject = subjectToEdit!!,
            onDismiss = { subjectToEdit = null },
            onSave = { updatedSubject ->
                viewModel.updateSubject(updatedSubject)
                subjectToEdit = null
            }
        )
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Card(elevation = CardDefaults.cardElevation(2.dp)) {
            var subjectName by remember { mutableStateOf("") }
            var subjectCode by remember { mutableStateOf("") }

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Add a New Course", style = MaterialTheme.typography.titleLarge)
                OutlinedTextField(value = subjectName, onValueChange = { subjectName = it }, label = { Text("Course Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subjectCode, onValueChange = { subjectCode = it }, label = { Text("Course Code") }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    coroutineScope.launch {
                        val currentSchedule = viewModel.schedule.value
                        if (currentSchedule != null && subjectName.isNotBlank() && subjectCode.isNotBlank()) {
                            val color = ColorUtils.getColorForSubject(subjects.size).toArgb()
                            viewModel.addSubject(currentSchedule.id, subjectName, subjectCode, color)
                            subjectName = ""
                            subjectCode = ""
                        }
                    }
                }, modifier = Modifier.align(Alignment.End)) {
                    Text("Add Course")
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Text("Your Courses", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(subjects) { subject ->
                Card(elevation = CardDefaults.cardElevation(2.dp)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(Color(subject.color)))
                        Spacer(Modifier.width(16.dp))
                        Text("${subject.name} [${subject.code}]", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                        IconButton(onClick = { subjectToEdit = subject }) { Icon(Icons.Default.Edit, "Edit Course") }
                        IconButton(onClick = { viewModel.deleteSubject(subject) }) { Icon(Icons.Default.Delete, "Delete Course", tint = Color.Gray) }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildScheduleScreen(viewModel: ScheduleViewModel) {
    val daysOfWeek = listOf("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    var selectedDay by remember { mutableStateOf(daysOfWeek.first()) }
    var entryToEdit by remember { mutableStateOf<ScheduleEntry?>(null) }
    val subjects by viewModel.subjects.collectAsState()
    val entriesByDay by viewModel.scheduleByDay.collectAsState()

    val entriesForSelectedDay = remember(selectedDay, entriesByDay) {
        entriesByDay[selectedDay.uppercase()] ?: emptyList()
    }

    if (entryToEdit != null) {
        EditScheduleEntryDialog(
            entry = entryToEdit!!,
            subjects = subjects,
            dayOfWeek = selectedDay,
            onDismiss = { entryToEdit = null },
            onSave = {
                if (it.id == 0) viewModel.addScheduleEntry(it) else viewModel.updateScheduleEntry(it)
                entryToEdit = null
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { entryToEdit = ScheduleEntry(id = 0, subjectId = -1, dayOfWeek = selectedDay, startTime = "", endTime = "", roomNumber = "") }) {
                Icon(Icons.Default.Add, "Add class for $selectedDay")
            }
        }
    ) {
        Column(modifier = Modifier.padding(it).padding(16.dp)) {
            var dayDropdownExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = dayDropdownExpanded, onExpandedChange = { dayDropdownExpanded = !dayDropdownExpanded }) {
                OutlinedTextField(value = selectedDay, onValueChange = {}, readOnly = true, label = { Text("Day of the Week") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dayDropdownExpanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                ExposedDropdownMenu(expanded = dayDropdownExpanded, onDismissRequest = { dayDropdownExpanded = false }) {
                    daysOfWeek.forEach { day -> DropdownMenuItem(text = { Text(day) }, onClick = { selectedDay = day; dayDropdownExpanded = false }) }
                }
            }

            LazyColumn(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(entriesForSelectedDay) { (_, subject, entry) ->
                    Card(elevation = CardDefaults.cardElevation(2.dp), onClick = { entryToEdit = entry }) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(4.dp, 48.dp).background(Color(subject.color), RoundedCornerShape(2.dp)))
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${entry.startTime} - ${entry.endTime}", fontWeight = FontWeight.Bold)
                                Text("${subject.name} [${subject.code}]")
                                Text("Room: ${entry.roomNumber}", color = Color.Gray)
                            }
                            IconButton(onClick = { viewModel.deleteScheduleEntry(entry) }) { Icon(Icons.Default.Delete, "Delete Entry", tint = Color.Gray) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditCourseDialog(subject: Subject, onDismiss: () -> Unit, onSave: (Subject) -> Unit) {
    var subjectName by remember { mutableStateOf(subject.name) }
    var subjectCode by remember { mutableStateOf(subject.code) }
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Edit Course", style = MaterialTheme.typography.headlineSmall)
                OutlinedTextField(value = subjectName, onValueChange = { subjectName = it }, label = { Text("Course Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subjectCode, onValueChange = { subjectCode = it }, label = { Text("Course Code") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSave(subject.copy(name = subjectName, code = subjectCode)) }) { Text("Save") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScheduleEntryDialog(
    entry: ScheduleEntry,
    subjects: List<Subject>,
    dayOfWeek: String,
    onDismiss: () -> Unit,
    onSave: (ScheduleEntry) -> Unit
) {
    var selectedSubject by remember { mutableStateOf(subjects.find { it.id == entry.subjectId }) }
    var startTime by remember { mutableStateOf(entry.startTime) }
    var endTime by remember { mutableStateOf(entry.endTime) }
    var room by remember { mutableStateOf(entry.roomNumber) }
    var subjectDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (entry.id == 0) "Add Class" else "Edit Class", style = MaterialTheme.typography.headlineSmall)
                ExposedDropdownMenuBox(expanded = subjectDropdownExpanded, onExpandedChange = { subjectDropdownExpanded = !subjectDropdownExpanded }) {
                    OutlinedTextField(value = selectedSubject?.name ?: "", onValueChange = {}, readOnly = true, label = { Text("Course") }, modifier = Modifier.menuAnchor().fillMaxWidth(), trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectDropdownExpanded) })
                    ExposedDropdownMenu(expanded = subjectDropdownExpanded, onDismissRequest = { subjectDropdownExpanded = false }) {
                        subjects.forEach { subject -> DropdownMenuItem(text = { Text(subject.name) }, onClick = { selectedSubject = subject; subjectDropdownExpanded = false }) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = startTime, onValueChange = {startTime = it}, label = { Text("Start Time (HH:mm)") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = endTime, onValueChange = {endTime = it}, label = { Text("End Time (HH:mm)") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = room, onValueChange = { room = it }, label = { Text("Room Number") }, modifier = Modifier.fillMaxWidth())
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { selectedSubject?.id?.let { onSave(entry.copy(subjectId = it, dayOfWeek = dayOfWeek, startTime = startTime, endTime = endTime, roomNumber = room)) } }, enabled = selectedSubject != null && startTime.isNotBlank() && endTime.isNotBlank() && room.isNotBlank()) { Text("Save") }
                }
            }
        }
    }
}
