package org.catrobat.catroid.ui.hitbox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
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
        ImageButton btnSave = findViewById(R.id.hitbox_btn_save);
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

        btnSave.setOnClickListener(v -> saveAndExit());
    }

    private void saveAndExit() {
        List<HitboxData> hitboxes = editorView.getHitboxes();
        currentLook.setHitboxes(hitboxes);

        // Save project to persist hitbox data
        try {
            XstreamSerializer.getInstance().saveProject(
                ProjectManager.getInstance().getCurrentProject()
            );
        } catch (Exception e) {
            // Best effort — hitboxes are in memory regardless
        }

        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Don't save on back — user must explicitly press save
        setResult(RESULT_CANCELED);
        super.onBackPressed();
    }
}
