# Physics Sketch Design

## Goal

Build a minimal Android drawing app for physics lecture illustrations. The first version must let the user draw with a stylus, add and move common geometry objects, add text/math symbols, and export transparent PNG images.

## Product Decisions

- Platform order: finish the Android app first, then add a separate Apple tablet version.
- The Apple tablet version reuses the same drawing/document data model so lecture sets can be moved between Android and iPad later.
- The editor canvas is visually white, but transparent export omits the white background.
- Export buttons are named `Экспортировать без фона` and `Экспортировать с фоном`.
- All drawing content is black in v1.
- Imported PNG/PDF pages are treated as editable background objects: the app draws annotations over them, not into the original file.
- Times New Roman is loaded from `assets/fonts/times_new_roman.ttf` when present, otherwise Android `serif` is used.

## Architecture

- UI uses Jetpack Compose for panels, dialogs, toolbars, and navigation.
- The drawing surface uses a custom Android `View` wrapped in Compose, because stylus events, long press movement, hit testing, PDF/PNG rendering, and bitmap export need exact control.
- Drawing data stays vector-first. A drawing is a list of `DrawableObject` items, and PNG export renders the current vector state.
- Android is implemented first. The iPad version comes later as a separate port that reads the same portable JSON object format.

## Main Modules

- `core/model`: persistent drawing objects and documents.
- `core/geometry`: hit testing, bounds, translation, angle math, and shape helpers.
- `core/tools`: editor tools and `ShapeRegistry.kt`, the obvious extension point for new object types.
- `core/render`: Android Canvas rendering and bitmap export.
- `ui`: Compose shell plus `DrawingCanvasView`.

## First Implementation Stage

- Create the Android Kotlin project skeleton.
- Implement vector drawing objects: freehand stroke, line, arrow, circle, rectangle, text, angle marker, image/page placeholder.
- Implement `ShapeRegistry.kt` with built-in shape tools.
- Implement the custom canvas with stylus drawing, object insertion, selection, object menu, long-click movement, duplication, deletion, and transparent/white PNG export entry points.
- Keep persistence file-based for now; Room can be added later only if search, tags, or larger libraries make it useful.

## Current Android V2 Slice

- Sets and drawings are stored in local app files as JSON.
- Polymorphic drawing objects use stable type names such as `arrow`, `line`, `circle`, `text`, and `imagePage`.
- PNG/PDF import adds an `ImagePageObject` under the current drawing. Multi-page PDF import asks for the page number and stores it as `pageIndex`.
- Imported PNG/PDF page objects can be moved by long press and resized from the object menu.
- Drawings inside a lecture set can be renamed from the full-screen drawing picker.
- Undo/redo is managed per open drawing and covers edits that pass through the canvas document update path.
- The eraser removes whole non-freehand objects and cuts freehand strokes into remaining pieces.
- Pen width and eraser radius are adjustable from the main toolbar.
- ZIP export contains `set.json`, each drawing JSON file, and transparent PNG renders for every drawing in the set.

## Later Apple Tablet Stage

- Create a separate iPad project after Android has the core editor, storage, import, and ZIP export working.
- Keep the saved drawing format compatible with Android instead of tying project files to Android-only classes.
- Port the UI concepts, not the Android implementation details: Pencil drawing surface, shape tools, object menu, lecture set browser, and transparent PNG export.
