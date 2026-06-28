package org.catrobat.catroid.codeanalysis

enum class Severity {
    WARNING,
    ERROR,
    SUGGESTION
}

data class AnalysisResult(
    val severity: Severity,
    val message: String
)