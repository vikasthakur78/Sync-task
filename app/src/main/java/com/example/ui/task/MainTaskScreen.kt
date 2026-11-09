package com.example.ui.task

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.Task
import com.example.ui.auth.signOutUser
import com.example.ui.task.components.AVAILABLE_CATEGORIES
import com.example.ui.task.components.AddEditTaskBottomSheet
import com.example.ui.task.components.SyncStatusBadge
import com.example.ui.task.components.TaskCard
import com.example.ui.task.components.TaskStatsHeaderCard
import com.example.ui.task.components.UserAccountDialog
import com.example.ui.task.components.getCategoryIcon
import com.example.ui.task.components.getPriorityColor
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTaskScreen(
    currentUserId: String,
    viewModel: TaskViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val credentialManager = remember { CredentialManager.create(context) }

    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val filteredTasks by viewModel.filteredTasks.collectAsStateWithLifecycle()
    val stats by viewModel.taskStatistics.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val selectedPriority by viewModel.selectedPriority.collectAsStateWithLifecycle()
    val statusTab by viewModel.statusTab.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()

    // Modals state
    var showAddEditSheet by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    var taskToDelete by remember { mutableStateOf<Task?>(null) }
    var showAccountDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Listen for user feedback events from ViewModel
    LaunchedEffect(viewModel) {
        viewModel.userEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(id = R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                actions = {
                    // Live Sync status badge
                    val hasPending = (syncState as? UiState.Success)?.data?.hasPendingWrites ?: false
                    val isCache = (syncState as? UiState.Success)?.data?.isFromCache ?: false
                    SyncStatusBadge(
                        hasPendingWrites = hasPending,
                        isFromCache = isCache,
                        modifier = Modifier.padding(end = 6.dp)
                    )

                    // User Profile menu
                    IconButton(
                        onClick = { showAccountDialog = true },
                        modifier = Modifier.testTag("user_account_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "User account",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    taskToEdit = null
                    showAddEditSheet = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Task", fontWeight = FontWeight.SemiBold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_task_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Header stats & search
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search tasks, notes or #tags...") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("task_search_input"),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Stats Summary Card
                TaskStatsHeaderCard(stats = stats)

                Spacer(modifier = Modifier.height(12.dp))

                // Status Tabs (All, Pending, Today, Completed)
                TabRow(
                    selectedTabIndex = statusTab.ordinal,
                    containerColor = Color.Transparent,
                    indicator = { tabPositions ->
                        SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[statusTab.ordinal]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    divider = {}
                ) {
                    StatusTab.entries.forEach { tab ->
                        Tab(
                            selected = statusTab == tab,
                            onClick = { viewModel.setStatusTab(tab) },
                            text = {
                                Text(
                                    text = tab.title,
                                    fontWeight = if (statusTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    color = if (statusTab == tab) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Filters Row: Categories & Sort
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            FilterChip(
                                selected = selectedCategory == null,
                                onClick = { viewModel.setSelectedCategory(null) },
                                label = { Text("All Categories") }
                            )
                        }
                        items(AVAILABLE_CATEGORIES) { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = {
                                    viewModel.setSelectedCategory(if (selectedCategory == cat) null else cat)
                                },
                                label = { Text(cat) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getCategoryIcon(cat),
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            )
                        }
                    }

                    // Sort button with dropdown
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort tasks",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            TaskSortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.displayName,
                                            fontWeight = if (sortOption == option) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        showSortMenu = false
                                    },
                                    trailingIcon = {
                                        if (sortOption == option) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Task List or Loading / Empty states
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (val state = syncState) {
                    is UiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Connecting to real-time database...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    is UiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Sync Notice",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = state.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                    is UiState.Success -> {
                        if (filteredTasks.isEmpty()) {
                            // Empty state
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                                    modifier = Modifier
                                        .fillMaxWidth(0.85f)
                                        .height(160.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.hero_tasks),
                                        contentDescription = "Empty tasks banner",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) "No matching tasks" else "All caught up!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (searchQuery.isNotEmpty()) {
                                        "Try adjusting your filters or search keywords."
                                    } else {
                                        "Tap '+ New Task' below to create a task that syncs across all your devices."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(18.dp))
                                Button(
                                    onClick = {
                                        taskToEdit = null
                                        showAddEditSheet = true
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Create a Task")
                                }
                            }
                        } else {
                            // Populated task list
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(
                                    items = filteredTasks,
                                    key = { it.id }
                                ) { task ->
                                    TaskCard(
                                        task = task,
                                        onToggleCompletion = { viewModel.toggleTaskCompletion(task) },
                                        onEdit = {
                                            taskToEdit = task
                                            showAddEditSheet = true
                                        },
                                        onDelete = { taskToDelete = task }
                                    )
                                }
                                item {
                                    Spacer(modifier = Modifier.height(72.dp)) // Padding for FAB
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Task Modal Bottom Sheet
    if (showAddEditSheet) {
        AddEditTaskBottomSheet(
            sheetState = sheetState,
            taskToEdit = taskToEdit,
            onDismiss = { showAddEditSheet = false },
            onSaveTask = { title, description, priority, category, dueDateMillis, tags ->
                val existing = taskToEdit
                if (existing == null) {
                    viewModel.createTask(
                        title = title,
                        description = description,
                        priority = priority,
                        category = category,
                        dueDateMillis = dueDateMillis,
                        tags = tags
                    )
                } else {
                    val updated = existing.copy(
                        title = title,
                        description = description,
                        priority = priority,
                        category = category,
                        dueDate = dueDateMillis?.let { com.google.firebase.Timestamp(java.util.Date(it)) },
                        tags = tags
                    )
                    viewModel.updateTask(updated)
                }
            }
        )
    }

    // Delete confirmation dialog
    taskToDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { taskToDelete = null },
            title = { Text("Delete Task?") },
            text = { Text("Are you sure you want to delete '${task.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTask(task.id)
                        taskToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // User account details and Sign Out Dialog
    if (showAccountDialog) {
        val authUser = Firebase.auth.currentUser
        UserAccountDialog(
            userEmail = authUser?.email,
            userId = currentUserId,
            totalTasks = stats.total,
            onSignOut = {
                signOutUser(
                    credentialManager = credentialManager,
                    scope = coroutineScope
                )
            },
            onDismiss = { showAccountDialog = false }
        )
    }
}
