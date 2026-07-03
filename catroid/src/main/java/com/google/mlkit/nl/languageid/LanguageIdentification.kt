package com.google.mlkit.nl.languageid

import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks

class LanguageIdentification {
    companion object {
        fun getClient(): LanguageIdentifier = LanguageIdentifier()
    }
}

class LanguageIdentifier {
    fun identifyLanguage(text: String): Task<String> = Tasks.forResult("und")
}
