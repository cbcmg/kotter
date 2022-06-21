import com.varabyte.kotter.foundation.collections.liveListOf
import com.varabyte.kotter.foundation.input.*
import com.varabyte.kotter.foundation.session
import com.varabyte.kotter.foundation.liveVarOf
import com.varabyte.kotter.foundation.runUntilSignal
import com.varabyte.kotter.foundation.text.*
import com.varabyte.kotter.runtime.MainRenderScope
import java.net.URI

fun main() = session {
    // Scenario #1 - trivial but common case. A section exists to request a single input from the user
    run {
        var wantsToLearn by liveVarOf(false)
        section {
            text("Would you like to ");
            cyan {
                underline()
                anchor(URI("https://github.com/varabyte/kotter"))
                text("learn ")
                bold()
                text("Kotter")
                // anchor(URI("https://github.com/varabyte/kotter"), "Kotter") << a simpler usage
            };
            textLine("? (Y/n)")
            text("> "); input(Completions("yes", "no"), initialText = "y")

            if (wantsToLearn) {
                yellow(isBright = true) { p { textLine("""\(^o^)/""") } }
            }
        }.runUntilInputEntered {
            onInputEntered { wantsToLearn = "yes".startsWith(input.lowercase()) }
        }

        if (!wantsToLearn) return@session
    }

    // Scenario #2 - Recycling the same input to request multiple values.
    run {
        val numbers = liveListOf<Int>()
        var isDone by liveVarOf(false)

        section {
            cyan()
            textLine("Keeping typing numbers and we'll calculate the sum of them all.")
            textLine("Press ENTER on an empty line to finish.")
            textLine()
        }.run()

        section {
            if (!isDone) {
                numbers.forEach { num ->
                    textLine("   $num")
                }
                text(" + "); input(); textLine()
            } else {
                numbers.forEachIndexed { i, num ->
                    if (i < numbers.lastIndex) {
                        text("   ")
                    } else {
                        text(" + ")
                    }
                    textLine("$num")
                }
            }
            val sum = numbers.sum()
            textLine("-".repeat(sum.toString().length + 3))
            textLine("   $sum")
            textLine()
        }.runUntilSignal {
            onInputChanged { if (input.toIntOrNull() == null) rejectInput() }
            onInputEntered {
                numbers.add(input.toIntOrNull() ?: 0)
                if (input.isEmpty()) { isDone = true; signal() }
                clearInput() // Clear the input, so we can start collecting the next number
            }
        }
    }

    // Scenario #3 - Multiple inputs in the same block. Only one can be active at a time!
    run {
        section {
            cyan()
            textLine("Type in RGB values to choose a color.")
            textLine("Colors must be in the range 0-255.")
            textLine("Press enter to confirm a color value.")
            textLine("Use arrows to navigate between fields.")
            textLine("Press ESC when finished.")
            textLine()
        }.run()

        var selectedLine by liveVarOf(0)
        var warning by liveVarOf<String?>(null)
        val colors = liveListOf(0, 128, 255)

        // Has to extend "MainRenderScope" and not just "RenderScope" for access to the "input" function
        fun MainRenderScope.colorInput(line: Int, prompt: String) {
            scopedState {
                if (selectedLine == line) bold();
                text("$prompt: ")
                input(id = line, initialText = colors[line].toString(), isActive = selectedLine == line)
                textLine()
            }
        }

        // To allow for multiple inputs in the same block, they must all have unique IDs.
        // IDs can be any value -- use whatever you want! Here, we just use the current line number as each input's ID.
        section {
            colorInput(line = 0, "R")
            colorInput(line = 1, "G")
            colorInput(line = 2, "B")
            text("Result: ")
            scopedState {
                rgb(colors[0], colors[1], colors[2])
                invert()
                textLine("COLOR")
            }
            textLine()
            warning?.let { warning ->
                yellow { textLine(warning) }
                textLine()
            }
        }.runUntilSignal {
            onInputChanged { if (input.isNotBlank() && input.toIntOrNull() == null) rejectInput() }
            onInputEntered {
                warning = null
                val num = input.toIntOrNull() ?: 0
                if (num in 0..255) {
                    colors[selectedLine] = num
                } else {
                    warning = "Color values must be between 0 and 255."
                }
            }
            onInputDeactivated {
                val line = id as Int
                input = colors[line].toString()
            }
            onKeyPressed {
                when (key) {
                    Keys.UP -> selectedLine = (selectedLine + 3 - 1) % 3
                    Keys.DOWN -> selectedLine = (selectedLine + 1) % 3
                    Keys.ESC -> signal()
                }
            }
        }
    }
}