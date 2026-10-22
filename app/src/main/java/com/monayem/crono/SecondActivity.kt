package com.monayem.crono

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.monayem.crono.data.AppDatabase
import com.monayem.crono.data.ClassTestEntry
import com.monayem.crono.data.ExamEntry
import com.monayem.crono.data.ScheduleEntry
import com.monayem.crono.data.Subject
import com.monayem.crono.data.TimeUtils
import com.monayem.crono.ui.schedule.ScheduleViewModel
import com.monayem.crono.ui.schedule.ScheduleViewModelFactory
import com.monayem.crono.ui.theme.ColorUtils
import com.monayem.crono.ui.theme.CronoTheme
import com.monayem.crono.ui.theme.FontSettingsManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SecondActivity : ComponentActivity() {
    private val viewModel: ScheduleViewModel by viewModels {
        ScheduleViewModelFactory(AppDatabase.getDatabase(this).scheduleDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CronoTheme {
                UpdateScheduleScreen(viewModel = viewModel, onNavigateBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateScheduleScreen(viewModel: ScheduleViewModel, onNavigateBack: () -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Schedule") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Back"
                        )
                    }
                },
                actions = {
                    var showSettings by remember { mutableStateOf(false) }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(
                            Icons.Default.Settings,
                            "Settings"
                        )
                    }
                    if (showSettings) FontSettingsDialog(onDismiss = { showSettings = false })
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Courses") })
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Classes") })
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Exams") })
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("CT") })
            }
            when (selectedTab) {
                0 -> ManageCoursesScreen(viewModel)
                1 -> BuildScheduleScreen(viewModel)
                2 -> ManageExamsScreen(viewModel)
                3 -> ManageClassTestsScreen(viewModel)
            }
        }
    }
}

@Composable
fun FontSettingsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { FontSettingsManager.getInstance(context) }
    var settings by remember { mutableStateOf(manager.getAllSettings()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust Font Sizes") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                FontSizeSlider("Schedule Name (Heading)", settings.titleLarge) {
                    settings = settings.copy(titleLarge = it); manager.saveFontSize(
                    "titleLarge",
                    it
                )
                }
                FontSizeSlider("Section Titles (e.g. Exams)", settings.headlineSmall) {
                    settings =
                        settings.copy(headlineSmall = it); manager.saveFontSize("headlineSmall", it)
                }
                FontSizeSlider("Course Name (Cards)", settings.titleMedium) {
                    settings = settings.copy(titleMedium = it); manager.saveFontSize(
                    "titleMedium",
                    it
                )
                }
                FontSizeSlider("Primary Info (Time/Date)", settings.bodyLarge) {
                    settings = settings.copy(bodyLarge = it); manager.saveFontSize("bodyLarge", it)
                }
                FontSizeSlider("Secondary Info (Room)", settings.bodyMedium) {
                    settings = settings.copy(bodyMedium = it); manager.saveFontSize(
                    "bodyMedium",
                    it
                )
                }
                FontSizeSlider(
                    "Small Labels (View All/CT Card Info)",
                    settings.bodySmall
                ) {
                    settings = settings.copy(bodySmall = it); manager.saveFontSize(
                    "bodySmall",
                    it
                )
                }
                FontSizeSlider("Tags (LIVE/CT/EXAM)", settings.labelSmall) {
                    settings = settings.copy(labelSmall = it); manager.saveFontSize(
                    "labelSmall",
                    it
                )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } }
    )
}

@Composable
fun FontSizeSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column {
        Text("$label: ${value.toInt()} sp", style = MaterialTheme.typography.bodySmall)
        Slider(value = value, onValueChange = onValueChange, valueRange = 8f..40f)
    }
}

@Composable
fun ManageCoursesScreen(viewModel: ScheduleViewModel) {
    val subjects by viewModel.subjects.collectAsState()
    val schedule by viewModel.schedule.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var subjectToEdit by remember { mutableStateOf<Subject?>(null) }

    LaunchedEffect(Unit) {
        if (schedule == null) viewModel.addSchedule("My Schedule")
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, "Add Course")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Your Courses",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            items(subjects) { subject ->
                CourseCard(
                    subject = subject,
                    onDelete = { viewModel.deleteSubject(it) },
                    onEdit = { subjectToEdit = it }
                )
            }
        }
    }

    if (showAddDialog) {
        AddCourseDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, code, instructor, note ->
                schedule?.id?.let { id ->
                    val color = ColorUtils.getColorForSubject(subjects.size).toArgb()
                    viewModel.addSubject(id, name, code, color, instructor, note)
                }
                showAddDialog = false
            }
        )
    }

    if (subjectToEdit != null) {
        AddCourseDialog(
            subject = subjectToEdit,
            onDismiss = { subjectToEdit = null },
            onSave = { name, code, instructor, note ->
                viewModel.updateSubject(
                    subjectToEdit!!.copy(
                        name = name,
                        code = code,
                        instructor = instructor,
                        note = note
                    )
                )
                subjectToEdit = null
            }
        )
    }
}

@Composable
fun CourseCard(subject: Subject, onDelete: (Subject) -> Unit, onEdit: (Subject) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit(subject) },
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(subject.color))
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        subject.code,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    if (subject.instructor.isNotBlank()) {
                        Text(
                            "Instr: ${subject.instructor}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                IconButton(onClick = { onEdit(subject) }) {
                    Icon(
                        Icons.Default.Edit,
                        "Edit",
                        tint = Color.Gray
                    )
                }
                IconButton(onClick = { onDelete(subject) }) {
                    Icon(
                        Icons.Default.Delete,
                        "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (subject.note.isNotBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(topStart = 8.dp),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = subject.note,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildScheduleScreen(viewModel: ScheduleViewModel) {
    val days = listOf("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    var selectedDay by remember { mutableStateOf(days.first()) }
    val subjects by viewModel.subjects.collectAsState()
    val entriesByDay by viewModel.scheduleByDay.collectAsState()
    var showAddEntry by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<ScheduleEntry?>(null) }

    val entries =
        remember(selectedDay, entriesByDay) { entriesByDay[selectedDay.uppercase()] ?: emptyList() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddEntry = true }) {
                Icon(
                    Icons.Default.Add,
                    "Add Class"
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier
            .padding(padding)
            .padding(16.dp)) {
            ScrollableTabRow(selectedTabIndex = days.indexOf(selectedDay), edgePadding = 0.dp) {
                days.forEach { day ->
                    Tab(
                        selected = selectedDay == day,
                        onClick = { selectedDay = day },
                        text = { Text(day.take(3)) })
                }
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries) { (_, subject, entry) ->
                    Card(modifier = Modifier
                        .fillMaxWidth()
                        .clickable { entryToEdit = entry }) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(4.dp, 40.dp)
                                    .background(Color(subject.color), RoundedCornerShape(2.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${entry.startTime} - ${entry.endTime}",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(subject.name)
                            }
                            IconButton(onClick = { entryToEdit = entry }) {
                                Icon(
                                    Icons.Default.Edit,
                                    null,
                                    tint = Color.Gray
                                )
                            }
                            IconButton(onClick = { viewModel.deleteScheduleEntry(entry) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    null,
                                    tint = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddEntry) {
        AddEntryDialog(
            subjects = subjects,
            day = selectedDay,
            onDismiss = { showAddEntry = false },
            onSave = { entry ->
                viewModel.addScheduleEntry(entry)
                showAddEntry = false
            })
    }

    if (entryToEdit != null) {
        AddEntryDialog(
            entry = entryToEdit,
            subjects = subjects,
            day = selectedDay,
            onDismiss = { entryToEdit = null },
            onSave = { entry ->
                viewModel.updateScheduleEntry(entry)
                entryToEdit = null
            })
    }
}

@Composable
fun ManageExamsScreen(viewModel: ScheduleViewModel) {
    val exams by viewModel.examsSorted.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    var showAddExam by remember { mutableStateOf(false) }
    var examToEdit by remember { mutableStateOf<ExamEntry?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                showAddExam = true
            }) { Icon(Icons.Default.Add, "Add Exam") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Exam Routine",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            items(exams) { (subject, exam) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { examToEdit = exam },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(subject.name, fontWeight = FontWeight.Bold)
                            Text("Date: ${exam.date}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Time: ${exam.startTime} - ${exam.endTime}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "Room: ${exam.roomNumber}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = { examToEdit = exam }) {
                            Icon(
                                Icons.Default.Edit,
                                null,
                                tint = Color.Gray
                            )
                        }
                        IconButton(onClick = { viewModel.deleteExamEntry(exam) }) {
                            Icon(
                                Icons.Default.Delete,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddExam) {
        AddExamDialog(subjects = subjects, onDismiss = { showAddExam = false }, onSave = { exam ->
            viewModel.addExamEntry(exam)
            showAddExam = false
        })
    }

    if (examToEdit != null) {
        AddExamDialog(
            exam = examToEdit,
            subjects = subjects,
            onDismiss = { examToEdit = null },
            onSave = { exam ->
                viewModel.updateExamEntry(exam)
                examToEdit = null
            })
    }
}

@Composable
fun ManageClassTestsScreen(viewModel: ScheduleViewModel) {
    val classTests by viewModel.classTestsSorted.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val scheduleEntries by viewModel.allScheduleEntries.collectAsState()
    var showAddCT by remember { mutableStateOf(false) }
    var ctToEdit by remember { mutableStateOf<ClassTestEntry?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = {
                showAddCT = true
            }) { Icon(Icons.Default.Add, "Add Class Test") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    "Class Test Routine",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            items(classTests) { (subject, ct) ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { ctToEdit = ct },
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(subject.name, fontWeight = FontWeight.Bold)
                            Text("Date: ${ct.date}", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Time: ${ct.startTime} - ${ct.endTime}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        IconButton(onClick = { ctToEdit = ct }) {
                            Icon(
                                Icons.Default.Edit,
                                null,
                                tint = Color.Gray
                            )
                        }
                        IconButton(onClick = { viewModel.deleteClassTestEntry(ct) }) {
                            Icon(
                                Icons.Default.Delete,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddCT) {
        AddCTDialog(
            subjects = subjects,
            scheduleEntries = scheduleEntries,
            onDismiss = { showAddCT = false },
            onSave = { ct ->
                viewModel.addClassTestEntry(ct)
                showAddCT = false
            })
    }

    if (ctToEdit != null) {
        AddCTDialog(
            ct = ctToEdit,
            subjects = subjects,
            scheduleEntries = scheduleEntries,
            onDismiss = { ctToEdit = null },
            onSave = { ct ->
                viewModel.updateClassTestEntry(ct)
                ctToEdit = null
            })
    }
}

@Composable
fun AddCourseDialog(
    subject: Subject? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(subject?.name ?: "") }
    var code by remember { mutableStateOf(subject?.code ?: "") }
    var instructor by remember { mutableStateOf(subject?.instructor ?: "") }
    var note by remember { mutableStateOf(subject?.note ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (subject == null) "Add New Course" else "Edit Course") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Course Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Course Code") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = instructor,
                    onValueChange = { instructor = it },
                    label = { Text("Instructor Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note / Tag (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (name.isNotBlank()) onSave(
                    name,
                    code,
                    instructor,
                    note
                )
            }) { Text(if (subject == null) "Add" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun isValidTime(time: String): Boolean = TimeUtils.isValidTime(time)
fun isValidDate(date: String): Boolean =
    date.matches(Regex("^([0-2]?[0-9]|3[01])-(0?[1-9]|1[0-2])(-(\\d{4}))?$"))

fun formatExamDate(date: String): String {
    val parts = date.split("-")
    if (parts.size < 3) {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return "${parts[0].padStart(2, '0')}-${parts[1].padStart(2, '0')}-$currentYear"
    }
    return date
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEntryDialog(
    entry: ScheduleEntry? = null,
    subjects: List<Subject>,
    day: String,
    onDismiss: () -> Unit,
    onSave: (ScheduleEntry) -> Unit
) {
    var selectedSubject by remember {
        mutableStateOf(subjects.find { it.id == entry?.subjectId } ?: subjects.firstOrNull())
    }
    var start by remember { mutableStateOf(entry?.startTime ?: "") }
    var end by remember { mutableStateOf(entry?.endTime ?: "") }
    var room by remember { mutableStateOf(entry?.roomNumber ?: "") }
    var expanded by remember { mutableStateOf(false) }
    var startError by remember { mutableStateOf(false) }
    var endError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry == null) "Add Class for $day" else "Edit Class") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = if (selectedSubject != null) "${selectedSubject!!.name} (${selectedSubject!!.code})" else "Select Course",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }) {
                        subjects.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.name} (${s.code})") },
                                onClick = { selectedSubject = s; expanded = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = start,
                    onValueChange = {
                        start = it; startError = it.isNotEmpty() && !isValidTime(it)
                    },
                    label = { Text("Start (HH:mm)") },
                    isError = startError,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it; endError = it.isNotEmpty() && !isValidTime(it) },
                    label = { Text("End (HH:mm)") },
                    isError = endError,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValidTime(start) && isValidTime(end)) {
                        val formattedStart = TimeUtils.parseAndFormatTime(start)
                        val formattedEnd = TimeUtils.parseAndFormatTime(end); selectedSubject?.let {
                            onSave(
                                entry?.copy(
                                    subjectId = it.id,
                                    startTime = formattedStart,
                                    endTime = formattedEnd,
                                    roomNumber = room
                                ) ?: ScheduleEntry(
                                    subjectId = it.id,
                                    dayOfWeek = day,
                                    startTime = formattedStart,
                                    endTime = formattedEnd,
                                    roomNumber = room
                                )
                            )
                        }
                    }
                },
                enabled = !startError && !endError && start.isNotBlank() && end.isNotBlank()
            ) { Text(if (entry == null) "Save" else "Update") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExamDialog(
    exam: ExamEntry? = null,
    subjects: List<Subject>,
    onDismiss: () -> Unit,
    onSave: (ExamEntry) -> Unit
) {
    var selectedSubject by remember {
        mutableStateOf(subjects.find { it.id == exam?.subjectId } ?: subjects.firstOrNull())
    }
    var date by remember { mutableStateOf(exam?.date ?: "") }
    var start by remember { mutableStateOf(exam?.startTime ?: "") }
    var end by remember { mutableStateOf(exam?.endTime ?: "") }
    var room by remember { mutableStateOf(exam?.roomNumber ?: "") }
    var expanded by remember { mutableStateOf(false) }
    var startError by remember { mutableStateOf(false) }
    var endError by remember { mutableStateOf(false) }
    var dateError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (exam == null) "Add Exam" else "Edit Exam") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = if (selectedSubject != null) "${selectedSubject!!.name} (${selectedSubject!!.code})" else "Select Course",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }) {
                        subjects.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.name} (${s.code})") },
                                onClick = { selectedSubject = s; expanded = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it; dateError = it.isNotEmpty() && !isValidDate(it) },
                    label = { Text("Date (dd-MM or dd-MM-yyyy)") },
                    isError = dateError,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = start,
                    onValueChange = {
                        start = it; startError = it.isNotEmpty() && !isValidTime(it)
                    },
                    label = { Text("Start Time (HH:mm)") },
                    isError = startError,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it; endError = it.isNotEmpty() && !isValidTime(it) },
                    label = { Text("End Time (HH:mm)") },
                    isError = endError,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Room") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isValidTime(start) && isValidTime(end) && isValidDate(
                            date
                        )
                    ) {
                        val finalDate = formatExamDate(date)
                        val formattedStart = TimeUtils.parseAndFormatTime(start)
                        val formattedEnd = TimeUtils.parseAndFormatTime(end); selectedSubject?.let {
                            onSave(
                                exam?.copy(
                                    subjectId = it.id,
                                    date = finalDate,
                                    startTime = formattedStart,
                                    endTime = formattedEnd,
                                    roomNumber = room
                                ) ?: ExamEntry(
                                    subjectId = it.id,
                                    date = finalDate,
                                    startTime = formattedStart,
                                    endTime = formattedEnd,
                                    roomNumber = room
                                )
                            )
                        }
                    }
                },
                enabled = !startError && !endError && !dateError && start.isNotBlank() && end.isNotBlank() && date.isNotBlank()
            ) { Text(if (exam == null) "Add Exam" else "Update Exam") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCTDialog(
    ct: ClassTestEntry? = null,
    subjects: List<Subject>,
    scheduleEntries: List<ScheduleEntry>,
    onDismiss: () -> Unit,
    onSave: (ClassTestEntry) -> Unit
) {
    var selectedSubject by remember {
        mutableStateOf(subjects.find { it.id == ct?.subjectId } ?: subjects.firstOrNull())
    }
    var date by remember { mutableStateOf(ct?.date ?: "") }
    var useCustomTime by remember { mutableStateOf(ct != null) }
    var start by remember { mutableStateOf(ct?.startTime ?: "") }
    var end by remember { mutableStateOf(ct?.endTime ?: "") }
    var expanded by remember { mutableStateOf(false) }
    var dateError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (ct == null) "Add Class Test" else "Edit Class Test") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = if (selectedSubject != null) "${selectedSubject!!.name} (${selectedSubject!!.code})" else "Select Course",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }) {
                        subjects.forEach { s ->
                            DropdownMenuItem(
                                text = { Text("${s.name} (${s.code})") },
                                onClick = { selectedSubject = s; expanded = false })
                        }
                    }
                }
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it; dateError = it.isNotEmpty() && !isValidDate(it) },
                    label = { Text("Date (dd-MM-yyyy)") },
                    isError = dateError,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = useCustomTime, onCheckedChange = { useCustomTime = it })
                    Text("Set custom time?")
                }
                if (useCustomTime) {
                    OutlinedTextField(
                        value = start,
                        onValueChange = { start = it },
                        label = { Text("Start Time (HH:mm)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = end,
                        onValueChange = { end = it },
                        label = { Text("End Time (HH:mm)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (isValidDate(date) && selectedSubject != null) {
                    val finalDate = formatExamDate(date)
                    try {
                        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                        val ctDateObj = sdf.parse(finalDate)
                        val dayOfWeek =
                            SimpleDateFormat("EEEE", Locale.US).format(ctDateObj).uppercase()

                        val entryForDay = scheduleEntries.find {
                            it.subjectId == selectedSubject!!.id && it.dayOfWeek.equals(
                                dayOfWeek,
                                ignoreCase = true
                            )
                        }

                        val finalStart =
                            if (useCustomTime && start.isNotBlank()) TimeUtils.parseAndFormatTime(
                                start
                            ) else entryForDay?.startTime ?: ""
                        val finalEnd =
                            if (useCustomTime && end.isNotBlank()) TimeUtils.parseAndFormatTime(end) else entryForDay?.endTime
                                ?: ""

                        onSave(
                            ct?.copy(
                                subjectId = selectedSubject!!.id,
                                date = finalDate,
                                startTime = finalStart,
                                endTime = finalEnd,
                                roomNumber = ""
                            ) ?: ClassTestEntry(
                                subjectId = selectedSubject!!.id,
                                date = finalDate,
                                startTime = finalStart,
                                endTime = finalEnd,
                                roomNumber = ""
                            )
                        )
                    } catch (e: Exception) {
                    }
                }
            }) { Text(if (ct == null) "Add" else "Save") }
        }
    )
}
