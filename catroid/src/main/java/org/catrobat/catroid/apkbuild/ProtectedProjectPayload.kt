package org.catrobat.catroid.apkbuild

object ProtectedProjectPayload {
    const val ENCRYPTED_ASSET_NAME = "neocatroid.dat"
    const val KEY_ASSET_NAME = "neocatroid.key"
    // Подпись целостности: хэш сертификата подписи + HMAC зашифрованного проекта.
    const val SIG_ASSET_NAME = "neocatroid.sig"
    const val PASSWORD = "SA?D3Ft?ZZHufE9Ma#NA#A9HdQDAWbJ8WHfDPKfD4!G3ST+!=x;Z!wPD=7;B=9JTHRHsT@zZH@kFUu8tgQ8FLH%RPpZpLwJC2A*e"
}
