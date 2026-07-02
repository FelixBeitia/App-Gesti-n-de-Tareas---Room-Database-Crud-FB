package com.example.tareasapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object): define las operaciones CRUD contra la tabla "tasks".
 * Room genera la implementación en tiempo de compilación.
 */
@Dao
interface TaskDao {

    /** Devuelve todas las tareas ordenadas: primero las pendientes, luego por id descendente. */
    @Query("SELECT * FROM tasks ORDER BY completed ASC, id DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)
}
