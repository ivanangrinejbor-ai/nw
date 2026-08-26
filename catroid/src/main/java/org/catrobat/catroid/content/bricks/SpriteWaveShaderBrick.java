/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2026 The Catrobat Team
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
package org.catrobat.catroid.content.bricks;

import org.catrobat.catroid.R;
import org.catrobat.catroid.content.Sprite;
import org.catrobat.catroid.content.actions.ScriptSequenceAction;
import org.catrobat.catroid.formulaeditor.Formula;

public class SpriteWaveShaderBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public SpriteWaveShaderBrick() {
        addAllowedBrickField(BrickField.VALUE, R.id.brick_sprite_wave_amplitude);
        addAllowedBrickField(BrickField.TEXT, R.id.brick_sprite_wave_phase);
    }

    public SpriteWaveShaderBrick(double amplitude, double phase) {
        this(new Formula(amplitude), new Formula(phase));
    }

    public SpriteWaveShaderBrick(Formula amplitude, Formula phase) {
        this();
        setFormulaWithBrickField(BrickField.VALUE, amplitude);
        setFormulaWithBrickField(BrickField.TEXT, phase);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_sprite_wave_shader;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory()
                .createSpriteWaveShaderAction(sprite, sequence,
                        getFormulaWithBrickField(BrickField.VALUE),
                        getFormulaWithBrickField(BrickField.TEXT)));
    }
}
