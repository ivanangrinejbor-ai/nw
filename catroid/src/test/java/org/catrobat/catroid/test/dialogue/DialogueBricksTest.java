package org.catrobat.catroid.test.dialogue;

import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.bricks.CloseDialogueBrick;
import org.catrobat.catroid.content.bricks.JumpToNodeBrick;
import org.catrobat.catroid.content.bricks.StartDialogueBrick;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.StartScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Sprite.class)
public class DialogueBricksTest {

    @Test
    public void testStartDialogueBrickAddActionToSequence() {
        Sprite sprite = mock(Sprite.class);
        ScriptSequenceAction sequence = mock(ScriptSequenceAction.class);
        org.catrobat.catroid.content.ActionFactory factory =
                mock(org.catrobat.catroid.content.ActionFactory.class);
        when(sprite.getActionFactory()).thenReturn(factory);

        StartDialogueBrick brick = new StartDialogueBrick("test.json");
        brick.addActionToSequence(sprite, sequence);

        verify(factory).createStartDialogueAction(eq(sprite), eq(sequence), any(Formula.class));
    }

    @Test
    public void testCloseDialogueBrickAddActionToSequence() {
        Sprite sprite = mock(Sprite.class);
        ScriptSequenceAction sequence = mock(ScriptSequenceAction.class);
        org.catrobat.catroid.content.ActionFactory factory =
                mock(org.catrobat.catroid.content.ActionFactory.class);
        when(sprite.getActionFactory()).thenReturn(factory);

        CloseDialogueBrick brick = new CloseDialogueBrick();
        brick.addActionToSequence(sprite, sequence);

        verify(factory).createCloseDialogueAction(eq(sprite), eq(sequence));
    }

    @Test
    public void testJumpToNodeBrickAddActionToSequence() {
        Sprite sprite = mock(Sprite.class);
        ScriptSequenceAction sequence = mock(ScriptSequenceAction.class);
        org.catrobat.catroid.content.ActionFactory factory =
                mock(org.catrobat.catroid.content.ActionFactory.class);
        when(sprite.getActionFactory()).thenReturn(factory);

        JumpToNodeBrick brick = new JumpToNodeBrick("node_1");
        brick.addActionToSequence(sprite, sequence);

        verify(factory).createJumpToNodeAction(eq(sprite), eq(sequence), any(Formula.class));
    }

    @Test
    public void testStartDialogueBrickConstructorFromString() throws org.catrobat.catroid.formulaeditor.InterpretationException {
        StartDialogueBrick brick = new StartDialogueBrick("my/dialogue.json");
        Formula formula = brick.getFormulaWithBrickField(Brick.BrickField.DIALOGUE_FILE);
        assertEquals("my/dialogue.json", formula.interpretString(null));
    }

    @Test
    public void testJumpToNodeBrickConstructorFromString() throws org.catrobat.catroid.formulaeditor.InterpretationException {
        JumpToNodeBrick brick = new JumpToNodeBrick("hello_node");
        Formula formula = brick.getFormulaWithBrickField(Brick.BrickField.DIALOGUE_NODE_ID);
        assertEquals("hello_node", formula.interpretString(null));
    }

    @Test
    public void testBrickCategoryRegistration() {
        StartDialogueBrick brick1 = new StartDialogueBrick();
        CloseDialogueBrick brick2 = new CloseDialogueBrick();
        JumpToNodeBrick brick3 = new JumpToNodeBrick();

        assertNotNull(brick1.getViewResource());
        assertNotNull(brick2.getViewResource());
        assertNotNull(brick3.getViewResource());
    }
}
