package com.essential.shortcuts

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.text.DecimalFormat
import kotlin.math.pow
import kotlin.math.sqrt

class BasicCalculatorActivity : AppCompatActivity() {

    private lateinit var tvResult: TextView
    private lateinit var tvExpression: TextView
    private lateinit var tvHistory: TextView
    private lateinit var gridButtons: GridLayout
    private lateinit var scrollHistory: ScrollView

    private var currentInput = ""
    private var expression = ""
    private var lastResult = "0"
    private var isNewCalculation = true
    private val historyList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_basic_calculator)

        tvResult = findViewById(R.id.tv_result)
        tvExpression = findViewById(R.id.tv_expression)
        tvHistory = findViewById(R.id.tv_history)
        gridButtons = findViewById(R.id.grid_buttons)
        scrollHistory = findViewById(R.id.scroll_history)

        tvHistory.movementMethod = ScrollingMovementMethod()

        val btnBack = findViewById<ImageButton>(R.id.btn_back)
        btnBack.setOnClickListener { finish() }

        setupCalculatorButtons()
    }

    private fun setupCalculatorButtons() {
        // Modern Layout: 
        // Row 1: AC, ( ), %, ÷
        // Row 2: 7, 8, 9, ×
        // Row 3: 4, 5, 6, -
        // Row 4: 1, 2, 3, +
        // Row 5: 0, ., ⌫, =
        
        val buttons = listOf(
            ButtonData("AC", ButtonType.CLEAR, R.color.calc_btn_function_bg, R.color.calc_btn_function_text),
            ButtonData("( )", ButtonType.PARENTHESES, R.color.calc_btn_function_bg, R.color.calc_btn_function_text),
            ButtonData("^", ButtonType.POWER, R.color.calc_btn_function_bg, R.color.calc_btn_function_text),
            ButtonData("÷", ButtonType.OPERATOR, R.color.calc_btn_operator_bg, R.color.calc_btn_operator_text),

            ButtonData("7", ButtonType.NUMBER, R.color.calc_btn_number_bg, R.color.calc_btn_number_text),
            ButtonData("8", ButtonType.NUMBER, R.color.calc_btn_number_bg, R.color.calc_btn_number_text),
            ButtonData("9", ButtonType.NUMBER, R.color.calc_btn_number_bg, R.color.calc_btn_number_text),
            ButtonData("×", ButtonType.OPERATOR, R.color.calc_btn_operator_bg, R.color.calc_btn_operator_text),

            ButtonData("4", ButtonType.NUMBER, R.color.calc_btn_number_bg, R.color.calc_btn_number_text),
            ButtonData("5", ButtonType.NUMBER, R.color.calc_btn_number_bg, R.color.calc_btn_number_text),
            ButtonData("6", ButtonType.NUMBER, R.color.calc_btn_number_bg, R.color.calc_btn_number_text),
            ButtonData("-", ButtonType.OPERATOR, R.color.calc_btn_operator_bg, R.color.calc_btn_operator_text),

            ButtonData("1", ButtonType.NUMBER, R.color.calc_btn_number_bg, R.color.calc_btn_number_text),
            ButtonData("2", ButtonType.NUMBER, R.color.calc_btn_number_bg, R.color.calc_btn_number_text),
            ButtonData("3", ButtonType.NUMBER, R.color.calc_btn_number_bg, R.color.calc_btn_number_text),
            ButtonData("+", ButtonType.OPERATOR, R.color.calc_btn_operator_bg, R.color.calc_btn_operator_text),

            ButtonData("0", ButtonType.NUMBER, R.color.calc_btn_number_bg, R.color.calc_btn_number_text),
            ButtonData(".", ButtonType.DECIMAL, R.color.calc_btn_number_bg, R.color.calc_btn_number_text),
            ButtonData("√", ButtonType.SQRT, R.color.calc_btn_function_bg, R.color.calc_btn_function_text),
            ButtonData("=", ButtonType.EQUALS, R.color.calc_btn_operator_bg, R.color.calc_btn_operator_text)
        )

        var row = 0
        var col = 0
        val columns = 4
        val rows = 5

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
            params.setMargins(12, 12, 12, 12)

            button.layoutParams = params
            button.text = buttonData.text
            button.textSize = 24f
            button.cornerRadius = cornerRadiusPx
            button.insetTop = 0
            button.insetBottom = 0 
            
            button.setBackgroundColor(ContextCompat.getColor(this, buttonData.backgroundColor))
            button.setTextColor(ContextCompat.getColor(this, buttonData.textColor))
            button.elevation = 4f 
            
            button.setOnClickListener { onButtonClick(buttonData) }

            gridButtons.addView(button)

            col++
            if (col >= 4) {
                col = 0
                row++
            }
        }
    }

    private fun onButtonClick(buttonData: ButtonData) {
        when (buttonData.type) {
            ButtonType.NUMBER -> appendNumber(buttonData.text)
            ButtonType.DECIMAL -> appendDecimal()
            ButtonType.OPERATOR -> appendOperator(buttonData.text)
            ButtonType.EQUALS -> calculate()
            ButtonType.CLEAR -> clear()
            ButtonType.PARENTHESES -> appendParentheses()
            ButtonType.POWER -> appendOperator("^")
            ButtonType.SQRT -> appendFunction("√")
        }
    }

    private fun appendNumber(number: String) {
        if (isNewCalculation) {
            clear()
            isNewCalculation = false
        }
        currentInput += number
        updateExpressionDisplay()
        updateLiveResult()
    }

    private fun appendDecimal() {
        if (isNewCalculation) {
            clear()
            currentInput = "0."
            isNewCalculation = false
        } else {
            // Check if last number segment already has decimal
            val lastNumber = currentInput.split(Regex("[^0-9.]")).lastOrNull() ?: ""
            if (!lastNumber.contains(".")) {
                currentInput += if (currentInput.isEmpty()) "0." else "."
            }
        }
        updateExpressionDisplay()
    }

    private fun appendOperator(op: String) {
        if (isNewCalculation) {
            isNewCalculation = false
            currentInput = lastResult
        }
        if (currentInput.isNotEmpty() && !isOperator(currentInput.last())) {
            currentInput += op
            updateExpressionDisplay()
        } else if (currentInput.isNotEmpty() && isOperator(currentInput.last())) {
            // Replace last operator
            currentInput = currentInput.dropLast(1) + op
            updateExpressionDisplay()
        }
    }

    private fun appendFunction(func: String) {
        if (isNewCalculation) {
            clear()
            isNewCalculation = false
        }
        currentInput += func + "("
        updateExpressionDisplay()
    }

    private fun appendParentheses() {
        if (isNewCalculation) {
            clear()
            isNewCalculation = false
        }
        
        val openCount = currentInput.count { it == '(' }
        val closeCount = currentInput.count { it == ')' }
        
        if (currentInput.isEmpty() || isOperator(currentInput.last()) || currentInput.last() == '(') {
            currentInput += "("
        } else if (openCount > closeCount && !isOperator(currentInput.last())) {
            currentInput += ")"
        } else {
            currentInput += "×("
        }
        updateExpressionDisplay()
    }

    private fun calculate() {
        if (currentInput.isEmpty()) return

        try {
            val result = evaluateExpression(currentInput)
            val formattedResult = formatResult(result)
            
            // Add to history
            addToHistory("$currentInput = $formattedResult")
            
            tvResult.text = formattedResult
            lastResult = formattedResult
            currentInput = formattedResult // Allow chaining
            expression = ""
            isNewCalculation = true
            
        } catch (e: Exception) {
            tvResult.text = "Error"
            isNewCalculation = true
        }
    }

    private fun updateLiveResult() {
        if (currentInput.isEmpty()) {
            tvResult.text = "0"
            return
        }
        try {
            // Simple check to avoid evaluating incomplete expressions excessively
            if (!isOperator(currentInput.last())) {
                val result = evaluateExpression(currentInput)
                tvResult.text = formatResult(result)
            }
        } catch (e: Exception) {
            // Ignore errors during live typing
        }
    }

    private fun evaluateExpression(expr: String): Double {
        // This is a simplified parser. For production, a robust parser (like Shunting Yard) is better.
        // Here we handle basic precedence manually for this demo.
        // Replace symbols for standard evaluation
        var evalStr = expr.replace("×", "*").replace("÷", "/")
        
        // Handle square root: √ needs parsing
        // Evaluating string expressions in Kotlin natively is tricky without libraries.
        // Implementing a basic recursive parser.
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
                    if (eat('+'.code)) x += parseTerm() // addition
                    else if (eat('-'.code)) x -= parseTerm() // subtraction
                    else return x
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    if (eat('*'.code)) x *= parseFactor() // multiplication
                    else if (eat('/'.code)) x /= parseFactor() // division
                    else return x
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor() // unary plus
                if (eat('-'.code)) return -parseFactor() // unary minus

                var x: Double
                val startPos = pos
                if (eat('('.code)) { // parentheses
                    x = parseExpression()
                    eat(')'.code)
                } else if (eat('√'.code)) { // sqrt
                     if (eat('('.code)) {
                        x = sqrt(parseExpression())
                        eat(')'.code)
                    } else {
                        x = sqrt(parseFactor())
                    }
                } else if ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) { // numbers
                    while ((ch >= '0'.code && ch <= '9'.code) || ch == '.'.code) nextChar()
                    x = evalStr.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }

                if (eat('^'.code)) x = x.pow(parseFactor()) // exponentiation

                return x
            }
        }.parse()
    }

    private fun isOperator(c: Char): Boolean {
        return c == '+' || c == '-' || c == '×' || c == '÷' || c == '^'
    }

    private fun addToHistory(entry: String) {
        historyList.add(entry)
        if (historyList.size > 10) historyList.removeAt(0) // Keep last 10
        
        val sb = StringBuilder()
        for (item in historyList) {
            sb.append(item).append("\n\n")
        }
        tvHistory.text = sb.toString()
        
        // Scroll to bottom
        scrollHistory.post {
            scrollHistory.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    private fun formatResult(result: Double): String {
        if (result.isInfinite() || result.isNaN()) return "Error"
        val df = DecimalFormat("#.########")
        return df.format(result)
    }

    private fun clear() {
        currentInput = ""
        expression = ""
        tvResult.text = "0"
        tvExpression.text = ""
        isNewCalculation = true
    }

    private fun updateExpressionDisplay() {
        tvExpression.text = currentInput
    }

    data class ButtonData(
        val text: String,
        val type: ButtonType,
        val backgroundColor: Int,
        val textColor: Int
    )

    enum class ButtonType {
        NUMBER, DECIMAL, OPERATOR, EQUALS, CLEAR, PARENTHESES, POWER, SQRT
    }
}
