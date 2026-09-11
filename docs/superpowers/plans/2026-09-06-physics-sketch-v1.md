# Physics Sketch V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the first runnable Kotlin Android version of a minimal physics lecture sketch editor.

**Architecture:** Compose owns app chrome and dialogs. A custom Android Canvas view owns precise pointer handling, hit testing, object editing, and bitmap export. Drawing content is vector-first with one registry file for adding new shape tools.

**Tech Stack:** Kotlin, Android Gradle Plugin, Jetpack Compose, Android Canvas, Kotlin serialization-ready data classes.

**Spec:** `docs/superpowers/specs/2026-09-06-physics-sketch-design.md`

## Global Constraints

- Project lives under `users/telegram/2063198410/PhysicsSketchApp`.
- Finish Android first. Add the Apple tablet copy only after the Android app has the core editor, storage, import, and ZIP export working.
- Keep the drawing/document model portable enough that an iPad version can reuse the saved project format later.
- Export buttons use `Экспортировать без фона` and `Экспортировать с фоном`.
- Drawing content is black only.
- Editor canvas is white, transparent export omits the white background.
- New shapes are registered in `ShapeRegistry.kt`.

---

### Task 1: Project Skeleton

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/colors.xml`

**Interfaces:**
- Produces a standard Android application module named `app`.

- [x] Create Gradle and manifest files for a minimal Compose Android app.

### Task 2: Core Model And Geometry

**Files:**
- Create: `app/src/main/java/com/fedbaq/physicssketch/core/model/DrawingDocument.kt`
- Create: `app/src/main/java/com/fedbaq/physicssketch/core/model/DrawableObject.kt`
- Create: `app/src/main/java/com/fedbaq/physicssketch/core/model/DrawingObjects.kt`
- Create: `app/src/main/java/com/fedbaq/physicssketch/core/geometry/Geometry.kt`

**Interfaces:**
- Produces `DrawableObject`, `DrawingDocument`, and concrete vector objects.
- Produces geometry helpers for distance, bounds, translation, and line angle calculation.

- [x] Add model classes before UI so the editor has a clean data boundary.

### Task 3: Shape Registry And Tools

**Files:**
- Create: `app/src/main/java/com/fedbaq/physicssketch/core/tools/DrawingTool.kt`
- Create: `app/src/main/java/com/fedbaq/physicssketch/core/tools/ShapeRegistry.kt`

**Interfaces:**
- Produces one obvious place to add new shape object factories.

- [x] Register arrow, circle, rectangle, vertical line, horizontal line, and diagonal line.

### Task 4: Rendering And Export

**Files:**
- Create: `app/src/main/java/com/fedbaq/physicssketch/core/render/DrawingRenderer.kt`

**Interfaces:**
- Produces `DrawingRenderer.render(...)` and `DrawingRenderer.exportBitmap(...)`.

- [x] Render vector objects on an Android Canvas.
- [x] Support white-background and transparent-background export.

### Task 5: Editor UI

**Files:**
- Create: `app/src/main/java/com/fedbaq/physicssketch/MainActivity.kt`
- Create: `app/src/main/java/com/fedbaq/physicssketch/ui/DrawingCanvasView.kt`
- Create: `app/src/main/java/com/fedbaq/physicssketch/ui/EditorScreen.kt`

**Interfaces:**
- Produces a usable first screen with left set panel, top tools, canvas, text options, object menu, and export buttons.

- [x] Compose main editor shell.
- [x] Custom view pointer handling for stylus/freehand, shape insertion, selection, long-click movement, delete, duplicate, and move mode.

### Task 6: Local Verification

**Files:**
- Read existing project files.

**Interfaces:**
- Produces a concise handoff explaining what was built and what still needs Gradle/Android Studio verification.

- [x] Inspect project file structure.
- [x] Report verification limits if Gradle is not run.

### Task 7: Portable Storage And Set Management

**Files:**
- Create: `app/src/main/java/com/fedbaq/physicssketch/core/model/DrawingSet.kt`
- Create: `app/src/main/java/com/fedbaq/physicssketch/data/SketchLibrary.kt`
- Create: `app/src/main/java/com/fedbaq/physicssketch/data/LocalDrawingStore.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/ui/EditorScreen.kt`

**Interfaces:**
- Produces JSON-backed drawing sets that can later be reused by an iPad port.

- [x] Add stable Kotlin serialization for drawing objects.
- [x] Add create, rename, delete set actions.
- [x] Add drawing picker with full-content preview cards.
- [x] Persist library changes to local app storage.

### Task 8: Import And ZIP Export

**Files:**
- Create: `app/src/main/java/com/fedbaq/physicssketch/core/render/ZipExporter.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/core/render/DrawingRenderer.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/ui/EditorScreen.kt`

**Interfaces:**
- Produces `Открыть PNG/PDF` and ZIP export for the selected drawing set.

- [x] Add PNG import through Android document picker.
- [x] Add first-page PDF import through Android document picker.
- [x] Render imported image/page objects on canvas, previews, and export.
- [x] Export selected set as ZIP with JSON and transparent PNG files.

### Task 9: Drawing Rename And Page Object Sizing

**Files:**
- Modify: `app/src/main/java/com/fedbaq/physicssketch/core/geometry/Geometry.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/core/model/DrawingSet.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/core/model/DrawingObjects.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/ui/DrawingCanvasView.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/ui/EditorScreen.kt`
- Test: `app/src/test/java/com/fedbaq/physicssketch/core/model/DrawingSetTest.kt`
- Test: `app/src/test/java/com/fedbaq/physicssketch/core/geometry/GeometryTest.kt`

**Interfaces:**
- Produces drawing rename from the picker and page-object size controls for imported PNG/PDF backgrounds.

- [x] Add tests for drawing rename and bounds fitting/scaling helpers.
- [x] Add `DrawingSet.renameDrawing`.
- [x] Add geometry helpers for centered scaling and canvas fitting.
- [x] Add object menu controls: `Меньше`, `Больше`, `Вписать`.
- [x] Use a fitted default frame for newly imported PNG/PDF pages.

### Task 10: Undo Redo And Eraser

**Files:**
- Create: `app/src/main/java/com/fedbaq/physicssketch/core/model/DrawingHistory.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/core/tools/DrawingTool.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/ui/DrawingCanvasView.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/ui/EditorScreen.kt`
- Test: `app/src/test/java/com/fedbaq/physicssketch/core/model/DrawingHistoryTest.kt`

**Interfaces:**
- Produces per-document undo/redo history and an object eraser tool.

- [x] Add tests for undo, redo, and clearing redo after a new edit.
- [x] Add portable `DrawingHistory`.
- [x] Add toolbar buttons `Назад`, `Вперёд`, and `Ластик`.
- [x] Wire canvas edits through history.
- [x] Make the eraser remove the top object under the pointer.

### Task 11: Freehand Stroke Erasing

**Files:**
- Modify: `app/src/main/java/com/fedbaq/physicssketch/core/model/DrawingDocument.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/core/model/DrawingObjects.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/ui/DrawingCanvasView.kt`
- Test: `app/src/test/java/com/fedbaq/physicssketch/core/model/FreehandStrokeEraserTest.kt`

**Interfaces:**
- Produces partial erasing for freehand strokes while keeping object erasing for shapes/text/pages.

- [x] Add tests for splitting a freehand stroke and dropping tiny one-point fragments.
- [x] Add `FreehandStroke.erasedBy`.
- [x] Add `DrawingDocument.replaceObjectWith`.
- [x] Make eraser drag call `eraseAt` continuously.
- [x] Keep non-freehand erasing as whole-object deletion.

### Task 12: Pen And Eraser Size Controls

**Files:**
- Modify: `app/src/main/java/com/fedbaq/physicssketch/core/model/DrawingObjects.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/core/render/DrawingRenderer.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/ui/DrawingCanvasView.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/ui/EditorScreen.kt`
- Test: `app/src/test/java/com/fedbaq/physicssketch/core/model/FreehandStrokeEraserTest.kt`

**Interfaces:**
- Produces adjustable pen width and eraser radius from the toolbar.

- [x] Add a test showing erased stroke pieces keep the original pen width.
- [x] Add `strokeWidth` to `FreehandStroke` with a default for old JSON files.
- [x] Render freehand strokes with their stored width.
- [x] Pass `penStrokeWidth` and `eraserRadius` from Compose into `DrawingCanvasView`.
- [x] Add toolbar sliders for `Перо` and `Ластик`.

### Task 10: File Picker Save And ZIP Import

**Files:**
- Create: `app/src/main/java/com/fedbaq/physicssketch/data/PortableSetArchive.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/core/render/PngExporter.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/core/render/ZipExporter.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/ui/EditorScreen.kt`
- Test: `app/src/test/java/com/fedbaq/physicssketch/data/PortableSetArchiveTest.kt`

**Interfaces:**
- Produces system-picker saving for PNG/ZIP and archive import for lecture sets.

- [x] Extract portable ZIP JSON read/write logic.
- [x] Add a unit test for ZIP JSON round-trip.
- [x] Add `Сохранить без фона` and `Сохранить с фоном`.
- [x] Add `Сохранить ZIP`.
- [x] Add `Импорт ZIP`.

### Task 11: PDF Page Selection

**Files:**
- Create: `app/src/main/java/com/fedbaq/physicssketch/core/render/PdfPageReader.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/core/model/DrawingObjects.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/core/render/DrawingRenderer.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/ui/EditorScreen.kt`
- Test: `app/src/test/java/com/fedbaq/physicssketch/core/model/DrawingSerializationTest.kt`

**Interfaces:**
- Produces page-aware PDF import and rendering.

- [x] Add `ImagePageObject.pageIndex` with default `0`.
- [x] Add serialization coverage for `pageIndex`.
- [x] Read PDF page count through `PdfRenderer`.
- [x] Ask for page number before inserting a multi-page PDF.
- [x] Render the stored PDF page index instead of always page `0`.

### Task 12: Physics Symbol Presets

**Files:**
- Modify: `app/src/main/java/com/fedbaq/physicssketch/core/tools/SymbolPalette.kt`
- Modify: `app/src/main/java/com/fedbaq/physicssketch/ui/EditorScreen.kt`
- Test: `app/src/test/java/com/fedbaq/physicssketch/core/tools/SymbolPaletteTest.kt`

**Interfaces:**
- Produces categorized text presets for common physics and math labels.

- [x] Add categories for Greek, math, mechanics, and electricity labels.
- [x] Add toolbar preset buttons that select the text tool.
