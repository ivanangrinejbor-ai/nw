package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenHttpResponseReceivedScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;
import java.util.List;

public class WhenHttpResponseReceivedBrick extends FormulaBrick implements ScriptBrick {
    private static final long serialVersionUID = 1L;
    private WhenHttpResponseReceivedScript script;

    public WhenHttpResponseReceivedBrick() { this(new WhenHttpResponseReceivedScript()); }
    public WhenHttpResponseReceivedBrick(WhenHttpResponseReceivedScript script) {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_when_http_response_request_id);
        script.setScriptBrick(this);
        this.script = script;
        this.formulaMap = script.getFormulaMap();
    }
    @Override public Brick clone() throws CloneNotSupportedException {
        WhenHttpResponseReceivedBrick clone = (WhenHttpResponseReceivedBrick) super.clone();
        clone.script = (WhenHttpResponseReceivedScript) script.clone();
        clone.script.setScriptBrick(clone);
        clone.formulaMap = clone.script.getFormulaMap();
        return clone;
    }
    @Override public int getViewResource() { return R.layout.brick_when_http_response_received; }
    @Override public Script getScript() { return script; }
    @Override public int getPositionInScript() { return -1; }
    @Override public void addToFlatList(List<Brick> bricks) { super.addToFlatList(bricks); for (Brick brick : script.getBrickList()) brick.addToFlatList(bricks); }
    @Override public List<Brick> getDragAndDropTargetList() { return script.getBrickList(); }
    @Override public int getPositionInDragAndDropTargetList() { return -1; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) { }
}
