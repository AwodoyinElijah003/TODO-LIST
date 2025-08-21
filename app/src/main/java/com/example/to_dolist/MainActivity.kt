package com.example.to_dolist
import android.util.Log

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope // Import for lifecycleScope
import kotlinx.coroutines.launch         // Import for launch

// ---------------- Default Boards ----------------
private val defaultBoards = listOf(
    Board("Inspiration", Color(0xFFFDE68A), emptyList()),
    Board("Travel Plans", Color(0xFFE9D5FF), emptyList()),
    Board("Work", Color(0xFFFFE4E6), emptyList()),
    Board("Groceries", Color(0xFF9EE6C3), emptyList())
)

// ---------------- MainActivity ----------------
class MainActivity : ComponentActivity() {
    private lateinit var boardDataStore: BoardDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        boardDataStore = BoardDataStore(this)

        setContent {
            ToDoTheme {
                val nav = rememberNavController()

                // Collect boards from datastore (with defaults on first run)
                val boards = boardDataStore.getBoards()
                    .collectAsState(initial = defaultBoards)

                // Whenever boards.value changes, save it
                // This LaunchedEffect is good for observing changes and saving.
                LaunchedEffect(boards.value) {
                    boardDataStore.saveBoards(boards.value)
                }

                Surface(color = MaterialTheme.colorScheme.background) {
                    // Inside MainActivity's setContent
                    AppNav(
                        nav = nav,
                        boards = boards.value,
                        onUpdateBoards = { updatedBoards ->
                            Log.d("MainActivity", "onUpdateBoards called with: $updatedBoards") // <-- ADD THIS
                            lifecycleScope.launch {
                                Log.d("MainActivity", "Attempting to save boards in coroutine...") // <-- ADD THIS
                                boardDataStore.saveBoards(updatedBoards)
                                Log.d("MainActivity", "boardDataStore.saveBoards finished.") // <-- ADD THIS
                            }
                        }
                    )

                }
            }
        }
    }
}





// ---------------- Navigation ----------------
sealed class Route(val path: String) {
    data object Onboarding : Route("onboarding")
    data object Boards : Route("boards")
    data class BoardDetail(val title: String) : Route("board/$title")
}

@Composable
fun AppNav(
    nav: NavHostController,
    boards: List<Board>,
    onUpdateBoards: (List<Board>) -> Unit
) {
    NavHost(navController = nav, startDestination = Route.Onboarding.path) {
        composable(Route.Onboarding.path) {
            OnboardingScreen(onStart = { nav.navigate(Route.Boards.path) })
        }
        composable(Route.Boards.path) {
            BoardsScreen(
                boards = boards,
                onUpdateBoards = onUpdateBoards,
                onOpenBoard = { board ->
                    nav.navigate("board/${board.title}")
                }
            )
        }
        composable("board/{title}") { backStackEntry ->
            val title = backStackEntry.arguments?.getString("title") ?: "Board"
            val board = boards.find { it.title == title }
            if (board != null) {
                TaskDetailScreen(
                    title = board.title,
                    tasks = board.tasks,
                    onBack = { nav.popBackStack() },
                    onUpdateTasks = { updatedTasks ->
                        onUpdateBoards(
                            boards.map {
                                if (it.title == board.title) it.copy(tasks = updatedTasks) else it
                            }
                        )
                    }
                )
            }
        }
    }
}


// ---------------- Theme ----------------
private val Blue = Color(0xFF3B82F6)
private val MintContainer = Color(0xFFE6F6EF)
private val SoftBg = Color(0xFFF7F8FC)
private val TextPrimary = Color(0xFF0F172A)
private val TextMuted = Color(0xFF64748B)

@Composable
fun ToDoTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = Blue,
        onPrimary = Color.White,
        background = SoftBg,
        surface = Color.White,
        onSurface = TextPrimary,
        secondaryContainer = MintContainer,
        outlineVariant = Color(0xFFE5E7EB)
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            titleLarge = MaterialTheme.typography.titleLarge.copy(
                fontSize = 28.sp, fontWeight = FontWeight.SemiBold
            ),
            titleMedium = MaterialTheme.typography.titleMedium.copy(
                fontSize = 20.sp, fontWeight = FontWeight.SemiBold
            ),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
        ),
        content = content
    )
}

// ---------------- Data Models ----------------
data class TaskItem(
    val id: Int,
    val text: String,
    val done: Boolean = false,
    val isEditing: Boolean = false
)

data class Board(
    val title: String,
    val color: Color,
    val tasks: List<TaskItem>,
    val isEditing: Boolean = false
)




data class SerializableTaskItem(
    val id: Int,
    val text: String,
    val done: Boolean,
    val isEditing: Boolean
)
fun TaskItem.toSerializable() = SerializableTaskItem(id, text, done, isEditing)
fun SerializableTaskItem.toDomain() = TaskItem(id, text, done, isEditing)

data class SerializableBoard(
    val title: String,
    val color: SerializableColor, // Use SerializableColor
    val tasks: List<SerializableTaskItem>,
    val isEditing: Boolean
)

fun Board.toSerializable() =
SerializableBoard(
title,
color.toSerializable(), // ✅ saves as Long
tasks.map { it.toSerializable() },
isEditing
)

fun SerializableBoard.toDomain() =
    Board(
        title,
        color.toComposeColor(), // ✅ restores Color from Long
        tasks.map { it.toDomain() },
        isEditing
    )


// ---------------- Onboarding Screen ----------------
@Composable
fun OnboardingScreen(onStart: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(12.dp))

            Image(
                painter = painterResource(id = R.drawable.onboarding_illustration),
                contentDescription = "Onboarding Illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("To-Do List", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Organize your tasks into categories and track progress easily.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = TextMuted),
                )
            }

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Get Started")
            }
        }
    }
}

// ---------------- Boards Screen ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardsScreen(
    boards: List<Board>,
    onUpdateBoards: (List<Board>) -> Unit,
    onOpenBoard: (Board) -> Unit,
    // 🎨 Random board colors
    boardColors: List<Color> = listOf(
        Color(0xFFFDE68A), // yellow
        Color(0xFFE9D5FF), // purple
        Color(0xFFFFE4E6), // pink
        Color(0xFF9EE6C3), // green
        Color(0xFFBBDEFB), // light blue
        Color(0xFFFFF59D), // lemon
        Color(0xFFFFCCBC), // peach
        Color(0xFFD1C4E9)  // lavender
    )
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tasks")
                        Text("Create your categorized boards", fontSize = 13.sp, color = TextMuted)
                    }
                }
            )
        },
        bottomBar = { BottomBar() },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val randomColor = boardColors.random()
                    val newBoard = Board("New Board ${boards.size + 1}", randomColor, emptyList())
                    onUpdateBoards(boards + newBoard)
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Board") },
                text = { Text("Board") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            contentPadding = PaddingValues(bottom = 96.dp) // 👈 space for FAB
        ) {
            items(boards, key = { it.title }) { board ->
                BoardCard(
                    board = board,
                    onClick = { onOpenBoard(board) },
                    onRename = { title, newTitle ->
                        onUpdateBoards(
                            boards.map {
                                if (it.title == title) it.copy(title = newTitle, isEditing = false) else it
                            }
                        )
                    },
                    onToggleEdit = { title ->
                        onUpdateBoards(
                            boards.map {
                                if (it.title == title) it.copy(isEditing = !it.isEditing) else it
                            }
                        )
                    },
                    onDelete = { title ->
                        onUpdateBoards(boards.filterNot { it.title == title })
                    }
                )
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun BoardCard(
    board: Board,
    onClick: () -> Unit,
    onRename: (String, String) -> Unit,
    onToggleEdit: (String) -> Unit,
    onDelete: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(board.title) }

    val doneCount = board.tasks.count { it.done }
    val total = board.tasks.size
    val subtitle = if (total == 0) "No tasks yet" else "$doneCount of $total Tasks"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(board.color.copy(alpha = 0.35f))
            .clickable(enabled = !board.isEditing, onClick = onClick)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (board.isEditing) {
                BasicTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    textStyle = TextStyle(color = TextPrimary, fontSize = 20.sp),
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { onRename(board.title, editText) }) {
                    Icon(Icons.Default.Check, contentDescription = "Save")
                }
            } else {
                Column(Modifier.weight(1f)) {
                    Text(board.title, style = MaterialTheme.typography.titleMedium)
                    if (total > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text(subtitle, fontSize = 13.sp, color = TextMuted)
                    }
                }
            }

            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            expanded = false
                            onToggleEdit(board.title)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            expanded = false
                            onDelete(board.title)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomBar() {
    NavigationBar(containerColor = Color.Transparent) {
        NavigationBarItem(selected = true, onClick = { }, icon = { Icon(Icons.Default.Home, null) }, label = null)
        Spacer(Modifier.weight(1f))
        NavigationBarItem(selected = false, onClick = { }, icon = { Icon(Icons.Default.Settings, null) }, label = null)
    }
}

// ---------------- Task Detail Screen ----------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    title: String,
    tasks: List<TaskItem>,
    onBack: () -> Unit,
    onUpdateTasks: (List<TaskItem>) -> Unit
) {
    var items by remember { mutableStateOf(tasks) }
    val doneCount = items.count { it.done }

    // ✅ keep UI state in sync with parent
    LaunchedEffect(tasks) { items = tasks }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                },
                title = {
                    Column {
                        Text(title)
                        Text("$doneCount of ${items.size} Tasks", fontSize = 13.sp, color = TextMuted)
                    }
                }
            )
        },
        bottomBar = { BottomBar() },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    val nextId = (items.maxOfOrNull { it.id } ?: 0) + 1
                    val newTasks = items + TaskItem(nextId, "New Task $nextId")
                    items = newTasks
                    onUpdateTasks(newTasks)
                },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Task") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentPadding = PaddingValues(bottom = 96.dp) // 👈 keeps tasks clear of FAB
        ) {
            items(items, key = { it.id }) { task ->
                EditableTaskRow(
                    task = task,
                    onToggle = { id, now ->
                        val updated = items.map { if (it.id == id) it.copy(done = now) else it }
                        items = updated
                        onUpdateTasks(updated)
                    },
                    onDelete = { id ->
                        val updated = items.filterNot { it.id == id }
                        items = updated
                        onUpdateTasks(updated)
                    },
                    onRename = { id, newText ->
                        val updated = items.map { if (it.id == id) it.copy(text = newText, isEditing = false) else it }
                        items = updated
                        onUpdateTasks(updated)
                    },
                    onToggleEdit = { id ->
                        items = items.map { if (it.id == id) it.copy(isEditing = !it.isEditing) else it }
                    }
                )
            }
        }
    }
}

// ---------------- Editable Row ----------------
@Composable
fun EditableTaskRow(
    task: TaskItem,
    onToggle: (Int, Boolean) -> Unit,
    onDelete: (Int) -> Unit,
    onRename: (Int, String) -> Unit,
    onToggleEdit: (Int) -> Unit
) {
    var editText by remember { mutableStateOf(task.text) }
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = task.done, onCheckedChange = { onToggle(task.id, it) })

        if (task.isEditing) {
            BasicTextField(
                value = editText,
                onValueChange = { editText = it },
                textStyle = TextStyle(color = TextPrimary, fontSize = 16.sp),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )
            IconButton(onClick = { onRename(task.id, editText) }) {
                Icon(Icons.Default.Check, contentDescription = "Save")
            }
        } else {
            Text(
                task.text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp,
                    textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.done) TextMuted else TextPrimary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        expanded = false
                        onToggleEdit(task.id)
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        expanded = false
                        onDelete(task.id)
                    }
                )
            }
        }
    }
}
