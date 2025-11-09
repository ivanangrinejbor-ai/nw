package org.catrobat.catroid.utils.lunoscript

import android.util.Log


class Parser(private val tokens: List<Token>) {
    private var current = 0

    private class ParseError(message: String, token: Token) :
        LunoSyntaxError(message, token.line, token.position)

    fun parse(): ProgramNode {
        val statements = mutableListOf<Statement>()
        val startLine = if (tokens.isNotEmpty() && tokens[0].type != TokenType.EOF) tokens[0].line else 1
        Log.d("LunoParser-Trace", "--- STARTING PARSE ---")
        while (!isAtEnd()) {
            skipComments()
            if (isAtEnd()) break
            try {
                declaration()?.let { statements.add(it) }
            } catch (error: LunoSyntaxError) {
                println("Parser encountered an error: ${error.message}")
                synchronize()
            } catch (error: ParseError) {
                println("Parser encountered an error: ${error.message}")
                synchronize()
            }
        }
        Log.d("LunoParser-Trace", "--- FINISHED PARSE ---")
        return ProgramNode(statements, startLine)
    }

    private fun declaration(): Statement? {
        try {
            skipComments()
            if (isAtEnd()) return null
            if (match(TokenType.FUN)) return funDeclaration("function")
            if (match(TokenType.CLASS)) return classDeclaration()
            if (match(TokenType.VAR)) return varDeclaration()
            if (match(TokenType.IMPORT)) return importStatement()
            return statement()
        } catch (e: ParseError) {
            synchronize()
            return null
        }
    }

    private fun tryStatement(): Statement {
        val tryToken = previous()


        consume(TokenType.LBRACE, "Expect '{' before 'try' body.")
        val tryBlockBody = blockStatements()
        consume(TokenType.RBRACE, "Expect '}' after 'try' body.")
        val tryBlock = BlockStatement(tryBlockBody, tryToken.line)

        var catchVariable: Token? = null
        var catchBlock: Statement? = null
        if (match(TokenType.CATCH)) {
            consume(TokenType.LPAREN, "Expect '(' after 'catch'.")
            catchVariable = consume(TokenType.IDENTIFIER, "Expect exception variable name.")
            consume(TokenType.RPAREN, "Expect ')' after exception variable.")


            consume(TokenType.LBRACE, "Expect '{' before 'catch' body.")
            val catchBlockBody = blockStatements()
            consume(TokenType.RBRACE, "Expect '}' after 'catch' body.")
            catchBlock = BlockStatement(catchBlockBody, catchVariable.line)
        }

        var finallyBlock: Statement? = null
        if (match(TokenType.FINALLY)) {

            consume(TokenType.LBRACE, "Expect '{' before 'finally' body.")
            val finallyBlockBody = blockStatements()
            consume(TokenType.RBRACE, "Expect '}' after 'finally' body.")
            finallyBlock = BlockStatement(finallyBlockBody, previous().line)
        }

        if (catchBlock == null && finallyBlock == null) {
            throw error(tryToken, "A 'try' statement must have at least a 'catch' or a 'finally' block.")
        }


        return TryCatchStatement(tryBlock, catchVariable, catchBlock, finallyBlock, tryToken.line)
    }

    private fun importStatement(): Statement {
        val importToken = previous()
        val path = mutableListOf<Token>()


        do {
            path.add(consume(TokenType.IDENTIFIER, "Expect package or class name."))
        } while (match(TokenType.DOT))

        consumeSemicolon("Expect ';' or newline after import statement.")
        return ImportStatement(path, importToken.line)
    }

    private fun skipComments() {
        while (check(TokenType.COMMENT)) {
            advance()
        }
    }

    private fun classDeclaration(): Statement {
        val classToken = previous()
        val name = consume(TokenType.IDENTIFIER, "Expect class name.")
        Log.d("LunoParser-Trace", ">>> Entering classDeclaration for '${name.lexeme}'")
        val superclass = if (match(TokenType.LT)) {
            VariableExpr(consume(TokenType.IDENTIFIER, "Expect superclass name."), previous().line)
        } else null

        consume(TokenType.LBRACE, "Expect '{' before class body.")

        var staticBlock: BlockStatement? = null
        if (match(TokenType.STATIC)) {
            val staticToken = previous()
            consume(TokenType.LBRACE, "Expect '{' after 'static'.")
            val statements = blockStatements()
            consume(TokenType.RBRACE, "Expect '}' after static block.")
            staticBlock = BlockStatement(statements, staticToken.line)
        }

        val methods = mutableListOf<FunDeclarationStatement>()

        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            skipComments()
            if (check(TokenType.RBRACE)) break

            try {
                if (match(TokenType.FUN)) {
                    methods.add(funDeclaration("method") as FunDeclarationStatement)
                } else {
                    throw error(peek(), "Only 'fun' declarations are allowed inside a class body.")
                }
            } catch (e: ParseError) {
                println("Parser error inside class '${name.lexeme}': ${e.message}")
                synchronize()
            }
        }

        consume(TokenType.RBRACE, "Expect '}' after class body.")
        Log.d("LunoParser-Trace", "<<< Exiting classDeclaration for '${name.lexeme}'")
        return ClassDeclarationStatement(name, methods, superclass, staticBlock, classToken.line)
    }

    private fun functionExpression(): Expression {
        val funToken = previous()

        consume(TokenType.LPAREN, "Expect '(' after 'fun' for lambda expression.")
        val parameters = mutableListOf<Token>()
        if (!check(TokenType.RPAREN)) {
            do {
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."))
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RPAREN, "Expect ')' after parameters.")

        consume(TokenType.LBRACE, "Expect '{' before lambda body.")
        val bodyStatements = blockStatements()
        consume(TokenType.RBRACE, "Expect '}' after lambda body.")

        val body = BlockStatement(bodyStatements, funToken.line)
        return LambdaExpr(parameters, body, funToken.line)
    }

    private fun funDeclaration(kind: String): Statement {
        val funToken = previous()
        val name = consume(TokenType.IDENTIFIER, "Expect $kind name.")
        Log.d("LunoParser-Trace", ">>> Entering funDeclaration for '${name.lexeme}' (kind: $kind)")
        consume(TokenType.LPAREN, "Expect '(' after $kind name.")
        val parameters = mutableListOf<Token>()
        if (!check(TokenType.RPAREN)) {
            do {
                if (parameters.size >= 255) error(peek(), "Cannot have more than 255 parameters.")
                parameters.add(consume(TokenType.IDENTIFIER, "Expect parameter name."))
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RPAREN, "Expect ')' after parameters.")
        consume(TokenType.LBRACE, "Expect '{' before $kind body.")

        val bodyStatements = blockStatements()

        consume(TokenType.RBRACE, "Expect '}' after $kind body.")

        val body = BlockStatement(bodyStatements, funToken.line)
        Log.d("LunoParser-Trace", "<<< Exiting funDeclaration for '${name.lexeme}'")
        return FunDeclarationStatement(name, parameters, body, funToken.line)
    }

    private fun varDeclaration(): Statement {
        val keyword = previous()
        val isConstant = keyword.lexeme == "val"

        val name = consume(TokenType.IDENTIFIER, "Expect variable name.")
        Log.d("LunoParser", "Creating VarDeclarationStatement for '${name.lexeme}'")
        val initializer = if (match(TokenType.ASSIGN)) expression() else null
        consumeSemicolon("Expect ';' or newline after variable declaration.")

        return VarDeclarationStatement(name, initializer, isConstant, name.line)
    }

    private fun ifStatement(): Statement {
        val ifToken = previous()
        consume(TokenType.LPAREN, "Expect '(' after 'if'.")
        val condition = expression()
        consume(TokenType.RPAREN, "Expect ')' after if condition.")
        val thenBranch = statement()
        val elseBranch = if (match(TokenType.ELSE)) statement() else null
        return IfStatement(condition, thenBranch, elseBranch, ifToken, ifToken.line)
    }

    private fun whileStatement(): Statement {
        val whileToken = previous()
        consume(TokenType.LPAREN, "Expect '(' after 'while'.")
        val condition = expression()
        consume(TokenType.RPAREN, "Expect ')' after while condition.")
        val body = statement()
        return WhileStatement(condition, body, whileToken, whileToken.line)
    }

    private fun forStatement(): Statement {
        val forToken = previous()
        consume(TokenType.LPAREN, "Expect '(' after 'for'.")
        val variable = consume(TokenType.IDENTIFIER, "Expect loop variable name.")
        consume(TokenType.IN, "Expect 'in' after loop variable.")
        val iterable = expression()
        consume(TokenType.RPAREN, "Expect ')' after for-in expression.")
        val body = statement()
        return ForInStatement(variable, iterable, body, forToken, forToken.line)
    }

    private fun switchStatement(): Statement {
        val switchToken = previous()
        consume(TokenType.LPAREN, "Expect '(' after 'switch'.")
        val expr = expression()
        consume(TokenType.RPAREN, "Expect ')' after switch expression.")
        consume(TokenType.LBRACE, "Expect '{' before switch cases.")

        val cases = mutableListOf<SwitchCase>()
        var defaultCase: SwitchCase? = null

        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            val caseOrDefaultToken = peek()
            val isDefault: Boolean
            val values = mutableListOf<Expression>()

            if (match(TokenType.CASE)) {
                isDefault = false
                do {
                    values.add(expression())
                } while (match(TokenType.COMMA) && !check(TokenType.COLON))
            } else if (match(TokenType.DEFAULT)) {
                isDefault = true
                if (defaultCase != null) throw error(previous(), "Multiple default cases in switch statement.")
            } else {
                throw error(peek(), "Expect 'case' or 'default'.")
            }

            consume(TokenType.COLON, "Expect ':' after case/default.")
            val caseStatements = mutableListOf<Statement>()

            val caseBody = statement()

            val currentCase = SwitchCase(if (isDefault) null else values, caseBody, caseOrDefaultToken, isDefault)
            if (isDefault) defaultCase = currentCase else cases.add(currentCase)
        }
        consume(TokenType.RBRACE, "Expect '}' after switch body.")
        if (defaultCase != null) cases.add(defaultCase)

        return SwitchStatement(expr, cases, switchToken, switchToken.line)
    }

    private fun returnStatement(): Statement {
        val keyword = previous()
        val value = if (!check(TokenType.SEMICOLON) && !check(TokenType.RBRACE)) expression() else null
        consumeSemicolon("Expect ';' or newline after return value.")
        return ReturnStatement(keyword, value, keyword.line)
    }

    private fun blockStatements(): List<Statement> {
        Log.d("LunoParser-Trace", "    [BS] >> Entering blockStatements. Looking for RBRACE. Current token: ${peek().lexeme}")
        val statements = mutableListOf<Statement>()
        while (!check(TokenType.RBRACE) && !isAtEnd()) {
            declaration()?.let { statements.add(it) }
        }
        Log.d("LunoParser-Trace", "    [BS] << Exiting blockStatements. Current token: ${peek().lexeme}")
        return statements
    }

    private fun breakStatement(): Statement {
        val keyword = previous()
        consumeSemicolon("Expect ';' or newline after 'break'.")
        return BreakStatement(keyword, keyword.line)
    }

    private fun continueStatement(): Statement {
        val keyword = previous()
        consumeSemicolon("Expect ';' or newline after 'continue'.")
        return ContinueStatement(keyword, keyword.line)
    }


    private fun expressionStatement(): Statement {
        val expr = call()

        if (peek().type.isAssignmentOperator()) {
            Log.d("LunoParser", "Creating AssignmentStatement for target: $expr")
            if (!(expr is VariableExpr || expr is GetExpr || expr is IndexAccessExpr || expr is ThisExpr)) {
                throw error(peek(), "Invalid assignment target. Left side must be a variable, property, or index access.")
            }
            val operator = advance()
            val value = expression()
            consumeSemicolon("Expect ';' or newline after assignment statement.")
            return AssignmentStatement(expr, value, operator, operator.line)
        } else {
            Log.d("LunoParser", "Creating ExpressionStatement for expr: $expr")
            consumeSemicolon("Expect ';' or newline after expression statement.")
            return ExpressionStatement(expr, expr.line)
        }
    }


    private fun logicalOr(): Expression {
        var expr = logicalAnd()
        while (match(TokenType.OR)) {
            val operator = previous()
            val right = logicalAnd()
            expr = LogicalExpr(expr, operator, right, operator.line)
        }
        return expr
    }

    private fun logicalAnd(): Expression {
        var expr = equality()
        while (match(TokenType.AND)) {
            val operator = previous()
            val right = equality()
            expr = LogicalExpr(expr, operator, right, operator.line)
        }
        return expr
    }

    private fun equality(): Expression {
        var expr = comparison()
        while (match(TokenType.NEQ, TokenType.EQ)) {
            val operator = previous()
            val right = comparison()
            expr = BinaryExpr(expr, operator, right, operator.line)
        }
        return expr
    }

    private fun comparison(): Expression {
        var expr = term()
        while (match(TokenType.GT, TokenType.GTE, TokenType.LT, TokenType.LTE)) {
            val operator = previous()
            val right = term()
            expr = BinaryExpr(expr, operator, right, operator.line)
        }
        return expr
    }

    private fun term(): Expression {
        var expr = factor()
        while (match(TokenType.MINUS, TokenType.PLUS)) {
            val operator = previous()
            val right = factor()
            expr = BinaryExpr(expr, operator, right, operator.line)
        }
        return expr
    }

    private fun factor(): Expression {
        var expr = unary()
        while (match(TokenType.DIVIDE, TokenType.MULTIPLY, TokenType.MODULO)) {
            val operator = previous()
            val right = unary()
            expr = BinaryExpr(expr, operator, right, operator.line)
        }
        return expr
    }

    private fun unary(): Expression {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            val operator = previous()
            val right = unary()
            return UnaryExpr(operator, right, operator.line)
        }
        return call()
    }

    private fun call(): Expression {
        var expr = primary()
        while (true) {
            when {
                match(TokenType.LPAREN) -> expr = finishCall(expr)
                match(TokenType.LBRACKET) -> expr = finishIndexAccess(expr)
                match(TokenType.DOT) -> {
                    if (match(TokenType.CLASS)) {




                    } else {
                        val name = consume(TokenType.IDENTIFIER, "Expect property name after '.'.")
                        expr = GetExpr(expr, name, name.line)
                    }
                }
                else -> break
            }
        }
        return expr
    }

    private fun finishCall(callee: Expression): Expression {
        val arguments = mutableListOf<Expression>()
        val lparenToken = previous()
        if (!check(TokenType.RPAREN)) {
            do {
                if (arguments.size >= 255) error(peek(), "Cannot have more than 255 arguments.")
                arguments.add(expression())
            } while (match(TokenType.COMMA))
        }
        val paren = consume(TokenType.RPAREN, "Expect ')' after arguments.")
        return CallExpr(callee, arguments, paren, lparenToken.line)
    }

    private fun finishIndexAccess(callee: Expression): Expression {
        val bracketToken = previous()
        val index = expression()
        consume(TokenType.RBRACKET, "Expect ']' after index.")
        return IndexAccessExpr(callee, bracketToken, index, bracketToken.line)
    }


    private fun primary(): Expression {
        val token = peek()
        return when {
            match(TokenType.FALSE) -> LiteralExpr(LunoValue.Boolean(false), previous().line)
            match(TokenType.TRUE) -> LiteralExpr(LunoValue.Boolean(true), previous().line)
            match(TokenType.NULL) -> LiteralExpr(LunoValue.Null, previous().line)
            match(TokenType.THIS) -> ThisExpr(previous(), previous().line)
            match(TokenType.NUMBER_LITERAL) -> LiteralExpr(LunoValue.Number(previous().literal as Double), previous().line)
            match(TokenType.FLOAT_LITERAL) -> LiteralExpr(LunoValue.Float(previous().literal as Float), previous().line)
            match(TokenType.STRING_LITERAL) -> LiteralExpr(LunoValue.String(previous().literal as String), previous().line)
            match(TokenType.IDENTIFIER) -> VariableExpr(previous(), previous().line)
            match(TokenType.F_STRING) -> parseFString()
            match(TokenType.FUN) -> functionExpression()
            match(TokenType.LPAREN) -> {
                val lParenLine = previous().line
                val expr = expression()
                consume(TokenType.RPAREN, "Expect ')' after expression.")

                expr
            }
            match(TokenType.LBRACKET) -> listLiteral()




            peek().type == TokenType.LBRACE && isExpressionContext() -> mapLiteral()


            else -> throw error(peek(), "Expect expression, found ${peek().type}.")
        }
    }

    private fun parseFString(): Expression {
        val fStringToken = previous()
        val content = fStringToken.literal as String
        val parts = mutableListOf<Expression>()
        var currentIndex = 0

        while (currentIndex < content.length) {
            val exprStart = content.indexOf('{', currentIndex)

            if (exprStart == -1) {
                if (currentIndex < content.length) {
                    val textPart = content.substring(currentIndex)
                    parts.add(LiteralExpr(LunoValue.String(textPart), fStringToken.line))
                }
                break
            }

            if (exprStart > currentIndex) {
                val textPart = content.substring(currentIndex, exprStart)
                parts.add(LiteralExpr(LunoValue.String(textPart), fStringToken.line))
            }

            val exprEnd = findMatchingBrace(content, exprStart)
            if (exprEnd == -1) {
                throw error(fStringToken, "Unmatched '{' in f-string.")
            }

            val exprCode = content.substring(exprStart + 1, exprEnd)
            if (exprCode.isBlank()) {
                throw error(fStringToken, "Empty expression block '{}' in f-string is not allowed.")
            }

            try {
                val expressionLexer = Lexer(exprCode)
                val expressionTokens = expressionLexer.scanTokens()
                val expressionParser = Parser(expressionTokens)
                val expressionAst = expressionParser.expression()
                parts.add(expressionAst)
            } catch (e: Exception) {
                throw error(fStringToken, "Invalid expression inside f-string: {${exprCode}}")
            }

            currentIndex = exprEnd + 1
        }

        return InterpolatedStringExpr(parts, fStringToken.line)
    }

    private fun findMatchingBrace(content: String, start: Int): Int {
        var braceDepth = 1
        for (i in start + 1 until content.length) {
            when (content[i]) {
                '{' -> braceDepth++
                '}' -> {
                    braceDepth--
                    if (braceDepth == 0) return i
                }
            }
        }
        return -1
    }

    private fun listLiteral(): Expression {
        val bracket = previous()
        val elements = mutableListOf<Expression>()
        if (!check(TokenType.RBRACKET)) {
            do {
                elements.add(expression())
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RBRACKET, "Expect ']' after list elements.")
        return ListLiteralExpr(elements, bracket, bracket.line)
    }

    private fun mapLiteral(): Expression {
        val brace = consume(TokenType.LBRACE, "Expect '{' for map literal.")
        val entries = mutableMapOf<Token, Expression>()
        if (!check(TokenType.RBRACE)) {
            do {

                val keyToken = when {
                    check(TokenType.IDENTIFIER) -> advance()
                    check(TokenType.STRING_LITERAL) -> advance()
                    else -> throw error(peek(), "Expect identifier or string as map key.")
                }
                consume(TokenType.COLON, "Expect ':' after map key.")
                val valueExpr = expression()
                entries[keyToken] = valueExpr
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RBRACE, "Expect '}' after map entries.")
        return MapLiteralExpr(entries, brace, brace.line)
    }



    private var expressionDepth = 0
    private fun isExpressionContext(): Boolean {



        return true
    }





    private fun statement(): Statement {
        skipComments()
        if (isAtEnd()) throw error(peek(), "Unexpected end of file.")


        return when {
            match(TokenType.IF) -> ifStatement()
            match(TokenType.WHILE) -> whileStatement()
            match(TokenType.FOR) -> forStatement()
            match(TokenType.SWITCH) -> switchStatement()
            match(TokenType.RETURN) -> returnStatement()
            match(TokenType.BREAK) -> breakStatement()
            match(TokenType.CONTINUE) -> continueStatement()
            match(TokenType.TRY) -> tryStatement()


            match(TokenType.LBRACE) -> {
                val lbraceToken = previous()
                val statements = blockStatements()
                consume(TokenType.RBRACE, "Expect '}' after block.")
                BlockStatement(statements, lbraceToken.line)
            }


            else -> expressionStatement()
        }
    }

    private fun expression(): Expression = logicalOr()


    private fun TokenType.isAssignmentOperator(): Boolean {
        return this == TokenType.ASSIGN || this == TokenType.PLUS_ASSIGN || this == TokenType.MINUS_ASSIGN ||
                this == TokenType.MULTIPLY_ASSIGN || this == TokenType.DIVIDE_ASSIGN || this == TokenType.MODULO_ASSIGN
    }



    private fun match(vararg types: TokenType): Boolean {
        for (type in types) {
            if (check(type)) {
                advance()
                return true
            }
        }
        return false
    }

    private fun consume(type: TokenType, message: String): Token {
        Log.d("LunoScriptParser", "CONSUME: Attempting to consume ${type}. Current peek is ${peek().type} ('${peek().lexeme}')")
        if (check(type)) {
            val consumedToken = advance()
            Log.d("LunoScriptParser", "CONSUME: Successfully consumed ${consumedToken.type}. Next peek is ${if(!isAtEnd()) peek().type else "EOF"}")
            return consumedToken
        }
        Log.e("LunoScriptParser", "CONSUME_ERROR: Failed to consume ${type}. Peek is ${peek().type}. Throwing error: $message")
        throw error(peek(), message)
    }

    private fun consumeSemicolon(message: String) {
        if (match(TokenType.SEMICOLON)) return

        if (check(TokenType.RBRACE) || isAtEnd() || previous().line < peek().line) return


        if (tokens[current-1].type != TokenType.RBRACE && tokens[current-1].type != TokenType.LBRACE) {


        }
    }


    private fun check(type: TokenType): Boolean = if (isAtEnd()) false else peek().type == type
    private fun advance(): Token {
        if (!isAtEnd()) current++
        return previous()
    }

    private fun isAtEnd(): Boolean = peek().type == TokenType.EOF
    private fun peek(): Token = tokens[current]
    private fun previous(): Token = tokens[current - 1]

    private fun error(token: Token, message: String): ParseError = ParseError(message, token)

    private fun synchronize() {
        val problemToken = peek()
        Log.d("LunoScriptParser", "SYNC: Attempting to recover. Error occurred at or before: ${problemToken.lexeme} (${problemToken.type}) L${problemToken.line}. Previous: ${if(current > 0) previous().lexeme else "N/A"}")

        if (!isAtEnd()) {
            advance()
        }


        while (!isAtEnd()) {

            if (previous().type == TokenType.SEMICOLON) {
                Log.d("LunoScriptParser", "SYNC: Resuming after semicolon.")
                return
            }


            when (peek().type) {
                TokenType.CLASS,
                TokenType.FUN,
                TokenType.VAR,
                TokenType.FOR,
                TokenType.IF,
                TokenType.WHILE,
                TokenType.SWITCH,
                TokenType.RETURN,
                TokenType.TRY,
                TokenType.RBRACE -> {
                    Log.d("LunoScriptParser", "SYNC: Resuming at keyword ${peek().type}.")
                    return
                }
                else -> {
                    // Ничего не делаем, просто пропускаем токен
                }
            }
            advance()
        }
        Log.d("LunoScriptParser", "SYNC: Recovery reached EOF.")
    }
}