package com.essential.shortcuts

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.Gravity
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.text.DecimalFormat
import kotlin.math.*

class ScientificCalculatorActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private lateinit var tvExpression: TextView
    private lateinit var gridButtons: GridLayout

    private var currentInput = ""
    private var lastResult = "0"
    private var isNewCalculation = true
    private var isRadians = false // Default to Degrees

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scientific_calculator)

        tvResult = findViewById(R.id.tv_result)
        tvExpression = findViewById(R.id.tv_expression)
        gridButtons = findViewById(R.id.grid_buttons)

        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        btnBack.setOnClickListener { finish() }

        setupCalculatorButtons()
    }

    private fun setupCalculatorButtons() {
        // Layout imitating Casio fx-991cw layout
        // 5 columns
        val buttons = listOf(
            // Row 1 (Nav style)
            ButtonData("SHIFT", ButtonType.SHIFT, R.color.sci_fi_btn_nav_bg, R.color.sci_fi_display_text), // Yellowish text normally? Keep cyan
            ButtonData("ALPHA", ButtonType.ALPHA, R.color.sci_fi_btn_nav_bg, R.color.category_text_tools), // Red text
            ButtonData("RAD/DEG", ButtonType.TOGGLE_ANGLE, R.color.sci_fi_btn_nav_bg, R.color.sci_fi_btn_nav_text),
            ButtonData("MENU", ButtonType.MENU, R.color.sci_fi_btn_nav_bg, R.color.sci_fi_btn_nav_text),
            ButtonData("ON", ButtonType.ON, R.color.sci_fi_btn_nav_bg, R.color.sci_fi_btn_nav_text),

            // Row 2 (Sci)
            ButtonData("x⁻¹", ButtonType.FUNCTION, R.color.sci_fi_btn_sci_bg, R.color.sci_fi_btn_sci_text),
            ButtonData("√", ButtonType.FUNCTION, R.color.sci_fi_btn_sci_bg, R.color.sci_fi_btn_sci_text),
            ButtonData("x²", ButtonType.FUNCTION_APPEND, R.color.sci_fi_btn_sci_bg, R.color.sci_fi_btn_sci_text, "^2"),
            ButtonData("^", ButtonType.OPERATOR, R.color.sci_fi_btn_sci_bg, R.color.sci_fi_btn_sci_text),
            ButtonData("log", ButtonType.FUNCTION, R.color.sci_fi_btn_sci_bg, R.color.sci_fi_btn_sci_text),

            // Row 3 (Sci)
            ButtonData("ln", ButtonType.FUNCTION, R.color.sci_fi_btn_sci_bg, R.color.sci_fi_btn_sci_text),
            ButtonData("(-)", ButtonType.FUNCTION_APPEND, R.color.sci_fi_btn_sci_bg, R.color.sci_fi_btn_sci_text, "-"), // unary minus
            ButtonData("x³", ButtonType.FUNCTION_APPEND, R.color.sci_fi_btn_sci_bg, R.color.sci_fi_btn_sci_text, "^3"),
            ButtonData("hyp", ButtonType.MENU, R.color.sci_fi_btn_sci_bg, R.color.sci_fi_btn_sci_text),
            ButtonData("sin", ButtonType.FUNCTION, R.color.sci_fi_btn_sci_bg, R.color.sci_fi_btn_sci_text),

            // Row 4 (Sci)
            ButtonData("cos", ButtonType.FUNCTION, R.color.sci_fi_btn_sci_bg, R.color.sci_fi_btn_sci_text),
            ButtonData("tan", ButtonType.FUNCTION, R.color.sci_fi_btn_sci_bg, R.color.sci_fi_btn_sci_text),
            ButtonData("STO", ButtonType.MENU, R.color.sci_fi_btn_sci_bg, R.color.sci_fi_btn_sci_text),
            ButtonData("(", ButtonType.PARENTHESES, R.color.sci_fi_btn_sci_bg, R.color.sci_fi_btn_sci_text),
            ButtonData(")", ButtonType.PARENTHESES, R.color.sci_fi_btn_sci_bg, R.color.sci_fi_btn_sci_text),

            // Row 5 (Numpad top)
            ButtonData("7", ButtonType.NUMBER, R.color.sci_fi_btn_numpad_bg, R.color.sci_fi_btn_numpad_text),
            ButtonData("8", ButtonType.NUMBER, R.color.sci_fi_btn_numpad_bg, R.color.sci_fi_btn_numpad_text),
            ButtonData("9", ButtonType.NUMBER, R.color.sci_fi_btn_numpad_bg, R.color.sci_fi_btn_numpad_text),
            ButtonData("DEL", ButtonType.DELETE, R.color.sci_fi_btn_ac_bg, R.color.sci_fi_btn_ac_text),
            ButtonData("AC", ButtonType.CLEAR, R.color.sci_fi_btn_ac_bg, R.color.sci_fi_btn_ac_text),

            // Row 6 (Numpad mid)
            ButtonData("4", ButtonType.NUMBER, R.color.sci_fi_btn_numpad_bg, R.color.sci_fi_btn_numpad_text),
            ButtonData("5", ButtonType.NUMBER, R.color.sci_fi_btn_numpad_bg, R.color.sci_fi_btn_numpad_text),
            ButtonData("6", ButtonType.NUMBER, R.color.sci_fi_btn_numpad_bg, R.color.sci_fi_btn_numpad_text),
            ButtonData("×", ButtonType.OPERATOR, R.color.sci_fi_btn_operator_bg, R.color.sci_fi_btn_operator_text),
            ButtonData("÷", ButtonType.OPERATOR, R.color.sci_fi_btn_operator_bg, R.color.sci_fi_btn_operator_text),

            // Row 7 (Numpad mid)
            ButtonData("1", ButtonType.NUMBER, R.color.sci_fi_btn_numpad_bg, R.color.sci_fi_btn_numpad_text),
            ButtonData("2", ButtonType.NUMBER, R.color.sci_fi_btn_numpad_bg, R.color.sci_fi_btn_numpad_text),
            ButtonData("3", ButtonType.NUMBER, R.color.sci_fi_btn_numpad_bg, R.color.sci_fi_btn_numpad_text),
            ButtonData("+", ButtonType.OPERATOR, R.color.sci_fi_btn_operator_bg, R.color.sci_fi_btn_operator_text),
            ButtonData("-", ButtonType.OPERATOR, R.color.sci_fi_btn_operator_bg, R.color.sci_fi_btn_operator_text),

            // Row 8 (Numpad bot)
            ButtonData("0", ButtonType.NUMBER, R.color.sci_fi_btn_numpad_bg, R.color.sci_fi_btn_numpad_text),
            ButtonData(".", ButtonType.DECIMAL, R.color.sci_fi_btn_numpad_bg, R.color.sci_fi_btn_numpad_text),
            ButtonData("×10ˣ", ButtonType.FUNCTION_APPEND, R.color.sci_fi_btn_numpad_bg, R.color.sci_fi_btn_numpad_text, "×10^"),
            ButtonData("Ans", ButtonType.ANS, R.color.sci_fi_btn_numpad_bg, R.color.sci_fi_btn_numpad_text),
            ButtonData("=", ButtonType.EQUALS, R.color.sci_fi_btn_equals_bg, R.color.sci_fi_btn_equals_text)
        )

        val columns = 5
        val rows = 8
        var row = 0
        var col = 0

        gridButtons.columnCount = columns
        gridButtons.rowCount = rows
        
        val displayMetrics = resources.displayMetrics
        val cornerRadiusPx = (16 * displayMetrics.density).toInt()

        for (buttonData in buttons) {
            val button = MaterialButton(this)
            
            val params = GridLayout.LayoutParams()
            params.width = 0
            params.height = 0
            params.rowSpec = GridLayout.spec(row, GridLayout.FILL, 1f)
            params.columnSpec = GridLayout.spec(col, GridLayout.FILL, 1f)
            params.setMargins(4, 4, 4, 4)

            button.layoutParams = params
            button.text = buttonData.text
            button.textSize = 14f // Smaller text to fit
            button.isAllCaps = false
            button.cornerRadius = cornerRadiusPx
            button.insetTop = 0
            button.insetBottom = 0 
            
            button.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, buttonData.backgroundColor))
            button.setTextColor(ContextCompat.getColor(this, buttonData.textColor))
            button.elevation = 2f 
            
            button.setOnClickListener { onButtonClick(buttonData) }

            gridButtons.addView(button)

            col++
            if (col >= columns) {
                col = 0
                row++
            }
        }
    }

    private fun onButtonClick(buttonData: ButtonData) {
        when (buttonData.type) {
            ButtonType.NUMBER -> appendInput(buttonData.text)
            ButtonType.DECIMAL -> appendInput(".")
            ButtonType.OPERATOR -> appendOperator(buttonData.text)
            ButtonType.EQUALS -> calculate()
            ButtonType.CLEAR -> clear()
            ButtonType.DELETE -> deleteLast()
            ButtonType.PARENTHESES -> appendInput(buttonData.text)
            ButtonType.FUNCTION -> appendFunction(buttonData.text)
            ButtonType.FUNCTION_APPEND -> appendInput(buttonData.appendValue ?: buttonData.text)
            ButtonType.ANS -> appendInput("Ans")
            ButtonType.TOGGLE_ANGLE -> toggleAngleMode()
            ButtonType.SHIFT, ButtonType.ALPHA, ButtonType.MENU, ButtonType.ON -> { /* visual styling placeholder */ }
        }
    }

    private fun appendInput(value: String) {
        if (isNewCalculation) {
            currentInput = ""
            isNewCalculation = false
        }
        currentInput += value
        updateDisplay()
    }

    private fun appendOperator(op: String) {
        if (isNewCalculation) {
            currentInput = lastResult
            isNewCalculation = false
        }
        currentInput += op
        updateDisplay()
    }

    private fun appendFunction(func: String) {
        if (isNewCalculation) {
            currentInput = ""
            isNewCalculation = false
        }
        currentInput += "$func("
        updateDisplay()
    }

    private fun deleteLast() {
        if (currentInput.isNotEmpty()) {
            currentInput = currentInput.dropLast(1)
            updateDisplay()
        }
    }

    private fun clear() {
        currentInput = ""
        tvResult.text = "0"
        isNewCalculation = true
        updateDisplay()
    }

    private fun toggleAngleMode() {
        isRadians = !isRadians
        val tvMode = findViewById<TextView>(R.id.tv_mode_indicators)
        tvMode.text = if (isRadians) "RAD   MATH" else "DEG   MATH"
        if (currentInput.isNotEmpty()) {
            calculate()
        }
    }

    private fun calculate() {
        if (currentInput.isEmpty()) return
        try {
            // Replace Ans
            var evalStr = currentInput.replace("Ans", lastResult)
            
            val result = evaluateExpression(evalStr)
            val formattedResult = formatResult(result)
            
            tvResult.text = formattedResult
            lastResult = formattedResult
            isNewCalculation = true
            
        } catch (e: Exception) {
            tvResult.text = "Syntax Error"
            isNewCalculation = true
        }
    }

    private fun updateDisplay() {
        tvExpression.text = currentInput
    }

    private fun evaluateExpression(expr: String): Double {
        var evalStr = expr.replace("×", "*").replace("÷", "/")
        
        return object : Any() {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < evalStr.length) evalStr[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < evalStr.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    if (eat('+'.code)) x += parseTerm() 
                    else if (eat('-'.code)) x -= parseTerm() 
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor() 
                    else if (eat('/'.code)) x /= parseFactor() 
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor() 
                if (eat('-'.code)) return -parseFactor() 

                var x: Double
                val startPos = pos
                if (eat('('.code)) { 
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch >= 'a'.code && ch <= 'z'.code) || (ch >= 'A'.code && ch <= 'Z'.code) || ch == '√'.code) {
                    // Start of a function
                    while ((ch >= 'a'.code && ch <= 'z'.code) || (ch >= 'A'.code && ch <= 'Z'.code) || ch == '√'.code || ch == '⁻'.code || ch == '¹'.code) nextChar()
                    val func = evalStr.substring(startPos, pos)
                    
                    if (eat('('.code)) {
                        x = parseExpression()
                        eat(')'.code)
                    } else {
                        x = parseFactor()
                    }
                    
                    x = when (func) {
                        "sqrt", "√" -> sqrt(x)
                        "sin" -> sin(if (isRadians) x else Math.toRadians(x))
                        "cos" -> cos(if (isRadians) x else Math.toRadians(x))
                        "tan" -> tan(if (isRadians) x else Math.toRadians(x))
                        "log" -> log10(x)
                        "ln" -> ln(x)
                        "x⁻¹" -> 1 / x
                        else -> throw RuntimeException("Unknown function: $func")
                    }
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) {
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    x = evalStr.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }

                if (eat('^'.code)) x = x.pow(parseFactor())

                return x
            }
        }.parse()
    }

    private fun formatResult(result: Double): String {
        if (result.isInfinite() || result.isNaN()) return "Math Error"
        val df = DecimalFormat("#.##########")
        return df.format(result)
    }

    data class ButtonData(
        val text: String,
        val type: ButtonType,
        val backgroundColor: Int,
        val textColor: Int,
        val appendValue: String? = null
    )

    enum class ButtonType {
        NUMBER, DECIMAL, OPERATOR, EQUALS, CLEAR, DELETE, PARENTHESES, FUNCTION, FUNCTION_APPEND, ANS, TOGGLE_ANGLE, SHIFT, ALPHA, MENU, ON
    }
}
