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
            type == Boolean::class.javaPrimitiveType -> false
            type.isPrimitive -> 0
            else -> null
        }
    }

    @net.bytebuddy.implementation.bind.annotation.RuntimeType
    fun intercept(
        @net.bytebuddy.implementation.bind.annotation.Origin method: Method,
        @net.bytebuddy.implementation.bind.annotation.AllArguments args: Array<Any?>,
        @net.bytebuddy.implementation.bind.annotation.SuperCall superMethod: Callable<*>?
    ): Any? {
        val lunoMethod = lunoInstance.klass?.findMethod(method.name)

        if (lunoMethod == null) {
            return superMethod?.call()
        }

        val lunoArgs = args.map { LunoValue.fromKotlin(it) }
        val boundMethod = LunoValue.BoundMethod2(lunoInstance, lunoMethod)
        val dummyToken = Token(TokenType.EOF, "", null, -1, 0)
        val resultLunoValue = boundMethod.call(interpreter, lunoArgs, dummyToken)

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

            var builder = ByteBuddy().subclass(nativeSuperclass)

            builder = builder.constructor(ElementMatchers.any())
                .intercept(SuperMethodCall.INSTANCE)

            val bakedMethodNames = bakedMethods.keys.toTypedArray()

            for ((methodName, value) in bakedMethods) {
                val interceptor = if (value == null) {
                    FixedValue.nullValue()
                } else {
                    FixedValue.value(value)
                }
                builder = builder.method(ElementMatchers.named(methodName))
                    .intercept(interceptor)
            }

            builder = builder.method(
                ElementMatchers.`isMethod`<MethodDescription>()
                    .and(ElementMatchers.not(ElementMatchers.isStatic()))
                    .and(ElementMatchers.not(ElementMatchers.isFinalizer()))
                    .and(ElementMatchers.not(ElementMatchers.isPrivate()))
                    .and(ElementMatchers.not(ElementMatchers.namedOneOf(*bakedMethodNames)))
            )
                .intercept(MethodDelegation.to(handler))

            val applicationClassLoader = NativeProxyFactory::class.java.classLoader

            val dynamicType = builder.make()
                .load(applicationClassLoader, loadingStrategy)
                .loaded

            return try {
                val constructor = dynamicType.getDeclaredConstructor(Context::class.java)
                constructor.newInstance(context)
            } catch (e: NoSuchMethodException) {
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