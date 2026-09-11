package com.fedbaq.physicssketch.core.tools

data class SymbolPreset(
    val value: String,
    val label: String = value,
)

data class SymbolCategory(
    val title: String,
    val symbols: List<SymbolPreset>,
)

object SymbolPalette {
    val categories: List<SymbolCategory> = listOf(
        SymbolCategory(
            title = "Греческие",
            symbols = listOf(
                SymbolPreset("α"),
                SymbolPreset("β"),
                SymbolPreset("γ"),
                SymbolPreset("θ"),
                SymbolPreset("φ"),
                SymbolPreset("Φ"),
                SymbolPreset("π"),
                SymbolPreset("ω"),
                SymbolPreset("Δ"),
                SymbolPreset("Σ"),
            ),
        ),
        SymbolCategory(
            title = "Математика",
            symbols = listOf(
                SymbolPreset("∫"),
                SymbolPreset("∂"),
                SymbolPreset("∞"),
                SymbolPreset("°"),
                SymbolPreset("→"),
                SymbolPreset("⊥"),
                SymbolPreset("∥"),
                SymbolPreset("≈"),
                SymbolPreset("≠"),
                SymbolPreset("≤"),
                SymbolPreset("≥"),
                SymbolPreset("_"),
                SymbolPreset("^"),
            ),
        ),
        SymbolCategory(
            title = "Механика",
            symbols = listOf(
                SymbolPreset("F"),
                SymbolPreset("Fₜр"),
                SymbolPreset("N"),
                SymbolPreset("P"),
                SymbolPreset("v"),
                SymbolPreset("v₀"),
                SymbolPreset("a"),
                SymbolPreset("m"),
                SymbolPreset("g"),
                SymbolPreset("t"),
                SymbolPreset("s"),
            ),
        ),
        SymbolCategory(
            title = "Электричество",
            symbols = listOf(
                SymbolPreset("q"),
                SymbolPreset("I"),
                SymbolPreset("U"),
                SymbolPreset("R"),
                SymbolPreset("E"),
                SymbolPreset("B"),
                SymbolPreset("φ"),
                SymbolPreset("ε"),
            ),
        ),
    )

    val symbols: List<SymbolPreset> = categories.flatMap { it.symbols }
}
