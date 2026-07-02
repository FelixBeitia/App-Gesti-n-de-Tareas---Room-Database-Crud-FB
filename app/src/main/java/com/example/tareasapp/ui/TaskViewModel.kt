package com.example.tareasapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tareasapp.data.Task
import com.example.tareasapp.data.TaskDatabase
import com.example.tareasapp.data.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel: comunica la interfaz (Compose) con la base de datos (Room) a través
 * del Repository. Expone la lista de tareas como un StateFlow que la UI observa,
 * y ofrece las funciones para las operaciones CRUD.
 */
class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TaskRepository

    init {
        val dao = TaskDatabase.getDatabase(application).taskDao()
        repository = TaskRepository(dao)
    }

    /** Lista observable de tareas. La UI se recompone automáticamente al cambiar. */
    val tasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    /** Agrega una nueva tarea. Ignora títulos vacíos. */
    fun addTask(title: String) {
        val clean = title.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            repository.insert(Task(title = clean))
        }
    }

    /** Actualiza el título de una tarea existente (edición). */
    fun updateTaskTitle(task: Task, newTitle: String) {
        val clean = newTitle.trim()
        if (clean.isEmpty()) return
        viewModelScope.launch {
            repository.update(task.copy(title = clean))
        }
    }

    /** Marca/desmarca una tarea como completada. */
    fun toggleCompleted(task: Task) {
        viewModelScope.launch {
            repository.update(task.copy(completed = !task.completed))
        }
    }

    /** Elimina una tarea. */
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.delete(task)
        }
    }
}
