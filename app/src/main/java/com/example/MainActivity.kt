package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.repository.TaskRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.task.MainTaskScreen
import com.example.ui.task.TaskViewModel
import com.example.ui.task.TaskViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppContent()
            }
        }
    }
}

@Composable
fun AppContent() {
    val auth = Firebase.auth
    var currentUser by remember { mutableStateOf(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = auth.currentUser }
        )
    } else {
        val uid = currentUser!!.uid
        key(uid) {
            val context = LocalContext.current
            val repository = remember(uid) { TaskRepository(context) }
            val viewModel: TaskViewModel = viewModel(
                key = uid,
                factory = TaskViewModelFactory(repository, uid)
            )
            MainTaskScreen(
                currentUserId = uid,
                viewModel = viewModel
            )
        }
    }
}
