package com.monayem.crono.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.monayem.crono.data.ScheduleEntry
import com.monayem.crono.data.Subject
import com.monayem.crono.data.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSchedulePopup(
    subject: Subject,
    initialEntries: List<ScheduleEntry>,
    onDismiss: () -> Unit,
    onSave: (List<ScheduleEntry>) -> Unit
) {
    var entries by remember { mutableStateOf(initialEntries) }

    LaunchedEffect(initialEntries) {
        if (entries.isEmpty() && initialEntries.isNotEmpty()) {
            entries = initialEntries
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(text = subject.name, style = MaterialTheme.typography.headlineSmall)
            Text(text = "[${subject.code}]", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(modifier = Modifier.weight(1f)) {
                itemsIndexed(entries) { index, entry ->
                    ScheduleEntryRow(entry, onEntryChanged = { updatedEntry ->
                        val newEntries = entries.toMutableList()
                        newEntries[index] = updatedEntry
                        entries = newEntries
                    })
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            IconButton(onClick = {
                entries = entries + ScheduleEntry(subjectId = subject.id, dayOfWeek = "", startTime = "", endTime = "", roomNumber = "")
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add new schedule entry")
            }

            Row(modifier = Modifier.align(Alignment.End)) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    val formattedEntries = entries.map { entry ->
                        entry.copy(
                            startTime = TimeUtils.parseAndFormatTime(entry.startTime),
                            endTime = TimeUtils.parseAndFormatTime(entry.endTime)
                        )
                    }
                    onSave(formattedEntries)
                }) {
                    Text("Save")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEntryRow(entry: ScheduleEntry, onEntryChanged: (ScheduleEntry) -> Unit) {
    val daysOfWeek = listOf("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.weight(1.5f)
            ) {
                OutlinedTextField(
                    value = entry.dayOfWeek,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Day") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    daysOfWeek.forEach { day ->
                        DropdownMenuItem(text = { Text(day) }, onClick = {
                            onEntryChanged(entry.copy(dayOfWeek = day))
                            expanded = false
                        })
                    }
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = entry.roomNumber,
                onValueChange = { onEntryChanged(entry.copy(roomNumber = it)) },
                label = { Text("Room") },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = entry.startTime,
                onValueChange = { onEntryChanged(entry.copy(startTime = it)) },
                label = { Text("Start Time (hh:mm)") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = entry.endTime,
                onValueChange = { onEntryChanged(entry.copy(endTime = it)) },
                label = { Text("End Time (hh:mm)") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}