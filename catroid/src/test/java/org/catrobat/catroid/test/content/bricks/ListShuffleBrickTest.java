package org.catrobat.catroid.test.content.bricks;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.ActionFactory;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scene;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.actions.ShuffleListAction;
import org.catrobat.catroid.content.bricks.ListShuffleBrick;
import org.catrobat.catroid.formulaeditor.UserList;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@RunWith(JUnit4.class)
public class ListShuffleBrickTest {

    private Sprite sprite;

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
    public void testListShuffleBrickCreatesAction() {
        ActionFactory actionFactory = Mockito.mock(ActionFactory.class);
        sprite.setActionFactory(actionFactory);
        ListShuffleBrick brick = new ListShuffleBrick();
        UserList userList = new UserList("cards");
        brick.setUserList(userList);

        brick.addActionToSequence(sprite, new ScriptSequenceAction(Mockito.mock(Script.class)));

        verify(actionFactory).createShuffleListAction(org.mockito.ArgumentMatchers.eq(userList));
    }

    @Test
    public void testShuffleKeepsAllItems() {
        UserList userList = new UserList("cards", new ArrayList<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0)));
        ShuffleListAction action = new ShuffleListAction();
        action.setUserList(userList);

        action.act(0f);

        List<Object> shuffled = userList.getValue();
        assertEquals(5, shuffled.size());
        assertEquals(new HashSet<>(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0)), new HashSet<>(shuffled));
    }

    @Test
    public void testShuffleEmptyListDoesNotCrash() {
        UserList userList = new UserList("empty");
        ShuffleListAction action = new ShuffleListAction();
        action.setUserList(userList);

        action.act(0f);

        assertTrue(userList.getValue().isEmpty());
    }

    @Test
    public void testShuffleNullListDoesNotCrash() {
        ShuffleListAction action = new ShuffleListAction();
        action.setUserList(null);

        action.act(0f);
    }
}
