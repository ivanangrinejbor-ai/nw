package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.content.notification.ActionBehavior;
import org.catrobat.catroid.formulaeditor.Formula;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;
import org.catrobat.catroid.content.AdapterViewOnItemSelectedListenerImpl;
import android.content.Context;
import kotlin.Unit;

public class NotificationActionBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;
    private int behaviorIndex = 0;
    private boolean hasInput = false;

    public NotificationActionBrick() {
        addAllowedBrickField(BrickField.NOTIFICATION_ID, R.id.brick_notification_action_edit_id);
        addAllowedBrickField(BrickField.NOTIFICATION_ACTION_ID, R.id.brick_notification_action_edit_action_id);
        addAllowedBrickField(BrickField.TEXT, R.id.brick_notification_action_edit_text);
        addAllowedBrickField(BrickField.NOTIFICATION_ACTION_ICON, R.id.brick_notification_action_edit_icon);
        addAllowedBrickField(BrickField.NOTIFICATION_ACTION_HINT, R.id.brick_notification_action_edit_hint);
    }

    public NotificationActionBrick(Formula id, Formula actionId, Formula text, Formula icon, Formula hint) {
        this();
        setFormulaWithBrickField(BrickField.NOTIFICATION_ID, id);
        setFormulaWithBrickField(BrickField.NOTIFICATION_ACTION_ID, actionId);
        setFormulaWithBrickField(BrickField.TEXT, text);
        setFormulaWithBrickField(BrickField.NOTIFICATION_ACTION_ICON, icon);
        setFormulaWithBrickField(BrickField.NOTIFICATION_ACTION_HINT, hint);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_notification_action;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);

        Spinner behaviorSpinner = view.findViewById(R.id.brick_notification_action_behavior_spinner);
        String[] behaviorValues = {
            context.getString(R.string.notification_action_launch_app),
            context.getString(R.string.notification_action_background),
            context.getString(R.string.notification_action_silent),
            context.getString(R.string.notification_action_input)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, behaviorValues);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        behaviorSpinner.setAdapter(adapter);
        behaviorSpinner.setSelection(behaviorIndex);
        behaviorSpinner.setOnItemSelectedListener(new AdapterViewOnItemSelectedListenerImpl(position -> {
            behaviorIndex = position;
            return Unit.INSTANCE;
        }));

        CheckBox inputCheckbox = view.findViewById(R.id.brick_notification_action_input_checkbox);
        inputCheckbox.setChecked(hasInput);
        inputCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> hasInput = isChecked);

        return view;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        NotificationActionBrick clone = (NotificationActionBrick) super.clone();
        return clone;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createNotificationActionAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NOTIFICATION_ID),
                        getFormulaWithBrickField(BrickField.NOTIFICATION_ACTION_ID),
                        getFormulaWithBrickField(BrickField.TEXT),
                        getFormulaWithBrickField(BrickField.NOTIFICATION_ACTION_ICON),
                        getFormulaWithBrickField(BrickField.NOTIFICATION_ACTION_HINT),
                        behaviorIndex, hasInput));
    }
}
