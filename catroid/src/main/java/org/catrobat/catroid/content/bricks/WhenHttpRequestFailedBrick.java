package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.WhenHttpRequestFailedScript;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import java.util.List;

public class WhenHttpRequestFailedBrick extends FormulaBrick implements ScriptBrick {
    private static final long serialVersionUID = 1L;
    private WhenHttpRequestFailedScript script;

    public WhenHttpRequestFailedBrick() { this(new WhenHttpRequestFailedScript()); }
    public WhenHttpRequestFailedBrick(WhenHttpRequestFailedScript script) {
        addAllowedBrickField(BrickField.VALUE_1, R.id.brick_when_http_failed_request_id);
        script.setScriptBrick(this);
        this.script = script;
        this.formulaMap = script.getFormulaMap();
    }
    @Override public Brick clone() throws CloneNotSupportedException {
        WhenHttpRequestFailedBrick clone = (WhenHttpRequestFailedBrick) super.clone();
        clone.script = (WhenHttpRequestFailedScript) script.clone();
        clone.script.setScriptBrick(clone);
        clone.formulaMap = clone.script.getFormulaMap();
        return clone;
    }
    @Override public int getViewResource() { return R.layout.brick_when_http_request_failed; }
    @Override public Script getScript() { return script; }
    @Override public int getPositionInScript() { return -1; }
    @Override public void addToFlatList(List<Brick> bricks) { super.addToFlatList(bricks); for (Brick brick : script.getBrickList()) brick.addToFlatList(bricks); }
    @Override public List<Brick> getDragAndDropTargetList() { return script.getBrickList(); }
    @Override public int getPositionInDragAndDropTargetList() { return -1; }
    @Override public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) { }
}
