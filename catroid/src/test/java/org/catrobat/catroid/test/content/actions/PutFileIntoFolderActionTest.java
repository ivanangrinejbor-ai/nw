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
public class PutFileIntoFolderActionTest {

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
    public void testCopyFileToFolder() throws Exception {
        // Create source file in project
        File projectDir = project.getDirectory();
        File sourceFile = new File(projectDir, "test.txt");
        assertTrue(sourceFile.createNewFile());

        // Execute action
        sprite.getActionFactory()
                .createPutFileIntoFolderAction(sprite, new SequenceAction(),
                        new Formula("test.txt"), new Formula("myFolder"))
                .act(1.0f);

        // Verify file was copied
        File destFile = new File(new File(downloadsDir, "myFolder"), "test.txt");
        assertTrue("File should exist in destination folder", destFile.exists());
    }

    @Test
    public void testSourceFileNotFoundDoesNothing() {
        sprite.getActionFactory()
                .createPutFileIntoFolderAction(sprite, new SequenceAction(),
                        new Formula("nonexistent.txt"), new Formula("myFolder"))
                .act(1.0f);

        File destFile = new File(new File(downloadsDir, "myFolder"), "nonexistent.txt");
        assertFalse("File should not exist", destFile.exists());
    }

    @Test
    public void testPathTraversalIsPrevented() throws Exception {
        File projectDir = project.getDirectory();
        File sourceFile = new File(projectDir, "test.txt");
        assertTrue(sourceFile.createNewFile());

        // Attempt path traversal
        sprite.getActionFactory()
                .createPutFileIntoFolderAction(sprite, new SequenceAction(),
                        new Formula("test.txt"), new Formula("../outside"))
                .act(1.0f);

        // File should NOT be copied outside downloads dir
        File outsideFile = new File(downloadsDir.getParentFile(), "outside/test.txt");
        assertFalse("Path traversal should be prevented", outsideFile.exists());
    }

    @Test
    public void testNullSourceNameDoesNothing() {
        sprite.getActionFactory()
                .createPutFileIntoFolderAction(sprite, new SequenceAction(),
                        null, new Formula("folder"))
                .act(1.0f);
        // Should not throw
    }

    @Test
    public void testBlankFolderNameDoesNothing() throws Exception {
        File projectDir = project.getDirectory();
        File sourceFile = new File(projectDir, "test.txt");
        assertTrue(sourceFile.createNewFile());

        sprite.getActionFactory()
                .createPutFileIntoFolderAction(sprite, new SequenceAction(),
                        new Formula("test.txt"), new Formula(""))
                .act(1.0f);

        File destFile = new File(downloadsDir, "test.txt");
        assertFalse("File should not be copied with blank folder", destFile.exists());
    }
}
