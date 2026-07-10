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

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;

import org.catrobat.catroid.content.Scope;
import org.catrobat.catroid.physics.PhysicsLook;
import org.catrobat.catroid.utils.TouchUtil;

public class TouchDirectionAction extends TemporalAction {

    private Scope scope;

    @Override
    protected void update(float percent) {
        if (scope == null || scope.getSprite() == null || scope.getSprite().look == null) return;
        if (TouchUtil.getNumberOfCurrentTouches() < 1) {
            return;
        }
        float spriteX = scope.getSprite().look.getXInUserInterfaceDimensionUnit();
        float spriteY = scope.getSprite().look.getYInUserInterfaceDimensionUnit();
        float touchX = TouchUtil.getX(0);
        float touchY = TouchUtil.getY(0);

        double rotationDegrees;
        if (spriteX == touchX && spriteY == touchY) {
            rotationDegrees = 90;
        } else if (spriteX == touchX) {
            rotationDegrees = spriteY < touchY ? 0 : 180;
        } else if (spriteY == touchY) {
            rotationDegrees = spriteX < touchX ? 90 : -90;
        } else {
            rotationDegrees = 90f - Math.toDegrees(Math.atan2(touchY - spriteY, touchX - spriteX));
        }
        if (scope.getSprite().look instanceof PhysicsLook) {
            ((PhysicsLook) scope.getSprite().look).setFlippedByDirection((float) rotationDegrees);
        }
        scope.getSprite().look.setMotionDirectionInUserInterfaceDimensionUnit((float) rotationDegrees);
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }
}
