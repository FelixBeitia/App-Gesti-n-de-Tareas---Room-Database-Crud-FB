# Gestor de Tareas — Jetpack Compose + Room

Aplicación Android de gestión de tareas (To-Do) que permite **agregar, listar, editar,
eliminar y marcar como completadas** tareas, con almacenamiento local persistente
mediante **Room Database** e interfaz construida 100% en **Jetpack Compose**.

> Asignación: *Desarrollo de una Aplicación de Gestión de Tareas usando Jetpack Compose
> y Room Database.*

---

## 1. Tecnologías utilizadas

| Tecnología | Uso |
|---|---|
| Kotlin | Lenguaje de la app |
| Jetpack Compose (Material 3) | Interfaz de usuario declarativa |
| Room Database | Persistencia local (SQLite) |
| ViewModel + StateFlow | Estado y comunicación UI ↔ datos |
| Coroutines / Flow | Operaciones asíncronas y datos reactivos |
| Android Studio + Gradle (Kotlin DSL) | Entorno y build |

- `minSdk = 24`, `targetSdk = 34`, `compileSdk = 34`
- Kotlin `1.9.24`, AGP `8.5.2`, Compose Compiler `1.5.14`, Room `2.6.1`

---

## 2. Arquitectura utilizada

El proyecto sigue una arquitectura por capas basada en **MVVM (Model-View-ViewModel)**
con un **Repository** de por medio, tal como recomienda la guía oficial de Android:

```
┌──────────────────────────────────────────────┐
│                   UI (Compose)                │   TaskScreen.kt
│   - Lista de tareas (LazyColumn)              │   MainActivity.kt
│   - Diálogo agregar / editar                  │
└───────────────▲──────────────────────────────┘
                │ observa StateFlow / llama funciones
┌───────────────┴──────────────────────────────┐
│                  ViewModel                    │   TaskViewModel.kt
│   - Expone tasks: StateFlow<List<Task>>       │
│   - addTask / update / toggle / delete        │
└───────────────▲──────────────────────────────┘
                │
┌───────────────┴──────────────────────────────┐
│                 Repository                    │   TaskRepository.kt
│   - Única fuente de acceso a los datos        │
└───────────────▲──────────────────────────────┘
                │
┌───────────────┴──────────────────────────────┐
│              Room (capa de datos)             │   Task.kt (Entity)
│   Entity  ·  DAO  ·  Database                 │   TaskDao.kt / TaskDatabase.kt
└──────────────────────────────────────────────┘
```

**Flujo de datos (unidireccional):** la UI observa un `StateFlow` que emite la lista de
tareas. Cualquier cambio en la base de datos (insertar, actualizar, borrar) hace que Room
emita una nueva lista a través de `Flow`, el `StateFlow` se actualiza y Compose **se
recompone automáticamente**. La UI nunca toca la base de datos directamente: siempre pasa
por ViewModel → Repository → DAO.

### Estructura de paquetes

```
com.example.tareasapp
├── MainActivity.kt            → punto de entrada, monta Compose
├── data/
│   ├── Task.kt                → @Entity (tabla "tasks")
│   ├── TaskDao.kt             → @Dao con las operaciones CRUD
│   ├── TaskDatabase.kt        → @Database (Singleton de Room)
│   └── TaskRepository.kt      → capa de acceso a datos
└── ui/
    ├── TaskViewModel.kt       → estado + lógica (StateFlow)
    ├── TaskScreen.kt          → pantallas y componentes Compose
    └── theme/                 → colores, tipografía y tema Material 3
```

---

## 3. Implementación de Room

**Entity** — cada objeto `Task` es una fila de la tabla `tasks`:

```kotlin
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val completed: Boolean = false
)
```

**DAO** — define las operaciones CRUD. `getAllTasks()` devuelve un `Flow`, por lo que la
lista se actualiza sola cuando cambian los datos:

```kotlin
@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY completed ASC, id DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(task: Task)
    @Update suspend fun update(task: Task)
    @Delete suspend fun delete(task: Task)
}
```

**Database** — clase abstracta que Room implementa; se expone como Singleton para tener
una sola instancia en toda la app:

```kotlin
@Database(entities = [Task::class], version = 1, exportSchema = false)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    companion object { /* getDatabase(context) con patrón Singleton */ }
}
```

Las operaciones de escritura son funciones `suspend` y se ejecutan dentro de
`viewModelScope`, es decir, **fuera del hilo principal**, evitando bloquear la UI.

---

## 4. Implementación de Jetpack Compose

Toda la interfaz es declarativa (sin XML de layouts). Componentes clave en
`TaskScreen.kt`:

- **`Scaffold`** con `TopAppBar` y un `FloatingActionButton` (botón **+**) para agregar.
- **`LazyColumn`** que renderiza la lista de tareas de forma eficiente; cada ítem es una
  **tarjeta de vidrio** (`Box` con el modificador `glass`, ver §4.1) con:
  - `Checkbox` → marca la tarea como **completada** (aplica tachado al texto).
  - Botón **Editar** (ícono lápiz) → abre el diálogo en modo edición.
  - Botón **Eliminar** (ícono basura) → borra la tarea.
- **`AlertDialog` con `OutlinedTextField`** → formulario reutilizable para **agregar** y
  **editar** (mismo componente, cambia según haya o no una tarea en edición).
- **Estado reactivo:** la lista se obtiene con
  `viewModel.tasks.collectAsStateWithLifecycle()`. Cuando el `StateFlow` cambia, Compose
  recompone solo lo necesario. El estado local del diálogo se maneja con `remember` +
  `mutableStateOf`.
- **Material 3** con soporte de tema claro/oscuro y color dinámico (Material You) en
  Android 12+.

### 4.1 Diseño Glassmorphism (efecto vidrio esmerilado)

La interfaz aplica un estilo **glassmorphism**, inspirado en el *Liquid Glass* de Apple.
Como esta app es **Android/Jetpack Compose** (no SwiftUI), el efecto no usa ese framework:
se **recrea con las herramientas nativas de Compose** siguiendo la misma idea de diseño.

El efecto se logra con tres capas (todo en `TaskScreen.kt`):

1. **Fondo con degradado vibrante** (`Brush.linearGradient`, violeta → índigo → rosa) para
   que el vidrio tenga un color de fondo que "refractar".
2. **Superficies translúcidas** — un `Modifier.glass()` reutilizable que aplica
   `clip` + `background(Color.White.copy(alpha = 0.14f))` sobre el fondo.
3. **Borde de luz** — un `border` con degradado blanco que simula el canto del cristal.

```kotlin
private fun Modifier.glass(shape: Shape): Modifier = this
    .clip(shape)
    .background(Color.White.copy(alpha = 0.14f))          // capa translúcida
    .border(                                              // borde de luz (canto del vidrio)
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.10f))
        ),
        shape = shape
    )
```

**Referencia de diseño** (documentación oficial de Apple, *Applying Liquid Glass to custom
views*, traducida al español):

> https://developer-apple-com.translate.goog/documentation/swiftui/applying-liquid-glass-to-custom-views?_x_tr_sl=en&_x_tr_tl=es&_x_tr_hl=es&_x_tr_pto=tc&_x_tr_hist=true

---

## 5. Operaciones CRUD (resumen)

| Operación | Acción en la UI | Método ViewModel | Room |
|---|---|---|---|
| **Create** | Botón + → escribir → Agregar | `addTask(title)` | `insert` |
| **Read** | Lista automática | `tasks: StateFlow` | `getAllTasks` (Flow) |
| **Update (editar)** | Ícono lápiz → cambiar título → Guardar | `updateTaskTitle` | `update` |
| **Update (completar)** | Tocar el checkbox | `toggleCompleted` | `update` |
| **Delete** | Ícono basura | `deleteTask` | `delete` |

---

## 6. Cómo ejecutar

1. Abrir **Android Studio** (Hedgehog o superior).
2. `File > Open` y seleccionar la carpeta **`TareasApp`**.
3. Esperar el *Gradle Sync* (descarga dependencias automáticamente).
4. Ejecutar en un emulador o dispositivo físico (`Run ▶`).

> El archivo `local.properties` (ruta del SDK) lo genera Android Studio automáticamente y
> **no se sube** al repositorio.

Compilar por línea de comandos (opcional):

```bash
./gradlew assembleDebug        # macOS / Linux
gradlew.bat assembleDebug      # Windows
```
