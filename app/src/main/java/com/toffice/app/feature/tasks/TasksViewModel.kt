package com.toffice.app.feature.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toffice.app.data.task.Task
import com.toffice.app.data.task.TaskDao
import com.toffice.app.data.task.TaskPriority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TasksUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class TasksViewModel @Inject constructor(
    private val dao: TaskDao,
) : ViewModel() {

    val uiState: StateFlow<TasksUiState> =
        dao.observeAll()
            .map { TasksUiState(tasks = it, isLoading = false) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TasksUiState())

    fun addTask(title: String, notes: String, priority: TaskPriority, dueDate: Long?) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            dao.insert(Task(title = clean, notes = notes.trim(), priority = priority, dueDate = dueDate))
        }
    }

    fun toggleDone(task: Task) {
        viewModelScope.launch { dao.update(task.copy(isDone = !task.isDone)) }
    }

    fun delete(task: Task) {
        viewModelScope.launch { dao.delete(task) }
    }
}
