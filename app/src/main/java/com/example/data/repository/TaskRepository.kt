package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.Task
import com.example.data.util.OperationType
import com.example.data.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

data class TaskSyncSnapshot(
    val tasks: List<Task> = emptyList(),
    val hasPendingWrites: Boolean = false,
    val isFromCache: Boolean = false
)

class TaskRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(expectedUserId: String? = null): String {
        val currentUid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        if (expectedUserId != null && currentUid != expectedUserId) {
            throw SecurityException("Authenticated user does not match the requested user ID.")
        }
        return currentUid
    }

    fun observeTasks(userId: String): Flow<List<Task>> = flow {
        val path = "users/$userId/tasks"
        emitAll(
            db.collection("users").document(userId).collection("tasks")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(Task::class.java).sortedByDescending { it.createdAt }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeTasksWithSyncStatus(userId: String): Flow<TaskSyncSnapshot> = flow {
        val path = "users/$userId/tasks"
        emitAll(
            db.collection("users").document(userId).collection("tasks")
                .snapshots()
                .map { snapshot ->
                    val tasks = snapshot.toObjects(Task::class.java).sortedByDescending { it.createdAt }
                    TaskSyncSnapshot(
                        tasks = tasks,
                        hasPendingWrites = snapshot.metadata.hasPendingWrites(),
                        isFromCache = snapshot.metadata.isFromCache
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createTask(task: Task): Result<String> {
        val uid = requireUserId(task.userId.ifBlank { null })
        val docRef = db.collection("users").document(uid).collection("tasks").document()
        val payload = mutableMapOf<String, Any?>(
            "userId" to uid,
            "title" to task.title,
            "description" to task.description.ifBlank { null },
            "priority" to task.priority,
            "status" to task.status,
            "category" to task.category,
            "isCompleted" to task.isCompleted,
            "dueDate" to task.dueDate,
            "completedAt" to task.completedAt,
            "tags" to task.tags,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateTask(task: Task): Result<Unit> {
        val uid = requireUserId(task.userId)
        require(task.id.isNotBlank()) { "Task ID must not be blank for update" }
        requireNotNull(task.createdAt) { "Task createdAt must not be null for update" }
        val docRef = db.collection("users").document(uid).collection("tasks").document(task.id)

        val updates = mutableMapOf<String, Any?>(
            "title" to task.title,
            "description" to task.description.ifBlank { null },
            "priority" to task.priority,
            "status" to task.status,
            "category" to task.category,
            "isCompleted" to task.isCompleted,
            "dueDate" to task.dueDate,
            "completedAt" to task.completedAt,
            "tags" to task.tags,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleTaskCompletion(task: Task): Result<Unit> {
        val newIsCompleted = !task.isCompleted
        val newStatus = if (newIsCompleted) "completed" else "todo"
        val newCompletedAt = if (newIsCompleted) FieldValue.serverTimestamp() else null

        val uid = requireUserId(task.userId)
        require(task.id.isNotBlank()) { "Task ID must not be blank" }
        val docRef = db.collection("users").document(uid).collection("tasks").document(task.id)

        val updates = mutableMapOf<String, Any?>(
            "isCompleted" to newIsCompleted,
            "status" to newStatus,
            "completedAt" to newCompletedAt,
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        return try {
            docRef.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteTask(userId: String, taskId: String): Result<Unit> {
        val uid = requireUserId(userId)
        val docRef = db.collection("users").document(uid).collection("tasks").document(taskId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getUserTasks(userId: String): Result<List<Task>> {
        val path = "users/$userId/tasks"
        return try {
            val snapshot = db.collection("users").document(userId).collection("tasks").get().await()
            Result.success(snapshot.toObjects(Task::class.java))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getTaskById(userId: String, taskId: String): Result<Task> {
        val path = "users/$userId/tasks/$taskId"
        val docRef = db.collection("users").document(userId).collection("tasks").document(taskId)
        return try {
            val snapshot = docRef.get().await()
            val task = snapshot.toObject(Task::class.java)
                ?: throw NoSuchElementException("Task not found at $path")
            Result.success(task)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }
}
