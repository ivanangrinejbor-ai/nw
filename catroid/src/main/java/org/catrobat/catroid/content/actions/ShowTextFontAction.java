/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2022 The Catrobat Team
 * (<http://developer.catrobat.org/credits>)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * An additional term exception under section 7 of the GNU Affero
 * General Public License, version 3, is available at
 * http://developer.catrobat.org/license_additional_term
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.catrobat.catroid.content.actions;

import android.util.Log;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;
import com.badlogic.gdx.utils.Array;

import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;
import org.catrobat.catroid.formulaeditor.UserVariable;
import org.catrobat.catroid.stage.ShowTextActor;
import org.catrobat.catroid.stage.StageActivity;
import org.catrobat.catroid.utils.ShowTextUtils.AndroidStringProvider;

import java.io.File;

public class ShowTextFontAction extends TemporalAction {

    public static final String TAG = ShowTextFontAction.class.getSimpleName();

    private Formula xPosition;
    private Formula yPosition;
    private Formula relativeTextSize;
    private Formula color;
    private Formula text;

    private Formula file;
    private Formula name;
    private Scope scope;
    private int alignment;
    private ShowTextActor actor;

    private UserVariable variableToShow;

    AndroidStringProvider androidStringProvider;

    @Override
    protected void begin() {
        try {
            String namestr = (name.interpretString(scope) != null) ? name.interpretString(scope) : "dummyActor";
            String textstr = (text.interpretString(scope) != null) ? text.interpretString(scope) : "NaN";
            String file_font = (file != null && file.interpretString(scope) != null) ? file.interpretString(scope) : "font.ttf";
            File fontFile = (scope != null && scope.getProject() != null) ? scope.getProject().getFile(file_font) : null;
            String fontPath = (fontFile != null && fontFile.exists()) ? fontFile.getAbsolutePath() : null;

            variableToShow = new UserVariable(namestr, textstr);
            int xPosition = this.xPosition != null ? this.xPosition.interpretInteger(scope) : 0;
            int yPosition = this.yPosition != null ? this.yPosition.interpretInteger(scope) : 0;
            float relativeTextSize = this.relativeTextSize != null ? (this.relativeTextSize.interpretFloat(scope) / 100f) : 1f;
            String color = this.color != null ? this.color.interpretString(scope) : "#FFFFFF";

            var stageListener = StageActivity.getActiveStageListener();
            if (stageListener != null && stageListener.getStage() != null) {
                Array<Actor> stageActors = stageListener.getStage().getActors();
                for (Actor a : stageActors) {
                    if (a instanceof ShowTextActor) {
                        ShowTextActor showTextActor = (ShowTextActor) a;
                        if (showTextActor.getVariableNameToCompare().equals(variableToShow.getName())
                                && showTextActor.getSprite().equals(scope.getSprite())) {
                            a.remove();
                        }
                    }
                }
                actor = new ShowTextActor(true, variableToShow, xPosition, yPosition, relativeTextSize,
                        color, scope.getSprite(), alignment, androidStringProvider);
                if (fontPath != null) {
                    actor.setFont(fontPath);
                }
                actor.setWrap(true);
                stageListener.addActor(actor);
            }
            variableToShow.setVisible(true);
        } catch (Exception e) {
            Log.d(TAG, "Exception in ShowTextFontAction: " + e);
        }
    }

    @Override
    protected void update(float percent) {
        if (scope == null) return;
        try {
            int xPosition = this.xPosition.interpretInteger(scope);
            int yPosition = this.yPosition.interpretInteger(scope);

            if (actor != null) {
                actor.setPositionX(xPosition);
                actor.setPositionY(yPosition);
            }
        } catch (InterpretationException e) {
            Log.d(TAG, "InterpretationException");
        }
    }

    public void setPosition(Formula xPosition, Formula yPosition) {
        this.xPosition = xPosition;
        this.yPosition = yPosition;
    }

    public void setRelativeTextSize(Formula relativeTextSize) {
        this.relativeTextSize = relativeTextSize;
    }

    public void setColor(Formula color) {
        this.color = color;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public void setText(Formula text) {
        this.text = text;
    }

    public void setFile(Formula file) {
        this.file = file;
    }

    public void setName(Formula name) {
        this.name = name;
    }

    public void setAlignment(int alignment) {
        this.alignment = alignment;
    }

    public void setAndroidStringProvider(AndroidStringProvider androidStringProvider) {
        this.androidStringProvider = androidStringProvider;
    }
}
