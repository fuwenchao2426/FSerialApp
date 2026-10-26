package com.example.formula

import kotlin.math.*

/**
 * High-performance, safe mathematical formula evaluation engine.
 * Supports:
 * - Variable placeholder: [x] (case-insensitive)
 * - Basic operators: +, -, *, /, %, ()
 * - Bitwise operators: &, |, ~, ^, <<, >>
 * - Conditional ternary: if(condition, trueValue, falseValue)
 * - Comparisons: ==, !=, <, <=, >, >=
 * - Logical operators: &&, ||, !
 * - Formatting functions: Abs, Floor, Ceiling, Round(val, digits), Truncate
 * - Trigonometric: Sin, Cos, Tan, Asin, Acos, Atan
 * - Exponential/Logarithmic: Exp, Pow(base, exp), Sqrt, Log(val, base), Log10(val)
 */
object FormulaEvaluator {

    /**
     * Evaluates the formula given raw value [x].
     * Returns fallback (original x) if evaluation fails or formula is empty.
     */
    fun evaluate(formula: String?, x: Double, fallback: Double = x): Double {
        if (formula.isNullOrBlank()) return x
        val trimmed = formula.trim()
        if (trimmed == "[x]" || trimmed == "[X]" || trimmed == "x" || trimmed == "X") return x

        return try {
            val parser = Parser(trimmed, x)
            val result = parser.parseExpression()
            if (result.isNaN() || result.isInfinite()) fallback else result
        } catch (_: Exception) {
            fallback
        }
    }

    /**
     * Validates whether a formula is syntactically sound.
     * Returns null if valid, or an error message string if invalid.
     */
    fun validate(formula: String?): String? {
        if (formula.isNullOrBlank()) return null
        return try {
            val parser = Parser(formula, 100.0)
            val result = parser.parseExpression()
            if (result.isNaN() || result.isInfinite()) "计算结果产生非数或溢出" else null
        } catch (e: Exception) {
            e.message ?: "语法错误"
        }
    }

    private class Parser(private val input: String, private val xVal: Double) {
        private var pos = 0
        private val len = input.length

        fun parseExpression(): Double {
            val result = parseLogicalOr()
            skipWhitespace()
            return result
        }

        private fun parseLogicalOr(): Double {
            var left = parseLogicalAnd()
            while (true) {
                skipWhitespace()
                if (match("||")) {
                    val right = parseLogicalAnd()
                    left = if (toBool(left) || toBool(right)) 1.0 else 0.0
                } else {
                    break
                }
            }
            return left
        }

        private fun parseLogicalAnd(): Double {
            var left = parseBitwiseOr()
            while (true) {
                skipWhitespace()
                if (match("&&")) {
                    val right = parseBitwiseOr()
                    left = if (toBool(left) && toBool(right)) 1.0 else 0.0
                } else {
                    break
                }
            }
            return left
        }

        private fun parseBitwiseOr(): Double {
            var left = parseBitwiseXor()
            while (true) {
                skipWhitespace()
                if (peek() == '|' && peekNext() != '|') {
                    pos++
                    val right = parseBitwiseXor()
                    left = (left.toLong() or right.toLong()).toDouble()
                } else {
                    break
                }
            }
            return left
        }

        private fun parseBitwiseXor(): Double {
            var left = parseBitwiseAnd()
            while (true) {
                skipWhitespace()
                if (peek() == '^') {
                    pos++
                    val right = parseBitwiseAnd()
                    left = (left.toLong() xor right.toLong()).toDouble()
                } else {
                    break
                }
            }
            return left
        }

        private fun parseBitwiseAnd(): Double {
            var left = parseEquality()
            while (true) {
                skipWhitespace()
                if (peek() == '&' && peekNext() != '&') {
                    pos++
                    val right = parseEquality()
                    left = (left.toLong() and right.toLong()).toDouble()
                } else {
                    break
                }
            }
            return left
        }

        private fun parseEquality(): Double {
            var left = parseRelational()
            while (true) {
                skipWhitespace()
                if (match("==")) {
                    val right = parseRelational()
                    left = if (abs(left - right) < 1e-9) 1.0 else 0.0
                } else if (match("!=")) {
                    val right = parseRelational()
                    left = if (abs(left - right) >= 1e-9) 1.0 else 0.0
                } else {
                    break
                }
            }
            return left
        }

        private fun parseRelational(): Double {
            var left = parseShift()
            while (true) {
                skipWhitespace()
                if (match("<=")) {
                    val right = parseShift()
                    left = if (left <= right) 1.0 else 0.0
                } else if (match(">=")) {
                    val right = parseShift()
                    left = if (left >= right) 1.0 else 0.0
                } else if (peek() == '<' && peekNext() != '<') {
                    pos++
                    val right = parseShift()
                    left = if (left < right) 1.0 else 0.0
                } else if (peek() == '>' && peekNext() != '>') {
                    pos++
                    val right = parseShift()
                    left = if (left > right) 1.0 else 0.0
                } else {
                    break
                }
            }
            return left
        }

        private fun parseShift(): Double {
            var left = parseAdditive()
            while (true) {
                skipWhitespace()
                if (match("<<")) {
                    val right = parseAdditive()
                    left = (left.toLong() shl right.toInt()).toDouble()
                } else if (match(">>")) {
                    val right = parseAdditive()
                    left = (left.toLong() shr right.toInt()).toDouble()
                } else {
                    break
                }
            }
            return left
        }

        private fun parseAdditive(): Double {
            var left = parseMultiplicative()
            while (true) {
                skipWhitespace()
                val c = peek()
                if (c == '+') {
                    pos++
                    left += parseMultiplicative()
                } else if (c == '-') {
                    pos++
                    left -= parseMultiplicative()
                } else {
                    break
                }
            }
            return left
        }

        private fun parseMultiplicative(): Double {
            var left = parseUnary()
            while (true) {
                skipWhitespace()
                val c = peek()
                if (c == '*') {
                    pos++
                    left *= parseUnary()
                } else if (c == '/') {
                    pos++
                    val right = parseUnary()
                    left = if (right == 0.0) 0.0 else left / right
                } else if (c == '%') {
                    pos++
                    val right = parseUnary()
                    left = if (right == 0.0) 0.0 else left % right
                } else {
                    break
                }
            }
            return left
        }

        private fun parseUnary(): Double {
            skipWhitespace()
            val c = peek()
            if (c == '+') {
                pos++
                return parseUnary()
            }
            if (c == '-') {
                pos++
                return -parseUnary()
            }
            if (c == '!') {
                pos++
                return if (toBool(parseUnary())) 0.0 else 1.0
            }
            if (c == '~') {
                pos++
                return (parseUnary().toLong().inv()).toDouble()
            }
            return parsePrimary()
        }

        private fun parsePrimary(): Double {
            skipWhitespace()
            val c = peek()

            // Grouping: ( expr )
            if (c == '(') {
                pos++
                val value = parseLogicalOr()
                skipWhitespace()
                if (peek() == ')') pos++
                return value
            }

            // Variable: [x] or [X]
            if (c == '[') {
                pos++
                skipWhitespace()
                val varName = readIdentifier()
                skipWhitespace()
                if (peek() == ']') pos++
                return if (varName.equals("x", ignoreCase = true)) xVal else 0.0
            }

            // Numbers
            if (c.isDigit() || (c == '.' && peekNext().isDigit())) {
                return readNumber()
            }

            // Identifiers / Functions (if, Sin, Cos, Round, etc.)
            if (c.isLetter() || c == '_') {
                val ident = readIdentifier()
                skipWhitespace()
                if (peek() == '(') {
                    pos++ // skip '('
                    return evalFunction(ident)
                } else {
                    if (ident.equals("x", ignoreCase = true)) return xVal
                    if (ident.equals("pi", ignoreCase = true)) return Math.PI
                    if (ident.equals("e", ignoreCase = true)) return Math.E
                    return 0.0
                }
            }

            // Advance to prevent infinite loop on unexpected char
            if (pos < len) pos++
            return 0.0
        }

        private fun evalFunction(name: String): Double {
            val args = mutableListOf<Double>()
            skipWhitespace()
            if (peek() != ')') {
                while (true) {
                    args.add(parseLogicalOr())
                    skipWhitespace()
                    if (peek() == ',') {
                        pos++
                        skipWhitespace()
                    } else {
                        break
                    }
                }
            }
            if (peek() == ')') pos++

            val upper = name.uppercase()
            return when (upper) {
                "IF" -> {
                    val cond = args.getOrElse(0) { 0.0 }
                    val trueVal = args.getOrElse(1) { 0.0 }
                    val falseVal = args.getOrElse(2) { 0.0 }
                    if (toBool(cond)) trueVal else falseVal
                }
                "ABS" -> abs(args.getOrElse(0) { 0.0 })
                "FLOOR" -> floor(args.getOrElse(0) { 0.0 })
                "CEILING", "CEIL" -> ceil(args.getOrElse(0) { 0.0 })
                "ROUND" -> {
                    val v = args.getOrElse(0) { 0.0 }
                    val decimals = args.getOrElse(1) { 0.0 }.toInt()
                    val factor = 10.0.pow(decimals.toDouble())
                    (v * factor).roundToLong() / factor
                }
                "TRUNCATE", "TRUNC" -> {
                    val v = args.getOrElse(0) { 0.0 }
                    if (v >= 0) floor(v) else ceil(v)
                }
                "SIN" -> sin(args.getOrElse(0) { 0.0 })
                "COS" -> cos(args.getOrElse(0) { 0.0 })
                "TAN" -> tan(args.getOrElse(0) { 0.0 })
                "ASIN" -> asin(args.getOrElse(0) { 0.0 })
                "ACOS" -> acos(args.getOrElse(0) { 0.0 })
                "ATAN" -> atan(args.getOrElse(0) { 0.0 })
                "EXP" -> exp(args.getOrElse(0) { 0.0 })
                "POW" -> {
                    val base = args.getOrElse(0) { 0.0 }
                    val exp = args.getOrElse(1) { 1.0 }
                    base.pow(exp)
                }
                "SQRT" -> {
                    val v = args.getOrElse(0) { 0.0 }
                    if (v < 0) 0.0 else sqrt(v)
                }
                "LOG" -> {
                    val v = args.getOrElse(0) { 1.0 }
                    val base = args.getOrElse(1) { Math.E }
                    if (v <= 0 || base <= 0 || base == 1.0) 0.0 else ln(v) / ln(base)
                }
                "LOG10" -> {
                    val v = args.getOrElse(0) { 1.0 }
                    if (v <= 0) 0.0 else log10(v)
                }
                else -> args.getOrElse(0) { 0.0 }
            }
        }

        private fun readNumber(): Double {
            val start = pos
            while (pos < len && (input[pos].isDigit() || input[pos] == '.')) {
                pos++
            }
            return input.substring(start, pos).toDoubleOrNull() ?: 0.0
        }

        private fun readIdentifier(): String {
            val start = pos
            while (pos < len && (input[pos].isLetterOrDigit() || input[pos] == '_')) {
                pos++
            }
            return input.substring(start, pos)
        }

        private fun skipWhitespace() {
            while (pos < len && input[pos].isWhitespace()) {
                pos++
            }
        }

        private fun peek(): Char = if (pos < len) input[pos] else '\u0000'

        private fun peekNext(): Char = if (pos + 1 < len) input[pos + 1] else '\u0000'

        private fun match(expected: String): Boolean {
            if (pos + expected.length <= len && input.substring(pos, pos + expected.length) == expected) {
                pos += expected.length
                return true
            }
            return false
        }

        private fun toBool(value: Double): Boolean = abs(value) > 1e-9
    }
}
