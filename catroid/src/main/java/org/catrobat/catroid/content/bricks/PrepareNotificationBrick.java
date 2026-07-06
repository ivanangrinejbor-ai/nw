package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

import android.app.NotificationManager;
import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Spinner;

import org.catrobat.catroid.content.AdapterViewOnItemSelectedListenerImpl;
import kotlin.Unit;

public class PrepareNotificationBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    private int importanceLevel = NotificationManager.IMPORTANCE_DEFAULT;
    private boolean isPinned = false;

    private static final int IMPORTANCE_MIN = 0;
    private static final int IMPORTANCE_LOW = 1;
    private static final int IMPORTANCE_DEFAULT = 2;
    private static final int IMPORTANCE_HIGH = 3;
    private static final int IMPORTANCE_MAX = 4;

    public PrepareNotificationBrick() {
        addAllowedBrickField(BrickField.NOTIFICATION_ID, R.id.brick_prepare_notification_edit_id);
        addAllowedBrickField(BrickField.NOTIFICATION_CHANNEL, R.id.brick_prepare_notification_edit_channel);
        addAllowedBrickField(BrickField.NOTIFICATION_TITLE, R.id.brick_prepare_notification_edit_title);
        addAllowedBrickField(BrickField.NOTIFICATION_TEXT, R.id.brick_prepare_notification_edit_text);
        addAllowedBrickField(BrickField.NOTIFICATION_ICON, R.id.brick_prepare_notification_edit_icon);
    }

    public PrepareNotificationBrick(Formula id, Formula channel, Formula title, Formula text, Formula icon) {
        this();
        setFormulaWithBrickField(BrickField.NOTIFICATION_ID, id);
        setFormulaWithBrickField(BrickField.NOTIFICATION_CHANNEL, channel);
        setFormulaWithBrickField(BrickField.NOTIFICATION_TITLE, title);
        setFormulaWithBrickField(BrickField.NOTIFICATION_TEXT, text);
        setFormulaWithBrickField(BrickField.NOTIFICATION_ICON, icon);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_prepare_notification;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);

        Spinner importanceSpinner = view.findViewById(R.id.brick_prepare_notification_importance_spinner);
        String[] importanceValues = new String[5];
        importanceValues[IMPORTANCE_MIN] = context.getString(R.string.notification_importance_min);
        importanceValues[IMPORTANCE_LOW] = context.getString(R.string.notification_importance_low);
        importanceValues[IMPORTANCE_DEFAULT] = context.getString(R.string.notification_importance_default);
        importanceValues[IMPORTANCE_HIGH] = context.getString(R.string.notification_importance_high);
        importanceValues[IMPORTANCE_MAX] = context.getString(R.string.notification_importance_max);
        ArrayAdapter<String> importanceAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, importanceValues);
        importanceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        importanceSpinner.setAdapter(importanceAdapter);
        importanceSpinner.setSelection(importanceLevel);
        importanceSpinner.setOnItemSelectedListener(new AdapterViewOnItemSelectedListenerImpl(position -> {
            importanceLevel = position;
            return Unit.INSTANCE;
        }));

        CheckBox pinnedCheckbox = view.findViewById(R.id.brick_prepare_notification_pinned);
        pinnedCheckbox.setChecked(isPinned);
        pinnedCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            isPinned = isChecked;
        });

        return view;
    }

    @Override
    public Brick clone() throws CloneNotSupportedException {
        PrepareNotificationBrick clone = (PrepareNotificationBrick) super.clone();
        return clone;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createPrepareNotificationAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.NOTIFICATION_ID),
                        getFormulaWithBrickField(BrickField.NOTIFICATION_CHANNEL),
                        getFormulaWithBrickField(BrickField.NOTIFICATION_TITLE),
                        getFormulaWithBrickField(BrickField.NOTIFICATION_TEXT),
                        getFormulaWithBrickField(BrickField.NOTIFICATION_ICON),
                        importanceLevel, isPinned));
    }
}
