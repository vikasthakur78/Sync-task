package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Task
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TaskRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createTask_validPayload_createsDocumentAndReturnsId(): Unit = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = TaskRepository(firestore)

        val task = Task(
            userId = uid,
            title = TASK_TITLE,
            description = "Detailed test description",
            priority = "high",
            status = "todo",
            category = "Personal",
            isCompleted = false,
            tags = listOf("test", "work")
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createTask(task) }
        assertTrue(createResult.isSuccess)
        val taskId = createResult.getOrThrow()
        assertTrue(taskId.isNotEmpty())
    }

    @Test
    fun getUserTasks_authenticatedOwner_returnsMatchingTasks(): Unit = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = TaskRepository(firestore)
        val task = Task(
            userId = uid,
            title = "Test Retrieve Task",
            priority = "medium",
            status = "todo",
            category = "Work"
        )
        val taskId = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createTask(task).getOrThrow() }

        val tasksResult = withTimeout(DEFAULT_TIMEOUT_MS) { repository.getUserTasks(uid) }
        assertTrue(tasksResult.isSuccess)
        assertTrue(tasksResult.getOrThrow().map { it.id }.contains(taskId))
    }

    @Test
    fun observeTasks_authenticatedOwner_emitsRealtimeUpdates(): Unit = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repository = TaskRepository(firestore)
        val task = Task(
            userId = uid,
            title = "Realtime Sync Task",
            priority = "urgent",
            status = "todo",
            category = "Projects"
        )
        val taskId = withTimeout(DEFAULT_TIMEOUT_MS) { repository.createTask(task).getOrThrow() }

        val emittedTasks = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeTasks(uid).first { list -> list.any { it.id == taskId } }
        }
        assertTrue(emittedTasks.map { it.id }.contains(taskId))
    }

    @Test
    fun getTaskById_crossUserAccess_failsWithPermissionDenied(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = TaskRepository(firestore)
        val task = Task(
            userId = aliceUid,
            title = "Alice Secret Task",
            priority = "low",
            status = "todo",
            category = "Personal"
        )
        val taskId = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.createTask(task).getOrThrow() }

        val bobUid = signInTestUser(BOB_EMAIL)
        val bobRepo = TaskRepository(firestore)

        try {
            withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getTaskById(aliceUid, taskId).getOrThrow() }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
        } catch (e: FirebaseFirestoreException) {
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, e.code)
        }
    }

    @Test
    fun observeTasks_unauthenticatedUser_failsWithPermissionDenied(): Unit = runBlocking {
        auth.signOut()
        val repository = TaskRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) { repository.observeTasks("random_user_123").first() }
            fail("Expected exception on unauthenticated query")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull(firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@test.com"
        const val BOB_EMAIL = "bob@test.com"
        const val TASK_TITLE = "Sample Task"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
