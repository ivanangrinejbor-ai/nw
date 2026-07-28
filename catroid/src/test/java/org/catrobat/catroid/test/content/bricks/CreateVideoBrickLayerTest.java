package org.catrobat.catroid.test.content.bricks;

import org.catrobat.catroid.content.bricks.CreateVideoBrick;
import org.catrobat.catroid.content.bricks.Brick;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(Sprite.class)
public class CreateVideoBrickLayerTest {

    @Test
    public void testLayerFieldRegistered() throws org.catrobat.catroid.formulaeditor.InterpretationException {
        CreateVideoBrick brick = new CreateVideoBrick();
        Formula layerFormula = brick.getFormulaWithBrickField(Brick.BrickField.VIDEO_LAYER);
        assertNotNull("VIDEO_LAYER field should be registered", layerFormula);
    }

    @Test
    public void testLayerDefaultValueIs2() throws org.catrobat.catroid.formulaeditor.InterpretationException {
        CreateVideoBrick brick = new CreateVideoBrick();
        Formula layerFormula = brick.getFormulaWithBrickField(Brick.BrickField.VIDEO_LAYER);
        assertEquals("Default layer should be 2 (above all)", 2,
                layerFormula.interpretInteger(null).intValue());
    }

    @Test
    public void testLayerConstructorFromInt() throws org.catrobat.catroid.formulaeditor.InterpretationException {
        CreateVideoBrick brick = new CreateVideoBrick();
        brick.setFormulaWithBrickField(Brick.BrickField.VIDEO_LAYER, new Formula(1));
        Formula layerFormula = brick.getFormulaWithBrickField(Brick.BrickField.VIDEO_LAYER);
        assertEquals("Layer should be 1", 1,
                layerFormula.interpretInteger(null).intValue());
    }

    @Test
    public void testLayerConstructorFromZero() throws org.catrobat.catroid.formulaeditor.InterpretationException {
        CreateVideoBrick brick = new CreateVideoBrick();
        brick.setFormulaWithBrickField(Brick.BrickField.VIDEO_LAYER, new Formula(0));
        Formula layerFormula = brick.getFormulaWithBrickField(Brick.BrickField.VIDEO_LAYER);
        assertEquals("Layer should be 0", 0,
                layerFormula.interpretInteger(null).intValue());
    }

    @Test
    public void testLayerPreservedAfterDeserialization() throws org.catrobat.catroid.formulaeditor.InterpretationException {
        CreateVideoBrick brick = new CreateVideoBrick();
        brick.setFormulaWithBrickField(Brick.BrickField.VIDEO_LAYER, new Formula(1));

        Formula layer = brick.getFormulaWithBrickField(Brick.BrickField.VIDEO_LAYER);
        assertEquals("Layer should survive get/set round-trip", 1,
                layer.interpretInteger(null).intValue());
    }

    @Test
    public void testAllFieldsPresent() throws org.catrobat.catroid.formulaeditor.InterpretationException {
        CreateVideoBrick brick = new CreateVideoBrick();

        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.NAME));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.FILE));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.POSX));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.POSY));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.WIDTH));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.HEIGHT));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.LOOPED));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.CONTROLS));
        assertNotNull(brick.getFormulaWithBrickField(Brick.BrickField.VIDEO_LAYER));
    }

    @Test
    public void testAddActionToSequenceIncludesLayer() throws org.catrobat.catroid.formulaeditor.InterpretationException {
        Sprite sprite = mock(Sprite.class);
        ScriptSequenceAction sequence = mock(ScriptSequenceAction.class);
        org.catrobat.catroid.content.ActionFactory factory =
                mock(org.catrobat.catroid.content.ActionFactory.class);
        when(sprite.getActionFactory()).thenReturn(factory);

        CreateVideoBrick brick = new CreateVideoBrick();
        brick.setFormulaWithBrickField(Brick.BrickField.VIDEO_LAYER, new Formula(0));
        brick.addActionToSequence(sprite, sequence);

        verify(factory).videoAction(
                eq(sprite), eq(sequence),
                any(Formula.class), any(Formula.class), any(Formula.class), any(Formula.class),
                any(Formula.class), any(Formula.class), any(Formula.class), any(Formula.class),
                any(Formula.class));
    }
}
