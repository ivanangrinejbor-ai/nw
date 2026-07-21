package org.catrobat.catroid.ai.tool

import org.catrobat.catroid.formulaeditor.Formula
import org.catrobat.catroid.formulaeditor.FormulaElement
import org.catrobat.catroid.formulaeditor.FormulaElement.ElementType
import org.catrobat.catroid.formulaeditor.Functions
import org.catrobat.catroid.formulaeditor.Operators

object FormulaParser {

    /** Parse [input] and return a [Formula] wrapping the built [FormulaElement] tree. */
    fun parse(input: String): Formula {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return Formula("0")
        val tokens = tokenize(trimmed)
        val ctx = ParseContext(tokens)
        val tree = parseOr(ctx)
        if (ctx.pos < ctx.tokens.size) {
            throw IllegalArgumentException(
                "Unexpected token at position ${ctx.pos}: '${ctx.tokens[ctx.pos].text}'"
            )
        }
        return Formula(tree)
    }

    // ------------------------------------------------------------- tokens

    private enum class TType { NUM, STR, IDENT, OP, LPAREN, RPAREN, COMMA, EOF }
    private data class Tok(val type: TType, val text: String)

    private val OPERATORS = setOf(
        "+", "-", "*", "/", "^",
        "=", "!=", "<>", "<=", ">=", "<", ">",
        "and", "or", "not", "mod"
    )

    private fun tokenize(input: String): List<Tok> {
        val out = mutableListOf<Tok>()
        var i = 0
        val n = input.length
        while (i < n) {
            val c = input[i]
            when {
                c.isWhitespace() -> i++
                c == '(' -> { out.add(Tok(TType.LPAREN, "(")); i++ }
                c == ')' -> { out.add(Tok(TType.RPAREN, ")")); i++ }
                c == ',' -> { out.add(Tok(TType.COMMA, ",")); i++ }
                c == '"' -> {
                    // string literal
                    val sb = StringBuilder()
                    i++ // skip opening quote
                    while (i < n && input[i] != '"') {
                        if (input[i] == '\\' && i + 1 < n) { sb.append(input[i + 1]); i += 2 }
                        else { sb.append(input[i]); i++ }
                    }
                    if (i < n) i++ // skip closing quote
                    out.add(Tok(TType.STR, sb.toString()))
                }
                c.isDigit() || (c == '.' && i + 1 < n && input[i + 1].isDigit()) -> {
                    val sb = StringBuilder()
                    while (i < n && (input[i].isDigit() || input[i] == '.')) {
                        sb.append(input[i]); i++
                    }
                    out.add(Tok(TType.NUM, sb.toString()))
                }
                c.isLetter() || c == '_' -> {
                    val sb = StringBuilder()
                    while (i < n && (input[i].isLetterOrDigit() || input[i] == '_')) {
                        sb.append(input[i]); i++
                    }
                    val word = sb.toString()
                    val lower = word.lowercase()
                    // Two-char operators spelled as words: "and", "or", "not", "mod"
                    if (lower in OPERATORS) out.add(Tok(TType.OP, lower))
                    else out.add(Tok(TType.IDENT, word))
                }
                // Multi-char operators
                c == '!' && i + 1 < n && input[i + 1] == '=' -> {
                    out.add(Tok(TType.OP, "!=")); i += 2
                }
                c == '<' && i + 1 < n && input[i + 1] == '>' -> {
                    out.add(Tok(TType.OP, "<>")); i += 2
                }
                c == '<' && i + 1 < n && input[i + 1] == '=' -> {
                    out.add(Tok(TType.OP, "<=")); i += 2
                }
                c == '>' && i + 1 < n && input[i + 1] == '=' -> {
                    out.add(Tok(TType.OP, ">=")); i += 2
                }
                c in setOf('+', '-', '*', '/', '^', '=', '<', '>') -> {
                    out.add(Tok(TType.OP, c.toString())); i++
                }
                else -> throw IllegalArgumentException("Unexpected character '$c' at position $i")
            }
        }
        out.add(Tok(TType.EOF, ""))
        return out
    }

    // ------------------------------------------------------------- parser

    private class ParseContext(val tokens: List<Tok>) {
        var pos = 0
        fun peek(): Tok = tokens[pos]
        fun eat(): Tok = tokens[pos++]
        fun match(type: TType, text: String? = null): Boolean {
            val t = tokens[pos]
            return t.type == type && (text == null || t.text.equals(text, ignoreCase = true))
        }
        fun eatIf(type: TType, text: String? = null): Boolean {
            if (match(type, text)) { pos++; return true }
            return false
        }
    }

    // Precedence levels (lowest to highest):
    //  or
    //  and
    //  =, !=, <>, <, <=, >, >=
    //  +, -
    //  *, /, mod
    //  ^ (right-assoc)
    //  unary -, not
    //  primary

    private fun parseOr(ctx: ParseContext): FormulaElement {
        var left = parseAnd(ctx)
        while (ctx.match(TType.OP, "or")) {
            ctx.eat()
            val right = parseAnd(ctx)
            left = binaryOp(Operators.LOGICAL_OR, left, right)
        }
        return left
    }

    private fun parseAnd(ctx: ParseContext): FormulaElement {
        var left = parseComparison(ctx)
        while (ctx.match(TType.OP, "and")) {
            ctx.eat()
            val right = parseComparison(ctx)
            left = binaryOp(Operators.LOGICAL_AND, left, right)
        }
        return left
    }

    private fun parseComparison(ctx: ParseContext): FormulaElement {
        val left = parseAdd(ctx)
        val t = ctx.peek()
        if (t.type == TType.OP && t.text in setOf("=", "!=", "<>", "<", "<=", ">", ">=")) {
            val opText = when (val raw = t.text) {
                "<>" -> "!="
                else -> raw
            }
            ctx.eat()
            val right = parseAdd(ctx)
            return binaryOp(opText, left, right)
        }
        return left
    }

    private fun parseAdd(ctx: ParseContext): FormulaElement {
        var left = parseMul(ctx)
        while (ctx.match(TType.OP, "+") || ctx.match(TType.OP, "-")) {
            val op = ctx.eat().text
            val right = parseMul(ctx)
            left = binaryOp(if (op == "+") Operators.PLUS else Operators.MINUS, left, right)
        }
        return left
    }

    private fun parseMul(ctx: ParseContext): FormulaElement {
        var left = parsePow(ctx)
        while (ctx.match(TType.OP, "*") || ctx.match(TType.OP, "/") || ctx.match(TType.OP, "mod")) {
            val op = ctx.eat().text
            val right = parsePow(ctx)
            val opName = when (op) {
                "*" -> Operators.MULT
                "/" -> Operators.DIVIDE
                else -> Operators.MOD
            }
            left = binaryOp(opName, left, right)
        }
        return left
    }

    private fun parsePow(ctx: ParseContext): FormulaElement {
        val base = parseUnary(ctx)
        if (ctx.match(TType.OP, "^")) {
            ctx.eat()
            val exp = parsePow(ctx) // right-associative
            return binaryOp(Operators.POW, base, exp)
        }
        return base
    }

    private fun parseUnary(ctx: ParseContext): FormulaElement {
        if (ctx.match(TType.OP, "-")) {
            ctx.eat()
            val operand = parseUnary(ctx)
            return FormulaElement(ElementType.OPERATOR, Operators.MINUS.name, null, null, operand)
        }
        if (ctx.match(TType.OP, "not")) {
            ctx.eat()
            val operand = parseUnary(ctx)
            return FormulaElement(ElementType.OPERATOR, Operators.LOGICAL_NOT.name, null, null, operand)
        }
        return parsePrimary(ctx)
    }

    private fun parsePrimary(ctx: ParseContext): FormulaElement {
        val t = ctx.peek()
        // Parenthesised sub-expression
        if (t.type == TType.LPAREN) {
            ctx.eat()
            val inner = parseOr(ctx)
            if (!ctx.eatIf(TType.RPAREN)) {
                throw IllegalArgumentException("Expected ')' after sub-expression")
            }
            return inner
        }
        // Number literal
        if (t.type == TType.NUM) {
            ctx.eat()
            return FormulaElement(ElementType.NUMBER, t.text, null)
        }
        // String literal
        if (t.type == TType.STR) {
            ctx.eat()
            return FormulaElement(ElementType.STRING, t.text, null)
        }
        // Identifier: function call or variable
        if (t.type == TType.IDENT) {
            ctx.eat()
            val identName = t.text
            // Function call?
            if (ctx.match(TType.LPAREN)) {
                ctx.eat()
                val args = mutableListOf<FormulaElement>()
                if (!ctx.match(TType.RPAREN)) {
                    args.add(parseOr(ctx))
                    while (ctx.eatIf(TType.COMMA)) {
                        args.add(parseOr(ctx))
                    }
                }
                if (!ctx.eatIf(TType.RPAREN)) {
                    throw IllegalArgumentException("Expected ')' after function args for '$identName'")
                }
                return buildFunction(identName.uppercase(), args)
            }
            // Constants
            val upper = identName.uppercase()
            if (upper == "TRUE") return FormulaElement(ElementType.FUNCTION, Functions.TRUE.name, null)
            if (upper == "FALSE") return FormulaElement(ElementType.FUNCTION, Functions.FALSE.name, null)
            if (upper == "PI") return FormulaElement(ElementType.FUNCTION, Functions.PI.name, null)
            // Otherwise: user variable
            return FormulaElement(ElementType.USER_VARIABLE, identName, null)
        }
        throw IllegalArgumentException("Unexpected token: '${t.text}'")
    }

    // ------------------------------------------------------------- helpers

    private fun binaryOp(op: Operators, left: FormulaElement, right: FormulaElement): FormulaElement =
        FormulaElement(ElementType.OPERATOR, op.name, null, left, right)

    private fun binaryOp(opName: String, left: FormulaElement, right: FormulaElement): FormulaElement =
        FormulaElement(ElementType.OPERATOR, opName, null, left, right)

    private fun buildFunction(name: String, args: List<FormulaElement>): FormulaElement {
        // Resolve the function name against the Functions enum. If the user wrote
        // "length", we map it to LENGTH; "random" -> RAND, etc.
        val resolved = resolveFunctionName(name)
            ?: throw IllegalArgumentException("Unknown function: '$name'")
        return when (args.size) {
            0 -> FormulaElement(ElementType.FUNCTION, resolved, null)
            1 -> {
                val el = FormulaElement(ElementType.FUNCTION, resolved, null)
                el.leftChild = args[0]
                el
            }
            2 -> {
                val el = FormulaElement(ElementType.FUNCTION, resolved, null)
                el.leftChild = args[0]
                el.rightChild = args[1]
                el
            }
            else -> {
                val el = FormulaElement(ElementType.FUNCTION, resolved, null)
                el.leftChild = args[0]
                el.rightChild = args[1]
                el.additionalChildren = args.drop(2)
                el
            }
        }
    }

    /** Map common aliases (e.g. "RANDOM" -> "RAND", "LENGTH" -> "LENGTH") to the enum name. */
    private fun resolveFunctionName(name: String): String? {
        // Direct match
        if (Functions.values().any { it.name == name }) return name
        // Common aliases the model might use
        val aliases = mapOf(
            "RANDOM" to Functions.RAND.name,
            "LEN" to Functions.LENGTH.name,
            "SQRT" to Functions.SQRT.name,
            "ABS" to Functions.ABS.name,
            "ROUND" to Functions.ROUND.name,
            "FLOOR" to Functions.FLOOR.name,
            "CEIL" to Functions.CEIL.name,
            "MIN" to Functions.MIN.name,
            "MAX" to Functions.MAX.name,
            "CLAMP" to Functions.CLAMP.name,
            "MOD" to Functions.MOD.name,
            "SIN" to Functions.SIN.name,
            "COS" to Functions.COS.name,
            "TAN" to Functions.TAN.name,
            "ARCSIN" to Functions.ARCSIN.name,
            "ARCCOS" to Functions.ARCCOS.name,
            "ARCTAN" to Functions.ARCTAN.name,
            "EXP" to Functions.EXP.name,
            "POWER" to Functions.POWER.name,
            "LN" to Functions.LN.name,
            "LOG" to Functions.LOG.name,
            "JOIN" to Functions.JOIN.name,
            "IF" to Functions.IF_THEN_ELSE.name,
            "IF_THEN_ELSE" to Functions.IF_THEN_ELSE.name,
            "CONTAINS" to Functions.CONTAINS.name,
            "REPLACE" to Functions.REPLACE.name,
            "UPPER" to Functions.UPPER.name,
            "LOWER" to Functions.LOWER.name,
            "REVERSE" to Functions.REVERSE.name,
            "LETTER" to Functions.LETTER.name,
            "SUBTEXT" to Functions.SUBTEXT.name
        )
        return aliases[name]
    }
}
