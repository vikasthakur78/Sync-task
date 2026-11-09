package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Task(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val priority: String = "medium", // "low", "medium", "high", "urgent"
    val status: String = "todo",     // "todo", "in_progress", "completed"
    val category: String = "Personal",
    val isCompleted: Boolean = false,
    val dueDate: Timestamp? = null,
    val completedAt: Timestamp? = null,
    val tags: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
