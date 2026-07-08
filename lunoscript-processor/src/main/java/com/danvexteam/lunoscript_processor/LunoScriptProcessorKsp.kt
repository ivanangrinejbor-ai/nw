package org.catrobat.catroid.lunoscript.processor

import com.danvexteam.lunoscript_annotations.LunoClass
import com.danvexteam.lunoscript_annotations.LunoFunction
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.*
import java.io.File
import java.io.OutputStreamWriter

class LunoScriptProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return LunoScriptProcessor(environment)
    }
}

class LunoScriptProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {
    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) return emptyList()
        val annotatedFunctions = resolver.getSymbolsWithAnnotation(LunoFunction::class.qualifiedName!!)
        val annotatedClasses = resolver.getSymbolsWithAnnotation(LunoClass::class.qualifiedName!!)
        
        val generatedClassName = "GeneratedLunoRegistry"
        val packageName = "org.catrobat.catroid.utils.lunoscript.generated"
        
        val interpreterClass = ClassName("org.catrobat.catroid.utils.lunoscript", "Interpreter")
        val lunoValueClass = ClassName("org.catrobat.catroid.utils.lunoscript", "LunoValue")
        val asSpecificKotlinTypeFun = MemberName("org.catrobat.catroid.utils.lunoscript", "asSpecificKotlinType", true)
        val callableNativeFuncClass = ClassName("org.catrobat.catroid.utils.lunoscript", "CallableNativeLunoFunction")
        val lunoRuntimeErrorClass = ClassName("org.catrobat.catroid.utils.lunoscript", "LunoRuntimeError")
        
        val registerFunctionBuilder = FunSpec.builder("registerAllNatives")
            .addParameter("interpreter", interpreterClass)
        
        // Process annotated functions
        annotatedFunctions
            .filterIsInstance<KSFunctionDeclaration>()
            .forEach { funcElement ->
                val parent = funcElement.parentDeclaration
                if (parent == null) {
                    environment.logger.info("Processing function: ${funcElement.simpleName.asString()}")
                    
                    val functionName = funcElement.simpleName.asString()
                    
                    val lunoFuncAnnotation = funcElement.annotations.filterIsInstance<KSAnnotation>().firstOrNull { it.shortName.asString() == "LunoFunction" }
                    val lunoFuncName = lunoFuncAnnotation?.arguments?.firstOrNull()?.value?.toString()?.ifEmpty { functionName } ?: functionName
                    
                    val arity = funcElement.parameters.size
                    
                    registerFunctionBuilder.addCode("interpreter.defineNative(%S, %L..%L) { _, arguments ->\n", lunoFuncName, arity, arity)
                    registerFunctionBuilder.addStatement("val funcNameForError = %S", lunoFuncName)
                    
                    val argNames = funcElement.parameters.mapIndexed { index, param ->
                        val argName = "arg$index"
                        val paramType = getKotlinType(param.type.toTypeName().copy(nullable = false))
                        
                        registerFunctionBuilder.addStatement(
                            "val %L = arguments[%L].%M<%T>(funcNameForError, %L)",
                            argName, index, asSpecificKotlinTypeFun, paramType, index + 1
                        )
                        argName
                    }
                    val joinedArgs = argNames.joinToString(", ")
                    
                    val returnType = funcElement.returnType?.resolve()?.toTypeName()
                    if (returnType != null && returnType != UNIT) {
                        registerFunctionBuilder.addStatement("val result = %L", functionName)
                        registerFunctionBuilder.addStatement("%T.fromKotlin(result)", lunoValueClass)
                    } else {
                        registerFunctionBuilder.addStatement("%L", functionName)
                        registerFunctionBuilder.addStatement("%T.Null", lunoValueClass)
                    }
                    registerFunctionBuilder.addCode("}\n")
                }
            }
        
        // Process annotated classes
        annotatedClasses
            .filterIsInstance<KSClassDeclaration>()
            .filter { Modifier.ABSTRACT !in it.modifiers }
            .forEach { classElement ->
                environment.logger.info("Processing class: ${classElement.simpleName.asString()}")
                
                val classTypeName = classElement.toClassName()
                val lunoClassAnnotation = classElement.annotations.filterIsInstance<KSAnnotation>().firstOrNull { it.shortName.asString() == "LunoClass" }
                val lunoClassName = lunoClassAnnotation?.arguments?.firstOrNull()?.value?.toString()?.ifEmpty { classElement.simpleName.asString() } ?: classElement.simpleName.asString()
                
                val primaryConstructor = classElement.primaryConstructor ?: return@forEach
                val paramsCount = primaryConstructor.parameters.size
                
                registerFunctionBuilder.addCode(
                    "interpreter.defineNative(%S, %L..%L) { _, args ->\n",
                    lunoClassName,
                    paramsCount,
                    paramsCount
                )
                
                registerFunctionBuilder.beginControlFlow("when (args.size)")
                registerFunctionBuilder.beginControlFlow("%L ->", paramsCount)
                
                val argNames = primaryConstructor.parameters.mapIndexed { index, param ->
                    val argName = "arg$index"
                    val paramType = getKotlinType(param.type.toTypeName().copy(nullable = false))
                    registerFunctionBuilder.addStatement(
                        "val %L = args[%L].%M<%T>(%S, %L)",
                        argName,
                        index,
                        asSpecificKotlinTypeFun,
                        paramType,
                        "$lunoClassName constructor",
                        index + 1
                    )
                    argName
                }
                val joinedArgs = argNames.joinToString(", ")
                
                registerFunctionBuilder.addStatement("%T.NativeObject(%T(%L))", lunoValueClass, classTypeName, joinedArgs)
                registerFunctionBuilder.addCode("}\n")
                registerFunctionBuilder.addStatement("else -> throw %T(%S, -1)", lunoRuntimeErrorClass, "Invalid argument count for $lunoClassName constructor")
                registerFunctionBuilder.endControlFlow() // when
                registerFunctionBuilder.addCode("}\n")
            }
        
        val fileSpec = FileSpec.builder(packageName, generatedClassName)
            .addFunction(registerFunctionBuilder.build())
            .build()
        
        environment.codeGenerator.createNewFile(
            Dependencies(aggregating = true),
            packageName,
            generatedClassName,
            "kt"
        ).use { outputStream ->
            OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                fileSpec.writeTo(writer)
            }
        }
        
        generated = true
        return emptyList()
    }
    
    private fun getKotlinType(typeName: TypeName): TypeName {
        if (typeName !is ClassName) return typeName
        return when (typeName.canonicalName) {
            "java.lang.String" -> ClassName("kotlin", "String")
            "java.lang.Double" -> ClassName("kotlin", "Double")
            "java.lang.Boolean" -> ClassName("kotlin", "Boolean")
            "java.lang.Integer" -> ClassName("kotlin", "Int")
            "java.lang.Float" -> ClassName("kotlin", "Float")
            "java.lang.Object" -> ClassName("kotlin", "Any")
            "java.io.File" -> ClassName("java.io", "File")
            "org.catrobat.catroid.utils.lunoscript.LunoValue.Float" -> ClassName("kotlin", "Float")
            else -> typeName
        }
    }
}