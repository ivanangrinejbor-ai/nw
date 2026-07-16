package org.catrobat.catroid.apkbuild

object ProtectedProjectPayload {
    const val ENCRYPTED_ASSET_NAME = "neocatroid.dat"
    const val KEY_ASSET_NAME = "neocatroid.key"
    /** Static fallback password used when no key file is present (backward compat). */
    const val PASSWORD = "NeoCatroid:BakedProject:Payload:v1"
}
