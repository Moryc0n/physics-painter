package com.fedbaq.physicssketch.core.tools

import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolPaletteTest {
    @Test
    fun containsPhiAngleSymbols() {
        val symbols = SymbolPalette.symbols.map { it.value }

        assertTrue(symbols.contains("φ"))
        assertTrue(symbols.contains("Φ"))
    }

    @Test
    fun containsTextIndexMarkers() {
        val symbols = SymbolPalette.categories.first { it.title == "Математика" }.symbols.map { it.value }

        assertTrue(symbols.contains("_"))
        assertTrue(symbols.contains("^"))
    }

    @Test
    fun groupsCommonPhysicsLabelsByCategory() {
        val mechanics = SymbolPalette.categories.first { it.title == "Механика" }.symbols.map { it.value }
        val electricity = SymbolPalette.categories.first { it.title == "Электричество" }.symbols.map { it.value }

        assertTrue(mechanics.contains("Fₜр"))
        assertTrue(mechanics.contains("v₀"))
        assertTrue(electricity.contains("q"))
        assertTrue(electricity.contains("E"))
    }
}
