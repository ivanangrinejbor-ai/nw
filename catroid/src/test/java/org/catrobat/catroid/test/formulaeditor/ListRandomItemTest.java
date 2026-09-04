package org.catrobat.catroid.test.formulaeditor;

import com.badlogic.gdx.scenes.scene2d.actions.SequenceAction;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.content.Project;
import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.FormulaElement;
import org.catrobat.catroid.formulaeditor.Functions;
import org.catrobat.catroid.formulaeditor.UserList;
import org.catrobat.catroid.test.MockUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(JUnit4.class)
public class ListRandomItemTest {

    private static final String LIST_NAME = "cards";

    private Project project;
    private Scope scope;

    @Before
    public void setUp() {
        project = new Project(MockUtil.mockContextForProject(), "testProject");
        Sprite sprite = new Sprite("sprite");
        project.getDefaultScene().addSprite(sprite);
        ProjectManager.getInstance().setCurrentProject(project);
        ProjectManager.getInstance().setCurrentSprite(sprite);
        scope = new Scope(project, sprite, new SequenceAction());
    }

    private Formula randomItemFormula() {
        FormulaElement listRef = new FormulaElement(FormulaElement.ElementType.USER_LIST, LIST_NAME, null);
        FormulaElement func = new FormulaElement(FormulaElement.ElementType.FUNCTION,
                Functions.LIST_RANDOM_ITEM.name(), null);
        func.setLeftChild(listRef);
        return new Formula(func);
    }

    @Test
    public void testRandomItemComesFromList() throws Exception {
        List<Object> values = new ArrayList<>(Arrays.asList(1.0, "two", 3.0));
        project.addUserList(new UserList(LIST_NAME, values));
        Formula formula = randomItemFormula();

        for (int i = 0; i < 50; i++) {
            Object result = formula.interpretObject(scope);
            assertTrue(values.contains(result));
        }
    }

    @Test
    public void testRandomItemEmptyListReturnsEmptyString() throws Exception {
        project.addUserList(new UserList(LIST_NAME));

        assertEquals("", randomItemFormula().interpretObject(scope));
    }

    @Test
    public void testRandomItemMissingListReturnsEmptyString() throws Exception {
        assertEquals("", randomItemFormula().interpretObject(scope));
    }

    @Test
    public void testRandomItemEventuallyHitsEveryElement() throws Exception {
        List<Object> values = new ArrayList<>(Arrays.asList("a", "b", "c"));
        project.addUserList(new UserList(LIST_NAME, values));
        Formula formula = randomItemFormula();

        HashSet<Object> seen = new HashSet<>();
        for (int i = 0; i < 200 && seen.size() < 3; i++) {
            seen.add(formula.interpretObject(scope));
        }
        assertEquals(new HashSet<>(values), seen);
    }
}
