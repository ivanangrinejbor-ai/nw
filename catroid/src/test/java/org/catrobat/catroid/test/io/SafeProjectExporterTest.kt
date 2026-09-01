package org.catrobat.catroid.test.io

import org.catrobat.catroid.content.Project
import org.catrobat.catroid.io.SafeProjectExporter
import org.catrobat.catroid.test.MockUtil
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipFile

class SafeProjectExporterTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun safeExportContainsBakedRuntimeWithoutEditableSource() {
        val project = Project(MockUtil.mockContextForProject(), "SafeExport")
        val projectDir = temporaryFolder.newFolder("project")
        project.directory = projectDir
        File(projectDir, "code.xml").writeText("<program/>")
        File(projectDir, "files").mkdirs()
        File(projectDir, "files/private.txt").writeText("private")
        val output = File(temporaryFolder.root, "safe.catrobat")

        SafeProjectExporter.export(MockUtil.mockContextForProject(), project, output)

        ZipFile(output).use { zip ->
            assertNotNull(zip.getEntry("init.bin"))
            assertNull(zip.getEntry("code.xml"))
            assertNotNull(zip.getEntry("files/private.txt"))
        }
    }
}
