package com.example.tareasapp.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository: capa intermedia entre el ViewModel y el DAO.
 * Abstrae el origen de datos; si mañana cambiamos Room por una API remota,
 * solo se modifica aquí y el resto de la app no se entera.
 */
class TaskRepository(private val taskDao: TaskDao) {

    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    suspend fun insert(task: Task) = taskDao.insert(task)

    suspend fun update(task: Task) = taskDao.update(task)

    suspend fun delete(task: Task) = taskDao.delete(task)
}
