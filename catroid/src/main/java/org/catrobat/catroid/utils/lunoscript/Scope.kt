package org.catrobat.catroid.utils.lunoscript

class Scope(internal val enclosing: Scope? = null) {
    data class ScopeEntry(val value: LunoValue, val isConstant: Boolean)


    val values: MutableMap<String, ScopeEntry> = mutableMapOf()

    fun assign(nameToken: Token, value: LunoValue): LunoValue {
        val name = nameToken.lexeme
        if (values.containsKey(name)) {
            if (values[name]!!.isConstant) {
                throw LunoRuntimeError("Cannot assign to a constant variable '${name}'.", nameToken.line)
            }
            values[name] = values[name]!!.copy(value = value)
            return value
        }
        if (enclosing != null) {
            return enclosing.assign(nameToken, value)
        }
        throw LunoRuntimeError("Undefined variable '${name}'.", nameToken.line)
    }

    fun define(name: String, value: LunoValue, isConstant: Boolean) {
        android.util.Log.d("LunoScope", "DEFINE: ${if(isConstant) "val" else "var"} '$name' = $value in scope $this")
        if (values.containsKey(name)) {
        }
        values[name] = ScopeEntry(value, isConstant)
    }

    fun define(name: String, value: LunoValue) {
        define(name, value, false)
    }

    fun get(nameToken: Token): LunoValue {
        val name = nameToken.lexeme
        android.util.Log.d("LunoScope", "GET: var '$name' from scope $this (enclosing: $enclosing). Keys: ${values.keys}")
        if (values.containsKey(name)) {
            return values[name]!!.value
        }
        if (enclosing != null) {
            return enclosing.get(nameToken)
        }
        throw LunoRuntimeError("Undefined variable '${name}'.", nameToken.line)
    }

    fun getAt(distance: Int, name: String): LunoValue {
        return ancestor(distance).values[name]?.value ?: LunoValue.Null
    }

    fun assignAt(distance: Int, nameToken: Token, value: LunoValue) {
        val ancestorScope = ancestor(distance)
        val name = nameToken.lexeme

        if (ancestorScope.values.containsKey(name)) {
            if (ancestorScope.values[name]!!.isConstant) {
                throw LunoRuntimeError("Cannot assign to a constant variable '${name}'.", nameToken.line)
            }
            ancestorScope.values[name] = ancestorScope.values[name]!!.copy(value = value)
        } else {
            ancestorScope.values[name] = ScopeEntry(value, false)
        }
    }

    private fun ancestor(distance: Int): Scope {
        var scope: Scope = this
        for (i in 0 until distance) {
            scope = scope.enclosing ?: throw LunoRuntimeError("Scope nesting error.")
        }
        return scope
    }

    private val locals: MutableMap<AstNode, Int> = mutableMapOf()
    fun resolve(expr: AstNode, depth: Int) {
        locals[expr] = depth
    }
    fun lookUpVariable(name: Token, expr: AstNode): LunoValue {
        val distance = locals[expr]
        return if (distance != null) {
            getAt(distance, name.lexeme)
        } else {
            get(name)
        }
    }
}