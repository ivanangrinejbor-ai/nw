package org.catrobat.catroid.content.actions;

import com.badlogic.gdx.scenes.scene2d.actions.TemporalAction;

import org.catrobat.catroid.common.LookColorProfile;
import org.catrobat.catroid.common.LookData;
import org.catrobat.catroid.content.Sprite;

public class CaptureLookColorProfileAction extends TemporalAction {
	private Sprite sprite;

	@Override
	protected void update(float percent) {
		if (sprite == null) {
			return;
		}
		LookData lookData = sprite.look.getLookData();
		if (lookData != null && lookData.getPixmap() != null) {
			sprite.setLookColorProfile(LookColorProfile.fromPixmap(lookData.getPixmap()));
		}
	}

	public void setSprite(Sprite sprite) {
		this.sprite = sprite;
	}
}
