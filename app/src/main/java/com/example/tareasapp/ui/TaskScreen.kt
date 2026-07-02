package com.example.tareasapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tareasapp.data.Task

/* ------------------------------------------------------------------------- *
 *  Glassmorphism (efecto "vidrio esmerilado")
 *
 *  Inspirado en el "Liquid Glass" de Apple. Como esta app es Android/Compose
 *  y no SwiftUI, el efecto se recrea con las herramientas nativas de Compose:
 *    1. un fondo con degradado vibrante (para que el vidrio tenga qué reflejar)
 *    2. superficies translúcidas (alpha bajo) sobre ese fondo
 *    3. un borde de luz (degradado blanco) que simula el canto del cristal
 *
 *  Referencia de diseño (docs de Apple, traducidas):
 *  https://developer-apple-com.translate.goog/documentation/swiftui/applying-liquid-glass-to-custom-views?_x_tr_sl=en&_x_tr_tl=es&_x_tr_hl=es
 * ------------------------------------------------------------------------- */

/** Degradado de fondo sobre el que "flota" el vidrio. */
private val GlassBackground = Brush.linearGradient(
    colors = listOf(
        Color(0xFF3B0764), // violeta profundo
        Color(0xFF4F46E5), // índigo
        Color(0xFFDB2777)  // rosa/magenta
    )
)

/** Convierte cualquier superficie en "vidrio": translúcida + borde de luz. */
private fun Modifier.glass(shape: Shape): Modifier = this
    .clip(shape)
    .background(Color.White.copy(alpha = 0.14f))
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.10f)
            )
        ),
        shape = shape
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel = viewModel()) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()

    // Estado del diálogo de agregar/editar.
    // Si taskBeingEdited es null y el diálogo está abierto -> modo "agregar".
    // Si tiene una tarea -> modo "editar".
    var showDialog by remember { mutableStateOf(false) }
    var taskBeingEdited by remember { mutableStateOf<Task?>(null) }

    // Fondo con degradado; el Scaffold va transparente encima para que se vea.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GlassBackground)
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Gestor de Tareas",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = {
                        taskBeingEdited = null
                        showDialog = true
                    },
                    containerColor = Color.White.copy(alpha = 0.18f),
                    contentColor = Color.White,
                    modifier = Modifier.glass(RoundedCornerShape(18.dp))
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Agregar tarea")
                }
            }
        ) { innerPadding ->

            if (tasks.isEmpty()) {
                EmptyState(Modifier.padding(innerPadding))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskItem(
                            task = task,
                            onToggle = { viewModel.toggleCompleted(task) },
                            onEdit = {
                                taskBeingEdited = task
                                showDialog = true
                            },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        TaskFormDialog(
            initialTask = taskBeingEdited,
            onDismiss = { showDialog = false },
            onConfirm = { title ->
                val editing = taskBeingEdited
                if (editing == null) {
                    viewModel.addTask(title)
                } else {
                    viewModel.updateTaskTitle(editing, title)
                }
                showDialog = false
            }
        )
    }
}

@Composable
private fun TaskItem(
    task: Task,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(20.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.completed,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color.White,
                    uncheckedColor = Color.White.copy(alpha = 0.7f),
                    checkmarkColor = Color(0xFF4F46E5)
                )
            )
            Text(
                text = task.title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                color = if (task.completed) Color.White.copy(alpha = 0.6f) else Color.White
            )
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Editar tarea",
                    tint = Color.White
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Eliminar tarea",
                    tint = Color(0xFFFF8A9B)
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No hay tareas.\nToca + para agregar tu primera tarea.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun TaskFormDialog(
    initialTask: Task?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialTask?.title ?: "") }
    val isEditing = initialTask != null

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color(0xFF2E1065).copy(alpha = 0.92f),
        titleContentColor = Color.White,
        textContentColor = Color.White,
        title = { Text(if (isEditing) "Editar tarea" else "Nueva tarea") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Título de la tarea") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        focusedLabelColor = Color.White,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.White,
                    disabledContentColor = Color.White.copy(alpha = 0.4f)
                )
            ) {
                Text(if (isEditing) "Guardar" else "Agregar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.85f))
            ) {
                Text("Cancelar")
            }
        }
    )
}
