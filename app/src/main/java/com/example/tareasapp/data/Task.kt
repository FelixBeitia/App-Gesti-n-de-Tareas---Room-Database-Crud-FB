package com.example.tareasapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity que representa la tabla "tasks" en la base de datos Room.
 * Cada instancia es una fila.
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val completed: Boolean = false
)
