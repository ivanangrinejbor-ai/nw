package org.catrobat.catroid.test.apkbuild

import org.catrobat.catroid.io.ProjectCrypto
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ProjectCryptoPasswordTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val hex32 = Regex("^[0-9a-f]{32}$")

    private fun sampleZip(): ByteArray {
        val zipBytes = ByteArrayOutputStream()
        ZipOutputStream(zipBytes).use { zos ->
            zos.putNextEntry(ZipEntry("code.xml"))
            zos.write("<program/>".toByteArray(StandardCharsets.UTF_8))
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("images/img.png"))
            zos.write(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8))
            zos.closeEntry()
        }
        return zipBytes.toByteArray()
    }

    @Test
    fun generatedPasswordIs32HexChars() {
        repeat(50) {
            val pwd = ProjectCrypto.generateRandomPassword()
            assertEquals("password must be 32 hex chars", 32, pwd.length)
            assertTrue("password must be hex: $pwd", hex32.matches(pwd))
        }
    }

    @Test
    fun generatedPasswordsAreUniquePerCall() {
        val seen = mutableSetOf<String>()
        repeat(50) {
            val pwd = ProjectCrypto.generateRandomPassword()
            assertTrue("passwords must be unique (duplicate: $pwd)", seen.add(pwd))
        }
    }

    private fun encryptLikeBakedApk(source: File, dest: File, password: String) {
        ProjectCrypto.encrypt(source, dest, password, locked = true)
    }

    @Test
    fun bakedPayloadRoundTripWithRandomPassword() {
        val plainZip = tempFolder.newFile("project.zip")
        plainZip.writeBytes(sampleZip())

        val password = ProjectCrypto.generateRandomPassword()
        val encrypted = File(tempFolder.root, "neocatroid.dat")
        encryptLikeBakedApk(plainZip, encrypted, password)

        assertTrue("payload must be detected as encrypted", ProjectCrypto.isEncrypted(encrypted))
        assertTrue("baked payload must be locked (NCPX)", ProjectCrypto.isLocked(encrypted))

        val decryptedZip = File(tempFolder.root, "decrypted.zip")
        assertTrue("decrypt with the same random password must succeed", ProjectCrypto.decrypt(encrypted, decryptedZip, password))
        assertArrayEquals("decrypted bytes must equal original project zip", plainZip.readBytes(), decryptedZip.readBytes())
    }

    @Test
    fun keyAssetFileContentDecryptsPayload() {
        val plainZip = tempFolder.newFile("project.zip")
        plainZip.writeBytes(sampleZip())

        val password = ProjectCrypto.generateRandomPassword()
        val encrypted = File(tempFolder.root, "neocatroid.dat")
        encryptLikeBakedApk(plainZip, encrypted, password)

        // RuntimeLoaderActivity: assets/neocatroid.key contains the password as plain text.
        val keyFile = File(tempFolder.root, "neocatroid.key")
        keyFile.writeText(password)
        val passwordFromKey = keyFile.readText().trim()

        val decryptedZip = File(tempFolder.root, "decrypted.zip")
        assertTrue(ProjectCrypto.decrypt(encrypted, decryptedZip, passwordFromKey))
        assertArrayEquals(plainZip.readBytes(), decryptedZip.readBytes())
    }

    @Test
    fun wrongPasswordCannotDecryptBakedPayload() {
        val plainZip = tempFolder.newFile("project.zip")
        plainZip.writeBytes(sampleZip())

        val password = ProjectCrypto.generateRandomPassword()
        val encrypted = File(tempFolder.root, "neocatroid.dat")
        encryptLikeBakedApk(plainZip, encrypted, password)

        val wrongKey = File(tempFolder.root, "neocatroid.key")
        wrongKey.writeText(ProjectCrypto.generateRandomPassword())

        val decryptedZip = File(tempFolder.root, "decrypted.zip")
        assertFalse(
            "decrypt with a different key asset content must fail (GCM tag mismatch)",
            ProjectCrypto.decrypt(encrypted, decryptedZip, wrongKey.readText().trim())
        )
        assertFalse("no partial output may remain after failed decrypt", decryptedZip.exists())
    }

    @Test
    fun plainZipIsNotRecognizedAsEncrypted() {
        val plain = tempFolder.newFile("plain.zip")
        plain.writeBytes(sampleZip())

        assertFalse(ProjectCrypto.isEncrypted(plain))
        assertFalse(ProjectCrypto.isLocked(plain))
        val out = File(tempFolder.root, "out.zip")
        assertFalse("decrypt of a plain zip must return false", ProjectCrypto.decrypt(plain, out, "whatever"))
    }

    @Test
    fun ncpwContainerHeaderRoundTrip() {
        val password = ProjectCrypto.generateRandomPassword()
        val out = ByteArrayOutputStream()
        ProjectCrypto.writePasswordContainerHeader(out, password)
        val bytes = out.toByteArray()

        val input = ByteArrayInputStream(bytes)
        val magic = ByteArray(4)
        assertEquals(4, input.read(magic))
        assertEquals("NCPW", String(magic, StandardCharsets.US_ASCII))

        val lenBytes = ByteArray(4)
        assertEquals(4, input.read(lenBytes))
        val len = ((lenBytes[0].toInt() and 0xFF) shl 24) or
            ((lenBytes[1].toInt() and 0xFF) shl 16) or
            ((lenBytes[2].toInt() and 0xFF) shl 8) or
            (lenBytes[3].toInt() and 0xFF)
        assertEquals("password length must match", password.toByteArray(StandardCharsets.UTF_8).size, len)

        val pwdBytes = ByteArray(len)
        assertEquals(len, input.read(pwdBytes))
        assertEquals("password must survive the container", password, String(pwdBytes, StandardCharsets.UTF_8))
        assertEquals("container must be exactly magic+len+password", 4 + 4 + len, bytes.size)
    }

    @Test
    fun ncpwHeaderThenChunkedEncryptionProducesDecryptablePayload() {
        // Full EXE-style stream: NCPW header + NCPS chunked directory encryption.
        val projectDir = tempFolder.newFolder("project")
        File(projectDir, "code.xml").writeText("<program/>")
        File(projectDir, "images").mkdirs()
        File(projectDir, "images/img.png").writeBytes(byteArrayOf(9, 8, 7, 6))

        val password = ProjectCrypto.generateRandomPassword()
        val payload = ByteArrayOutputStream()
        ProjectCrypto.writePasswordContainerHeader(payload, password)
        ProjectCrypto.encryptDirectoryToStreamChunked(projectDir, payload, password, segmentSize = 64)

        // DesktopStage.readPasswordContainer parses the same layout.
        val input = ByteArrayInputStream(payload.toByteArray())
        val magic = ByteArray(4)
        assertEquals(4, input.read(magic))
        assertEquals("NCPW", String(magic, StandardCharsets.US_ASCII))
        val lenBytes = ByteArray(4)
        assertEquals(4, input.read(lenBytes))
        val len = ((lenBytes[0].toInt() and 0xFF) shl 24) or
            ((lenBytes[1].toInt() and 0xFF) shl 16) or
            ((lenBytes[2].toInt() and 0xFF) shl 8) or
            (lenBytes[3].toInt() and 0xFF)
        val pwdBytes = ByteArray(len)
        assertEquals(len, input.read(pwdBytes))
        assertEquals(password, String(pwdBytes, StandardCharsets.UTF_8))

        val innerMagic = ByteArray(4)
        assertEquals(4, input.read(innerMagic))
        assertEquals("inner payload must be NCPS", "NCPS", String(innerMagic, StandardCharsets.US_ASCII))
        assertEquals("payload must contain encrypted data after the header", true, input.available() > 32)
    }

    @Test
    fun protectedContainerRoundTrip() {
        val plainZip = tempFolder.newFile("project.zip")
        plainZip.writeBytes(sampleZip())

        val password = ProjectCrypto.generateRandomPassword()
        val ncpp = File(tempFolder.root, "inner.ncpp")
        ProjectCrypto.encrypt(plainZip, ncpp, password)
        val container = File(tempFolder.root, "project.neotrobat")
        ProjectCrypto.wrapPasswordContainerFile(ncpp, container, password)

        assertTrue("container must be detected", ProjectCrypto.isPasswordContainer(container))
        assertEquals("embedded password must be readable", password, ProjectCrypto.readPasswordContainer(container))
        assertTrue("inner must still be encrypted (NCPP)", ProjectCrypto.isEncrypted(ncpp))

        val decrypted = File(tempFolder.root, "decrypted.zip")
        assertTrue("decrypting the container must succeed", ProjectCrypto.decryptPasswordContainer(container, decrypted))
        assertArrayEquals("decrypted bytes must equal original zip", plainZip.readBytes(), decrypted.readBytes())
    }

    @Test
    fun plainZipIsNotAProtectedContainer() {
        val plain = tempFolder.newFile("plain.zip")
        plain.writeBytes(sampleZip())
        assertFalse(ProjectCrypto.isPasswordContainer(plain))
        assertEquals(null, ProjectCrypto.readPasswordContainer(plain))
    }

    @Test
    fun truncatedContainerIsRejected() {
        val plainZip = tempFolder.newFile("project.zip")
        plainZip.writeBytes(sampleZip())
        val password = ProjectCrypto.generateRandomPassword()
        val ncpp = File(tempFolder.root, "inner.ncpp")
        ProjectCrypto.encrypt(plainZip, ncpp, password)
        val container = File(tempFolder.root, "project.neotrobat")
        ProjectCrypto.wrapPasswordContainerFile(ncpp, container, password)

        val truncated = File(tempFolder.root, "truncated.neotrobat")
        truncated.writeBytes(container.readBytes().copyOf(6))
        assertFalse("too-short file must not be a container", ProjectCrypto.isPasswordContainer(truncated))
        assertEquals(null, ProjectCrypto.readPasswordContainer(truncated))
        assertFalse("decrypt must fail on truncated container", ProjectCrypto.decryptPasswordContainer(truncated, File(tempFolder.root, "out.zip")))
    }

    @Test
    fun corruptedContainerFailsDecryption() {
        val plainZip = tempFolder.newFile("project.zip")
        plainZip.writeBytes(sampleZip())
        val password = ProjectCrypto.generateRandomPassword()
        val ncpp = File(tempFolder.root, "inner.ncpp")
        ProjectCrypto.encrypt(plainZip, ncpp, password)
        val container = File(tempFolder.root, "project.neotrobat")
        ProjectCrypto.wrapPasswordContainerFile(ncpp, container, password)

        val corrupted = File(tempFolder.root, "corrupted.neotrobat")
        val bytes = container.readBytes()
        bytes[bytes.size - 1] = (bytes.last().toInt() xor 0xFF).toByte()
        corrupted.writeBytes(bytes)
        assertFalse("GCM tag must reject corrupted content", ProjectCrypto.decryptPasswordContainer(corrupted, File(tempFolder.root, "out.zip")))
    }
}
