/*
 * Catroid: An on-device visual programming system for Android devices
 * Copyright (C) 2010-2024 The Catrobat Team
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

public class RunVMBrick extends FormulaBrick {
    private static final long serialVersionUID = 1L;

    public RunVMBrick() {
        addAllowedBrickField(BrickField.VM_MEMORY, R.id.brick_memory_edit);
        addAllowedBrickField(BrickField.VM_CPU, R.id.brick_cpu_edit);
        addAllowedBrickField(BrickField.VM_HDA, R.id.brick_hda_edit);
        addAllowedBrickField(BrickField.VM_CDROM, R.id.brick_cdrom_edit);
    }

    public RunVMBrick(String memory, String cpu, String hda, String cdrom) {
        this(new Formula(memory), new Formula(cpu), new Formula(hda), new Formula(cdrom));
    }

    public RunVMBrick(Formula memory, Formula cpu, Formula hda, Formula cdrom) {
        this();
        setFormulaWithBrickField(BrickField.VM_MEMORY, memory);
        setFormulaWithBrickField(BrickField.VM_CPU, cpu);
        setFormulaWithBrickField(BrickField.VM_HDA, hda);
        setFormulaWithBrickField(BrickField.VM_CDROM, cdrom);
    }

    @Override
    public int getViewResource() {
        return R.layout.brick_run_vm;
    }

    @Override
    public void addActionToSequence(Sprite sprite, ScriptSequenceAction sequence) {
        sequence.addAction(sprite.getActionFactory().createRunVMAction(sprite, sequence,
                getFormulaWithBrickField(BrickField.VM_MEMORY),
                getFormulaWithBrickField(BrickField.VM_CPU),
                getFormulaWithBrickField(BrickField.VM_HDA),
                getFormulaWithBrickField(BrickField.VM_CDROM)
        ));
    }
}