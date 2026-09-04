package org.catrobat.catroid.apkbuildV3

import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class V3PermissionsTest {

    private val NAME_ATTR = 0x01010003

    private fun permissionNames(manifest: AndroidManifestBlock): List<String> {
        manifest.getOrCreateApplicationElement()
        return manifest.manifestElement.listElements("uses-permission")
            .map { it.searchAttributeByResourceId(NAME_ATTR)?.valueAsString ?: "" }
    }

    @Test
    fun syncPermissions_replacesTemplatePermissionsWithSelected() {
        val manifest = AndroidManifestBlock.empty()
        manifest.addUsesPermission("android.permission.CAMERA")
        manifest.addUsesPermission("android.permission.RECORD_AUDIO")
        manifest.addUsesPermission("android.permission.NFC")

        V3ApkAssembler.syncPermissions(manifest, listOf("android.permission.INTERNET"))

        assertEquals(listOf("android.permission.INTERNET"), permissionNames(manifest))
    }

    @Test
    fun syncPermissions_dedupesSelectedPermissions() {
        val manifest = AndroidManifestBlock.empty()

        V3ApkAssembler.syncPermissions(
            manifest,
            listOf(
                "android.permission.INTERNET",
                "android.permission.INTERNET",
                "android.permission.VIBRATE",
                "android.permission.VIBRATE"
            )
        )

        assertEquals(
            listOf("android.permission.INTERNET", "android.permission.VIBRATE"),
            permissionNames(manifest)
        )
    }

    @Test
    fun syncPermissions_emptySelectionClearsAll() {
        val manifest = AndroidManifestBlock.empty()
        manifest.addUsesPermission("android.permission.CAMERA")

        V3ApkAssembler.syncPermissions(manifest, emptyList())

        assertTrue(permissionNames(manifest).isEmpty())
    }
}
