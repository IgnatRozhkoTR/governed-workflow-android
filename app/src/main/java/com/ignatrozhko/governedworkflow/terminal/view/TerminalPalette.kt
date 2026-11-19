package com.ignatrozhko.governedworkflow.terminal.view

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * xterm.js colors for one app theme. Background and text are the Checkpoint terminal tokens; the ANSI colors reuse
 * the status tokens (green ok, red danger, yellow warn, blue action) so tool output reads like the rest of the app.
 */
enum class TerminalPalette(
    val background: String,
    private val foreground: String,
    private val selection: String,
    private val dim: String,
    private val black: String,
    private val white: String,
    private val brightWhite: String,
    private val red: String,
    private val green: String,
    private val yellow: String,
    private val blue: String,
    private val magenta: String,
    private val cyan: String,
) {
    Night(
        background = "#090C11",
        foreground = "#D7DEE8",
        selection = "rgba(142, 162, 255, 0.35)",
        dim = "#9BA6B5",
        black = "#1C2430",
        white = "#D7DEE8",
        brightWhite = "#FFFFFF",
        red = "#FF9A94",
        green = "#7FD1A6",
        yellow = "#E5C07B",
        blue = "#8EA2FF",
        magenta = "#C9A2FF",
        cyan = "#7FCFDB",
    ),
    Day(
        background = "#FBFBF9",
        foreground = "#1B222D",
        selection = "rgba(43, 75, 255, 0.22)",
        dim = "#566070",
        black = "#1B222D",
        white = "#E9EBE6",
        brightWhite = "#FBFBF9",
        red = "#B3261E",
        green = "#1E7A4E",
        yellow = "#7A5B00",
        blue = "#2B4BFF",
        magenta = "#8A3FB8",
        cyan = "#0F6E7D",
    ),
    ;

    fun toXtermTheme(): JsonObject = buildJsonObject {
        put("background", background)
        put("foreground", foreground)
        put("cursor", foreground)
        put("cursorAccent", background)
        put("selectionBackground", selection)
        put("black", black)
        put("white", white)
        put("brightBlack", dim)
        put("brightWhite", brightWhite)
        listOf("red" to red, "green" to green, "yellow" to yellow, "blue" to blue, "magenta" to magenta, "cyan" to cyan).forEach { (name, color) ->
            put(name, color)
            put("bright${name.replaceFirstChar(Char::uppercase)}", color)
        }
    }
}
