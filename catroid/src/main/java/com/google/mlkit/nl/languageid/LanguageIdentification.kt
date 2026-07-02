package com.google.mlkit.nl.languageid

class LanguageIdentification {
    companion object {
        fun getClient(options: Options): LanguageIdentification = LanguageIdentification()
   }

    class Options {
        companion object {
            fun builder(): Builder = Builder()
        }

        class Builder {
			fun setConfidenceThreshold(threshold: Float): Builder = this
            fun build(): Options = Options()
        }
    }

    fun identifyLanguage(text: String) = com.google.android.gms.tasks.Tasks.forResult("en")
    fun identifyPossibleLanguages(text: String) = com.google.android.gms.tasks.Tasks.forResult(emptyList<IdentifiedLanguage>())

    class IdentifiedLanguage {
        fun getLanguageTag(): String = "en"
        fun getConfidence(): Float = 1f
    }
}