package com.fedbaq.physicssketch.ui

import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.fedbaq.physicssketch.R
import com.fedbaq.physicssketch.core.model.ArrowObject
import com.fedbaq.physicssketch.core.model.DrawableObject
import com.fedbaq.physicssketch.core.model.DrawingDocument
import com.fedbaq.physicssketch.core.model.DrawingSet
import com.fedbaq.physicssketch.core.model.ImagePageObject
import com.fedbaq.physicssketch.core.geometry.Geometry
import com.fedbaq.physicssketch.core.geometry.SketchBounds
import com.fedbaq.physicssketch.core.render.PngExporter
import com.fedbaq.physicssketch.core.render.PdfPageReader
import com.fedbaq.physicssketch.core.render.ZipExporter
import com.fedbaq.physicssketch.core.tools.DrawingTool
import com.fedbaq.physicssketch.core.tools.ShapeRegistry
import com.fedbaq.physicssketch.core.tools.SymbolPalette
import com.fedbaq.physicssketch.data.LocalDrawingStore
import com.fedbaq.physicssketch.data.PortableSetArchive
import com.fedbaq.physicssketch.data.SketchLibrary
import java.util.UUID
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen() {
    val context = LocalContext.current
    val store = remember { LocalDrawingStore(context) }
    var library by remember { mutableStateOf(store.load()) }
    var selectedSetId by remember { mutableStateOf(library.sets.first().id) }
    var selectedDrawingId by remember { mutableStateOf(library.sets.first().drawings.first().id) }
    var activeTool by remember { mutableStateOf(DrawingTool.Pen) }
    var activeShapeId by remember { mutableStateOf(ShapeRegistry.ARROW) }
    var textValue by remember { mutableStateOf("α") }
    var italic by remember { mutableStateOf(true) }
    var vectorText by remember { mutableStateOf(false) }
    var selectedObject by remember { mutableStateOf<DrawableObject?>(null) }
    var showObjectActionBar by remember { mutableStateOf(false) }
    var showDrawingPicker by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showRenameDrawingDialog by remember { mutableStateOf(false) }
    var showSavePanel by remember { mutableStateOf(false) }
    var showObjectMenu by remember { mutableStateOf(false) }
    var drawingToRenameId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var canvasView by remember { mutableStateOf<DrawingCanvasView?>(null) }
    var lastExport by remember { mutableStateOf<Bitmap?>(null) }
    var pendingPngBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPdfPageCount by remember { mutableStateOf(0) }
    var pendingPdfPageText by remember { mutableStateOf("1") }
    var canUndo by remember { mutableStateOf(false) }
    var canRedo by remember { mutableStateOf(false) }
    var penStrokeWidth by remember { mutableStateOf(5f) }
    var eraserRadius by remember { mutableStateOf(30f) }
    var pendingDeleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val currentSet = library.sets.firstOrNull { it.id == selectedSetId } ?: library.sets.first()
    val currentDocument = currentSet.drawings.firstOrNull { it.id == selectedDrawingId } ?: currentSet.drawings.first()

    fun saveLibrary(next: SketchLibrary) {
        library = next
        store.save(next)
    }

    fun updateCurrentSet(nextSet: DrawingSet) {
        val next = library.copy(sets = library.sets.map { if (it.id == nextSet.id) nextSet else it })
        saveLibrary(next)
    }

    fun createSet() {
        val newSet = DrawingSet.defaultSet().copy(title = "Лекция ${library.sets.size + 1}")
        saveLibrary(library.copy(sets = library.sets + newSet))
        selectedSetId = newSet.id
        selectedDrawingId = newSet.drawings.first().id
    }

    fun addDrawing() {
        val nextSet = currentSet.addBlankDrawing()
        updateCurrentSet(nextSet)
        selectedDrawingId = nextSet.drawings.last().id
        showDrawingPicker = false
    }

    fun deleteDrawing(document: DrawingDocument) {
        val result = library.deleteDrawing(currentSet.id, document.id)
        saveLibrary(result.library)
        selectedSetId = result.selectedSetId
        selectedDrawingId = result.selectedDrawingId
        showDrawingPicker = false
    }

    fun insertImportedPage(uri: Uri, pageIndex: Int = 0) {
        val imported = ImagePageObject(
            id = UUID.randomUUID().toString(),
            frame = defaultImportFrame(canvasView),
            sourceUri = uri.toString(),
            pageIndex = pageIndex,
        )
        updateCurrentSet(currentSet.replaceDrawing(currentDocument.addObject(imported)))
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val mimeType = context.contentResolver.getType(uri).orEmpty()
            if (mimeType == "application/pdf") {
                val pageCount = PdfPageReader.pageCount(context, uri)
                if (pageCount <= 1) {
                    insertImportedPage(uri)
                } else {
                    pendingPdfUri = uri
                    pendingPdfPageCount = pageCount
                    pendingPdfPageText = "1"
                }
            } else {
                insertImportedPage(uri)
            }
        }
    }

    val importZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val importedSet = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    PortableSetArchive.readSet(input)
                }
            }.getOrNull()

            if (importedSet == null) {
                Toast.makeText(context, "Не удалось открыть ZIP", Toast.LENGTH_SHORT).show()
            } else {
                val safeSet = importedSet.copy(
                    id = UUID.randomUUID().toString(),
                    drawings = importedSet.drawings.ifEmpty { listOf(DrawingSet.blankDrawing("Рисунок 1")) },
                )
                saveLibrary(library.copy(sets = library.sets + safeSet))
                selectedSetId = safeSet.id
                selectedDrawingId = safeSet.drawings.first().id
                Toast.makeText(context, "Набор импортирован", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val savePngLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        val bitmap = pendingPngBitmap
        if (uri != null && bitmap != null) {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                PngExporter.writePng(bitmap, output)
            }
            Toast.makeText(context, "PNG сохранен", Toast.LENGTH_SHORT).show()
        }
        pendingPngBitmap = null
    }

    val saveZipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                ZipExporter.writeSetArchive(context, currentSet, output)
            }
            Toast.makeText(context, "ZIP сохранен", Toast.LENGTH_SHORT).show()
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        navigationIcon = {
                            Row(
                                modifier = Modifier.padding(start = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                SavePanelButton(
                                    expanded = showSavePanel,
                                    onExpandedChange = { showSavePanel = it },
                                    onExportZip = { ZipExporter.shareSet(context, currentSet) },
                                    onSaveZip = { saveZipLauncher.launch("${currentSet.title}.zip") },
                                    onImportZip = {
                                        importZipLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                                    },
                                )
                                OutlinedButton(onClick = { canvasView?.undo() }, enabled = canUndo, shape = RoundedCornerShape(6.dp)) {
                                    Text("Назад")
                                }
                                OutlinedButton(onClick = { canvasView?.redo() }, enabled = canRedo, shape = RoundedCornerShape(6.dp)) {
                                    Text("Вперёд")
                                }
                            }
                        },
                        title = { Text(currentDocument.title) },
                        actions = {
                            ObjectMenuButton(
                                expanded = showObjectMenu,
                                onExpandedChange = { showObjectMenu = it },
                                activeShapeId = activeShapeId,
                                onShapeSelected = {
                                    activeTool = DrawingTool.Shape
                                    activeShapeId = it
                                },
                            )
                        },
                    )
                    ActionBar(
                        sets = library.sets,
                        selectedSetId = selectedSetId,
                        onSelectSet = { set ->
                            selectedSetId = set.id
                            selectedDrawingId = set.drawings.first().id
                        },
                        onCreateSet = ::createSet,
                        onOpenDrawings = { showDrawingPicker = true },
                        onRenameSet = {
                            renameText = currentSet.title
                            showRenameDialog = true
                        },
                        onDeleteDrawing = {
                            pendingDeleteAction = { deleteDrawing(currentDocument) }
                        },
                    )
                }
            },
            bottomBar = {
                ToolBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 176.dp),
                    activeTool = activeTool,
                    onToolSelected = { activeTool = it },
                    penStrokeWidth = penStrokeWidth,
                    onPenStrokeWidthChange = { penStrokeWidth = it },
                    eraserRadius = eraserRadius,
                    onEraserRadiusChange = { eraserRadius = it },
                    textValue = textValue,
                    onTextValueChange = { textValue = it },
                    italic = italic,
                    onItalicChange = { italic = it },
                    vectorText = vectorText,
                    onVectorTextChange = { vectorText = it },
                )
            },
        ) { padding ->
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                        .background(Color(0xFFF2F2F2)),
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White),
                        factory = { viewContext ->
                            DrawingCanvasView(viewContext).also { view ->
                                canvasView = view
                                view.onObjectSelected = {
                                    selectedObject = it
                                    if (it == null) {
                                        showObjectActionBar = false
                                    }
                                }
                                view.onObjectActionBarVisibleChanged = {
                                    showObjectActionBar = it && selectedObject != null
                                }
                                view.onHistoryChanged = { undo, redo ->
                                    canUndo = undo
                                    canRedo = redo
                                }
                            }
                        },
                        update = { view ->
                            view.document = currentDocument
                            view.activeTool = activeTool
                            view.activeShapeId = activeShapeId
                            view.textValue = textValue
                            view.italicText = italic
                            view.vectorText = vectorText
                            view.penStrokeWidth = penStrokeWidth
                            view.eraserRadius = eraserRadius
                            view.onHistoryChanged = { undo, redo ->
                                canUndo = undo
                                canRedo = redo
                            }
                            view.onDocumentChanged = { changed ->
                                updateCurrentSet(currentSet.replaceDrawing(changed))
                            }
                            view.onObjectActionBarVisibleChanged = {
                                showObjectActionBar = it && selectedObject != null
                            }
                        },
                    )
                    if (showObjectActionBar) {
                        selectedObject?.let { target ->
                            ObjectActionBar(
                                target = target,
                                canvasWidth = canvasView?.width ?: 0,
                                onDelete = {
                                    showObjectActionBar = false
                                    canvasView?.deleteSelected()
                                },
                                onDuplicate = {
                                    canvasView?.duplicateSelected()
                                },
                                onRotate = {
                                    showObjectActionBar = false
                                    canvasView?.toggleRotationWheel()
                                },
                            )
                        }
                    }
                    lastExport?.let { bitmap ->
                        ExportPreview(bitmap = bitmap, onClose = { lastExport = null })
                    }
                }
        }
    }

    if (showDrawingPicker) {
        DrawingPickerDialog(
            set = currentSet,
            selectedDrawingId = selectedDrawingId,
            onClose = { showDrawingPicker = false },
            onSelectDrawing = {
                selectedDrawingId = it.id
                showDrawingPicker = false
            },
            onAddDrawing = ::addDrawing,
            onDeleteDrawing = { document ->
                pendingDeleteAction = { deleteDrawing(document) }
            },
            onRenameDrawing = { document ->
                drawingToRenameId = document.id
                renameText = document.title
                showRenameDrawingDialog = true
            },
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Название набора") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    updateCurrentSet(currentSet.renamed(renameText))
                    showRenameDialog = false
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Отмена")
                }
            },
        )
    }

    if (showRenameDrawingDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDrawingDialog = false },
            title = { Text("Название рисунка") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    drawingToRenameId?.let { id ->
                        updateCurrentSet(currentSet.renameDrawing(id, renameText))
                    }
                    showRenameDrawingDialog = false
                    drawingToRenameId = null
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRenameDrawingDialog = false
                    drawingToRenameId = null
                }) {
                    Text("Отмена")
                }
            },
        )
    }

    pendingPdfUri?.let { pdfUri ->
        AlertDialog(
            onDismissRequest = { pendingPdfUri = null },
            title = { Text("Страница PDF") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Всего страниц: $pendingPdfPageCount")
                    OutlinedTextField(
                        value = pendingPdfPageText,
                        onValueChange = { pendingPdfPageText = it.filter(Char::isDigit) },
                        singleLine = true,
                        label = { Text("Номер страницы") },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val pageNumber = pendingPdfPageText.toIntOrNull()?.coerceIn(1, pendingPdfPageCount) ?: 1
                    insertImportedPage(pdfUri, pageIndex = pageNumber - 1)
                    pendingPdfUri = null
                }) {
                    Text("Открыть")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingPdfUri = null }) {
                    Text("Отмена")
                }
            },
        )
    }

    pendingDeleteAction?.let { deleteAction ->
        AlertDialog(
            onDismissRequest = { pendingDeleteAction = null },
            title = { Text("точно удалить? (рисунок)") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDeleteAction = null
                    deleteAction()
                }) {
                    Text("да")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteAction = null }) {
                    Text("нет")
                }
            },
        )
    }
}

private fun defaultImportFrame(canvasView: DrawingCanvasView?): SketchBounds {
    val canvasWidth = canvasView?.width?.toFloat()?.takeIf { it > 0f } ?: 1200f
    val canvasHeight = canvasView?.height?.toFloat()?.takeIf { it > 0f } ?: 900f
    return Geometry.fitBoundsInside(
        sourceWidth = 900f,
        sourceHeight = 1200f,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        padding = 64f,
    )
}

@Composable
private fun BoxScope.ObjectActionBar(
    target: DrawableObject,
    canvasWidth: Int,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onRotate: () -> Unit,
) {
    val bounds = target.bounds()
    val canRotate = target !is ArrowObject
    val barWidth = if (canRotate) 296 else 212
    val maxX = (canvasWidth - barWidth - 8).coerceAtLeast(8)
    val x = bounds.left.roundToInt().coerceIn(8, maxX)
    val y = (bounds.top.roundToInt() - 220).coerceAtLeast(8)

    Surface(
        modifier = Modifier
            .offset { IntOffset(x, y) }
            .width(barWidth.dp),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 6.dp,
        color = Color.White,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDelete) {
                Text("Удалить")
            }
            TextButton(onClick = onDuplicate) {
                Text("Дублировать")
            }
            if (canRotate) {
                TextButton(onClick = onRotate) {
                    Text("Вращать")
                }
            }
        }
    }
}

@Composable
private fun ActionBar(
    sets: List<DrawingSet>,
    selectedSetId: String,
    onSelectSet: (DrawingSet) -> Unit,
    onCreateSet: () -> Unit,
    onOpenDrawings: () -> Unit,
    onRenameSet: () -> Unit,
    onDeleteDrawing: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(onClick = onCreateSet, shape = RoundedCornerShape(6.dp)) {
            Text("+ лекция")
        }
        sets.forEach { set ->
            ToolButton(set.title, selectedSetId == set.id) { onSelectSet(set) }
        }
        OutlinedButton(onClick = onOpenDrawings, shape = RoundedCornerShape(6.dp)) {
            Text("Рисунки")
        }
        OutlinedButton(onClick = onRenameSet, shape = RoundedCornerShape(6.dp)) {
            Text("Название")
        }
        OutlinedButton(onClick = onDeleteDrawing, shape = RoundedCornerShape(6.dp)) {
            Text("Удалить")
        }
    }
}

@Composable
private fun SavePanelButton(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onExportZip: () -> Unit,
    onSaveZip: () -> Unit,
    onImportZip: () -> Unit,
) {
    Box {
        IconButton(onClick = { onExpandedChange(!expanded) }) {
            Image(
                painter = painterResource(R.drawable.send_icon),
                contentDescription = "Скинуть ZIP",
                modifier = Modifier.size(28.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            SavePanelItem("Открыть ZIP") {
                onExpandedChange(false)
                onImportZip()
            }
            SavePanelItem("Скачать ZIP") {
                onExpandedChange(false)
                onSaveZip()
            }
            SavePanelItem("Скинуть ZIP") {
                onExpandedChange(false)
                onExportZip()
            }
        }
    }
}

@Composable
private fun SavePanelItem(label: String, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
    )
}

@Composable
private fun ObjectMenuButton(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    activeShapeId: String,
    onShapeSelected: (String) -> Unit,
) {
    Box {
        IconButton(onClick = { onExpandedChange(!expanded) }) {
            Image(
                painter = painterResource(R.drawable.object_cube),
                contentDescription = "Объекты",
                modifier = Modifier.size(28.dp),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            ShapeRegistry.tools.forEach { tool ->
                DropdownMenuItem(
                    text = { Text(tool.title) },
                    onClick = {
                        onExpandedChange(false)
                        onShapeSelected(tool.id)
                    },
                    leadingIcon = {
                        if (activeShapeId == tool.id) {
                            Text("✓")
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ToolBar(
    modifier: Modifier = Modifier,
    activeTool: DrawingTool,
    onToolSelected: (DrawingTool) -> Unit,
    penStrokeWidth: Float,
    onPenStrokeWidthChange: (Float) -> Unit,
    eraserRadius: Float,
    onEraserRadiusChange: (Float) -> Unit,
    textValue: String,
    onTextValueChange: (String) -> Unit,
    italic: Boolean,
    onItalicChange: (Boolean) -> Unit,
    vectorText: Boolean,
    onVectorTextChange: (Boolean) -> Unit,
) {
    Column(
        modifier = modifier
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolButton("Выбор", activeTool == DrawingTool.Select) { onToolSelected(DrawingTool.Select) }
            ToolButton("Стилус", activeTool == DrawingTool.Pen) { onToolSelected(DrawingTool.Pen) }
            ToolButton("Ластик", activeTool == DrawingTool.Eraser) { onToolSelected(DrawingTool.Eraser) }
            ToolButton("Текст", activeTool == DrawingTool.Text) { onToolSelected(DrawingTool.Text) }
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = textValue,
                onValueChange = onTextValueChange,
                modifier = Modifier.width(180.dp),
                singleLine = true,
                label = { Text("Буква / символ") },
            )
            Checkbox(checked = italic, onCheckedChange = onItalicChange)
            Text("курсив")
            Checkbox(checked = vectorText, onCheckedChange = onVectorTextChange)
            Text("вектор")
            ToolSlider(
                label = "Перо",
                value = penStrokeWidth,
                range = 2f..18f,
                onValueChange = onPenStrokeWidthChange,
            )
            ToolSlider(
                label = "Ластик",
                value = eraserRadius,
                range = 12f..80f,
                onValueChange = onEraserRadiusChange,
            )
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SymbolPalette.symbols.forEach { preset ->
                OutlinedButton(
                    onClick = {
                        onTextValueChange(preset.value)
                        onToolSelected(DrawingTool.Text)
                    },
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Text(preset.label)
                }
            }
        }
    }
}

@Composable
private fun ToolSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
) {
    Row(
        modifier = Modifier.width(260.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$label ${value.toInt()}")
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DrawingPickerDialog(
    set: DrawingSet,
    selectedDrawingId: String,
    onClose: () -> Unit,
    onSelectDrawing: (DrawingDocument) -> Unit,
    onAddDrawing: () -> Unit,
    onDeleteDrawing: (DrawingDocument) -> Unit,
    onRenameDrawing: (DrawingDocument) -> Unit,
) {
    Dialog(onDismissRequest = onClose) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(8.dp),
            shadowElevation = 8.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(set.title, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = onAddDrawing) { Text("+ рисунок") }
                    TextButton(onClick = onClose) { Text("Закрыть") }
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(set.drawings) { document ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (document.id == selectedDrawingId) Color.Black else Color(0xFFCCCCCC)),
                            shape = RoundedCornerShape(6.dp),
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                AndroidView(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .height(110.dp)
                                        .background(Color.White)
                                        .border(1.dp, Color(0xFFE0E0E0)),
                                    factory = { DocumentPreviewView(it) },
                                    update = { it.document = document },
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(document.title)
                                    Text("${document.objects.size} объектов", style = MaterialTheme.typography.bodySmall)
                                }
                                Button(onClick = { onSelectDrawing(document) }) { Text("Открыть") }
                                TextButton(onClick = { onRenameDrawing(document) }) { Text("Название") }
                                TextButton(onClick = { onDeleteDrawing(document) }) { Text("Удалить") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolButton(title: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, shape = RoundedCornerShape(6.dp)) { Text(title) }
    } else {
        OutlinedButton(onClick = onClick, shape = RoundedCornerShape(6.dp)) { Text(title) }
    }
}

@Composable
private fun BoxScope.ExportPreview(bitmap: Bitmap, onClose: () -> Unit) {
    Surface(
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(24.dp)
            .width(220.dp),
        shadowElevation = 6.dp,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("Предпросмотр экспорта")
            Spacer(modifier = Modifier.height(8.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(Color(0xFFE8E8E8)),
            ) {
                drawImage(bitmap.asImageBitmap())
                drawRect(Color.Black, style = Stroke(width = 1f))
            }
            TextButton(onClick = onClose) { Text("Закрыть") }
        }
    }
}
