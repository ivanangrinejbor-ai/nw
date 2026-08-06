package org.catrobat.catroid.stage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Verifies the NCPW password-container format produced by the Android side
 * (ProjectCrypto.writePasswordContainerHeader + encryptDirectoryToStreamChunked):
 * DesktopStage must decrypt the inner NCPS stream with the password embedded in
 * the container, not with the shared legacy constant.
 */
class DesktopStagePasswordContainerTest {

    private fun deriveKey(password: String, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password.toCharArray(), salt, 100_000, 256)
        return SecretKeySpec(
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded,
            "AES"
        )
    }

    private fun chunkedEncrypt(zipBytes: ByteArray, password: String): ByteArray {
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val ivPrefix = ByteArray(8).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(password, salt)
        val out = ByteArrayOutputStream()
        out.write("NCPS".toByteArray(Charsets.US_ASCII))
        out.write(salt)
        out.write(ivPrefix)
        val segmentSize = 64
        var segmentIndex = 0
        var offset = 0
        while (offset < zipBytes.size) {
            val n = minOf(segmentSize, zipBytes.size - offset)
            val iv = ByteArray(12)
            System.arraycopy(ivPrefix, 0, iv, 0, 8)
            iv[8] = (segmentIndex ushr 24).toByte()
            iv[9] = (segmentIndex ushr 16).toByte()
            iv[10] = (segmentIndex ushr 8).toByte()
            iv[11] = segmentIndex.toByte()
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
            val ct = cipher.doFinal(zipBytes, offset, n)
            out.write(byteArrayOf(
                (ct.size ushr 24).toByte(),
                (ct.size ushr 16).toByte(),
                (ct.size ushr 8).toByte(),
                ct.size.toByte()
            ))
            out.write(ct)
            offset += n
            segmentIndex++
        }
        return out.toByteArray()
    }

    private fun sampleZip(): ByteArray {
        val zipBytes = ByteArrayOutputStream()
        ZipOutputStream(zipBytes).use { zos ->
            zos.putNextEntry(ZipEntry("code.xml"))
            zos.write("hello-neocatroid".toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        return zipBytes.toByteArray()
    }

    private fun wrapContainer(payload: ByteArray, password: String): ByteArray {
        val container = ByteArrayOutputStream()
        container.write("NCPW".toByteArray(Charsets.US_ASCII))
        val pwd = password.toByteArray(Charsets.UTF_8)
        container.write(byteArrayOf(
            (pwd.size ushr 24).toByte(),
            (pwd.size ushr 16).toByte(),
            (pwd.size ushr 8).toByte(),
            pwd.size.toByte()
        ))
        container.write(pwd)
        container.write(payload)
        return container.toByteArray()
    }

    private fun extractedContent(dir: File): String {
        val codeXml = File(dir, "code.xml")
        assertTrue("code.xml must be extracted", codeXml.exists())
        return codeXml.readText()
    }

    @Test
    fun ncpsInsideNcpwDecryptsWithEmbeddedPassword() {
        val password = "0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f0f"
        val payload = chunkedEncrypt(sampleZip(), password)
        val container = wrapContainer(payload, password)

        val result = DesktopStage.extractProjectFromStream(ByteArrayInputStream(container))
        assertNotNull("NCPW container must extract", result)
        assertEquals("hello-neocatroid", extractedContent(result!!))
    }

    @Test
    fun wrongPasswordInsideNcpwFails() {
        val payload = chunkedEncrypt(sampleZip(), "11111111111111111111111111111111")
        val container = wrapContainer(payload, "22222222222222222222222222222222")

        val result = DesktopStage.extractProjectFromStream(ByteArrayInputStream(container))
        assertNull("GCM tag must reject the wrong password", result)
    }

    @Test
    fun malformedContainerRejected() {
        val container = ByteArrayOutputStream()
        container.write("NCPW".toByteArray(Charsets.US_ASCII))
        container.write(byteArrayOf(0, 0, 0, 0))
        container.write("x".toByteArray())

        val result = DesktopStage.extractProjectFromStream(ByteArrayInputStream(container.toByteArray()))
        assertNull("zero-length password must be rejected", result)
    }

    @Test
    fun legacyRawNcpsStillExtracts() {
        val payload = chunkedEncrypt(sampleZip(), DesktopStage.PAYLOAD_PASSWORD)
        val result = DesktopStage.extractProjectFromStream(ByteArrayInputStream(payload))
        assertNotNull("plain NCPS must keep working", result)
        assertEquals("hello-neocatroid", extractedContent(result!!))
    }

    @Test
    fun plainZipWithoutMagicStillExtracts() {
        val result = DesktopStage.extractProjectFromStream(ByteArrayInputStream(sampleZip()))
        assertNotNull("unencrypted zip must keep working", result)
        assertEquals("hello-neocatroid", extractedContent(result!!))
    }

    @Test
    fun emptyStreamReturnsNull() {
        assertNull(DesktopStage.extractProjectFromStream(ByteArrayInputStream(ByteArray(0))))
    }
}
