/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 *
 * Licensed under the GNU Affero General Public License, version 3.
 */
package org.catrobat.catroid.test.content.actions;

import android.os.Environment;

import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;
import com.badlogic.gdx.utils.GdxNativesLoader;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Environment.class, GdxNativesLoader.class})
public class PutFileIntoPathActionTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private Sprite sprite;
    private Scope scope;
    private Project project;
    private File downloadsDir;

    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(GdxNativesLoader.class);
        PowerMockito.mockStatic(Environment.class);

        project = new Project(MockUtil.mockContextForProject(), "Project");
        Scene scene = new Scene("test", project);
        sprite = new Sprite("testSprite");
        scene.addSprite(sprite);
        project.addScene(scene);
        ProjectManager.getInstance().setCurrentProject(project);
        scope = new Scope(project, sprite, new SequenceAction());

        downloadsDir = tempFolder.newFolder("Downloads");
        when(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                .thenReturn(downloadsDir);
    }

    @Test
    public void testCopyFileToPath() throws Exception {
        File projectDir = project.getDirectory();
        File sourceFile = new File(projectDir, "test.txt");
        assertTrue(sourceFile.createNewFile());

        File destDir = tempFolder.newFolder("dest");
        String destPath = new File(destDir, "copied.txt").getAbsolutePath();

        sprite.getActionFactory()
                .createPutFileIntoPathAction(sprite, new SequenceAction(),
                        new Formula("test.txt"), new Formula(destPath))
                .act(1.0f);

        assertTrue("File should exist at destination path", new File(destPath).exists());
    }

    @Test
    public void testCopyFileIntoDirectoryPath() throws Exception {
        File projectDir = project.getDirectory();
        File sourceFile = new File(projectDir, "test.txt");
        assertTrue(sourceFile.createNewFile());

        File destDir = tempFolder.newFolder("destDir");

        sprite.getActionFactory()
                .createPutFileIntoPathAction(sprite, new SequenceAction(),
                        new Formula("test.txt"), new Formula(destDir.getAbsolutePath()))
                .act(1.0f);

        File fileInDir = new File(destDir, "test.txt");
        assertTrue("File should be copied into directory", fileInDir.exists());
    }

    @Test
    public void testPathTraversalIsPrevented() throws Exception {
        File projectDir = project.getDirectory();
        File sourceFile = new File(projectDir, "test.txt");
        assertTrue(sourceFile.createNewFile());

        String traversalPath = new File(downloadsDir.getParentFile(), "outside.txt").getAbsolutePath();

        sprite.getActionFactory()
                .createPutFileIntoPathAction(sprite, new SequenceAction(),
                        new Formula("test.txt"), new Formula(traversalPath))
                .act(1.0f);

        assertFalse("Path traversal should be prevented", new File(traversalPath).exists());
    }

    @Test
    public void testSourceFileNotFoundDoesNothing() {
        String destPath = new File(downloadsDir, "copied.txt").getAbsolutePath();

        sprite.getActionFactory()
                .createPutFileIntoPathAction(sprite, new SequenceAction(),
                        new Formula("nonexistent.txt"), new Formula(destPath))
                .act(1.0f);

        assertFalse("File should not exist", new File(destPath).exists());
    }

    @Test
    public void testNullSourceNameDoesNothing() {
        sprite.getActionFactory()
                .createPutFileIntoPathAction(sprite, new SequenceAction(),
                        null, new Formula("/some/path"))
                .act(1.0f);
    }

    @Test
    public void testBlankPathDoesNothing() throws Exception {
        File projectDir = project.getDirectory();
        File sourceFile = new File(projectDir, "test.txt");
        assertTrue(sourceFile.createNewFile());

        sprite.getActionFactory()
                .createPutFileIntoPathAction(sprite, new SequenceAction(),
                        new Formula("test.txt"), new Formula(""))
                .act(1.0f);

        File destFile = new File(downloadsDir, "test.txt");
        assertFalse("File should not be copied with blank path", destFile.exists());
    }
}
