package org.catrobat.catroid.utils.lunoscript

import android.annotation.SuppressLint
import android.content.Context
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import org.catrobat.catroid.CatroidApplication
import java.io.File


sealed class LunoValue {
    object Null : LunoValue() {
        override fun toString(): kotlin.String = "null"
    }

    data class Number(val value: Double) : LunoValue() {
        override fun toString(): kotlin.String = if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    }

    data class String(val value: kotlin.String) : LunoValue() {
        override fun toString(): kotlin.String = value
        fun toLunoScriptString(): kotlin.String = "\"$value\""
    }

    data class Boolean(val value: kotlin.Boolean) : LunoValue() {
        override fun toString(): kotlin.String = value.toString()
    }

    data class NativeClass(val klass: Class<*>) : Callable() {


        override fun arity(): IntRange = 0..Int.MAX_VALUE


        @SuppressLint("NewApi")
        override fun call(interpreter: Interpreter, arguments: kotlin.collections.List<LunoValue>, callSiteToken: Token): LunoValue {

            val candidates = klass.constructors.filter { it.parameterCount == arguments.size }

            if (candidates.isEmpty()) {
                throw LunoRuntimeError("No constructor for class '${klass.simpleName}' found with ${arguments.size} arguments.", callSiteToken.line)
            }


            for (constructor in candidates) {
                try {

                    val kotlinArgs = arguments.mapIndexed { index, lunoValue ->
                        val targetType = constructor.parameterTypes[index]
                        interpreter.lunoValueToKotlin(lunoValue, targetType)
                    }.toTypedArray()


                    val newInstance = constructor.newInstance(*kotlinArgs)
                    return NativeObject(newInstance)

                } catch (e: LunoRuntimeError) {


                    continue
                } catch (e: Exception) {

                    continue
                }
            }


            val argTypes = arguments.joinToString(", ") { it::class.simpleName ?: "unknown" }
            throw LunoRuntimeError("Could not find a suitable constructor for '${klass.simpleName}' with argument types: ($argTypes)", callSiteToken.line)
        }

        override fun toString(): kotlin.String = "<native class ${klass.name}>"
    }

    fun lunoValueToKotlin(value: LunoValue, targetClass: Class<*>): Any? {

        if (value is LunoValue.Null) {


            return null
        }


        if (value is LunoValue.NativeObject && targetClass.isInstance(value.obj)) {
            return value.obj
        }




        if (FileHandle::class.java.isAssignableFrom(targetClass)) {
            return when (value) {

                is LunoValue.String -> Gdx.files.absolute(value.value)

                is LunoValue.NativeObject -> {
                    if (value.obj is File) {
                        Gdx.files.absolute(value.obj.absolutePath)
                    } else {
                        null
                    }
                }
                else -> null
            }
        }


        if (File::class.java.isAssignableFrom(targetClass)) {
            if (value is LunoValue.String) {
                return File(value.value)
            }
        }


        val result = when (targetClass.name) {
            "java.lang.String", "kotlin.String" -> (value as? LunoValue.String)?.value
            "int", "java.lang.Integer" -> (value as? LunoValue.Number)?.value?.toInt()
            "double", "java.lang.Double" -> (value as? LunoValue.Number)?.value
            "float", "java.lang.Float" -> (value as? LunoValue.Number)?.value?.toFloat()
            "boolean", "java.lang.Boolean" -> (value as? LunoValue.Boolean)?.value
            else -> null
        }

        if (result != null) {
            return result
        }


        val valueTypeName = if (value is LunoValue.NativeObject) value.obj::class.java.simpleName else value::class.simpleName
        throw LunoRuntimeError("Cannot convert LunoValue type `${valueTypeName}` to native type `${targetClass.simpleName}`")
    }

    data class List(val elements: MutableList<LunoValue>) : LunoValue() {
        override fun toString(): kotlin.String = elements.joinToString(prefix = "[", postfix = "]", separator = ", ") { it.toString() }
    }

    data class Float(val value: kotlin.Float) : LunoValue() {

        override fun toString(): kotlin.String = "${value}f"
    }

    abstract class Callable : LunoValue() {
        abstract fun arity(): IntRange
        abstract fun call(interpreter: Interpreter, arguments: kotlin.collections.List<LunoValue>, callSiteToken: Token): LunoValue
    }


    data class NativeCallable(val callable: CallableNativeLunoFunction) : Callable() {
        override fun arity(): IntRange = callable.arity

        override fun call(interpreter: Interpreter, arguments: kotlin.collections.List<LunoValue>, callSiteToken: Token): LunoValue {
            if (arguments.size !in callable.arity) {
                val expected = if (callable.arity.first == callable.arity.last) callable.arity.first.toString() else "${callable.arity.first}..${callable.arity.last}"
                throw LunoRuntimeError("Native function '${callable.name}' expected $expected arguments, but got ${arguments.size}.", callSiteToken.line)
            }
            try {
                return callable.function.invoke(interpreter, arguments)
            } catch (e: Exception) {
                throw LunoRuntimeError("Error in native function '${callable.name}': ${e.cause?.message ?: e.message}", callSiteToken.line, e)
            }
        }
    }


    data class LunoFunction(val declaration: FunDeclarationStatement, val closure: Scope) : Callable() {
        override fun arity(): IntRange = declaration.params.size..declaration.params.size

        override fun call(interpreter: Interpreter, arguments: kotlin.collections.List<LunoValue>, callSiteToken: Token): LunoValue {
            val environment = Scope(closure)
            for (i in declaration.params.indices) {
                environment.define(declaration.params[i].lexeme, arguments.getOrElse(i) { Null })
            }
            try {
                interpreter.executeBlock(declaration.body.statements, environment)
            } catch (returnValue: ReturnSignal) {
                return returnValue.value
            }
            return Null
        }
    }


    data class BoundMethod(val instance: Any, val methodName: String) : Callable() {

        override fun arity(): IntRange = 0..Int.MAX_VALUE

        override fun call(interpreter: Interpreter, arguments: kotlin.collections.List<LunoValue>, callSiteToken: Token): LunoValue {
            return interpreter.callNativeMethod(instance, methodName.toString(), arguments, callSiteToken.line)
        }
    }

    data class BoundMethod2(val receiver: LunoObject, val method: LunoFunction) : Callable() {
        override fun arity(): IntRange = method.arity()

        override fun call(interpreter: Interpreter, arguments: kotlin.collections.List<LunoValue>, callSiteToken: Token): LunoValue {

            val environment = Scope(method.closure)

            environment.define("this", receiver)


            for (i in method.declaration.params.indices) {
                environment.define(method.declaration.params[i].lexeme, arguments.getOrElse(i) { Null })
            }
            try {
                interpreter.executeBlock(method.declaration.body.statements, environment)
            } catch (returnValue: ReturnSignal) {
                return returnValue.value
            }
            return Null
        }
    }


    data class LunoObject(
        val klass: LunoClass?,
        val fields: MutableMap<kotlin.String, LunoValue> = mutableMapOf()
    ) : LunoValue() {
        override fun toString(): kotlin.String {
            return klass?.name?.let { "$it instance" } ?: fields.toString()
        }
    }


    data class LunoClass(
        val name: kotlin.String,
        val declaration: ClassDeclarationStatement,
        val methods: Map<kotlin.String, LunoFunction>,
        val lunoSuperclass: LunoClass? = null,
        val nativeSuperclass: Class<*>? = null
    ) : Callable() {

        override fun arity(): IntRange {
            return methods["init"]?.arity() ?: 0..0
        }

        override fun call(interpreter: Interpreter, arguments: kotlin.collections.List<LunoValue>, callSiteToken: Token): LunoValue {
            val instance = LunoObject(this, mutableMapOf())
            var finalInstance: LunoValue = instance

            val bakedMethods: MutableMap<kotlin.String, Any?> = mutableMapOf()


            declaration.staticBlock?.let { staticBlock ->

                val staticScope = Scope(interpreter.globals)
                try {

                    interpreter.executeBlock(staticBlock.statements, staticScope)


                    for ((propertyName, scopeEntry) in staticScope.values) {

                        val methodName = "get" + propertyName.replaceFirstChar { it.uppercase() }


                        val kotlinValue = interpreter.lunoValueToKotlin(scopeEntry.value, Object::class.java)
                        bakedMethods[methodName] = kotlinValue
                    }
                } catch (e: Exception) {
                    throw LunoRuntimeError("Error executing static block for class '${this.name}': ${e.message}", declaration.line, e)
                }
            }

            if (nativeSuperclass != null) {
                val context = CatroidApplication.getAppContext()
                    ?: throw LunoRuntimeError("An Android Context is required to create native-backed objects.", callSiteToken.line)

                val proxy = NativeProxyFactory.createProxy(instance, nativeSuperclass, interpreter, context, bakedMethods)
                finalInstance = LunoValue.NativeObject(proxy)
            }


            methods["init"]?.let { initMethod ->
                val environment = Scope(initMethod.closure)
                environment.define("this", finalInstance)
                for (i in initMethod.declaration.params.indices) {
                    environment.define(initMethod.declaration.params[i].lexeme, arguments.getOrElse(i) { Null })
                }
                try {
                    interpreter.executeBlock(initMethod.declaration.body.statements, environment)
                } catch (ret: ReturnSignal) { /* 'init' не должен возвращать значение */ }
            }

            return finalInstance
        }

        fun findMethod(name: kotlin.String): LunoFunction? {
            return methods[name] ?: lunoSuperclass?.findMethod(name)
        }

        override fun toString(): kotlin.String = "<class $name>"
    }



    data class NativeObject(val obj: Any) : LunoValue() {
        override fun toString(): kotlin.String = "NativeObject(${obj::class.simpleName})"
    }


    fun isTruthy(): kotlin.Boolean {
        return when (this) {
            is Null -> false
            is Boolean -> this.value
            is Number -> this.value != 0.0
            is String -> this.value.isNotEmpty()
            is List -> this.elements.isNotEmpty()
            is LunoObject -> true
            is Callable -> true
            is NativeObject -> true
            is BoundMethod -> true
            is Float -> this.value != 0f
        }
    }

    companion object {
        fun fromKotlin(value: Any?): LunoValue {
            return when (value) {
                null -> Null
                is kotlin.Number -> Number(value.toDouble())
                is kotlin.String -> String(value)
                is kotlin.Boolean -> Boolean(value)
                is LunoValue -> value
                is Context -> NativeObject(value)
                is kotlin.collections.List<*> -> List(value.map { fromKotlin(it) }.toMutableList())
                is Map<*, *> -> {
                    val fields = value.entries.associate { (k, v) ->
                        k.toString() to fromKotlin(v)
                    }.toMutableMap()
                    LunoObject(null, fields)
                }
                is CallableNativeLunoFunction -> NativeCallable(value)
                else -> NativeObject(value)
            }
        }
    }
}