package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Script;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.UserDefinedScriptV2;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.ui.BrickLayout;

public class UserDefinedReceiverBrickV2 extends ScriptBrickBaseType {
    private static final long serialVersionUID = 2L;

    private UserDefinedScriptV2 userDefinedScriptV2;
    private UserDefinedBrickV2 userDefinedBrickV2;

    public UserDefinedReceiverBrickV2(UserDefinedBrickV2 userDefinedBrickV2) {
        this.userDefinedBrickV2 = userDefinedBrickV2;
        this.userDefinedScriptV2 = new UserDefinedScriptV2(userDefinedBrickV2.getUserDefinedBrickID(), userDefinedBrickV2.getBlockName(), userDefinedBrickV2.getParameterNames());
        this.userDefinedScriptV2.setScriptBrick(this);
    }

    public UserDefinedReceiverBrickV2(UserDefinedScriptV2 script) {
        this.userDefinedScriptV2 = script;
        this.userDefinedScriptV2.setScriptBrick(this);
    }

    public UserDefinedBrickV2 getUserDefinedBrickV2() {
        return userDefinedBrickV2;
    }

    @Override
    public Script getScript() {
        return userDefinedScriptV2;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        UserDefinedReceiverBrickV2 clone = (UserDefinedReceiverBrickV2) super.clone();
        clone.userDefinedScriptV2 = (UserDefinedScriptV2) userDefinedScriptV2.clone();
        clone.userDefinedScriptV2.setScriptBrick(clone);
        if (this.userDefinedBrickV2 != null) {
            clone.userDefinedBrickV2 = (UserDefinedBrickV2) this.userDefinedBrickV2.clone();
        }
        return clone;
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_user_defined_script;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);
        LinearLayout userBrickSpace = view.findViewById(R.id.user_brick_space);
        userBrickSpace.removeAllViews();

        if (userDefinedBrickV2 != null) {
            userBrickSpace.addView(userDefinedBrickV2.getView(context));
        } else if (userDefinedScriptV2 != null) {
            TextView tv = new TextView(new ContextThemeWrapper(context, R.style.BrickText));
            tv.setText("✦ Define V2: " + userDefinedScriptV2.getBlockName());
            userBrickSpace.addView(tv);
        }
        return view;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
    }
}
