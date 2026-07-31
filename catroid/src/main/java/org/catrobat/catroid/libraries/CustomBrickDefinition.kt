package org.catrobat.catroid.libraries

enum class ParameterType { TEXT_FIELD, VARIABLE_DROPDOWN, LIST_DROPDOWN }

data class BrickParameter(
    val type: ParameterType,
    val nameInLuno: String
)

data class CustomBrickDefinition(
    val id: String,
    val headerText: String,
    val parameters: List<BrickParameter>,
    val lunoFunctionName: String,
    val ownerLibraryId: String
)