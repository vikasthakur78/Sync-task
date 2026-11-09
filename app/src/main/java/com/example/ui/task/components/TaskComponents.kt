package com.example.ui.task.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Task
import com.example.ui.task.TaskStatistics
import com.example.ui.theme.PriorityHigh
import com.example.ui.theme.PriorityLow
import com.example.ui.theme.PriorityMedium
import com.example.ui.theme.PriorityUrgent
import com.example.ui.theme.SyncCached
import com.example.ui.theme.SyncOnline
import com.example.ui.theme.SyncPending
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Categories
val AVAILABLE_CATEGORIES = listOf("Personal", "Work", "Projects", "Shopping", "Health", "Other")

fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "work" -> Icons.Default.Work
        "personal" -> Icons.Default.Person
        "projects" -> Icons.Default.Folder
        "shopping" -> Icons.Default.ShoppingCart
        "health" -> Icons.Default.FitnessCenter
        else -> Icons.Default.Folder
    }
}

fun getPriorityColor(priority: String): Color {
    return when (priority.lowercase()) {
        "urgent" -> PriorityUrgent
        "high" -> PriorityHigh
        "medium" -> PriorityMedium
        "low" -> PriorityLow
        else -> PriorityMedium
    }
}

@Composable
fun SyncStatusBadge(
    hasPendingWrites: Boolean,
    isFromCache: Boolean,
    modifier: Modifier = Modifier
) {
    val (label, icon, color) = when {
        hasPendingWrites -> Triple("Syncing...", Icons.Default.CloudSync, SyncPending)
        isFromCache -> Triple("Offline Cache", Icons.Default.CloudOff, SyncCached)
        else -> Triple("Synced", Icons.Default.CloudDone, SyncOnline)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "syncPulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = if (hasPendingWrites) 0.5f else 1.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
            .alpha(if (hasPendingWrites) alphaAnim else 1f)
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun TaskStatsHeaderCard(
    stats: TaskStatistics,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Task Progress",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${stats.completed} of ${stats.total} completed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "${(stats.completionPercentage * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LinearProgressIndicator(
                progress = { stats.completionPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCounterBadge(label = "Total", count = stats.total, color = MaterialTheme.colorScheme.primary)
                StatCounterBadge(label = "Pending", count = stats.pending, color = PriorityMedium)
                StatCounterBadge(label = "Done", count = stats.completed, color = PriorityLow)
                if (stats.overdue > 0) {
                    StatCounterBadge(label = "Overdue", count = stats.overdue, color = PriorityUrgent)
                }
            }
        }
    }
}

@Composable
private fun StatCounterBadge(
    label: String,
    count: Int,
    color: Color
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.09f),
        modifier = Modifier.padding(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$count $label",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

@Composable
fun TaskCard(
    task: Task,
    onToggleCompletion: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val priorityColor = getPriorityColor(task.priority)
    val isOverdue = !task.isCompleted && (task.dueDate?.toDate()?.time ?: Long.MAX_VALUE) < System.currentTimeMillis()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .testTag("task_card_${task.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (task.isCompleted) 0.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Checkbox / Completion toggle
            Surface(
                onClick = onToggleCompletion,
                shape = CircleShape,
                color = if (task.isCompleted) priorityColor else Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    if (task.isCompleted) priorityColor else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .size(28.dp)
                    .padding(top = 2.dp)
                    .testTag("task_checkbox_${task.id}")
            ) {
                if (task.isCompleted) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Task completed",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Body
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.isCompleted) {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Task options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Task") },
                                onClick = {
                                    menuExpanded = false
                                    onEdit()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Task") },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            )
                        }
                    }
                }

                // Description (if available)
                if (task.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Metadata Row (Priority, Category, Due date)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Priority Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = priorityColor.copy(alpha = 0.12f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(priorityColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = task.priority.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = priorityColor
                            )
                        }
                    }

                    // Category Pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(task.category),
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = task.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Due Date Pill
                    task.dueDate?.toDate()?.let { due ->
                        val dateFormatted = formatDueDate(due)
                        val dueColor = if (isOverdue) PriorityUrgent else MaterialTheme.colorScheme.onSurfaceVariant
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isOverdue) PriorityUrgent.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isOverdue) Icons.Default.Warning else Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp),
                                    tint = dueColor
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isOverdue) "Overdue ($dateFormatted)" else dateFormatted,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
                                    color = dueColor
                                )
                            }
                        }
                    }
                }

                // Tags Row
                if (task.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        task.tags.forEach { tag ->
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

fun formatDueDate(date: Date): String {
    val now = Calendar.getInstance()
    val dueCal = Calendar.getInstance().apply { time = date }

    val isSameDay = now.get(Calendar.YEAR) == dueCal.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == dueCal.get(Calendar.DAY_OF_YEAR)

    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val isTomorrow = tomorrow.get(Calendar.YEAR) == dueCal.get(Calendar.YEAR) &&
                     tomorrow.get(Calendar.DAY_OF_YEAR) == dueCal.get(Calendar.DAY_OF_YEAR)

    return when {
        isSameDay -> "Today"
        isTomorrow -> "Tomorrow"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditTaskBottomSheet(
    sheetState: SheetState,
    taskToEdit: Task?,
    onDismiss: () -> Unit,
    onSaveTask: (title: String, description: String, priority: String, category: String, dueDateMillis: Long?, tags: List<String>) -> Unit
) {
    var title by remember(taskToEdit) { mutableStateOf(taskToEdit?.title ?: "") }
    var description by remember(taskToEdit) { mutableStateOf(taskToEdit?.description ?: "") }
    var priority by remember(taskToEdit) { mutableStateOf(taskToEdit?.priority ?: "medium") }
    var category by remember(taskToEdit) { mutableStateOf(taskToEdit?.category ?: "Personal") }
    var dueDateMillis by remember(taskToEdit) {
        mutableStateOf(taskToEdit?.dueDate?.toDate()?.time)
    }
    var tagInput by remember { mutableStateOf("") }
    val tags = remember(taskToEdit) {
        mutableStateListOf<String>().apply {
            taskToEdit?.tags?.let { addAll(it) }
        }
    }

    var titleError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (taskToEdit == null) "New Task" else "Edit Task",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close sheet")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    if (titleError && it.isNotBlank()) titleError = false
                },
                label = { Text("Task Title *") },
                isError = titleError,
                supportingText = if (titleError) {
                    { Text("Title is required", color = MaterialTheme.colorScheme.error) }
                } else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_title_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Description Field
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Notes (Optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("task_desc_input"),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Priority Selector
            Text(
                text = "Priority",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("low", "medium", "high", "urgent").forEach { p ->
                    val isSelected = priority.equals(p, ignoreCase = true)
                    val pColor = getPriorityColor(p)
                    FilterChip(
                        selected = isSelected,
                        onClick = { priority = p },
                        label = { Text(p.replaceFirstChar { it.uppercase() }) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(pColor)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = pColor.copy(alpha = 0.18f),
                            selectedLabelColor = pColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Category Selector
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AVAILABLE_CATEGORIES.forEach { cat ->
                    val isSelected = category.equals(cat, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { category = cat },
                        label = { Text(cat) },
                        leadingIcon = {
                            Icon(
                                imageVector = getCategoryIcon(cat),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Due Date Quick Options
            Text(
                text = "Due Date",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val hasDueDate = dueDateMillis != null
                FilterChip(
                    selected = !hasDueDate,
                    onClick = { dueDateMillis = null },
                    label = { Text("No Due Date") }
                )
                FilterChip(
                    selected = hasDueDate && isDateToday(dueDateMillis),
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 0)
                        }
                        dueDateMillis = cal.timeInMillis
                    },
                    label = { Text("Today") }
                )
                FilterChip(
                    selected = hasDueDate && isDateTomorrow(dueDateMillis),
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 0)
                        }
                        dueDateMillis = cal.timeInMillis
                    },
                    label = { Text("Tomorrow") }
                )
                FilterChip(
                    selected = hasDueDate && !isDateToday(dueDateMillis) && !isDateTomorrow(dueDateMillis),
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 7)
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 0)
                        }
                        dueDateMillis = cal.timeInMillis
                    },
                    label = { Text("Next Week") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tags section
            Text(
                text = "Tags (${tags.size}/5)",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = tagInput,
                    onValueChange = { tagInput = it },
                    placeholder = { Text("Add tag (e.g. project1)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val cleanTag = tagInput.trim().removePrefix("#").lowercase()
                        if (cleanTag.isNotBlank() && tags.size < 5 && !tags.contains(cleanTag)) {
                            tags.add(cleanTag)
                            tagInput = ""
                        }
                    },
                    enabled = tagInput.isNotBlank() && tags.size < 5,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add tag")
                }
            }

            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    tags.forEach { tag ->
                        AssistChip(
                            onClick = { tags.remove(tag) },
                            label = { Text("#$tag") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove tag",
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save Button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        titleError = true
                    } else {
                        onSaveTask(
                            title.trim(),
                            description.trim(),
                            priority,
                            category,
                            dueDateMillis,
                            tags.toList()
                        )
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_task_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (taskToEdit == null) "Create Task" else "Save Changes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

fun isDateToday(millis: Long?): Boolean {
    if (millis == null) return false
    val now = Calendar.getInstance()
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return now.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
           now.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
}

fun isDateTomorrow(millis: Long?): Boolean {
    if (millis == null) return false
    val tom = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return tom.get(Calendar.YEAR) == cal.get(Calendar.YEAR) &&
           tom.get(Calendar.DAY_OF_YEAR) == cal.get(Calendar.DAY_OF_YEAR)
}

@Composable
fun UserAccountDialog(
    userEmail: String?,
    userId: String,
    totalTasks: Int,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Sync Account", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Signed in as:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = userEmail ?: "Google User",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Cloud Persistence: Active",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$totalTasks tasks synced across your devices with automatic offline resilience.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                    onSignOut()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Sign Out")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
