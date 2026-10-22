package com.monayem.crono

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monayem.crono.data.AppDatabase
import com.monayem.crono.data.ClassTestEntry
import com.monayem.crono.data.ConflictPreferenceManager
import com.monayem.crono.data.ExamEntry
import com.monayem.crono.data.ScheduleEntry
import com.monayem.crono.data.Subject
import com.monayem.crono.data.TimeUtils
import com.monayem.crono.data.UnifiedEvent
import com.monayem.crono.ui.schedule.ScheduleViewModel
import com.monayem.crono.ui.schedule.ScheduleViewModelFactory
import com.monayem.crono.ui.theme.CronoTheme
import com.monayem.crono.ui.theme.LocalFontSizes
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: ScheduleViewModel by viewModels {
        ScheduleViewModelFactory(AppDatabase.getDatabase(this).scheduleDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Crono)
        super.onCreate(savedInstanceState)
        setContent {
            CronoTheme {
                val schedule by viewModel.schedule.collectAsState()
                val scheduleByDay by viewModel.scheduleByDay.collectAsState()
                val exams by viewModel.examsSorted.collectAsState()
                val classTests by viewModel.classTestsSorted.collectAsState()
                val legendSubjects by viewModel.legendSubjects.collectAsState()
                val allScheduleEntries by viewModel.allScheduleEntries.collectAsState()

                val showAllCTsFromIntent = remember {
                    mutableStateOf(intent?.getBooleanExtra("SHOW_ALL_CTS", false) ?: false)
                }

                MainScreen(
                    viewModel,
                    schedule?.scheduleName ?: "Crono",
                    scheduleByDay,
                    exams,
                    classTests,
                    legendSubjects,
                    allScheduleEntries,
                    showAllCTsFromIntent
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    viewModel: ScheduleViewModel,
    titleText: String,
    scheduleByDay: Map<String, List<Triple<String, Subject, ScheduleEntry>>>,
    exams: List<Pair<Subject, ExamEntry>>,
    classTests: List<Pair<Subject, ClassTestEntry>>,
    legendSubjects: List<Subject>,
    allScheduleEntries: List<ScheduleEntry>,
    showAllCTsInitial: MutableState<Boolean>
) {
    val context = LocalContext.current
    val fontSizes = LocalFontSizes.current
    var isShortView by remember { mutableStateOf(true) }
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedSubjectDetails by remember { mutableStateOf<Subject?>(null) }

    var showCTConfirmation by remember { mutableStateOf<Pair<Subject, ClassTestEntry>?>(null) }
    var showAllCTsDialog by remember { mutableStateOf(showAllCTsInitial.value) }

    var dismissedClusterIds by remember {
        mutableStateOf(ConflictPreferenceManager.getSavedDismissedSet(context))
    }
    var resolvedClusterSelections by remember {
        mutableStateOf(ConflictPreferenceManager.getSavedSelectionsMap(context))
    }
    var activeConflictDialogCluster by remember { mutableStateOf<List<UnifiedEvent>?>(null) }
    var postSelectionNotice by remember {
        mutableStateOf<Pair<UnifiedEvent, List<UnifiedEvent>>?>(
            null
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            currentTime = Calendar.getInstance()
        }
    }

    val todayDateStr = SimpleDateFormat("dd-MM-yyyy", Locale.US).format(currentTime.time)

    val todayEvents = remember(scheduleByDay, classTests, exams, currentTime) {
        val currentDayName =
            SimpleDateFormat("EEEE", Locale.US).format(currentTime.time).uppercase()
        val list = mutableListOf<UnifiedEvent>()

        scheduleByDay[currentDayName]?.forEach { (_, subject, entry) ->
            list.add(
                UnifiedEvent(
                    id = "CLASS_${entry.id}",
                    type = UnifiedEvent.EventType.CLASS,
                    subjectName = subject.name,
                    subjectCode = subject.code,
                    startTime = entry.startTime,
                    endTime = entry.endTime,
                    dateOrDay = currentDayName,
                    originalSubject = subject,
                    originalEntry = entry
                )
            )
        }

        classTests.filter { it.second.date == todayDateStr }.forEach { (subject, ct) ->
            val displayStart = if (ct.startTime.isNotBlank()) ct.startTime else {
                val entry = allScheduleEntries.find {
                    it.subjectId == subject.id && it.dayOfWeek.equals(
                        currentDayName,
                        ignoreCase = true
                    )
                }
                entry?.startTime ?: "09:00"
            }
            val displayEnd = if (ct.endTime.isNotBlank()) ct.endTime else {
                val entry = allScheduleEntries.find {
                    it.subjectId == subject.id && it.dayOfWeek.equals(
                        currentDayName,
                        ignoreCase = true
                    )
                }
                entry?.endTime ?: "10:00"
            }
            list.add(
                UnifiedEvent(
                    id = "CT_${ct.id}",
                    type = UnifiedEvent.EventType.CT,
                    subjectName = subject.name,
                    subjectCode = subject.code,
                    startTime = displayStart,
                    endTime = displayEnd,
                    dateOrDay = todayDateStr,
                    originalSubject = subject,
                    originalEntry = ct
                )
            )
        }

        exams.filter { it.second.date == todayDateStr }.forEach { (subject, exam) ->
            list.add(
                UnifiedEvent(
                    id = "EXAM_${exam.id}",
                    type = UnifiedEvent.EventType.EXAM,
                    subjectName = subject.name,
                    subjectCode = subject.code,
                    startTime = exam.startTime,
                    endTime = exam.endTime,
                    dateOrDay = todayDateStr,
                    originalSubject = subject,
                    originalEntry = exam
                )
            )
        }

        list
    }

    val nowMinutes = remember(currentTime) {
        currentTime.get(Calendar.HOUR_OF_DAY) * 60 + currentTime.get(Calendar.MINUTE)
    }

    val activeConflictClusters = remember(todayEvents, nowMinutes) {
        val allClusters = TimeUtils.findConflictingEventClusters(todayEvents)

        allClusters.mapNotNull { cluster ->
            val remainingActive = cluster.filter { event ->
                val endMin = TimeUtils.getMinutesOfDay(event.endTime) ?: 0
                nowMinutes < endMin
            }

            if (remainingActive.isEmpty()) {
                null
            } else {
                val minStartMin =
                    remainingActive.mapNotNull { TimeUtils.getMinutesOfDay(it.startTime) }
                        .minOrNull() ?: 0
                val diffMin = minStartMin - nowMinutes
                if (diffMin <= 60) remainingActive else null
            }
        }.filter { it.isNotEmpty() }
    }

    LaunchedEffect(
        activeConflictClusters,
        nowMinutes,
        dismissedClusterIds,
        resolvedClusterSelections
    ) {
        for (cluster in activeConflictClusters) {
            val fullClusterId =
                "${todayDateStr}_" + cluster.map { it.id }.sorted().joinToString("_")

            if (cluster.size == 1) {
                val singleActiveEvent = cluster.first()
                if (resolvedClusterSelections[fullClusterId] != singleActiveEvent.id) {
                    ConflictPreferenceManager.saveResolvedChoice(
                        context,
                        fullClusterId,
                        singleActiveEvent.id
                    )
                    resolvedClusterSelections =
                        resolvedClusterSelections + (fullClusterId to singleActiveEvent.id)
                }
            } else if (cluster.size >= 2) {
                val isResolved = resolvedClusterSelections.containsKey(fullClusterId)
                val isDismissed = dismissedClusterIds.contains(fullClusterId)

                if (!isResolved && !isDismissed && activeConflictDialogCluster == null && postSelectionNotice == null) {
                    activeConflictDialogCluster = cluster
                }
            }
        }
    }

    val dismissedConflictCluster =
        remember(activeConflictClusters, dismissedClusterIds, resolvedClusterSelections) {
            activeConflictClusters.find { cluster ->
                if (cluster.size < 2) return@find false
                val clusterId =
                    "${todayDateStr}_" + cluster.map { it.id }.sorted().joinToString("_")
                dismissedClusterIds.contains(clusterId) && !resolvedClusterSelections.containsKey(
                    clusterId
                )
            }
        }

    var activeFollowUpCluster by remember {
        mutableStateOf<Triple<String, UnifiedEvent, List<UnifiedEvent>>?>(
            null
        )
    }

    LaunchedEffect(todayEvents, nowMinutes, resolvedClusterSelections) {
        val allClustersToday = TimeUtils.findConflictingEventClusters(todayEvents)

        for (cluster in allClustersToday) {
            val fullClusterId =
                "${todayDateStr}_" + cluster.map { it.id }.sorted().joinToString("_")
            val chosenId = resolvedClusterSelections[fullClusterId]

            if (chosenId != null && chosenId != "NONE") {
                val chosenEvent = cluster.find { it.id == chosenId }
                if (chosenEvent != null) {
                    val chosenEndMin = TimeUtils.getMinutesOfDay(chosenEvent.endTime) ?: 0

                    if (nowMinutes >= chosenEndMin) {
                        val ongoingAlternatives = cluster.filter { event ->
                            if (event.id == chosenId) return@filter false
                            val startMin = TimeUtils.getMinutesOfDay(event.startTime) ?: 0
                            val endMin = TimeUtils.getMinutesOfDay(event.endTime) ?: 0
                            nowMinutes >= startMin && nowMinutes < endMin
                        }

                        if (ongoingAlternatives.isNotEmpty()) {
                            val handled = ConflictPreferenceManager.isSwitchedPromptHandled(
                                context,
                                fullClusterId
                            )
                            val dismissed = ConflictPreferenceManager.isSwitchedPromptDismissed(
                                context,
                                fullClusterId
                            )

                            if (!handled && !dismissed && activeFollowUpCluster == null && activeConflictDialogCluster == null && postSelectionNotice == null) {
                                activeFollowUpCluster =
                                    Triple(fullClusterId, chosenEvent, ongoingAlternatives)
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    val dismissedFollowUpCluster = remember(todayEvents, nowMinutes, resolvedClusterSelections) {
        val allClustersToday = TimeUtils.findConflictingEventClusters(todayEvents)
        var result: Triple<String, UnifiedEvent, List<UnifiedEvent>>? = null

        for (cluster in allClustersToday) {
            val fullClusterId =
                "${todayDateStr}_" + cluster.map { it.id }.sorted().joinToString("_")
            val chosenId = resolvedClusterSelections[fullClusterId]

            if (chosenId != null && chosenId != "NONE") {
                val chosenEvent = cluster.find { it.id == chosenId }
                if (chosenEvent != null) {
                    val chosenEndMin = TimeUtils.getMinutesOfDay(chosenEvent.endTime) ?: 0

                    if (nowMinutes >= chosenEndMin) {
                        val ongoingAlternatives = cluster.filter { event ->
                            if (event.id == chosenId) return@filter false
                            val startMin = TimeUtils.getMinutesOfDay(event.startTime) ?: 0
                            val endMin = TimeUtils.getMinutesOfDay(event.endTime) ?: 0
                            nowMinutes >= startMin && nowMinutes < endMin
                        }

                        if (ongoingAlternatives.isNotEmpty()) {
                            val dismissed = ConflictPreferenceManager.isSwitchedPromptDismissed(
                                context,
                                fullClusterId
                            )
                            val handled = ConflictPreferenceManager.isSwitchedPromptHandled(
                                context,
                                fullClusterId
                            )
                            if (dismissed && !handled) {
                                result = Triple(fullClusterId, chosenEvent, ongoingAlternatives)
                                break
                            }
                        }
                    }
                }
            }
        }
        result
    }

    LaunchedEffect(classTests, currentTime) {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        val pendingCT = classTests.find { (_, ct) ->
            if (ct.isConfirmed) return@find false
            try {
                val ctDate = sdf.parse(ct.date)!!
                val today = sdf.parse(todayDateStr)!!
                ctDate.before(today) || if (ct.date == todayDateStr) isTimePassed(ct.startTime, currentTime)
                else false
            } catch (e: Exception) {
                false
            }
        }
        if (pendingCT != null) showCTConfirmation = pendingCT
    }

    val examInfo = remember(exams, currentTime) {
        if (exams.isEmpty()) return@remember null
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(
            Calendar.SECOND,
            0
        ); set(Calendar.MILLISECOND, 0)
        }
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
        val examDates = exams.mapNotNull { (_, exam) ->
            try {
                Calendar.getInstance().apply {
                    time = sdf.parse(exam.date)!!; set(
                    Calendar.HOUR_OF_DAY,
                    0
                ); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
            } catch (e: Exception) {
                null
            }
        }.sorted()
        if (examDates.isEmpty()) return@remember null
        val firstExam = examDates.first()
        val lastExam = examDates.last()
        val nextExam = examDates.find { it.timeInMillis >= today.timeInMillis }
        val daysLeft =
            nextExam?.let { ((it.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)).toInt() }
                ?: -1
        val isInExamPeriod = today.timeInMillis in firstExam.timeInMillis..lastExam.timeInMillis
        Triple(daysLeft, isInExamPeriod, nextExam != null)
    }

    val daysUntilExam = examInfo?.first ?: -1
    val isInExamPeriod = examInfo?.second ?: false
    val isExamModeActive = isInExamPeriod || (daysUntilExam in 0..2)
    val showExamNotification = !isExamModeActive && (daysUntilExam in 3..7)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(titleText, style = MaterialTheme.typography.titleLarge)
                        if (isExamModeActive) Text(
                            "EXAM ROUTINE ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        context.startActivity(
                            Intent(
                                context,
                                SecondActivity::class.java
                            )
                        )
                    }) { Icon(Icons.Filled.Edit, contentDescription = "Manage") }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier
            .fillMaxSize()
            .padding(padding)) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (dismissedFollowUpCluster != null) {
                    val (_, endedEvent, ongoingAlternatives) = dismissedFollowUpCluster
                    ConflictBanner(
                        message = "${endedEvent.subjectName} ended. ${ongoingAlternatives.first().subjectName} is still ongoing. Tap to update choice.",
                        onClick = {
                            activeFollowUpCluster = dismissedFollowUpCluster
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                } else if (dismissedConflictCluster != null) {
                    ConflictBanner(
                        message = "Conflict at ${dismissedConflictCluster.first().startTime}: ${dismissedConflictCluster.size} overlapping events. Tap to choose.",
                        onClick = {
                            activeConflictDialogCluster = dismissedConflictCluster
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                }

                if (isExamModeActive) {
                    Text(
                        "Exam Routine",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        exams.forEach { (subject, exam) ->
                            ExamCard(
                                subject,
                                exam,
                                onDetailsClick = { selectedSubjectDetails = it })
                        }
                    }
                } else {
                    if (showExamNotification) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Max),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ExamCountdownCard(daysLeft = daysUntilExam)
                            }
                            if (classTests.isNotEmpty()) {
                                Spacer(Modifier.width(8.dp))
                                Surface(
                                    onClick = { showAllCTsDialog = true },
                                    color = Color(0xFF3D5A80),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxHeight()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "CT",
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = fontSizes.bodyLarge.sp
                                        )
                                    }
                                }
                            }
                        }
                        if (classTests.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    if (classTests.isNotEmpty()) {
                        classTests.take(1).forEach { (subject, ct) ->
                            CTCard(
                                subject,
                                ct,
                                allScheduleEntries,
                                showViewAll = true,
                                onViewAllClick = { showAllCTsDialog = true },
                                onDetailsClick = { selectedSubjectDetails = it })
                        }
                        Spacer(Modifier.height(16.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) { SegmentedButton(isShortView, onToggle = { isShortView = it }) }
                    Spacer(Modifier.height(16.dp))

                    val currentDay =
                        SimpleDateFormat("EEEE", Locale.US).format(currentTime.time).uppercase()
                    val displayedSchedule =
                        if (isShortView) scheduleByDay.filter { it.key == currentDay } else scheduleByDay

                    if (displayedSchedule.isEmpty()) {
                        EmptyStateCard(if (isShortView) "No classes today!" else "Schedule is empty.")
                    } else {
                        LazyRow(
                            horizontalArrangement = if (isShortView) Arrangement.Center else Arrangement.spacedBy(
                                16.dp
                            ), modifier = if (isShortView) Modifier.fillMaxWidth() else Modifier
                        ) {
                            items(displayedSchedule.entries.toList()) { (day, entries) ->
                                DayColumn(
                                    day = day,
                                    entries = entries,
                                    currentTime = currentTime,
                                    currentDay = currentDay,
                                    todayDateStr = todayDateStr,
                                    classTests = classTests,
                                    isDailyView = isShortView,
                                    attendingEventMap = resolvedClusterSelections,
                                    allTodayEvents = todayEvents,
                                    onDetailsClick = { selectedSubjectDetails = it }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }

            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                Legend(legendSubjects)
                Spacer(Modifier.height(8.dp))
                Text("Monayem", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
    }

    if (showAllCTsDialog) AllCTsDialog(
        classTests = classTests,
        allScheduleEntries = allScheduleEntries,
        onDismiss = { showAllCTsDialog = false },
        onDetailsClick = { selectedSubjectDetails = it })
    if (selectedSubjectDetails != null) SubjectDetailsDialog(
        subject = selectedSubjectDetails!!,
        onDismiss = { selectedSubjectDetails = null })
    if (showCTConfirmation != null) CTConfirmationDialog(
        subjectAndCT = showCTConfirmation!!,
        viewModel = viewModel,
        onDismiss = { showCTConfirmation = null })

    if (activeConflictDialogCluster != null) {
        val cluster = activeConflictDialogCluster!!
        val clusterId = "${todayDateStr}_" + cluster.map { it.id }.sorted().joinToString("_")
        ConflictResolutionDialog(
            cluster = cluster,
            onSelectEvent = { chosen ->
                ConflictPreferenceManager.saveResolvedChoice(context, clusterId, chosen.id)
                resolvedClusterSelections = resolvedClusterSelections + (clusterId to chosen.id)
                activeConflictDialogCluster = null
                val unselected = cluster.filter { it.id != chosen.id }
                postSelectionNotice = chosen to unselected
            },
            onDismiss = {
                ConflictPreferenceManager.saveDismissedCluster(context, clusterId)
                dismissedClusterIds = dismissedClusterIds + clusterId
                activeConflictDialogCluster = null
            }
        )
    }

    if (postSelectionNotice != null) {
        val (selectedEvent, unselectedEvents) = postSelectionNotice!!
        ConflictResolvedNoticeDialog(
            selectedEvent = selectedEvent,
            unselectedEvents = unselectedEvents,
            onDismiss = { postSelectionNotice = null },
            onRescheduleUnselected = { unselected ->
                postSelectionNotice = null
                if (unselected.type == UnifiedEvent.EventType.CT) {
                    val ct = unselected.originalEntry as ClassTestEntry
                    val subject = unselected.originalSubject
                    showCTConfirmation = subject to ct
                } else {
                    context.startActivity(Intent(context, SecondActivity::class.java))
                }
            }
        )
    }

    if (activeFollowUpCluster != null) {
        val (clusterId, endedEvent, ongoingAlternatives) = activeFollowUpCluster!!
        FollowUpSwitchDialog(
            endedEvent = endedEvent,
            ongoingAlternatives = ongoingAlternatives,
            onSelectOption = { chosen ->
                activeFollowUpCluster = null
                ConflictPreferenceManager.saveSwitchedPromptHandled(context, clusterId)
                if (chosen != null) {
                    ConflictPreferenceManager.saveResolvedChoice(context, clusterId, chosen.id)
                    resolvedClusterSelections = resolvedClusterSelections + (clusterId to chosen.id)
                } else {
                    ConflictPreferenceManager.saveResolvedChoice(context, clusterId, "NONE")
                    resolvedClusterSelections = resolvedClusterSelections + (clusterId to "NONE")
                }
            },
            onDismiss = {
                activeFollowUpCluster = null
                ConflictPreferenceManager.saveSwitchedPromptDismissed(context, clusterId)
            }
        )
    }
}

@Composable
fun CTConfirmationDialog(
    subjectAndCT: Pair<Subject, ClassTestEntry>,
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit
) {
    val (subject, ct) = subjectAndCT
    val fontSizes = LocalFontSizes.current
    var showRescheduleDialog by remember { mutableStateOf(false) }

    if (!showRescheduleDialog) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("Class Test Confirmation", fontSize = fontSizes.titleMedium.sp) },
            text = {
                Text(
                    "Was the Class Test for ${subject.name} (on ${ct.date}) taken?",
                    fontSize = fontSizes.bodyMedium.sp
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.deleteClassTestEntry(ct); onDismiss() }) {
                    Text(
                        "Yes, Taken",
                        fontSize = fontSizes.bodyMedium.sp
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRescheduleDialog = true
                }) { Text("Reschedule", fontSize = fontSizes.bodyMedium.sp) }
            }
        )
    } else {
        var newDate by remember { mutableStateOf(ct.date) }
        var showTimeInput by remember { mutableStateOf(false) }
        var newTime by remember { mutableStateOf(ct.startTime) }
        var newEndTime by remember { mutableStateOf(ct.endTime) }

        AlertDialog(
            onDismissRequest = { showRescheduleDialog = false },
            title = { Text("Reschedule Class Test", fontSize = fontSizes.titleMedium.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Rescheduling for ${subject.name}", fontSize = fontSizes.bodyMedium.sp)
                    OutlinedTextField(
                        value = newDate,
                        onValueChange = { newDate = it },
                        label = { Text("Date (dd-MM-yyyy)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (showTimeInput) {
                        OutlinedTextField(
                            value = newTime,
                            onValueChange = { newTime = it },
                            label = { Text("Start Time (HH:mm)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newEndTime,
                            onValueChange = { newEndTime = it },
                            label = { Text("End Time (HH:mm)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newDate.isNotBlank()) {
                        val formattedStart = TimeUtils.parseAndFormatTime(newTime)
                        val formattedEnd =
                            TimeUtils.parseAndFormatTime(newEndTime); viewModel.updateClassTestEntry(
                            ct.copy(
                                date = newDate,
                                startTime = formattedStart,
                                endTime = formattedEnd,
                                isConfirmed = false
                            )
                        ); showRescheduleDialog = false; onDismiss()
                    }
                }) { Text("Confirm", fontSize = fontSizes.bodyMedium.sp) }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        showTimeInput = !showTimeInput
                    }) {
                        Text(
                            if (showTimeInput) "Hide Time" else "Set Time",
                            fontSize = fontSizes.bodyMedium.sp
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { showRescheduleDialog = false }) {
                        Text(
                            "Back",
                            fontSize = fontSizes.bodyMedium.sp
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun AllCTsDialog(
    classTests: List<Pair<Subject, ClassTestEntry>>,
    allScheduleEntries: List<ScheduleEntry>,
    onDismiss: () -> Unit,
    onDetailsClick: (Subject) -> Unit
) {
    val fontSizes = LocalFontSizes.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("All Class Tests", fontSize = fontSizes.titleMedium.sp) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (classTests.isEmpty()) Text(
                    "No class tests scheduled.",
                    fontSize = fontSizes.bodyMedium.sp
                )
                else classTests.forEach { (subject, ct) ->
                    CTCard(
                        subject,
                        ct,
                        allScheduleEntries,
                        onDetailsClick = { onDetailsClick(it); onDismiss() })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "Close",
                    fontSize = fontSizes.bodyMedium.sp
                )
            }
        }
    )
}

@Composable
fun CTCard(
    subject: Subject,
    ct: ClassTestEntry,
    allScheduleEntries: List<ScheduleEntry>,
    showViewAll: Boolean = false,
    onViewAllClick: () -> Unit = {},
    onDetailsClick: (Subject) -> Unit
) {
    val fontSizes = LocalFontSizes.current
    val displayTime = remember(ct, allScheduleEntries) {
        if (ct.startTime.isNotBlank()) {
            if (ct.endTime.isNotBlank()) "${ct.startTime} - ${ct.endTime}" else ct.startTime
        } else {
            try {
                val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                val ctDate = sdf.parse(ct.date)!!
                val dayOfWeek = SimpleDateFormat("EEEE", Locale.US).format(ctDate).uppercase()
                val entry =
                    allScheduleEntries.find { it.subjectId == subject.id && it.dayOfWeek.uppercase() == dayOfWeek }
                if (entry != null) "${entry.startTime} - ${entry.endTime}" else ""
            } catch (e: Exception) {
                ""
            }
        }
    }

    val remainingDaysText = remember(ct.date) {
        try {
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
            val ctDate = sdf.parse(ct.date)!!
            val todayCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val ctCal = Calendar.getInstance().apply {
                time = ctDate
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val diffDays =
                ((ctCal.timeInMillis - todayCal.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
            when {
                diffDays < 0 -> "Passed"
                diffDays == 0 -> "Today"
                else -> "${diffDays}d"
            }
        } catch (e: Exception) {
            "CT"
        }
    }

    val cardColor = remember(ct.id, subject.id) {
        val palette = listOf(
            Color(0xFF3D5A80), // Steel Blue
            Color(0xFF2E6B60), // Muted Teal
            Color(0xFF5C527F), // Soft Purple / Dusty Lavender
            Color(0xFF52796F), // Muted Sage
            Color(0xFF457B9D), // Dusty Cyan
            Color(0xFF6B4E71), // Muted Plum
            Color(0xFF3B5998), // Soft Indigo
            Color(0xFF2B4C7E)  // Navy Blue
        )
        val hash = (ct.id * 31 + subject.id).let { if (it < 0) -it else it }
        palette[hash % palette.size]
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetailsClick(subject) },
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = Color.White.copy(alpha = 0.25f), shape = RoundedCornerShape(6.dp)) {
                Text(
                    text = remainingDaysText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = (fontSizes.labelSmall * 0.9f).sp,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    subject.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSizes.titleMedium.sp,
                    color = Color.White
                )
                val infoText =
                    if (displayTime.isNotBlank()) "Date: ${ct.date} | Time: $displayTime" else "Date: ${ct.date}"
                Text(
                    infoText,
                    fontSize = fontSizes.bodySmall.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Normal
                )
            }
            if (showViewAll) {
                Surface(
                    color = Color.White.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(6.dp),
                    onClick = onViewAllClick
                ) {
                    Text(
                        "View All",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = fontSizes.bodySmall.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ExamCountdownCard(daysLeft: Int) {
    val fontSizes = LocalFontSizes.current
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            ); Spacer(Modifier.width(12.dp))
            Text(
                text = "Exam in $daysLeft days!",
                fontSize = fontSizes.bodyLarge.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
fun ExamCard(subject: Subject, exam: ExamEntry, onDetailsClick: (Subject) -> Unit) {
    val fontSizes = LocalFontSizes.current
    val backgroundColor = Color(subject.color)
    val contentColor = if (backgroundColor.luminance() > 0.5f) Color.Black else Color.White
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetailsClick(subject) },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    exam.date,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    fontSize = fontSizes.bodyLarge.sp
                ); Text(
                "EXAM",
                color = contentColor.copy(alpha = 0.7f),
                fontSize = fontSizes.labelSmall.sp
            )
            }
            Text(
                subject.name.uppercase(),
                fontWeight = FontWeight.Black,
                color = contentColor,
                fontSize = fontSizes.titleLarge.sp
            )
            Text(
                "${exam.startTime} - ${exam.endTime} | Room: ${exam.roomNumber}",
                color = contentColor.copy(alpha = 0.9f),
                fontSize = fontSizes.bodyLarge.sp
            )
        }
    }
}

@Composable
fun EmptyStateCard(message: String) {
    val fontSizes = LocalFontSizes.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = fontSizes.bodyLarge.sp
            )
        }
    }
}

@Composable
fun ConflictResolutionDialog(
    cluster: List<UnifiedEvent>,
    onSelectEvent: (UnifiedEvent) -> Unit,
    onDismiss: () -> Unit
) {
    val fontSizes = LocalFontSizes.current
    var selectedEvent by remember { mutableStateOf<UnifiedEvent?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Schedule Conflict Detected",
                    fontSize = fontSizes.titleMedium.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "You have ${cluster.size} events scheduled at the same time (${cluster.first().startTime} - ${cluster.first().endTime}). Which one will you attend?",
                    fontSize = fontSizes.bodyMedium.sp
                )
                cluster.forEach { event ->
                    val isSelected = selectedEvent?.id == event.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedEvent = event },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (isSelected) BorderStroke(
                            2.dp,
                            MaterialTheme.colorScheme.primary
                        ) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = { selectedEvent = event }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "${event.subjectName} (${event.subjectCode})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = fontSizes.bodyLarge.sp
                                )
                                Text(
                                    "Type: ${event.type.name} | Time: ${event.startTime} - ${event.endTime}",
                                    fontSize = fontSizes.bodySmall.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedEvent?.let { onSelectEvent(it) } },
                enabled = selectedEvent != null
            ) {
                Text("Confirm Selection", fontSize = fontSizes.bodyMedium.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not sure right now", fontSize = fontSizes.bodyMedium.sp)
            }
        }
    )
}

@Composable
fun ConflictResolvedNoticeDialog(
    selectedEvent: UnifiedEvent,
    unselectedEvents: List<UnifiedEvent>,
    onDismiss: () -> Unit,
    onRescheduleUnselected: (UnifiedEvent) -> Unit
) {
    val fontSizes = LocalFontSizes.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Choice Recorded",
                fontSize = fontSizes.titleMedium.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "You selected to attend ${selectedEvent.subjectName} (${selectedEvent.type.name}).",
                    fontSize = fontSizes.bodyMedium.sp,
                    fontWeight = FontWeight.SemiBold
                )
                val unselectedNames =
                    unselectedEvents.joinToString { "${it.subjectName} (${it.type.name})" }
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Notice: If $unselectedNames can be rescheduled, please make sure to add or update its schedule manually.",
                        modifier = Modifier.padding(12.dp),
                        fontSize = fontSizes.bodySmall.sp,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Got It", fontSize = fontSizes.bodyMedium.sp)
            }
        },
        dismissButton = {
            if (unselectedEvents.isNotEmpty()) {
                TextButton(onClick = {
                    onDismiss()
                    onRescheduleUnselected(unselectedEvents.first())
                }) {
                    Text("Reschedule Unselected", fontSize = fontSizes.bodyMedium.sp)
                }
            }
        }
    )
}

@Composable
fun ConflictBanner(message: String, onClick: () -> Unit) {
    val fontSizes = LocalFontSizes.current
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "Conflict",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = message,
                    fontSize = fontSizes.bodyMedium.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Text(
                text = "Choose",
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSizes.bodySmall.sp,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun FollowUpSwitchDialog(
    endedEvent: UnifiedEvent,
    ongoingAlternatives: List<UnifiedEvent>,
    onSelectOption: (UnifiedEvent?) -> Unit,
    onDismiss: () -> Unit
) {
    val fontSizes = LocalFontSizes.current
    var selectedOption by remember { mutableStateOf<UnifiedEvent?>(null) }
    var isNoneSelected by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Class Ended: Switch Attendance?",
                    fontSize = fontSizes.titleMedium.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Dismiss")
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Your class ${endedEvent.subjectName} (${endedEvent.startTime} - ${endedEvent.endTime}) has ended. The following class(es) are still ongoing:",
                    fontSize = fontSizes.bodyMedium.sp
                )
                ongoingAlternatives.forEach { event ->
                    val isSelected = !isNoneSelected && selectedOption?.id == event.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedOption = event
                                isNoneSelected = false
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = if (isSelected) BorderStroke(
                            2.dp,
                            MaterialTheme.colorScheme.primary
                        ) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedOption = event
                                    isNoneSelected = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "${event.subjectName} (${event.subjectCode})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = fontSizes.bodyLarge.sp
                                )
                                Text(
                                    "Ongoing until ${event.endTime}",
                                    fontSize = fontSizes.bodySmall.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedOption = null
                            isNoneSelected = true
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isNoneSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = if (isNoneSelected) BorderStroke(
                        2.dp,
                        MaterialTheme.colorScheme.primary
                    ) else null
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isNoneSelected,
                            onClick = {
                                selectedOption = null
                                isNoneSelected = true
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "I didn't join any other class",
                            fontWeight = FontWeight.Medium,
                            fontSize = fontSizes.bodyMedium.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isNoneSelected) {
                        onSelectOption(null)
                    } else if (selectedOption != null) {
                        onSelectOption(selectedOption)
                    }
                },
                enabled = isNoneSelected || selectedOption != null
            ) {
                Text("Confirm", fontSize = fontSizes.bodyMedium.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not sure right now", fontSize = fontSizes.bodyMedium.sp)
            }
        }
    )
}

fun getDateForDayOfWeek(dayName: String, currentTime: Calendar): String {
    val sdfDay = SimpleDateFormat("EEEE", Locale.US)
    val sdfDate = SimpleDateFormat("dd-MM-yyyy", Locale.US)
    val currentWeek = currentTime.get(Calendar.WEEK_OF_YEAR)
    val currentYear = currentTime.get(Calendar.YEAR)

    for (offset in -7..7) {
        val testCal = (currentTime.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, offset)
        }
        if (sdfDay.format(testCal.time).equals(dayName, ignoreCase = true)) {
            if (testCal.get(Calendar.WEEK_OF_YEAR) == currentWeek &&
                testCal.get(Calendar.YEAR) == currentYear
            ) {
                return sdfDate.format(testCal.time)
            }
        }
    }
    return ""
}

@Composable
fun DayColumn(
    day: String,
    entries: List<Triple<String, Subject, ScheduleEntry>>,
    currentTime: Calendar,
    currentDay: String,
    todayDateStr: String,
    classTests: List<Pair<Subject, ClassTestEntry>>,
    isDailyView: Boolean,
    attendingEventMap: Map<String, String>,
    allTodayEvents: List<UnifiedEvent>,
    onDetailsClick: (Subject) -> Unit
) {
    val fontSizes = LocalFontSizes.current
    val columnDateStr = remember(day, currentTime) {
        getDateForDayOfWeek(day, currentTime)
    }
    Column(modifier = Modifier.width(if (isDailySize(isDailyView)) 320.dp else 260.dp)) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                day,
                modifier = Modifier.padding(12.dp),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = fontSizes.bodyLarge.sp
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            entries.sortedBy { it.third.startTime }.forEach { (_, subject, entry) ->
                val hasCTOnThisDay = classTests.any { (ctSubject, ct) ->
                    ctSubject.id == subject.id && ct.date == columnDateStr
                }

                val currentEvent = UnifiedEvent(
                    id = "CLASS_${entry.id}",
                    type = UnifiedEvent.EventType.CLASS,
                    subjectName = subject.name,
                    subjectCode = subject.code,
                    startTime = entry.startTime,
                    endTime = entry.endTime,
                    dateOrDay = day,
                    originalSubject = subject,
                    originalEntry = entry
                )

                val isToday = day.equals(currentDay, ignoreCase = true)
                val conflictingOthers = if (isToday) allTodayEvents.filter {
                    it.id != currentEvent.id && TimeUtils.doEventsOverlap(
                        it,
                        currentEvent
                    )
                } else emptyList()
                val hasTimeConflict = conflictingOthers.isNotEmpty()

                val cluster = (listOf(currentEvent) + conflictingOthers).distinctBy { it.id }
                val clusterId =
                    "${todayDateStr}_" + cluster.map { it.id }.sorted().joinToString("_")
                val chosenEventId = if (hasTimeConflict) attendingEventMap[clusterId] else null

                ScheduleCard(
                    subject = subject,
                    entry = entry,
                    currentTime = currentTime,
                    currentDay = currentDay,
                    hasCTToday = hasCTOnThisDay,
                    isDailyView = isDailyView,
                    attendingEventId = chosenEventId,
                    hasTimeConflict = hasTimeConflict,
                    onDetailsClick = onDetailsClick
                )
            }
        }
    }
}

@Composable
fun ScheduleCard(
    subject: Subject,
    entry: ScheduleEntry,
    currentTime: Calendar,
    currentDay: String,
    hasCTToday: Boolean,
    isDailyView: Boolean,
    attendingEventId: String? = null,
    hasTimeConflict: Boolean = false,
    onDetailsClick: (Subject) -> Unit
) {
    val fontSizes = LocalFontSizes.current
    val isRunningTime = entry.dayOfWeek.equals(
        currentDay,
        ignoreCase = true
    ) && isCurrentTimeInRange(entry.startTime, entry.endTime, currentTime)
    val isRunning = isRunningTime && if (hasTimeConflict) {
        attendingEventId == "CLASS_${entry.id}"
    } else {
        true
    }
    val color = Color(subject.color)
    val contentColor = if (color.luminance() > 0.5f) Color.Black else Color.White
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onDetailsClick(subject) }) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "${entry.startTime} - ${entry.endTime}",
                    color = contentColor.copy(alpha = 0.8f),
                    fontSize = fontSizes.bodyLarge.sp
                )
                Text(
                    subject.name,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    fontSize = fontSizes.titleMedium.sp
                )
                Text(
                    "Room: ${entry.roomNumber}",
                    color = contentColor.copy(alpha = 0.8f),
                    fontSize = fontSizes.bodyMedium.sp
                )
            }
            Column(
                modifier = Modifier.align(Alignment.TopEnd),
                horizontalAlignment = Alignment.End
            ) {
                if (isRunning) {
                    Surface(color = Color.Red, shape = RoundedCornerShape(bottomStart = 8.dp)) {
                        Text(
                            "ON GOING",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSizes.labelSmall.sp
                        )
                    }
                }
                if (hasCTToday) {
                    if (isRunning) Spacer(Modifier.height(2.dp))
                    Surface(
                        color = Color(0xFF3D5A80),
                        shape = RoundedCornerShape(bottomStart = 8.dp)
                    ) {
                        Text(
                            "CT",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSizes.labelSmall.sp
                        )
                    }
                }
            }
            if (subject.note.isNotBlank()) {
                Surface(
                    color = contentColor.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(topStart = 8.dp),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Text(
                        text = subject.note,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = contentColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = fontSizes.labelSmall.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SegmentedButton(isShortView: Boolean, onToggle: (Boolean) -> Unit) {
    val fontSizes = LocalFontSizes.current
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        listOf(true to "Today", false to "Full Week").forEach { (value, label) ->
            val selected = isShortView == value
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onToggle(value) }
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    label,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSizes.bodyLarge.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Legend(subjects: List<Subject>) {
    val fontSizes = LocalFontSizes.current
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        subjects.forEach { subject ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(subject.color), RoundedCornerShape(2.dp))
                ); Spacer(Modifier.width(4.dp)); Text(
                subject.code,
                fontSize = fontSizes.labelSmall.sp
            )
            }
        }
    }
}

@Composable
fun SubjectDetailsDialog(subject: Subject, onDismiss: () -> Unit) {
    val fontSizes = LocalFontSizes.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(subject.name, fontSize = fontSizes.titleMedium.sp) },
        text = {
            Text(
                "Code: ${subject.code}\nInstructor: ${subject.instructor}",
                fontSize = fontSizes.bodyMedium.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    "OK",
                    fontSize = fontSizes.bodyMedium.sp
                )
            }
        })
}

fun isCurrentTimeInRange(startStr: String, endStr: String, currentTime: Calendar): Boolean =
    TimeUtils.isCurrentTimeInRange(startStr, endStr, currentTime)

fun isTimePassed(timeStr: String, currentTime: Calendar): Boolean =
    TimeUtils.isTimePassed(timeStr, currentTime)

fun isDailySize(isDaily: Boolean) = isDaily
