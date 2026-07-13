package org.catrobat.catroid.runtime

interface StringProvider {
    fun getString(resourceName: String): String
}

object StringProviderHolder {
    lateinit var provider: StringProvider
}
