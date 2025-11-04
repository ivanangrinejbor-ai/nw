package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SendVmInputBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SendVmInputBrick() {
        addAllowedBrickField(BrickField.INPUT_TEXT, R.id.brick_send_vm_input_text);
    }

    public SendVmInputBrick(String inputText) {
        this(new Formula(inputText));
    }

    public SendVmInputBrick(Formula inputText) {
        this();
        setFormulaWithBrickField(BrickField.INPUT_TEXT, inputText);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_send_vm_input;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSendVmInputAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.INPUT_TEXT)
                ));
    }
}