package com.monayem.classschedule

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monayem.classschedule.data.AppDatabase
import com.monayem.classschedule.data.ScheduleEntry
import com.monayem.classschedule.data.Subject
import com.monayem.classschedule.ui.schedule.ScheduleViewModel
import com.monayem.classschedule.ui.schedule.ScheduleViewModelFactory
import com.monayem.classschedule.ui.theme.ClassScheduleTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val viewModel: ScheduleViewModel by viewModels {
        ScheduleViewModelFactory(AppDatabase.getDatabase(this).scheduleDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClassScheduleTheme {
                val schedule by viewModel.schedule.collectAsState()
                val scheduleByDay by viewModel.scheduleByDay.collectAsState()
                val legendSubjects by viewModel.legendSubjects.collectAsState()

                MainScreen(schedule?.scheduleName, scheduleByDay, legendSubjects)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun MainScreen(
    scheduleName: String?,
    scheduleByDay: Map<String, List<Triple<String, Subject, ScheduleEntry>>>,
    legendSubjects: List<Subject>
) {
    val context = LocalContext.current
    var isShortView by remember { mutableStateOf(true) }

    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60000) // 1 minute
            currentTime = Calendar.getInstance()
        }
    }

    val currentDay = remember(currentTime) {
        val calendar = Calendar.getInstance()
        when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> "SATURDAY"
            Calendar.SUNDAY -> "SUNDAY"
            Calendar.MONDAY -> "MONDAY"
            Calendar.TUESDAY -> "TUESDAY"
            Calendar.WEDNESDAY -> "WEDNESDAY"
            Calendar.THURSDAY -> "THURSDAY"
            Calendar.FRIDAY -> "FRIDAY"
            else -> ""
        }
    }

    val displayedSchedule = if (isShortView) {
        scheduleByDay.filter { it.key == currentDay }
    } else {
        scheduleByDay
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(scheduleName ?: "CLASS SCHEDULE") },
                actions = {
                    IconButton(onClick = { context.startActivity(Intent(context, SecondActivity::class.java)) }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Update Schedule")
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SegmentedButton(isShortView, onToggle = { isShortView = it })
                }
                Spacer(Modifier.height(32.dp))
            }

            val lazyRowModifier = if (isShortView) Modifier.fillMaxWidth() else Modifier
            val scheduleArrangement = if (isShortView) Arrangement.Center else Arrangement.spacedBy(16.dp, Alignment.Start)

            if (displayedSchedule.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
                     Text(
                         text = if(isShortView) "You have no classes today!" else "Your schedule is empty. Add some classes!",
                         color = Color.Gray, 
                         fontSize = 18.sp
                     )
                }
            } else {
                LazyRow(
                    modifier = lazyRowModifier,
                    horizontalArrangement = scheduleArrangement,
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(displayedSchedule.entries.toList()) { (day, entries) ->
                        DayColumn(day, entries, currentTime, currentDay)
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.height(32.dp))
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                Legend(legendSubjects)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedButton(isShortView: Boolean, onToggle: (Boolean) -> Unit) {
    SingleChoiceSegmentedButtonRow {
        SegmentedButton(
            selected = isShortView,
            onClick = { onToggle(true) },
            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
        ) {
            Text("Today")
        }
        SegmentedButton(
            selected = !isShortView,
            onClick = { onToggle(false) },
            shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
        ) {
            Text("Full Week")
        }
    }
}

@Composable
fun DayColumn(day: String, entries: List<Triple<String, Subject, ScheduleEntry>>, currentTime: Calendar, currentDay: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(280.dp)) {
        Text(
            text = day,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(Color.White)
                .padding(vertical = 12.dp),
            textAlign = TextAlign.Center
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                .background(Color(0xFFF3F4F6))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (entries.isEmpty()) {
                Text("No classes", modifier = Modifier.padding(16.dp), color = Color.Gray)
            }
            entries.sortedBy { it.third.startTime }.forEach { (_, subject, entry) ->
                ScheduleCard(subject, entry, currentTime, currentDay)
            }
        }
    }
}

fun isCurrentTimeInRange(startTime: String, endTime: String, currentTime: Calendar): Boolean {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    try {
        val start = Calendar.getInstance().apply { time = timeFormat.parse(startTime) ?: return false }
        val end = Calendar.getInstance().apply { time = timeFormat.parse(endTime) ?: return false }
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, currentTime.get(Calendar.HOUR_OF_DAY))
            set(Calendar.MINUTE, currentTime.get(Calendar.MINUTE))
        }

        val startCal = Calendar.getInstance()
        startCal.set(Calendar.HOUR_OF_DAY, start.get(Calendar.HOUR_OF_DAY))
        startCal.set(Calendar.MINUTE, start.get(Calendar.MINUTE))
        startCal.set(Calendar.SECOND, 0)
        startCal.set(Calendar.MILLISECOND, 0)

        val endCal = Calendar.getInstance()
        endCal.set(Calendar.HOUR_OF_DAY, end.get(Calendar.HOUR_OF_DAY))
        endCal.set(Calendar.MINUTE, end.get(Calendar.MINUTE))
        endCal.set(Calendar.SECOND, 0)
        endCal.set(Calendar.MILLISECOND, 0)

        return now.after(startCal) && now.before(endCal)
    } catch (e: Exception) {
        return false
    }
}

@Composable
fun ScheduleCard(subject: Subject, entry: ScheduleEntry, currentTime: Calendar, currentDay: String) {
    val isRunning = entry.dayOfWeek.equals(currentDay, ignoreCase = true) && isCurrentTimeInRange(entry.startTime, entry.endTime, currentTime)
    val cardColor = Color(subject.color)

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(cardColor)
                .padding(16.dp)
        ) {
            Text(
                text = "${entry.startTime} - ${entry.endTime}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.Black.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${subject.name.uppercase()} (${subject.code.uppercase()})",
                fontSize = 14.sp,
                color = Color.Black.copy(alpha = 0.9f)
            )
            Text(
                text = "ROOM: ${entry.roomNumber}",
                fontSize = 14.sp,
                color = Color.Black.copy(alpha = 0.9f)
            )
        }
        if (isRunning) {
            Text(
                text = "ON GOING",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
                    .background(Color(0xFFE53935), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Legend(subjects: List<Subject>) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.Start) {
        Text("Monayem", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF374151))
        Spacer(Modifier.height(16.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subjects.forEach { subject ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(subject.color))
                    )
                    Text(text = subject.name.uppercase(), fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ClassScheduleTheme {
        // MainScreen("CLASS SCHEDULE", emptyMap(), emptyList()) // Preview updated
    }
}
