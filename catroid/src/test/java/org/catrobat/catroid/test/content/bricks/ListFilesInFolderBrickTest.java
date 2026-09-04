package org.catrobat.catroid.test.content.bricks;

import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.ActionFactory;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ListFilesInFolderAction;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.ListFilesInFolderBrick;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.UserList;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.io.File;

@RunWith(JUnit4.class)
public class ListFilesInFolderBrickTest {

    private Sprite sprite;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        Project project = new Project(MockUtil.mockContextForProject(), "Project");
        Scene currentlyPlayingScene = new Scene("Currently playing scene", project);
        sprite = new Sprite("Sprite");
        currentlyPlayingScene.addSprite(sprite);
        project.addScene(currentlyPlayingScene);
        ProjectManager.getInstance().setCurrentProject(project);
        ProjectManager.getInstance().setCurrentlyEditedScene(new Scene());
        ProjectManager.getInstance().setCurrentlyPlayingScene(currentlyPlayingScene);
    }

    @Test
    public void testListFilesInFolderBrickCreatesActionWithCorrectSprite() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);
        ListFilesInFolderBrick brick = new ListFilesInFolderBrick("MyFolder");
        UserList userList = new UserList("files");
        brick.setUserList(userList);

        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createListFilesInFolderAction(eq(sprite),
                any(SequenceAction.class), any(Formula.class), eq(userList));
    }

    @Test
    public void testBrickStringConstructor() {
        ListFilesInFolderBrick brick = new ListFilesInFolderBrick("MyFolder");
        assertEquals("MyFolder", brick.getFormulaWithBrickField(Brick.BrickField.VALUE)
                .getRoot().getValue());
    }

    @Test
    public void testListFileNamesInListsFilesSorted() throws Exception {
        File dir = temporaryFolder.newFolder("MyFolder");
        new File(dir, "b.txt").createNewFile();
        new File(dir, "a.txt").createNewFile();
        new File(dir, "sub").mkdir();

        ListFilesInFolderAction action = new ListFilesInFolderAction();

        assertEquals(java.util.Arrays.asList("a.txt", "b.txt"), action.listFileNamesIn(dir));
    }

    @Test
    public void testListFileNamesInMissingFolderIsEmpty() throws Exception {
        File missing = new File(temporaryFolder.getRoot(), "nope");

        ListFilesInFolderAction action = new ListFilesInFolderAction();

        assertTrue(action.listFileNamesIn(missing).isEmpty());
    }

    @Test
    public void testUpdateWithNullListDoesNotCrash() throws Exception {
        ListFilesInFolderAction action = new ListFilesInFolderAction();
        action.setUserList(null);
        action.setFolder(new Formula("MyFolder"));

        action.act(0f);
    }
}
