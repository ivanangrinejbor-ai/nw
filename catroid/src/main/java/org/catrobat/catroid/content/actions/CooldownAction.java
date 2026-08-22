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

import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;

import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.formulaeditor.Formula;
import org.catrobat.catroid.formulaeditor.InterpretationException;

public class CooldownAction extends Action {
    private Scope scope;
    private Formula cooldownFormula;
    private Action innerAction;
    private boolean initialized;
    private boolean shouldExecute;
    private long lastExecutionTime = 0L;

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public void setCooldown(Formula cooldown) {
        this.cooldownFormula = cooldown;
    }

    public void setInnerAction(Action innerAction) {
        this.innerAction = innerAction;
    }

    @Override
    public boolean act(float delta) {
        if (!initialized) {
            initialized = true;
            if (scope == null || cooldownFormula == null || innerAction == null) {
                return true;
            }
            try {
                double cooldownSec = cooldownFormula.interpretDouble(scope);
                long cooldownMs = (long) (cooldownSec * 1000.0);
                long now = System.currentTimeMillis();
                if (now - lastExecutionTime >= cooldownMs) {
                    lastExecutionTime = now;
                    shouldExecute = true;
                } else {
                    shouldExecute = false;
                    return true;
                }
            } catch (InterpretationException e) {
                return true;
            }
        }
        if (shouldExecute && innerAction != null) {
            return innerAction.act(delta);
        }
        return true;
    }

    @Override
    public void restart() {
        super.restart();
        initialized = false;
        shouldExecute = false;
        if (innerAction != null) {
            innerAction.restart();
        }
    }

    @Override
    public void setActor(Actor actor) {
        super.setActor(actor);
        if (innerAction != null) {
            innerAction.setActor(actor);
        }
    }
}
