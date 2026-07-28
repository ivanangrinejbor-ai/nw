package org.catrobat.catroid.content.bricks;

import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class LocalizeSpritesBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;
    private int languageSelection = 0;

    private static final String[] LANGUAGES = {
        "ru", "en", "de", "fr", "es", "it", "pt", "ja", "ko", "zh", "ar", "hi", "tr", "uk", "pl"
    };
    private static final String[] LANGUAGE_NAMES = {
        "Russian", "English", "German", "French", "Spanish", "Italian", "Portuguese",
        "Japanese", "Korean", "Chinese", "Arabic", "Hindi", "Turkish", "Ukrainian", "Polish"
    };

    public LocalizeSpritesBrick() {
        addAllowedBrickField(BrickField.STRING, R.id.brick_localize_edit);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_localize_sprites;
    }

    @Override
    public View getView(Context context) {
        super.getView(context);

        Spinner langSpinner = view.findViewById(R.id.brick_localize_lang_spinner);
        ArrayAdapter<String> langAdapter = new ArrayAdapter<>(context,
                R.layout.simple_spinner_item_white_text, LANGUAGE_NAMES);
        langAdapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item_white_text);
        langSpinner.setAdapter(langAdapter);
        langSpinner.setSelection(languageSelection);
        langSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { languageSelection = pos; }
            @Override public void onNothingSelected(AdapterView<?> p) { }
        });

        return view;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createLocalizeSpritesAction(sprite, sequence,
                new Formula(LANGUAGES[languageSelection]),
                getFormulaWithBrickField(BrickField.STRING)));
    }
}
