/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.catroid.apkbuildV3

import com.android.apksig.ApkVerifier
import com.reandroid.apk.ApkModule
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.archive.FileInputSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class V3PackageRenameTest {

    private val NAME_ATTR = 0x01010003
    private val AUTHORITIES_ATTR = 0x01010018

    @Test
    fun replacePackageInAuthority_rewritesPrefixExactAndList() {
        assertEquals(
            "com.example.game.fileProvider",
            V3ApkAssembler.replacePackageInAuthority(
                "org.catrobat.catroid.fileProvider", "org.catrobat.catroid", "com.example.game")
        )
        assertEquals(
            "com.example.game",
            V3ApkAssembler.replacePackageInAuthority(
                "org.catrobat.catroid", "org.catrobat.catroid", "com.example.game")
        )
        assertEquals(
            "com.example.game.fileProvider;com.example.game.other",
            V3ApkAssembler.replacePackageInAuthority(
                "org.catrobat.catroid.fileProvider;org.catrobat.catroid.other",
                "org.catrobat.catroid", "com.example.game")
        )
        assertEquals(
            "some.other.authority",
            V3ApkAssembler.replacePackageInAuthority(
                "some.other.authority", "org.catrobat.catroid", "com.example.game")
        )
        assertEquals(
            "org.catrobat.catroid2.fileProvider",
            V3ApkAssembler.replacePackageInAuthority(
                "org.catrobat.catroid2.fileProvider", "org.catrobat.catroid", "com.example.game")
        )
    }

    @Test
    fun applyPackageRename_qualifiesComponentsRewritesAuthorityAndKeepsFqn() {
        val manifest = AndroidManifestBlock.empty()
        manifest.setPackageName("org.catrobat.catroid")

        val app = manifest.getOrCreateApplicationElement()
        app.getOrCreateAndroidAttribute("name", NAME_ATTR).setValueAsString(".CatroidApplication")

        val main = app.createChildElement("activity")
        main.getOrCreateAndroidAttribute("name", NAME_ATTR).setValueAsString(".ui.MainMenuActivity")
        val filter = main.createChildElement("intent-filter")
        filter.createChildElement("action").getOrCreateAndroidAttribute("name", NAME_ATTR)
            .setValueAsString("android.intent.action.MAIN")
        filter.createChildElement("category").getOrCreateAndroidAttribute("name", NAME_ATTR)
            .setValueAsString("android.intent.category.LAUNCHER")

        val fqn = app.createChildElement("activity")
        fqn.getOrCreateAndroidAttribute("name", NAME_ATTR)
            .setValueAsString("org.catrobat.catroid.ui.neopaint.NeoPaintActivity")

        val provider = app.createChildElement("provider")
        provider.getOrCreateAndroidAttribute("name", NAME_ATTR)
            .setValueAsString("androidx.core.content.FileProvider")
        provider.getOrCreateAndroidAttribute("authorities", AUTHORITIES_ATTR)
            .setValueAsString("org.catrobat.catroid.fileProvider")

        V3ApkAssembler.applyPackageRename(manifest, "com.example.mygame")

        assertEquals("com.example.mygame", manifest.packageName)
        assertEquals("org.catrobat.catroid.CatroidApplication", manifest.applicationClassName)

        val names = manifest.listApplicationElementsByTag("activity")
            .map { AndroidManifestBlock.getAndroidNameValue(it) }
        assertTrue(names.contains("org.catrobat.catroid.ui.MainMenuActivity"))
        assertTrue(names.contains("org.catrobat.catroid.ui.neopaint.NeoPaintActivity"))

        val providerAuth = manifest.listApplicationElementsByTag("provider")
            .first().searchAttributeByResourceId(AUTHORITIES_ATTR)?.getValueString()
        assertEquals("com.example.mygame.fileProvider", providerAuth)
    }

    @Test
    fun makeRuntimeLoaderLauncher_onlyRuntimeLoaderIsLauncherAfterRename() {
        val manifest = AndroidManifestBlock.empty()
        manifest.setPackageName("org.catrobat.catroid")
        val app = manifest.getOrCreateApplicationElement()

        val main = app.createChildElement("activity")
        main.getOrCreateAndroidAttribute("name", NAME_ATTR).setValueAsString(".ui.MainMenuActivity")
        val filter = main.createChildElement("intent-filter")
        filter.createChildElement("action").getOrCreateAndroidAttribute("name", NAME_ATTR)
            .setValueAsString("android.intent.action.MAIN")
        filter.createChildElement("category").getOrCreateAndroidAttribute("name", NAME_ATTR)
            .setValueAsString("android.intent.category.LAUNCHER")

        V3ApkAssembler.applyPackageRename(manifest, "com.example.mygame")
        V3ApkAssembler.makeRuntimeLoaderLauncher(manifest)

        val launcherNames = manifest.listApplicationElementsByTag("activity")
            .filter { act ->
                act.listElements("intent-filter").any { f ->
                    f.listElements("category").any { c ->
                        c.searchAttributeByResourceId(NAME_ATTR)?.valueAsString == "android.intent.category.LAUNCHER"
                    }
                }
            }
            .map { AndroidManifestBlock.getAndroidNameValue(it) }

        assertEquals(
            listOf("org.catrobat.catroid.apkbuildV3.runtime.RuntimeLoaderActivityV3"),
            launcherNames
        )
    }

    @Test
    fun doSign_afterPackageRename_verifiesOnRealManifest() {
        val apkDir = File("build/outputs/apk/catroid/debug")
        val apk = apkDir.listFiles { _, name -> name.endsWith(".apk") }?.firstOrNull()
        assumeTrue("No built editor APK found; skipping signing verification", apk != null && apk.exists())
        val apkFile = apk!!

        val manifestBytes = ZipFile(apkFile).use { zf ->
            zf.getInputStream(zf.getEntry("AndroidManifest.xml")).readBytes()
        }
        val manifest = AndroidManifestBlock.load(ByteArrayInputStream(manifestBytes))

        val newPkg = "com.example.mygame"
        V3ApkAssembler.applyPackageRename(manifest, newPkg)
        assertEquals(newPkg, manifest.packageName)

        val zip = File.createTempFile("v3_sign_in_", ".apk")
        ZipOutputStream(zip.outputStream()).use { zos ->
            zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
            zos.write(manifest.bytes)
            zos.closeEntry()
        }
        val out = File.createTempFile("v3_sign_out_", ".apk")
        val keystore = File.createTempFile("v3_sign_key_", ".jks")
        V3ApkAssembler.doSign(zip, out, keystore, "neocatroidv3", "keystore")

        val result = ApkVerifier.Builder(out).build().verify()
        assertTrue("Renamed + reandroid-serialized APK must verify after signing", result.isVerified)

        zip.delete()
        out.delete()
        keystore.delete()
    }

    @Test
    fun exportTwoGames_coexistAndVerify() {
        val base = File("src/main/assets/template_runtime.apk")
        assumeTrue("template_runtime.apk not found; skipping two-APK coexistence build", base.exists())

        val game1 = File.createTempFile("v3_game1_", ".apk")
        val game2 = File.createTempFile("v3_game2_", ".apk")
        val ks1 = File.createTempFile("v3_ks1_", ".jks")
        val ks2 = File.createTempFile("v3_ks2_", ".jks")

        buildExportedApk(base, "org.test.game1", game1, ks1)
        buildExportedApk(base, "org.test.game2", game2, ks2)

        verifyExportedApk(game1, "org.test.game1")
        verifyExportedApk(game2, "org.test.game2")

        assertTrue("two exports must be distinct files", game1.absolutePath != game2.absolutePath)
    }

    private fun buildExportedApk(
        base: File,
        packageName: String,
        outApk: File,
        keystore: File
    ) {
        val work = File.createTempFile("v3_base_", ".apk")
        val repacked = File.createTempFile("v3_repack_", ".apk")
        base.copyTo(work, overwrite = true)
        ApkModule.loadApkFile(work).use { module ->
            val manifest = module.androidManifest
            manifest.versionCode = 1
            manifest.versionName = "1.0"
            manifest.minSdkVersion = 21
            manifest.targetSdkVersion = 35

            val payload = File.createTempFile("v3_payload_", ".ncv3")
            payload.writeText("NCPP-placeholder-encrypted-project")
            module.add(FileInputSource(payload, "assets/project.ncv3"))

            V3ApkAssembler.applyPackageRename(manifest, packageName)
            V3ApkAssembler.makeRuntimeLoaderLauncher(manifest)

            module.writeApk(repacked)
        }
        V3ApkAssembler.doSign(repacked, outApk, keystore, "neocatroidv3", "keystore")
    }

    private fun verifyExportedApk(apk: File, expectedPackage: String) {
        val manifestBytes = ZipFile(apk).use { zf ->
            zf.getInputStream(zf.getEntry("AndroidManifest.xml")).readBytes()
        }
        val manifest = AndroidManifestBlock.load(ByteArrayInputStream(manifestBytes))

        assertEquals(expectedPackage, manifest.packageName)

        val auth = manifest.listApplicationElementsByTag("provider")
            .first().searchAttributeByResourceId(AUTHORITIES_ATTR)?.getValueString()
        assertEquals("$expectedPackage.fileProvider", auth)

        val launcher = manifest.listApplicationElementsByTag("activity").firstOrNull { act ->
            act.listElements("intent-filter").any { f ->
                f.listElements("category").any { c ->
                    c.searchAttributeByResourceId(NAME_ATTR)?.getValueString() == "android.intent.category.LAUNCHER"
                }
            }
        }
        assertEquals(
            "org.catrobat.catroid.apkbuildV3.runtime.RuntimeLoaderActivityV3",
            launcher?.let { AndroidManifestBlock.getAndroidNameValue(it) }
        )

        val classRefAttrs = setOf("name", "parentActivityName", "process")
        manifest.recursiveAttributes().forEach { attr ->
            val value = attr.getValueString() ?: return@forEach
            if (attr.name !in classRefAttrs) {
                assertFalse("leftover old package in attribute '${attr.name}': $value", value.contains("org.catrobat.catroid"))
                assertFalse("unresolved placeholder in attribute '${attr.name}': $value", value.contains("\${"))
            }
        }

        val hasPayload = ZipFile(apk).use { it.getEntry("assets/project.ncv3") != null }
        assertTrue("export must contain the encrypted project payload", hasPayload)

        val result = ApkVerifier.Builder(apk).build().verify()
        assertTrue("Export $expectedPackage must verify after signing", result.isVerified)
    }
}
