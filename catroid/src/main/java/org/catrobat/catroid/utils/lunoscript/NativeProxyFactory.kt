package org.catrobat.catroid.utils.lunoscript

import android.content.Context
import net.bytebuddy.ByteBuddy
import net.bytebuddy.android.AndroidClassLoadingStrategy
import net.bytebuddy.description.method.MethodDescription
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy
import net.bytebuddy.implementation.FixedValue
import net.bytebuddy.implementation.MethodDelegation
import net.bytebuddy.implementation.SuperMethodCall
import net.bytebuddy.matcher.ElementMatchers
import net.bytebuddy.matcher.ElementMatchers.isMethod
import java.lang.reflect.Method
import java.util.concurrent.Callable


class LunoInvocationHandler(
    private val lunoInstance: LunoValue.LunoObject,
    private val interpreter: Interpreter
) {
    private fun getDefaultValueForType(type: Class<*>): Any? {
        return when {
            type == Void.TYPE -> null
            type == Boolean::class.javaPrimitiveType -> false // Для boolean -> false
            type.isPrimitive -> 0 // Для всех остальных примитивов (int, float и т.д.) -> 0
            else -> null // для всех объектных типов -> null
        }
    }

    @net.bytebuddy.implementation.bind.annotation.RuntimeType
    fun intercept(
        @net.bytebuddy.implementation.bind.annotation.Origin method: Method,
        @net.bytebuddy.implementation.bind.annotation.AllArguments args: Array<Any?>,
        // --- НОВОЕ: ByteBuddy передаст нам "вызыватель" родительского метода ---
        @net.bytebuddy.implementation.bind.annotation.SuperCall superMethod: Callable<*>?
    ): Any? {
        // Ищем, реализовал ли пользователь этот метод в своем LunoScript-классе
        val lunoMethod = lunoInstance.klass?.findMethod(method.name)

        if (lunoMethod == null) {
            // --- ГЛАВНОЕ ИСПРАВЛЕНИЕ ---
            // Если метод НЕ реализован в LunoScript, просто вызываем родительскую реализацию (super.method(...))
            return superMethod?.call()
        }

        // Если метод реализован в LunoScript, вызываем его
        val lunoArgs = args.map { LunoValue.fromKotlin(it) }
        val boundMethod = LunoValue.BoundMethod2(lunoInstance, lunoMethod)
        val dummyToken = Token(TokenType.EOF, "", null, -1, 0)
        val resultLunoValue = boundMethod.call(interpreter, lunoArgs, dummyToken)

        // Конвертируем результат обратно в Java-тип
        if (method.returnType == Void.TYPE) {
            return null
        }
        return interpreter.lunoValueToKotlin(resultLunoValue, method.returnType)
    }
}

object NativeProxyFactory {
    fun createProxy(
        lunoInstance: LunoValue.LunoObject,
        nativeSuperclass: Class<*>,
        interpreter: Interpreter,
        context: Context,
        bakedMethods: Map<String, Any?>
    ): Any {
        try {
            val handler = LunoInvocationHandler(lunoInstance, interpreter)
            val privateDir = context.getDir("bytebuddy", Context.MODE_PRIVATE)
            val loadingStrategy = AndroidClassLoadingStrategy.Wrapping(privateDir)

            // --- НАЧАЛО ИЗМЕНЕНИЙ ---
            var builder = ByteBuddy().subclass(nativeSuperclass)

            // Правило для конструкторов
            builder = builder.constructor(ElementMatchers.any())
                .intercept(SuperMethodCall.INSTANCE)

            // НОВОЕ ПРАВИЛО: Если мы получили layoutId, аппаратно переопределяем getViewResource
            val bakedMethodNames = bakedMethods.keys.toTypedArray()

            for ((methodName, value) in bakedMethods) {
                // --- ГЛАВНОЕ ИСПРАВЛЕНИЕ ---
                // Проверяем, не является ли значение null.
                val interceptor = if (value == null) {
                    FixedValue.nullValue() // Используем безопасный для null конструктор
                } else {
                    FixedValue.value(value) // Используем обычный конструктор
                }
                builder = builder.method(ElementMatchers.named(methodName))
                    .intercept(interceptor)
            }

            // 3. Правило для всех остальных методов, которые НЕ были "запечены"
            builder = builder.method(
                ElementMatchers.`isMethod`<MethodDescription>()
                    .and(ElementMatchers.not(ElementMatchers.isStatic()))
                    .and(ElementMatchers.not(ElementMatchers.isFinalizer()))
                    .and(ElementMatchers.not(ElementMatchers.isPrivate()))
                    // Исключаем все методы, которые мы уже обработали
                    .and(ElementMatchers.not(ElementMatchers.namedOneOf(*bakedMethodNames)))
            )
                .intercept(MethodDelegation.to(handler))

            val applicationClassLoader = NativeProxyFactory::class.java.classLoader

            val dynamicType = builder.make()
                .load(applicationClassLoader, loadingStrategy)
                .loaded
            // --- КОНЕЦ УНИВЕРСАЛЬНОЙ ЛОГИКИ ---

            return try {
                val constructor = dynamicType.getDeclaredConstructor(Context::class.java)
                constructor.newInstance(context)
            } catch (e: NoSuchMethodException) {
                // Если по какой-то причине такого конструктора нет (например, для не-View класса),
                // мы откатываемся к старому поведению и ищем конструктор без аргументов.
                dynamicType.getDeclaredConstructor().newInstance()
            }
        } catch (e: Exception) {
            throw LunoRuntimeError(
                "Failed to create native proxy for '${nativeSuperclass.simpleName}': ${e.cause?.message ?: e.message}",
                -1,
                e
            )
        }
    }
}