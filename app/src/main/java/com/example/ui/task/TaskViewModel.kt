package com.example.ui.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Task
import com.example.data.repository.TaskRepository
import com.example.data.repository.TaskSyncSnapshot
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class StatusTab(val title: String) {
    ALL("All"),
    PENDING("Pending"),
    TODAY("Today"),
    COMPLETED("Completed")
}

enum class TaskSortOption(val displayName: String) {
    NEWEST("Newest"),
    PRIORITY("Priority"),
    DUE_DATE("Due Date"),
    TITLE("Alphabetical")
}

data class TaskStatistics(
    val total: Int = 0,
    val completed: Int = 0,
    val pending: Int = 0,
    val overdue: Int = 0,
    val completionPercentage: Float = 0f
)

data class TaskFilterParams(
    val query: String = "",
    val category: String? = null,
    val priority: String? = null,
    val tab: StatusTab = StatusTab.ALL,
    val sort: TaskSortOption = TaskSortOption.NEWEST
)

class TaskViewModel(
    private val repository: TaskRepository,
    private val currentUserId: String
) : ViewModel() {

    // Snapshot stream with real-time sync & offline cache awareness
    val syncState: StateFlow<UiState<TaskSyncSnapshot>> = repository.observeTasksWithSyncStatus(currentUserId)
        .map<TaskSyncSnapshot, UiState<TaskSyncSnapshot>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            emit(UiState.Error(rootError.message ?: "Failed to observe tasks stream"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    private val _selectedPriority = MutableStateFlow<String?>(null)
    val selectedPriority: StateFlow<String?> = _selectedPriority.asStateFlow()

    private val _statusTab = MutableStateFlow(StatusTab.ALL)
    val statusTab: StateFlow<StatusTab> = _statusTab.asStateFlow()

    private val _sortOption = MutableStateFlow(TaskSortOption.NEWEST)
    val sortOption: StateFlow<TaskSortOption> = _sortOption.asStateFlow()

    private val _userEvent = MutableSharedFlow<String>()
    val userEvent = _userEvent.asSharedFlow()

    // 5-flow combine for all filter and sorting options
    private val filterParams: StateFlow<TaskFilterParams> = combine(
        _searchQuery,
        _selectedCategory,
        _selectedPriority,
        _statusTab,
        _sortOption
    ) { query, category, priority, tab, sort ->
        TaskFilterParams(query, category, priority, tab, sort)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = TaskFilterParams()
    )

    // Filtered & Sorted Tasks Flow (combine syncState with filterParams)
    val filteredTasks: StateFlow<List<Task>> = combine(
        syncState,
        filterParams
    ) { state, filter ->
        val rawList: List<Task> = (state as? UiState.Success<TaskSyncSnapshot>)?.data?.tasks ?: emptyList()

        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis

        rawList.asSequence()
            .filter { task ->
                if (filter.query.isBlank()) true
                else task.title.contains(filter.query, ignoreCase = true) ||
                     task.description.contains(filter.query, ignoreCase = true) ||
                     task.tags.any { it.contains(filter.query, ignoreCase = true) }
            }
            .filter { task ->
                filter.category == null || task.category.equals(filter.category, ignoreCase = true)
            }
            .filter { task ->
                filter.priority == null || task.priority.equals(filter.priority, ignoreCase = true)
            }
            .filter { task ->
                when (filter.tab) {
                    StatusTab.ALL -> true
                    StatusTab.PENDING -> !task.isCompleted
                    StatusTab.COMPLETED -> task.isCompleted
                    StatusTab.TODAY -> {
                        val due = task.dueDate?.toDate()?.time
                        due != null && due in startOfToday..endOfToday
                    }
                }
            }
            .sortedWith { a, b ->
                when (filter.sort) {
                    TaskSortOption.NEWEST -> {
                        val aTime = a.createdAt?.seconds ?: 0L
                        val bTime = b.createdAt?.seconds ?: 0L
                        bTime.compareTo(aTime)
                    }
                    TaskSortOption.PRIORITY -> {
                        val priorityWeight = mapOf("urgent" to 4, "high" to 3, "medium" to 2, "low" to 1)
                        val wA = priorityWeight[a.priority.lowercase()] ?: 0
                        val wB = priorityWeight[b.priority.lowercase()] ?: 0
                        wB.compareTo(wA)
                    }
                    TaskSortOption.DUE_DATE -> {
                        val aDue = a.dueDate?.seconds ?: Long.MAX_VALUE
                        val bDue = b.dueDate?.seconds ?: Long.MAX_VALUE
                        aDue.compareTo(bDue)
                    }
                    TaskSortOption.TITLE -> a.title.compareTo(b.title, ignoreCase = true)
                }
            }
            .toList()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    // Overall Task Statistics
    val taskStatistics: StateFlow<TaskStatistics> = syncState.map { state ->
        val list = (state as? UiState.Success<TaskSyncSnapshot>)?.data?.tasks ?: emptyList()
        val total = list.size
        val completed = list.count { it.isCompleted }
        val pending = total - completed
        val nowMillis = System.currentTimeMillis()
        val overdue = list.count { !it.isCompleted && (it.dueDate?.toDate()?.time ?: Long.MAX_VALUE) < nowMillis }
        val percentage = if (total > 0) (completed.toFloat() / total.toFloat()) else 0f

        TaskStatistics(
            total = total,
            completed = completed,
            pending = pending,
            overdue = overdue,
            completionPercentage = percentage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = TaskStatistics()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: String?) {
        _selectedCategory.value = category
    }

    fun setSelectedPriority(priority: String?) {
        _selectedPriority.value = priority
    }

    fun setStatusTab(tab: StatusTab) {
        _statusTab.value = tab
    }

    fun setSortOption(option: TaskSortOption) {
        _sortOption.value = option
    }

    fun createTask(
        title: String,
        description: String,
        priority: String,
        category: String,
        dueDateMillis: Long?,
        tags: List<String>
    ) {
        viewModelScope.launch {
            val task = Task(
                userId = currentUserId,
                title = title.trim(),
                description = description.trim(),
                priority = priority.lowercase(),
                status = "todo",
                category = category,
                isCompleted = false,
                dueDate = dueDateMillis?.let { Timestamp(Date(it)) },
                tags = tags.take(5)
            )

            val result = repository.createTask(task)
            if (result.isSuccess) {
                _userEvent.emit("Task created and queued for sync")
            } else {
                _userEvent.emit("Failed to create task: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            val result = repository.updateTask(task)
            if (result.isSuccess) {
                _userEvent.emit("Task updated successfully")
            } else {
                _userEvent.emit("Failed to update task: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            val result = repository.toggleTaskCompletion(task)
            if (result.isSuccess) {
                val statusText = if (!task.isCompleted) "completed" else "marked pending"
                _userEvent.emit("Task $statusText")
            } else {
                _userEvent.emit("Failed to toggle status: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val result = repository.deleteTask(currentUserId, taskId)
            if (result.isSuccess) {
                _userEvent.emit("Task deleted")
            } else {
                _userEvent.emit("Failed to delete task: ${result.exceptionOrNull()?.message}")
            }
        }
    }
}

class TaskViewModelFactory(
    private val repository: TaskRepository,
    private val currentUserId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            return TaskViewModel(repository, currentUserId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
