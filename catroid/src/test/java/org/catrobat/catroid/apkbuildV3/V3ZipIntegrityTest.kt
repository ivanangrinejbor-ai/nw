package org.catrobat.catroid.apkbuildV3

import com.android.apksig.ApkVerifier
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipFile

class V3ZipIntegrityTest {

    private data class ZipHealth(val entries: Int, val descriptors: Int, val eocd: Int, val cd: Int)

    private fun inspectZip(apk: File): ZipHealth {
        val data = apk.readBytes()
        val eocdPos = findSignatureLast(data, PK_EOCD)
        if (eocdPos < 0) return ZipHealth(0, 0, -1, -1)
        val count = data[eocdPos + 10].toInt() or (data[eocdPos + 11].toInt() shl 8)
        val cdSize = leInt(data, eocdPos + 12)
        val cdOffset = leInt(data, eocdPos + 16)
        if (data[cdOffset + 0].toInt() != 0x50 || data[cdOffset + 1].toInt() != 0x4b) {
            return ZipHealth(-1, 0, eocdPos, cdOffset)
        }
        var descriptors = 0
        var pos = 0
        var entries = 0
        while (pos + 30 <= cdOffset) {
            if (bytesAt(data, pos, PK_LOCAL)) {
                var flags = leShort(data, pos + 6)
                if ((flags and 0x08) != 0) descriptors++
                val csize = leInt(data, pos + 18)
                val fnLen = leShort(data, pos + 26)
                val exLen = leShort(data, pos + 28)
                pos += 30 + fnLen + exLen + csize
                entries++
            } else {
                break
            }
        }
        return ZipHealth(entries, descriptors, eocdPos, cdOffset)
    }

    private fun findSignatureLast(data: ByteArray, sig: ByteArray): Int {
        if (sig.isEmpty() || sig.size > data.size) return -1
        outer@ for (i in data.size - 1 downTo sig.size - 1) {
            if (data[i] == sig[sig.size - 1]) {
                for (j in 0 until sig.size) {
                    if (data[i - sig.size + 1 + j] != sig[j]) continue@outer
                }
                return i - sig.size + 1
            }
        }
        return -1
    }

    private fun leInt(data: ByteArray, pos: Int): Int =
        (data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8) or
                ((data[pos + 2].toInt() and 0xFF) shl 16) or ((data[pos + 3].toInt() and 0xFF) shl 24)

    private fun leShort(data: ByteArray, pos: Int): Int =
        (data[pos].toInt() and 0xFF) or ((data[pos + 1].toInt() and 0xFF) shl 8)

    private fun bytesAt(data: ByteArray, pos: Int, bytes: ByteArray): Boolean {
        if (pos + bytes.size > data.size) return false
        for (i in bytes.indices) if (data[pos + i] != bytes[i]) return false
        return true
    }

    private fun assertZipStructureHealthy(label: String, apk: File) {
        val h = inspectZip(apk)
        assert(h.eocd >= 0) { "$label: EOCD missing" }
        assert(h.entries > 1000) { "$label: broken local-header chain, entries=${h.entries}" }
        assert(h.descriptors == 0) { "$label: ${h.descriptors} entries use data descriptors (flags 0x08)" }
        println("$label: entries=${h.entries} descriptors=${h.descriptors} EOCD=${h.eocd} CD=${h.cd}")
    }

    @Test
    fun fullPipeline_producesInstallableSignedApk() {
        val base = File("src/main/assets/template_runtime.apk")
        assumeTrue("template_runtime.apk not found", base.exists())

        val work = File("build/tmp/v3_integrity")
        work.mkdirs()

        val payload = File(work, "project.ncv3")
        payload.writeBytes(ByteArray(64 * 1024) { it.toByte() })

        val keyResult = DynamicKeyManager.generateKey("IntegrityTest")
        assert(DynamicKeyManager.verifyKeyIntegrity(keyResult.keyFileContents)) { "generated key must pass integrity check" }
        val resolved = DynamicKeyManager.resolveStoredKey(
            keyResult.keyFileNames
                .filter { DynamicKeyManager.KEY_FILE_PREFIX in it && !it.contains("decoy") }
                .sortedBy { name ->
                    name.removePrefix(DynamicKeyManager.KEY_FILE_PREFIX)
                        .removeSuffix(".nk").substringBefore('_').toInt()
                }
                .map { name -> keyResult.keyFileContents[keyResult.keyFileNames.indexOf(name)] }
        )
        assert(resolved.contentEquals(keyResult.selectedKey)) { "resolved key must equal selected key" }

        val injected = File(work, "step1_injected.apk")
        V3ApkAssembler.injectAssets(
            baseApk = base,
            outApk = injected,
            payload = payload,
            keyFileNames = keyResult.keyFileNames,
            keyFileContents = keyResult.keyFileContents,
            templateType = TemplateType.FULL,
            workDir = work
        )
        assertZipStructureHealthy("step1 inject", injected)

        val patched = File(work, "step2_patched.apk")
        V3ApkAssembler.patchManifest(
            injected,
            patched,
            ApkBuilderV3Config(
                appName = "IntegrityTest",
                packageName = "com.example.integrity",
                templateType = TemplateType.FULL
            )
        )
        assertZipStructureHealthy("step2 patch", patched)

        val signed = File(work, "step3_signed.apk")
        val keystore = File(work, "ks.jks")
        V3ApkAssembler.doSign(patched, signed, keystore, "neocatroidv3", "keystore")
        assertZipStructureHealthy("step3 signed", signed)

        ZipFile(signed).use {
            assert(it.getEntry("assets/project.ncv3") != null)
            assert(it.getEntry("assets/${keyResult.keyFileNames.first()}") != null)
        }

        val result = ApkVerifier.Builder(signed).build().verify()
        assert(result.isVerified) { "Signed APK must verify: ${result.errors}" }

        println("FULL PIPELINE OK: ${signed.length() / (1024 * 1024)} MB")
    }

    companion object {
        private val PK_LOCAL = byteArrayOf(0x50, 0x4b, 0x03, 0x04)
        private val PK_EOCD = byteArrayOf(0x50, 0x4b, 0x05, 0x06)
    }
}