package com.fedbaq.physicssketch.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class LocalDrawingStore(context: Context) {
    private val storageFile = File(context.filesDir, "physics_sketch_library.json")
    private val json = Json {
        classDiscriminator = "type"
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    fun load(): SketchLibrary {
        if (!storageFile.exists()) return SketchLibrary.initial()
        return runCatching {
            json.decodeFromString<SketchLibrary>(storageFile.readText())
        }.getOrElse {
            SketchLibrary.initial()
        }
    }

    fun save(library: SketchLibrary) {
        storageFile.writeText(json.encodeToString(library))
    }
}
