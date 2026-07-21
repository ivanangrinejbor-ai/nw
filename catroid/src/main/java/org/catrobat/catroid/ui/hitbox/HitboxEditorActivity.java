package org.catrobat.catroid.ui.hitbox;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.TextView;

import org.catrobat.catroid.ProjectManager;
import org.catrobat.catroid.R;
import org.catrobat.catroid.common.HitboxData;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.io.XstreamSerializer;

import java.util.List;

/**
 * Activity for editing hitboxes of a specific LookData.
 * Launched from the Look list's 3-dot menu → "Hitbox Editor".
 */
public class HitboxEditorActivity extends Activity {

    public static final String EXTRA_LOOK_INDEX = "extra_look_index";
    public static final String EXTRA_SPRITE_NAME = "extra_sprite_name";

    private HitboxEditorView editorView;
    private TextView hintView;
    private ImageButton btnSave;
    private LookData currentLook;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hitbox_editor);

        editorView = findViewById(R.id.hitbox_canvas);
        hintView = findViewById(R.id.hitbox_hint);
        ImageButton btnBack = findViewById(R.id.hitbox_btn_back);
        ImageButton btnAdd = findViewById(R.id.hitbox_btn_add);
        ImageButton btnDelete = findViewById(R.id.hitbox_btn_delete);
        btnSave = findViewById(R.id.hitbox_btn_save);
        TextView title = findViewById(R.id.hitbox_title);

        // Resolve the LookData from intent extras
        int lookIndex = getIntent().getIntExtra(EXTRA_LOOK_INDEX, -1);
        var sprite = ProjectManager.getInstance().getCurrentSprite();
        if (sprite == null || lookIndex < 0 || lookIndex >= sprite.getLookList().size()) {
            finish();
            return;
        }
        currentLook = sprite.getLookList().get(lookIndex);
        title.setText(getString(R.string.hitbox_editor_title) + ": " + currentLook.getName());

        // Load sprite image
        if (currentLook.getFile() != null) {
            editorView.setSpriteImage(currentLook.getFile().getAbsolutePath());
        }

        // Load existing hitboxes
        editorView.setHitboxes(currentLook.getHitboxes());

        // Hint updates
        editorView.setOnHitboxChangeListener(() ->
            hintView.setText(R.string.hitbox_hint_modified)
        );

        // Toolbar actions
        btnBack.setOnClickListener(v -> finish());

        btnAdd.setOnClickListener(v -> {
            editorView.addHitbox();
            hintView.setText(R.string.hitbox_hint_added);
        });

        btnDelete.setOnClickListener(v -> {
            editorView.deleteSelected();
            hintView.setText(R.string.hitbox_hint_deleted);
        });

        btnSave.setOnClickListener(v -> showHitboxModeDialog());
    }

    private void showHitboxModeDialog() {
        View content = getLayoutInflater().inflate(R.layout.dialog_hitbox_mode, null);
        RadioButton fullOption = content.findViewById(R.id.hitbox_mode_full);
        RadioButton physicsOption = content.findViewById(R.id.hitbox_mode_physics);
        ImageButton infoFull = content.findViewById(R.id.hitbox_info_full);
        ImageButton infoPhysics = content.findViewById(R.id.hitbox_info_physics);

        if (currentLook.getHitboxMode() == LookData.HITBOX_MODE_FULL) {
            fullOption.setChecked(true);
        } else {
            physicsOption.setChecked(true);
        }

        infoFull.setOnClickListener(b -> showInfoDialog(
                R.string.hitbox_mode_info_full_title, R.string.hitbox_mode_info_full_text));
        infoPhysics.setOnClickListener(b -> showInfoDialog(
                R.string.hitbox_mode_info_physics_title, R.string.hitbox_mode_info_physics_text));

        new AlertDialog.Builder(this)
                .setTitle(R.string.hitbox_mode_dialog_title)
                .setView(content)
                .setPositiveButton(R.string.hitbox_save, (dialog, which) -> {
                    currentLook.setHitboxMode(fullOption.isChecked()
                            ? LookData.HITBOX_MODE_FULL : LookData.HITBOX_MODE_PHYSICS);
                    saveAndExit();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showInfoDialog(int titleRes, int textRes) {
        new AlertDialog.Builder(this)
                .setTitle(titleRes)
                .setMessage(textRes)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void saveAndExit() {
        // Update the in-memory model synchronously so the change is never lost,
        // even if the disk write below fails or the app is killed.
        final List<HitboxData> hitboxes = editorView.getHitboxes();
        currentLook.setHitboxes(hitboxes);

        // Persist OFF the UI thread. saveProject() serialises the ENTIRE project
        // to XML (hundreds of MB for large projects) and would ANR the UI otherwise.
        btnSave.setEnabled(false);
        hintView.setText(R.string.hitbox_hint_modified);
        new Thread(() -> {
            try {
                XstreamSerializer.getInstance().saveProject(
                    ProjectManager.getInstance().getCurrentProject());
            } catch (Exception ignored) {
                // Best effort — hitboxes are already in memory.
            }
            runOnUiThread(() -> {
                setResult(RESULT_OK);
                finish();
            });
        }, "hitbox-save").start();
    }

    @Override
    public void onBackPressed() {
        // Don't save on back — user must explicitly press save
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}
